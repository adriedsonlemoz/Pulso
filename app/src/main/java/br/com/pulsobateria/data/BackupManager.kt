package br.com.pulsobateria.data

import android.content.Context
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
 val exportedAt: Long
)

class BackupManager(
 private val context: Context,
 private val database: BatteryDatabase,
 private val settingsStore: SettingsStore
) {
 companion object {
  const val FORMAT = "br.com.pulsobateria.backup"
  const val VERSION = 1
  const val MAX_BYTES = 64 * 1024 * 1024
  private fun digest(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
   .digest(value).joinToString("") { "%02x".format(it) }
 }
 private val dao get() = database.sessions()

 suspend fun export(uri: Uri): Pair<Int,Int> = withContext(Dispatchers.IO) {
  val (sessions, samples) = database.withTransaction { dao.exportSessions() to dao.exportSamples() }
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
  require(envelope.getString("format")==FORMAT && envelope.getInt("version")==VERSION) { "Formato de backup incompatível." }
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
    samples=s.getInt("samples"),closed=s.getBoolean("closed"),reason=s.getString("reason"))
  }
  require(sessions.map { it.id }.toSet().size==sessions.size && sessions.all {
   it.id>0 && it.mode in setOf("CHARGING","DISCHARGING","FULL","IDLE","UNKNOWN") &&
   it.started>=0 && it.ended>=it.started && it.durationMs>=0 &&
   it.initial in 0..100 && it.final in 0..100 && it.samples>=0 && it.temperatureCount>=0 &&
   it.temperatureSum.isFinite() && it.reason.length<=256
  }) { "Sessão inválida ou ID repetido." }
  val arr=payload.getJSONArray("points")
  require(arr.length()<=240_000) { "Número de pontos inválido." }
  val sessionIds=sessions.map { it.id }.toHashSet()
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
   (it.currentMa==null || it.currentMa.isFinite() && kotlin.math.abs(it.currentMa)<=30_000) &&
   (it.temperatureC==null || it.temperatureC.isFinite() && it.temperatureC in -40.0..100.0) &&
   (it.currentSource==null || it.currentSource.length<=48)
  } && pointsBySession.values.all { it<=480 }) { "Pontos inválidos, órfãos ou duplicados." }
  BackupData(settings,sessions,points,exportedAt)
 }

 /** O chamador deve congelar o coletor antes de substituir o banco. */
 suspend fun restore(backup: BackupData) = withContext(Dispatchers.IO) {
  // O banco é substituído numa única transação Room: erro ou processo encerrado
  // antes do commit não deixa a tabela de sessões pela metade.
  database.withTransaction {
   dao.clearSamples(); dao.clearSessions()
   backup.sessions.forEach { session ->
    dao.insert(if(session.closed) session else session.copy(closed=true,reason="Restaurada de backup"))
   }
   backup.samples.chunked(250).forEach { dao.insertSamples(it) }
  }
  // DataStore é um armazenamento independente: pode falhar após o commit Room.
  settingsStore.replaceFromBackup(backup.settings)
 }
}
