package br.com.pulsobateria.data

import android.content.Context
import br.com.pulsobateria.core.HistoryValidation
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/** Cópia manual local, sem acesso à rede. O arquivo JSON não é criptografado. */
data class BackupData(
 val settings: Settings,
 val sessions: List<Session>,
 val samples: List<SessionSample>,
 val exportedAt: Long,
 val telemetry: List<TelemetrySample> = emptyList(),
 val labs: List<LabTest> = emptyList(), val includesLabs: Boolean = false
)

class BackupManager(
 private val context: Context,
 private val database: BatteryDatabase,
 private val settingsStore: SettingsStore
) {
 companion object {
  const val FORMAT = "br.com.pulsobateria.backup"
  const val VERSION = 4
  const val MAX_BYTES = 64 * 1024 * 1024
  private fun digest(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
   .digest(value).joinToString("") { "%02x".format(it) }
 }
 private val dao get() = database.sessions()
 private val labDao get() = database.labs()

 suspend fun export(uri: Uri): Pair<Int,Int> = withContext(Dispatchers.IO) {
  val snapshot = database.withTransaction {
   val labs = labDao.exportAll()
   require(labs.none { it.status=="RUNNING" }) { "Finalize o teste Labs antes de exportar o backup." }
   Pair(Triple(dao.exportSessions(),dao.exportSamples(),dao.exportTelemetry()),labs)
  }
  val (sessions, samples, telemetry) = snapshot.first
  val labs = snapshot.second
  val settings = settingsStore.flow.first()
  val data = JSONObject().apply {
   put("exportedAt",System.currentTimeMillis())
   put("settings",JSONObject().apply {
    put("theme",settings.theme); put("interval",settings.interval)
    put("fahrenheit",settings.fahrenheit)
    put("capacityMah",settings.capacityMah ?: JSONObject.NULL)
    // Estado de serviço em segundo plano nunca é restaurado automaticamente.
   })
   put("sessions",JSONArray().apply { sessions.forEach { s -> put(JSONObject().apply {
    put("id",s.id); put("mode",s.mode); put("started",s.started); put("ended",s.ended)
    put("durationMs",s.durationMs); put("initial",s.initial); put("final",s.final)
    put("temperatureSum",s.temperatureSum); put("temperatureCount",s.temperatureCount)
    put("samples",s.samples); put("closed",s.closed); put("reason",s.reason)
    put("chargeSource",s.chargeSource ?: JSONObject.NULL)
    put("chargerLabel",s.chargerLabel ?: JSONObject.NULL)
   }) } })
   put("telemetry",JSONArray().apply { telemetry.forEach { t -> put(JSONObject().apply {
    put("id",t.id); put("sessionId",t.sessionId); put("timestamp",t.timestamp)
    put("elapsedMs",t.elapsedMs); put("mode",t.mode); put("percent",t.percent)
    put("currentMa",t.currentMa ?: JSONObject.NULL)
    put("currentSource",t.currentSource ?: JSONObject.NULL)
    put("temperatureC",t.temperatureC ?: JSONObject.NULL)
    put("counterUah",t.counterUah ?: JSONObject.NULL)
   }) } })
   put("labs",JSONArray().apply { labs.forEach { t -> put(JSONObject().apply {
    put("id",t.id); put("sky",t.sky); put("skyChanges",t.skyChanges)
    put("panelWatts",t.panelWatts ?: JSONObject.NULL)
    put("targetMs",t.targetMs ?: JSONObject.NULL); put("startedAt",t.startedAt)
    put("deadlineAt",t.deadlineAt ?: JSONObject.NULL); put("endedAt",t.endedAt ?: JSONObject.NULL)
    put("status",t.status); put("initialPercent",t.initialPercent ?: JSONObject.NULL)
    put("finalPercent",t.finalPercent ?: JSONObject.NULL); put("chargingMs",t.chargingMs)
    put("notChargingMs",t.notChargingMs); put("weightedMaMs",t.weightedMaMs)
    put("validCurrentMs",t.validCurrentMs)
   }) } })
   put("points",JSONArray().apply { samples.forEach { p -> put(JSONObject().apply {
    put("id",p.id); put("sessionId",p.sessionId); put("timestamp",p.timestamp)
    put("percent",p.percent)
    put("currentMa",p.currentMa ?: JSONObject.NULL)
    put("temperatureC",p.temperatureC ?: JSONObject.NULL)
    put("currentSource",p.currentSource ?: JSONObject.NULL)
   }) } })
  }.toString()
  val bytes=data.toByteArray(Charsets.UTF_8)
  require(bytes.size < MAX_BYTES) { "Histórico excede o tamanho permitido para backup (64 MiB)." }
  val envelope=JSONObject().apply {
   put("format",FORMAT); put("version",VERSION)
   put("sha256",digest(bytes)); put("payloadJson",data)
  }.toString().toByteArray(Charsets.UTF_8)
  require(envelope.size < MAX_BYTES) { "Arquivo de backup excede 64 MiB." }
  val output=context.contentResolver.openOutputStream(uri,"wt") ?: error("Não foi possível abrir o arquivo de destino.")
  output.use { it.write(envelope); it.flush() }
  sessions.size to samples.size
 }

 suspend fun inspect(uri: Uri): BackupData = withContext(Dispatchers.IO) {
  val input=context.contentResolver.openInputStream(uri) ?: error("Não foi possível abrir o backup.")
  val buffer=ByteArrayOutputStream()
  input.use { stream ->
   val chunk=ByteArray(32*1024)
   while(true) {
    val n=stream.read(chunk)
    if(n<0) break
    require(buffer.size()+n <= MAX_BYTES) { "Backup muito grande (limite 64 MiB)." }
    buffer.write(chunk,0,n)
   }
  }
  val envelope=JSONObject(buffer.toString("UTF-8"))
  val backupVersion=envelope.getInt("version")
  require(envelope.getString("format")==FORMAT && backupVersion in 1..VERSION) { "Formato de backup incompatível." }
  val raw=envelope.getString("payloadJson")
  require(digest(raw.toByteArray(Charsets.UTF_8))==envelope.getString("sha256")) { "Backup alterado ou corrompido (SHA-256)." }
  val payload=JSONObject(raw)
  val exportedAt=payload.getLong("exportedAt")
  require(exportedAt>=0) { "Data do backup inválida." }
  val settingsObject=payload.getJSONObject("settings")
  val settings=Settings(theme=settingsObject.getString("theme"), interval=settingsObject.getInt("interval"),
   fahrenheit=settingsObject.getBoolean("fahrenheit"),continuous=false,
   capacityMah=if(settingsObject.isNull("capacityMah")) null else settingsObject.getInt("capacityMah"))
  require(settings.theme in setOf("system","dark","light") && settings.interval in setOf(15,30,60)) { "Preferências inválidas." }
  require(settings.capacityMah==null || settings.capacityMah in 500..30_000) { "Capacidade inválida." }
  val sesArr=payload.getJSONArray("sessions")
  require(sesArr.length() <= 500) { "Número de sessões inválido." }
  val sessions=(0 until sesArr.length()).map { i ->
   val s=sesArr.getJSONObject(i)
   Session(id=s.getLong("id"),mode=s.getString("mode"),started=s.getLong("started"),ended=s.getLong("ended"),
    durationMs=s.getLong("durationMs"), initial=s.getInt("initial"),final=s.getInt("final"),
    temperatureSum=s.getDouble("temperatureSum"),temperatureCount=s.getInt("temperatureCount"),
    samples=s.getInt("samples"),closed=s.getBoolean("closed"),reason=s.getString("reason"),
    chargeSource=if(!s.has("chargeSource") || s.isNull("chargeSource")) null else s.getString("chargeSource"),
    chargerLabel=if(!s.has("chargerLabel") || s.isNull("chargerLabel")) null else s.getString("chargerLabel"))
  }
  require(sessions.map { it.id }.toSet().size==sessions.size && sessions.all {
   it.id>0 && it.mode in setOf("CHARGING","DISCHARGING","FULL","IDLE","UNKNOWN") &&
   HistoryValidation.validSession(it.started,it.ended,it.durationMs,it.samples,it.temperatureCount,it.temperatureSum) &&
   it.initial in 0..100 && it.final in 0..100 && it.samples>=0 && it.temperatureCount>=0 &&
   it.temperatureSum.isFinite() && it.reason.length<=256 &&
   (it.chargeSource==null || (it.mode=="CHARGING" && it.chargeSource in setOf("Tomada","USB","Sem fio","Fonte externa"))) &&
   (it.chargerLabel==null || (it.mode=="CHARGING" && it.chargerLabel.length in 1..40 && it.chargerLabel.none { c -> Character.isISOControl(c) }))
  }) { "Sessão inválida ou ID repetido." }
  val arr=payload.getJSONArray("points")
  require(arr.length()<=240_000) { "Número de pontos inválido." }
  val sessionsById=sessions.associateBy { it.id }
  val sessionIds=sessionsById.keys
  val points=(0 until arr.length()).map { i ->
   val pt=arr.getJSONObject(i)
   SessionSample(id=pt.getLong("id"),sessionId=pt.getLong("sessionId"),timestamp=pt.getLong("timestamp"),
    percent=pt.getInt("percent"),
    currentMa=if(pt.isNull("currentMa")) null else pt.getDouble("currentMa"),
    temperatureC=if(pt.isNull("temperatureC")) null else pt.getDouble("temperatureC"),
    currentSource=if(pt.isNull("currentSource")) null else pt.getString("currentSource"))
  }
  val pointsBySession=points.groupingBy { it.sessionId }.eachCount()
  require(points.map { it.id }.toSet().size==points.size && points.all {
   it.id>0 && it.sessionId in sessionIds && it.timestamp>=0 && it.percent in 0..100 &&
   sessionsById[it.sessionId]?.let { session -> HistoryValidation.sampleWithinSession(it.timestamp,session.started,session.ended) } == true &&
   (it.currentMa==null || it.currentMa.isFinite() && kotlin.math.abs(it.currentMa)<=30_000) &&
   (it.temperatureC==null || it.temperatureC.isFinite() && it.temperatureC in -40.0..100.0) &&
   (it.currentSource==null || it.currentSource.length<=48)
  } && pointsBySession.values.all { it<=480 }) { "Pontos inválidos, órfãos ou duplicados." }
  // Backups v1 continuam aceitos; não fabricamos leituras técnicas retroativas.
  val rawTelemetry=payload.optJSONArray("telemetry")
  require(backupVersion==1 || rawTelemetry!=null) { "Backup sem telemetria." }
  require((rawTelemetry?.length() ?: 0)<=30_000) { "Telemetria acima do limite." }
  val telemetry=(0 until (rawTelemetry?.length() ?: 0)).map { i ->
   val t=rawTelemetry!!.getJSONObject(i)
   TelemetrySample(id=t.getLong("id"),sessionId=t.getLong("sessionId"),timestamp=t.getLong("timestamp"),
    elapsedMs=t.getLong("elapsedMs"),mode=t.getString("mode"),percent=t.getInt("percent"),
    currentMa=if(t.isNull("currentMa")) null else t.getDouble("currentMa"),
    currentSource=if(t.isNull("currentSource")) null else t.getString("currentSource"),
    temperatureC=if(t.isNull("temperatureC")) null else t.getDouble("temperatureC"),
    counterUah=if(t.isNull("counterUah")) null else t.getInt("counterUah"))
  }
  require(telemetry.map { it.id }.toSet().size==telemetry.size && telemetry.all { t ->
   t.id>0 && t.sessionId in sessionIds && t.timestamp>=0 && t.elapsedMs>=0 && t.percent in 0..100 &&
   t.mode in setOf("CHARGING","DISCHARGING","IDLE","FULL") &&
   sessionsById[t.sessionId]?.let { it.mode==t.mode && HistoryValidation.sampleWithinSession(t.timestamp,it.started,it.ended) }==true &&
   (t.currentMa==null || t.currentMa.isFinite() && kotlin.math.abs(t.currentMa)<=30_000) &&
   (t.temperatureC==null || t.temperatureC.isFinite() && t.temperatureC in -40.0..100.0) &&
   (t.currentSource==null || t.currentSource.length<=48) &&
   (t.counterUah==null || t.counterUah in 0..100_000_000)
  } && telemetry.groupingBy { it.sessionId }.eachCount().values.all { it<=1440 }) { "Telemetria inválida ou duplicada." }
  val rawLabs = payload.optJSONArray("labs")
  require(backupVersion < 4 || rawLabs != null) { "Backup v4 sem dados Labs." }
  require((rawLabs?.length() ?: 0) <= 100) { "Número de testes Labs inválido." }
  val labs = (0 until (rawLabs?.length() ?: 0)).map { i ->
   val t=rawLabs!!.getJSONObject(i)
   LabTest(id=t.getLong("id"),sky=t.getString("sky"),skyChanges=t.getString("skyChanges"),
    panelWatts=if(t.isNull("panelWatts")) null else t.getInt("panelWatts"),
    targetMs=if(t.isNull("targetMs")) null else t.getLong("targetMs"),
    startedAt=t.getLong("startedAt"),deadlineAt=if(t.isNull("deadlineAt")) null else t.getLong("deadlineAt"),
    endedAt=if(t.isNull("endedAt")) null else t.getLong("endedAt"), status=t.getString("status"),
    initialPercent=if(t.isNull("initialPercent")) null else t.getInt("initialPercent"),
    finalPercent=if(t.isNull("finalPercent")) null else t.getInt("finalPercent"),
    chargingMs=t.getLong("chargingMs"), notChargingMs=t.getLong("notChargingMs"),
    weightedMaMs=t.getDouble("weightedMaMs"),validCurrentMs=t.getLong("validCurrentMs"))
  }
  require(labs.map { it.id }.toSet().size==labs.size && labs.all { t ->
   t.id>0 && t.sky in setOf("forte","parcial","nublado","chuva") &&
   t.skyChanges.length in 1..3000 && t.panelWatts?.let { it in 1..200 } != false &&
   t.targetMs?.let { it in 60_000L..86_400_000L } != false &&
   t.startedAt>0 && t.endedAt!=null && t.endedAt>=t.startedAt &&
   t.status in setOf("STOPPED","COMPLETED") &&
   t.initialPercent?.let { it in 0..100 } != false && t.finalPercent?.let { it in 0..100 } != false &&
   t.chargingMs in 0..(t.endedAt-t.startedAt) &&
   t.notChargingMs in 0..(t.endedAt-t.startedAt-t.chargingMs) &&
   t.validCurrentMs in 0..t.chargingMs && t.weightedMaMs.isFinite() && t.weightedMaMs>=0 &&
   (t.deadlineAt==null || t.deadlineAt>=t.startedAt)
  }) { "Dados Labs inválidos." }
  BackupData(settings,sessions,points,exportedAt,telemetry,labs,backupVersion>=4)
 }

 /** O chamador deve congelar o coletor antes de substituir o banco. */
 suspend fun restore(backup: BackupData) = withContext(Dispatchers.IO) {
  // O banco é substituído numa única transação Room: erro ou processo encerrado
  // antes do commit não deixa a tabela de sessões pela metade.
  database.withTransaction {
   require(labDao.active()==null) { "Finalize o teste Labs antes de restaurar o backup." }
   dao.clearTelemetry(); dao.clearSamples(); dao.clearSessions()
   if (backup.includesLabs) {
    labDao.clear()
    if(backup.labs.isNotEmpty()) labDao.insertAll(backup.labs)
   }
   backup.sessions.forEach { session ->
    dao.insert(if(session.closed) session else session.copy(closed=true,reason="Restaurada de backup"))
   }
   backup.samples.chunked(250).forEach { dao.insertSamples(it) }
   backup.telemetry.chunked(250).forEach { dao.insertTelemetry(it) }
  }
  // DataStore é um armazenamento independente: pode falhar após o commit Room.
  settingsStore.replaceFromBackup(backup.settings)
 }
}
