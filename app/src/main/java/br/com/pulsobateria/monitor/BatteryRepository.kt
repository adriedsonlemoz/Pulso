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
 private val context: Context, private val dao: SessionDao, private val settings: SettingsStore, private val scope: CoroutineScope
) {
 private val manager = context.getSystemService(BatteryManager::class.java)
 private data class Command(val name: String, val ack: CompletableDeferred<Unit>? = null)
 private val commands = Channel<Command>(Channel.UNLIMITED)
 private val owners = mutableSetOf<String>()
 private val mutable = MutableStateFlow(Reading())
 val reading = mutable.asStateFlow()
 val sessions = dao.observe()
 val telemetry = dao.observeRecentTelemetry()
 fun sessionSamples(id: Long): Flow<List<SessionSample>> = dao.observeSamples(id)
 fun sessionTelemetry(id: Long): Flow<List<TelemetrySample>> = dao.observeSessionTelemetry(id)
 suspend fun setChargerLabel(id: Long, label: String?) { dao.setChargerLabel(id,label) }
 val serviceActive = MutableStateFlow(false)
 val serviceError = MutableStateFlow<String?>(null)
 private val preferences = settings.flow.stateIn(scope, SharingStarted.Eagerly, Settings())
 private var currentTracker = CurrentTracker()
 private var calibrationLoaded = false
 private var savedCalibration: String? = null
 private var calibrationScope: String? = null
 fun resetCalibration() { commands.trySend(Command("resetCalibration")) }
 private var restoring = false
 /** Impede a escrita de amostras enquanto o banco é substituído pelo backup. */
 suspend fun freezeForRestore() {
  val ack = CompletableDeferred<Unit>()
  commands.send(Command("freezeRestore",ack))
  try { ack.await() }
  catch(e: Exception) { commands.trySend(Command("resumeRestore")); throw e }
 }
 fun resumeAfterRestore() { commands.trySend(Command("resumeRestore")) }
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
 private var lastTelemetryElapsed: Long? = null
 private val receiver = object: BroadcastReceiver() {
  override fun onReceive(c: Context?, intent: Intent?) { commands.trySend(Command("read")) }
 }
 init {
  scope.launch {
   try { dao.closeInterrupted() } catch (_: Exception) { mutable.value = mutable.value.copy(error = "Não foi possível recuperar o histórico.") }
   for (message in commands) {
    val command=message.name
    try {
     when {
      command == "resetCalibration" -> {
       currentTracker=CurrentTracker(); calibrationLoaded=false; savedCalibration=null
       settings.calibration(null)
      }
      command == "freezeRestore" -> {
       restoring = true
       close("Histórico restaurado")
       points.clear(); currentTracker = CurrentTracker(); calibrationLoaded=false
       lastElapsed=0L; lastWall=0L; lastMode=Mode.UNKNOWN; paused=false
       mutable.value = Reading()
       message.ack?.complete(Unit)
      }
      command == "resumeRestore" && restoring -> {
       restoring=false; active=null
       lastStoredTimestamp=null; lastStoredPercent=null; lastTelemetryElapsed=null
       if(owners.isNotEmpty()) commands.trySend(Command("read"))
      }
      command.startsWith("+") -> {
       pauseTimer?.cancel(); pauseTimer = null
       owners.add(command.drop(1))
       if (!registered) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else context.registerReceiver(receiver, filter)
        registered = true
        ticker = scope.launch { while (isActive) { commands.send(Command("read")); delay(preferences.value.interval * 1000L) } }
       }
      }
      command.startsWith("-") -> {
       owners.remove(command.drop(1))
       if (owners.isEmpty()) {
        // Evitar médias de corrente atravessando períodos não observados.
        currentTracker = CurrentTracker(); calibrationLoaded=false
        ticker?.cancel(); ticker = null
        if (registered) { context.unregisterReceiver(receiver); registered = false }
        paused = true
        points.clear()
        active = active?.copy(reason = "Pausada • aguardando retomada")
        active?.let { dao.update(it) }
        pauseTimer?.cancel()
        pauseTimer = scope.launch { delay(BatteryMath.PAUSE_GRACE_MS); commands.send(Command("finishPause")) }
       }
      }
      command == "finishPause" && owners.isEmpty() && paused -> {
       close("Monitoramento pausado"); paused = false; lastElapsed = 0
      }
      command == "read" && owners.isNotEmpty() && !restoring -> sample()
     }
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) {
     if(command=="freezeRestore") { restoring=false; message.ack?.completeExceptionally(e) }
     mutable.value = mutable.value.copy(error = "Falha ao registrar a leitura. As próximas amostras serão tentadas automaticamente.")
    }
   }
  }
 }
 fun acquire(owner: String) { commands.trySend(Command("+$owner")) }
 fun release(owner: String) { commands.trySend(Command("-$owner")) }
 private suspend fun close(reason: String) {
  active?.let { dao.update(it.copy(closed = true, reason = reason)) }
  active = null
  lastStoredTimestamp = null; lastStoredPercent = null; lastTelemetryElapsed=null
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
  val chargeSource=if(mode==Mode.CHARGING) when {
   plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> "Tomada"
   plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB"
   plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> "Sem fio"
   plugged != 0 -> "Fonte externa"
   else -> null
  } else null
  // Se o Android identificar a troca de tipo de fonte, encerrar a sessão anterior.
  // Fontes do mesmo tipo ainda podem ser acessórios diferentes: só o nome manual distingue.
  if (active?.mode==Mode.CHARGING.name && mode==Mode.CHARGING &&
      active?.chargeSource!=null && chargeSource!=null && active?.chargeSource!=chargeSource) {
   close("Fonte de carga alterada")
   points.clear(); currentTracker=CurrentTracker(); calibrationLoaded=false
   lastElapsed=0L
  }
  if (paused && active != null && !BatteryMath.canResumePause(lastMode,mode,active!!.final,level ?: -1,lastElapsed,now,lastWall,wall)) {
   close("Intervalo sem observação")
  }
  if (!paused && lastElapsed > 0 && BatteryMath.startsNewSession(lastMode,mode,lastElapsed,now,lastWall,wall)) {
   close(if (lastMode != mode) "Estado alterado" else "Intervalo sem observação")
   points.clear(); currentTracker=CurrentTracker(); calibrationLoaded=false
  }
  if (level == null || mode == Mode.UNKNOWN) { close("Leitura indisponível"); points.clear(); currentTracker=CurrentTracker(); calibrationLoaded=false }
  // Carregar o valor real do DataStore antes de marcar calibração como carregada.
  if(!calibrationLoaded) {
   val stored=settings.flow.first().calibration
   if(calibrationScope==null) calibrationScope=settings.calibrationScope()
   currentTracker.loadCalibration(stored,wall,calibrationScope!!)
   savedCalibration=stored
   calibrationLoaded=true
  }
  val candidatePoints=if(level!=null && mode!=Mode.UNKNOWN)
   (points+Point(now,level)).filter { now-it.elapsed <= 30*60*1000L } else emptyList()
  val rate=BatteryMath.rate(candidatePoints,mode)
  val currentInfo=currentTracker.observe(rawNow,rawAverage,rawCounter,mode,now,rate,preferences.value.capacityMah,wall)
  val calibration=currentTracker.calibrationRecord(calibrationScope!!)
  if(calibration!=savedCalibration) {
   settings.calibration(calibration); savedCalibration=calibration
  }
  if(level!=null && mode!=Mode.UNKNOWN) {
   val previous=active ?: Session(mode=mode.name,started=wall,ended=wall,durationMs=0,initial=level,final=level,
    temperatureSum=0.0,temperatureCount=0,samples=0,chargeSource=chargeSource)
   val candidate=previous.copy(ended=wall,chargeSource=previous.chargeSource ?: chargeSource,
    durationMs=BatteryMath.observedDuration(previous.durationMs,lastElapsed,now,paused,previous.samples),final=level,
    temperatureSum=previous.temperatureSum+(temperature ?: 0.0),
    temperatureCount=previous.temperatureCount+(if(temperature!=null) 1 else 0),samples=previous.samples+1,reason="Em observação")
   val shouldStore=BatteryMath.shouldStoreSessionSample(lastStoredTimestamp,lastStoredPercent,wall,level)
   val sample=if(shouldStore) SessionSample(sessionId=previous.id,timestamp=wall,percent=level,
    currentMa=currentInfo.ma,temperatureC=temperature,currentSource=currentInfo.source.name) else null
   val shouldTelemetry=lastTelemetryElapsed==null || now < lastTelemetryElapsed!! || now-lastTelemetryElapsed!!>=15_000L
   val telemetry=if(shouldTelemetry) TelemetrySample(sessionId=previous.id,timestamp=wall,elapsedMs=now,
    mode=mode.name,percent=level,currentMa=currentInfo.ma,currentSource=currentInfo.source.name,
    temperatureC=temperature,counterUah=rawCounter.takeIf { it in 0..100_000_000 }) else null
   active=dao.saveObservation(candidate,sample,telemetry)
   if(shouldTelemetry) lastTelemetryElapsed=now
   if(shouldStore) { lastStoredTimestamp=wall; lastStoredPercent=level }
  }
  points.clear(); points.addAll(candidatePoints)
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
