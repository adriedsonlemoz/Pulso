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
 val temperature: Double? = null, val current: Double? = null, val currentInfo: CurrentResult = CurrentResult(),
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
 fun sessionSamples(id: Long): Flow<List<SessionSample>> = dao.observeSamples(id)
 val serviceActive = MutableStateFlow(false)
 val serviceError = MutableStateFlow<String?>(null)
 private val preferences = settings.flow.stateIn(scope, SharingStarted.Eagerly, Settings())
 private var currentTracker = CurrentTracker()
 private var ticker: Job? = null
 private var registered = false
 private var active: Session? = null
 private var paused = false
 private var pauseTimer: Job? = null
 private var lastElapsed = 0L
 private var lastWall = 0L
 private var lastMode = Mode.UNKNOWN
 private val points = mutableListOf<Point>()
 private var lastStoredTimestamp: Long? = null
 private var lastStoredPercent: Int? = null
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
       pauseTimer?.cancel(); pauseTimer = null
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
        // Evitar médias de corrente atravessando períodos não observados.
        currentTracker = CurrentTracker()
        ticker?.cancel(); ticker = null
        if (registered) { context.unregisterReceiver(receiver); registered = false }
        paused = true
        points.clear()
        active = active?.copy(reason = "Pausada • aguardando retomada")
        active?.let { dao.update(it) }
        pauseTimer?.cancel()
        pauseTimer = scope.launch { delay(BatteryMath.PAUSE_GRACE_MS); commands.send("finishPause") }
       }
      }
      command == "finishPause" && owners.isEmpty() && paused -> {
       close("Monitoramento pausado"); paused = false; lastElapsed = 0
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
  lastStoredTimestamp = null; lastStoredPercent = null
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
  if (!paused && mode == lastMode && lastElapsed > 0 && now - lastElapsed in 0..999) return
  val level = BatteryMath.percent(intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1), intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1))
  val temperature = BatteryMath.temperature(if (intent.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0) else null)
  // Retornar MIN_VALUE nas propriedades não suportadas. Não mascarar ausência com zero.
  val rawNow = runCatching { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }.getOrDefault(Int.MIN_VALUE)
  val rawAverage = runCatching { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) }.getOrDefault(Int.MIN_VALUE)
  val rawCounter = runCatching { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) }.getOrDefault(Int.MIN_VALUE)
  if (paused && active != null && !BatteryMath.canResumePause(lastMode,mode,active!!.final,level ?: -1,lastElapsed,now,lastWall,wall)) {
   close("Intervalo sem observação")
  }
  if (!paused && lastElapsed > 0 && BatteryMath.startsNewSession(lastMode,mode,lastElapsed,now,lastWall,wall)) {
   close(if (lastMode != mode) "Estado alterado" else "Intervalo sem observação")
   points.clear()
  }
  if (level == null || mode == Mode.UNKNOWN) { close("Leitura indisponível"); points.clear() }
  if (level != null && mode != Mode.UNKNOWN) {
   if (active == null) {
    val session = Session(mode=mode.name, started=wall, ended=wall, durationMs=0, initial=level, final=level,
     temperatureSum=0.0, temperatureCount=0, samples=0)
    active = session.copy(id=dao.insert(session)); dao.trim()
    lastStoredTimestamp = null; lastStoredPercent = null
   }
   active = active?.copy(ended=wall, durationMs=BatteryMath.observedDuration(active!!.durationMs,lastElapsed,now,paused,active!!.samples), final=level,
    temperatureSum=active!!.temperatureSum+(temperature ?: 0.0), temperatureCount=active!!.temperatureCount+(if(temperature != null) 1 else 0), samples=active!!.samples+1, reason="Em observação")
   active?.let { session ->
    dao.update(session)
    if (BatteryMath.shouldStoreSessionSample(lastStoredTimestamp,lastStoredPercent,wall,level)) {
     dao.insertSample(SessionSample(sessionId=session.id,timestamp=wall,percent=level))
     dao.trimSamples(session.id)
     lastStoredTimestamp = wall; lastStoredPercent = level
    }
   }
   points.add(Point(now,level))
   points.removeAll { now-it.elapsed > 30*60*1000L }
  }
  val rate = BatteryMath.rate(points,mode)
  val currentInfo = currentTracker.observe(rawNow,rawAverage,rawCounter,mode,now,rate,preferences.value.capacityMah)
  val rawSystemRemaining = if (Build.VERSION.SDK_INT >= 28 && mode == Mode.CHARGING) {
   runCatching { manager.computeChargeTimeRemaining() }.getOrNull()
  } else null
  // Nunca exibir um ETA do Android isolado: o fabricante pode retornar um
  // valor positivo, porém absurdo (ex.: 58% -> 100% em dois minutos).
  // Sem histórico local confiável, a previsão permanece em "Calculando".
  val systemRemaining = BatteryMath.validatedSystemChargeTimeMs(level,rawSystemRemaining,rate)
  mutable.value = Reading(percent=level, mode=mode, source=when {
   plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> "Tomada"
   plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB"
   plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> "Sem fio"
   plugged != 0 -> "Fonte externa"
   else -> "Na bateria"
  },temperature=temperature,current=currentInfo.ma,currentInfo=currentInfo,rate=rate,
   remaining=systemRemaining ?: BatteryMath.remainingMs(level,rate,mode),systemEstimate=systemRemaining != null,
   duration=active?.durationMs ?: 0,points=points.toList(),updated=wall)
  paused = false
  lastElapsed = now; lastWall = wall; lastMode = mode
 }
}
