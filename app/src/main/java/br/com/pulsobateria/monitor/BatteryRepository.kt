package br.com.pulsobateria.monitor

import android.content.*
import android.os.*
import br.com.pulsobateria.core.*
import br.com.pulsobateria.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

 data class Reading(
 val percent: Int? = null, val mode: Mode = Mode.UNKNOWN, val source: String = "Aguardando Android",
 val temperature: Double? = null, val current: Double? = null, val currentConflict: Boolean = false,
 val rate: Double? = null, val remaining: Long? = null, val systemEstimate: Boolean = false,
 val duration: Long = 0, val points: List<Point> = emptyList(), val updated: Long = 0,
 val error: String? = null
)
class BatteryRepository(
 private val context: Context, private val dao: SessionDao, settings: SettingsStore, private val scope: CoroutineScope
) {
 private val manager = context.getSystemService(BatteryManager::class.java)
 private val commands = Channel<String>(Channel.UNLIMITED)
 private val owners = mutableSetOf<String>()
 private val mutable = MutableStateFlow(Reading())
 val reading = mutable.asStateFlow()
 val sessions = dao.observe()
 val serviceActive = MutableStateFlow(false)
 val serviceError = MutableStateFlow<String?>(null)
 private val preferences = settings.flow.stateIn(scope, SharingStarted.Eagerly, Settings())
 private var ticker: Job? = null
 private var registered = false
 private var active: Session? = null
 private var startedElapsed = 0L
 private var lastElapsed = 0L
 private var lastWall = 0L
 private var lastMode = Mode.UNKNOWN
 private val points = mutableListOf<Point>()
 private val receiver = object: BroadcastReceiver() {
  override fun onReceive(c: Context?, intent: Intent?) { commands.trySend("read") }
 }
 init {
  scope.launch {
   try { dao.closeInterrupted() } catch (_: Exception) { mutable.value = mutable.value.copy(error = "Não foi possível recuperar o histórico.") }
   for (command in commands) {
    try {
     when {
      command.startsWith("+") -> {
       owners.add(command.drop(1))
       if (!registered) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else context.registerReceiver(receiver, filter)
        registered = true
        ticker = scope.launch { while (isActive) { commands.send("read"); delay(preferences.value.interval * 1000L) } }
       }
      }
      command.startsWith("-") -> {
       owners.remove(command.drop(1))
       if (owners.isEmpty()) {
        ticker?.cancel(); ticker = null
        if (registered) { context.unregisterReceiver(receiver); registered = false }
        close("Monitoramento pausado")
        points.clear(); lastElapsed = 0
       }
      }
      command == "read" && owners.isNotEmpty() -> sample()
     }
    } catch (e: CancellationException) { throw e }
    catch (_: Exception) { mutable.value = mutable.value.copy(error = "Falha ao registrar a leitura. As próximas amostras serão tentadas automaticamente.") }
   }
  }
 }
 fun acquire(owner: String) { commands.trySend("+$owner") }
 fun release(owner: String) { commands.trySend("-$owner") }
 private suspend fun close(reason: String) {
  active?.let { dao.update(it.copy(closed = true, reason = reason)) }
  active = null
 }
 private suspend fun sample() {
  val now = SystemClock.elapsedRealtime()
  val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
  val wall = System.currentTimeMillis()
  val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
  val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
  val mode = when(status) {
   BatteryManager.BATTERY_STATUS_CHARGING -> Mode.CHARGING
   BatteryManager.BATTERY_STATUS_DISCHARGING -> Mode.DISCHARGING
   BatteryManager.BATTERY_STATUS_FULL -> Mode.FULL
   BatteryManager.BATTERY_STATUS_NOT_CHARGING -> if (plugged == 0) Mode.DISCHARGING else Mode.IDLE
   else -> Mode.UNKNOWN
  }
  // State changes must remain immediate, even directly after another sample.
  if (mode == lastMode && lastElapsed > 0 && now - lastElapsed in 0..999) return
  val level = BatteryMath.percent(intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1), intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1))
  val temperature = BatteryMath.temperature(if (intent.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0) else null)
  val raw = runCatching { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }.getOrDefault(Int.MIN_VALUE)
  val current = BatteryMath.currentMa(raw, mode)
  val conflict = raw != Int.MIN_VALUE && raw != Int.MAX_VALUE && ((mode == Mode.CHARGING && raw < 0) || (mode == Mode.DISCHARGING && raw > 0))
  if (lastElapsed > 0 && BatteryMath.startsNewSession(lastMode,mode,lastElapsed,now,lastWall,wall)) {
   close(if (lastMode != mode) "Estado alterado" else "Intervalo sem observação")
   points.clear()
  }
  if (level == null || mode == Mode.UNKNOWN) { close("Leitura indisponível"); points.clear() }
  if (level != null && mode != Mode.UNKNOWN) {
   if (active == null) {
    startedElapsed = now
    val session = Session(mode=mode.name, started=wall, ended=wall, durationMs=0, initial=level, final=level,
     temperatureSum=0.0, temperatureCount=0, samples=0)
    active = session.copy(id=dao.insert(session)); dao.trim()
   }
   active = active?.copy(ended=wall, durationMs=now-startedElapsed, final=level,
    temperatureSum=active!!.temperatureSum+(temperature ?: 0.0), temperatureCount=active!!.temperatureCount+(if(temperature != null) 1 else 0), samples=active!!.samples+1)
   active?.let { dao.update(it) }
   points.add(Point(now,level))
   points.removeAll { now-it.elapsed > 30*60*1000L }
  }
  val rate = BatteryMath.rate(points,mode)
  val systemRemaining = if (Build.VERSION.SDK_INT >= 28 && mode == Mode.CHARGING) {
   runCatching { manager.computeChargeTimeRemaining() }.getOrNull()?.takeIf { it in 1..604_800_000L }
  } else null
  mutable.value = Reading(level, mode, when {
   plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> "Tomada"
   plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB"
   plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> "Sem fio"
   plugged != 0 -> "Fonte externa"
   else -> "Na bateria"
  },temperature,current,conflict,rate,systemRemaining ?: BatteryMath.remainingMs(level,rate,mode),systemRemaining != null,
   active?.durationMs ?: 0,points.toList(),wall)
  lastElapsed = now; lastWall = wall; lastMode = mode
 }
}
