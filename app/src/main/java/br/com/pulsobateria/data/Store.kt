package br.com.pulsobateria.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.*
import java.io.IOException
import android.os.Build
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import br.com.pulsobateria.core.ExternalLabsClient
import br.com.pulsobateria.core.ExternalLabsPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Context.preferences by preferencesDataStore("settings")
data class Settings(val theme: String = "system", val interval: Int = 30, val fahrenheit: Boolean = false, val continuous: Boolean = false, val capacityMah: Int? = null, val calibration: String? = null, val startOnBoot: Boolean = false,
 val alertsEnabled: Boolean = false, val lowBatteryAlert: Int = 20, val hotBatteryAlert: Int = 43,
 val chargeTargetAlert: Int = 80, val fastDrainAlert: Int = 20,
 val externalLabsEnabled: Boolean = false, val externalLabsClients: List<ExternalLabsClient> = emptyList())

class SettingsStore(private val context: Context) {
 private val theme = stringPreferencesKey("theme")
 private val interval = intPreferencesKey("interval")
 private val fahrenheit = booleanPreferencesKey("fahrenheit")
 private val continuous = booleanPreferencesKey("continuous")
 private val startOnBoot = booleanPreferencesKey("start_on_boot")
 private val capacity = intPreferencesKey("capacity_mah")
 private val calibration = stringPreferencesKey("current_calibration")
 private val alertsEnabled = booleanPreferencesKey("alerts_enabled")
 private val alertLow = intPreferencesKey("alert_low")
 private val alertTemp = intPreferencesKey("alert_temp")
 private val alertTarget = intPreferencesKey("alert_target")
 private val alertDrain = intPreferencesKey("alert_drain")
 private val extEnabled = booleanPreferencesKey("external_labs_enabled")
 private val extPackage = stringPreferencesKey("external_labs_package")
 private val extCert = stringPreferencesKey("external_labs_signer")
 private val extLastId = longPreferencesKey("external_labs_last_id")
 private val extClients = stringPreferencesKey("external_labs_clients_v2")

 private fun clients(raw: String?): List<ExternalLabsClient> = runCatching {
  val data = JSONArray(raw ?: "[]")
  val seen = mutableSetOf<String>()
  buildList {
   for (index in 0 until minOf(data.length(), ExternalLabsPolicy.MAX_CLIENTS)) {
    val node = data.optJSONObject(index) ?: continue
    val pkg = node.optString("package")
    val signer = node.optString("signer")
    if (!ExternalLabsPolicy.validIdentity(pkg, signer) || !seen.add(pkg)) continue
    val id = node.optLong("lastId", 0L).takeIf { it > 0L }
    add(ExternalLabsClient(pkg, signer, node.optBoolean("enabled", false), id))
   }
  }
 }.getOrDefault(emptyList())

 private fun oldClient(prefs: androidx.datastore.preferences.core.Preferences): List<ExternalLabsClient> {
  val pkg = prefs[extPackage] ?: return emptyList()
  val signer = prefs[extCert] ?: return emptyList()
  if (!ExternalLabsPolicy.validIdentity(pkg, signer)) return emptyList()
  return listOf(ExternalLabsClient(pkg, signer, true, prefs[extLastId]?.takeIf { it > 0L }))
 }

 private fun storedClients(prefs: androidx.datastore.preferences.core.Preferences): List<ExternalLabsClient> =
  if (prefs.contains(extClients)) clients(prefs[extClients]) else oldClient(prefs)

 private fun saveClients(prefs: androidx.datastore.preferences.core.MutablePreferences, values: List<ExternalLabsClient>) {
  val array = JSONArray()
  values.forEach { item ->
   array.put(JSONObject().put("package", item.packageName).put("signer", item.signer)
    .put("enabled", item.enabled).put("lastId", item.lastTestId ?: JSONObject.NULL))
  }
  prefs[extClients] = array.toString()
  prefs.remove(extPackage); prefs.remove(extCert); prefs.remove(extLastId)
 }
 val flow = context.preferences.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
  Settings(it[theme] ?: "system", (it[interval] ?: 30).coerceIn(15,60), it[fahrenheit] ?: false, it[continuous] ?: false,
   it[capacity]?.takeIf { mah -> mah in 500..30_000 }, it[calibration], it[startOnBoot] ?: false,
   it[alertsEnabled] ?: false, (it[alertLow] ?: 20).takeIf { x -> x in 5..40 } ?: 20,
   (it[alertTemp] ?: 43).takeIf { x -> x in 35..55 } ?: 43,
   (it[alertTarget] ?: 80).takeIf { x -> x in 60..100 } ?: 80,
   (it[alertDrain] ?: 20).takeIf { x -> x in 10..40 } ?: 20,
   it[extEnabled] ?: false, storedClients(it))
 }
 /** Identidade local excluída de backup + firmware: não confundir ajuste entre instalações. */
 suspend fun calibrationScope(): String = withContext(Dispatchers.IO) {
  val file=AtomicFile(File(context.noBackupFilesDir,"current-calibration-id"))
  val existing=runCatching { file.readFully().toString(Charsets.UTF_8) }.getOrNull()
  val id=existing?.takeIf { runCatching { UUID.fromString(it) }.isSuccess } ?: UUID.randomUUID().toString().also { value ->
   val output=file.startWrite()
   try { output.write(value.toByteArray(Charsets.UTF_8)); file.finishWrite(output) }
   catch (e: Exception) { file.failWrite(output); throw e }
  }
  MessageDigest.getInstance("SHA-256").digest((id+"|"+Build.FINGERPRINT).toByteArray(Charsets.UTF_8))
   .joinToString("") { "%02x".format(it) }
 }
 suspend fun theme(value: String) { context.preferences.edit { it[theme] = value } }
 suspend fun interval(value: Int) { context.preferences.edit { it[interval] = value.coerceIn(15,60) } }
 suspend fun fahrenheit(value: Boolean) { context.preferences.edit { it[fahrenheit] = value } }
 suspend fun continuous(value: Boolean) { context.preferences.edit { it[continuous] = value } }
 suspend fun startOnBoot(value: Boolean) { context.preferences.edit { it[startOnBoot] = value } }
 suspend fun alertsEnabled(value: Boolean) { context.preferences.edit { it[alertsEnabled] = value } }
 suspend fun alertLow(value: Int) { require(value in 5..40); context.preferences.edit { it[alertLow] = value } }
 suspend fun alertTemp(value: Int) { require(value in 35..55); context.preferences.edit { it[alertTemp] = value } }
 suspend fun alertTarget(value: Int) { require(value in 60..100); context.preferences.edit { it[alertTarget] = value } }
 suspend fun alertDrain(value: Int) { require(value in 10..40); context.preferences.edit { it[alertDrain] = value } }
 /** A seleção explícita autoriza este pacote, sem desautorizar os demais. Máximo de 20 clientes. */
 suspend fun pairExternalLabs(packageName: String, signer: String) {
  require(ExternalLabsPolicy.validIdentity(packageName, signer))
  context.preferences.edit { prefs ->
   val existing = storedClients(prefs)
   saveClients(prefs, ExternalLabsPolicy.pair(existing, packageName, signer))
  }
 }
 suspend fun enableExternalLabs(enabled: Boolean) {
  context.preferences.edit { prefs ->
   if (enabled) require(storedClients(prefs).isNotEmpty())
   prefs[extEnabled] = enabled
  }
 }
 suspend fun enableExternalPartner(packageName: String, enabled: Boolean) {
  context.preferences.edit { prefs ->
   val clients = storedClients(prefs)
   saveClients(prefs, ExternalLabsPolicy.enabled(clients, packageName, enabled))
  }
 }
 suspend fun removeExternalPartner(packageName: String) { context.preferences.edit { prefs ->
  val remaining = storedClients(prefs).filterNot { it.packageName == packageName }
  saveClients(prefs, remaining)
  if (remaining.isEmpty()) prefs[extEnabled] = false
 } }
 suspend fun clearExternalLabs() { context.preferences.edit { prefs ->
  prefs[extEnabled] = false; saveClients(prefs, emptyList())
 } }
 suspend fun rememberExternalLab(packageName: String, id: Long) {
  require(id > 0L)
  context.preferences.edit { prefs ->
   require(prefs[extEnabled] == true)
   val clients = storedClients(prefs)
   saveClients(prefs, ExternalLabsPolicy.remember(clients, packageName, id))
  }
 }
 suspend fun capacityMah(value: Int?) { require(value == null || value in 500..30_000); context.preferences.edit { if (value == null) it.remove(capacity) else it[capacity] = value } }
 suspend fun calibration(value: String?) { context.preferences.edit { if(value==null) it.remove(calibration) else it[calibration]=value } }
 suspend fun replaceFromBackup(value: Settings) {
  require(value.theme in listOf("system", "dark", "light") && value.interval in listOf(15,30,60))
  require(value.capacityMah == null || value.capacityMah in 500..30_000)
  context.preferences.edit {
   it[theme] = value.theme; it[interval] = value.interval; it[fahrenheit] = value.fahrenheit
   // Segurança: a preferência restaurada nunca inicia um serviço sem intervenção do usuário.
   it[continuous] = false
   it[startOnBoot] = false
   // Não ativar alertas silenciosamente após restaurar um backup.
   it[alertsEnabled] = false
   // Nunca restaurar acesso entre aplicativos de outro aparelho/backup.
   it[extEnabled] = false; saveClients(it, emptyList())
   it[alertLow] = 20; it[alertTemp] = 43; it[alertTarget] = 80; it[alertDrain] = 20
   if(value.capacityMah != null) it[capacity] = value.capacityMah else it.remove(capacity)
   // Nunca importar calibração específica de hardware de outro aparelho.
   it.remove(calibration)
  }

 }
}
@Entity(tableName = "sessions")
data class Session(
 @PrimaryKey(autoGenerate = true) val id: Long = 0,
 val mode: String, val started: Long, val ended: Long,
 val durationMs: Long, val initial: Int, val final: Int,
 val temperatureSum: Double, val temperatureCount: Int,
 val samples: Int, val closed: Boolean = false,
 val reason: String = "Em observação",
 val chargeSource: String? = null, val chargerLabel: String? = null
)
@Entity(
 tableName = "session_samples",
 foreignKeys = [ForeignKey(entity = Session::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
 indices = [Index("sessionId")]
)
data class SessionSample(
 @PrimaryKey(autoGenerate = true) val id: Long = 0,
 val sessionId: Long, val timestamp: Long, val percent: Int,
 val currentMa: Double? = null, val temperatureC: Double? = null, val currentSource: String? = null
)


/** Amostras técnicas independentes dos pontos compactados do gráfico de porcentagem. */
@Entity(tableName = "telemetry_samples",
 foreignKeys=[ForeignKey(entity=Session::class,parentColumns=["id"],childColumns=["sessionId"],onDelete=ForeignKey.CASCADE)],
 indices=[Index("sessionId"),Index("timestamp")])
data class TelemetrySample(
 @PrimaryKey(autoGenerate=true) val id: Long=0,
 val sessionId: Long, val timestamp: Long, val elapsedMs: Long,
 val mode: String, val percent: Int, val currentMa: Double?,
 val currentSource: String?, val temperatureC: Double?, val counterUah: Int?
)

/** Teste solar independente das sessões comuns. Tempos apenas de intervalos observados. */
@Entity(tableName="lab_tests")
data class LabTest(
 @PrimaryKey(autoGenerate=true) val id: Long = 0,
 val sky: String, val skyChanges: String, val panelWatts: Int?,
 val targetMs: Long?, val startedAt: Long, val deadlineAt: Long?,
 val endedAt: Long? = null, val status: String = "RUNNING",
 val initialPercent: Int? = null, val finalPercent: Int? = null,
 val chargingMs: Long = 0, val notChargingMs: Long = 0,
 val weightedMaMs: Double = 0.0, val validCurrentMs: Long = 0,
 val lastReadingAt: Long? = null, val lastElapsedMs: Long? = null,
 val lastMode: String? = null, val lastCurrentMa: Double? = null,
 val lastCurrentSource: String? = null,
 @ColumnInfo(defaultValue = "'SOLAR'") val kind: String = "SOLAR",
 @ColumnInfo(defaultValue = "0") val externalSeen: Boolean = false,
 val lastPlugged: Boolean? = null,
 // Temperaturas reais reportadas pelo Android; nullable em aparelhos sem sensor.
 val initialTemperatureC: Double? = null, val finalTemperatureC: Double? = null,
 val maxTemperatureC: Double? = null,
 // Condições informadas manualmente, nunca inferidas do uso de outros apps.
 @ColumnInfo(defaultValue = "'NAO_INFORMADA'") val screenCondition: String = "NAO_INFORMADA",
 @ColumnInfo(defaultValue = "0") val cameraReported: Boolean = false,
 @ColumnInfo(defaultValue = "0") val wifiReported: Boolean = false,
 @ColumnInfo(defaultValue = "0") val recordingReported: Boolean = false,
 @ColumnInfo(defaultValue = "''") val notes: String = ""
)

@Dao
interface LabDao {
 @Query("SELECT * FROM lab_tests ORDER BY id DESC LIMIT 100") fun observe(): Flow<List<LabTest>>
 @Query("SELECT * FROM lab_tests WHERE status='RUNNING' ORDER BY id DESC LIMIT 1") suspend fun active(): LabTest?
 @Query("SELECT * FROM lab_tests WHERE id=:id LIMIT 1") suspend fun byId(id: Long): LabTest?
 @Insert suspend fun insert(test: LabTest): Long
 @Update suspend fun update(test: LabTest)
 @Query("DELETE FROM lab_tests WHERE id = :id AND status != 'RUNNING'") suspend fun deleteFinished(id: Long): Int
 @Query("DELETE FROM lab_tests WHERE id NOT IN (SELECT id FROM lab_tests ORDER BY id DESC LIMIT 100)") suspend fun trim()
 @Query("SELECT * FROM lab_tests ORDER BY id ASC") suspend fun exportAll(): List<LabTest>
 @Insert suspend fun insertAll(items: List<LabTest>)
 @Query("DELETE FROM lab_tests") suspend fun clear()
}

@Dao
interface SessionDao {
 @Query("SELECT * FROM sessions ORDER BY id DESC LIMIT 500") fun observe(): Flow<List<Session>>
 /** Somente leituras reais da última semana; até o limite já imposto de 30 mil amostras. */
 @Query("SELECT * FROM telemetry_samples WHERE timestamp >= :since ORDER BY timestamp ASC, id ASC LIMIT 30000")
 fun observeWeeklyTelemetry(since: Long): Flow<List<TelemetrySample>>
 @Query("SELECT * FROM telemetry_samples ORDER BY id DESC LIMIT 1500")
 fun observeRecentTelemetry(): Flow<List<TelemetrySample>>
 @Query("SELECT * FROM telemetry_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC LIMIT 1440")
 fun observeSessionTelemetry(sessionId: Long): Flow<List<TelemetrySample>>
 @Query("UPDATE sessions SET chargerLabel = :label WHERE id = :id AND mode = 'CHARGING'")
 suspend fun setChargerLabel(id: Long, label: String?): Int
 @Query("SELECT * FROM telemetry_samples ORDER BY sessionId ASC, id ASC") suspend fun exportTelemetry(): List<TelemetrySample>
 @Query("DELETE FROM telemetry_samples") suspend fun clearTelemetry()
 @Insert suspend fun insertTelemetry(items: List<TelemetrySample>)
 @Insert suspend fun insertTelemetry(item: TelemetrySample)
 @Query("DELETE FROM telemetry_samples WHERE sessionId = :sessionId AND id NOT IN (SELECT id FROM telemetry_samples WHERE sessionId=:sessionId ORDER BY id DESC LIMIT 1440)")
 suspend fun trimTelemetrySession(sessionId: Long)
 @Query("DELETE FROM telemetry_samples WHERE id NOT IN (SELECT id FROM telemetry_samples ORDER BY id DESC LIMIT 30000)")
 suspend fun trimTelemetryGlobal()
 @Query("UPDATE sessions SET closed = 1, reason = 'Observação interrompida' WHERE closed = 0") suspend fun closeInterrupted()
 @Query("SELECT * FROM session_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
 fun observeSamples(sessionId: Long): Flow<List<SessionSample>>
 /** Consulta limitada para a análise local de até 24h, sem alterar schema nem retenção. */
 @Query("SELECT * FROM session_samples WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp ASC, id ASC LIMIT 8000")
 suspend fun batterySamplesWindow(start: Long, end: Long): List<SessionSample>
 /** Resumo, amostra e retenção são confirmados juntos, ou revertidos juntos. */
 @Transaction
 suspend fun saveObservation(session: Session, sample: SessionSample?, telemetry: TelemetrySample?=null): Session {
  val stored=if(session.id==0L) session.copy(id=insert(session)) else session.also { update(it) }
  if(sample!=null) { insertSample(sample.copy(sessionId=stored.id)); trimSamples(stored.id) }
  if(telemetry!=null) {
   insertTelemetry(telemetry.copy(sessionId=stored.id))
   trimTelemetrySession(stored.id)
   if(session.id==0L) trimTelemetryGlobal()
  }
  if(session.id==0L) trim()
  return stored
 }
 @Insert suspend fun insertSample(sample: SessionSample)
 @Insert suspend fun insertSamples(samples: List<SessionSample>)
 @Query("SELECT * FROM sessions ORDER BY id ASC") suspend fun exportSessions(): List<Session>
 @Query("SELECT * FROM session_samples ORDER BY sessionId ASC, id ASC") suspend fun exportSamples(): List<SessionSample>
 @Query("DELETE FROM session_samples") suspend fun clearSamples()
 @Query("DELETE FROM sessions") suspend fun clearSessions()
 // Manter o primeiro ponto e os 479 mais recentes evita crescimento sem limite por sessão.
 @Query("""DELETE FROM session_samples WHERE sessionId = :sessionId
 AND id != (SELECT MIN(id) FROM session_samples WHERE sessionId = :sessionId)
 AND id NOT IN (SELECT id FROM session_samples WHERE sessionId = :sessionId ORDER BY id DESC LIMIT 479)""")
 suspend fun trimSamples(sessionId: Long)
 @Insert suspend fun insert(session: Session): Long
 @Update suspend fun update(session: Session)
 @Query("DELETE FROM sessions WHERE id NOT IN (SELECT id FROM sessions ORDER BY id DESC LIMIT 500)") suspend fun trim()
}
@Database(entities = [Session::class, SessionSample::class, TelemetrySample::class, LabTest::class], version = 8, exportSchema = true)
abstract class BatteryDatabase: RoomDatabase() {
 abstract fun sessions(): SessionDao
 abstract fun labs(): LabDao
 companion object {
  val MIGRATION_7_8 = object : Migration(7, 8) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `initialTemperatureC` REAL")
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `finalTemperatureC` REAL")
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `maxTemperatureC` REAL")
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `screenCondition` TEXT NOT NULL DEFAULT 'NAO_INFORMADA'")
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `cameraReported` INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `wifiReported` INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `recordingReported` INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''")
   }
  }
  // Mantém todos os testes solares anteriores; as novas colunas têm defaults legados.
  val MIGRATION_6_7 = object : Migration(6, 7) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'SOLAR'")
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `externalSeen` INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE `lab_tests` ADD COLUMN `lastPlugged` INTEGER")
   }
  }
  val MIGRATION_5_6 = object : Migration(5, 6) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("""CREATE TABLE IF NOT EXISTS `lab_tests` (
     `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
     `sky` TEXT NOT NULL, `skyChanges` TEXT NOT NULL, `panelWatts` INTEGER,
     `targetMs` INTEGER, `startedAt` INTEGER NOT NULL, `deadlineAt` INTEGER,
     `endedAt` INTEGER, `status` TEXT NOT NULL, `initialPercent` INTEGER,
     `finalPercent` INTEGER, `chargingMs` INTEGER NOT NULL, `notChargingMs` INTEGER NOT NULL,
     `weightedMaMs` REAL NOT NULL, `validCurrentMs` INTEGER NOT NULL,
     `lastReadingAt` INTEGER, `lastElapsedMs` INTEGER, `lastMode` TEXT,
     `lastCurrentMa` REAL, `lastCurrentSource` TEXT
    )""")
   }
  }
  // Acrescenta apenas a tabela nova. Os resumos da v1 permanecem intocados.
  val MIGRATION_1_2 = object : Migration(1, 2) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("""CREATE TABLE IF NOT EXISTS `session_samples` (
      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      `sessionId` INTEGER NOT NULL,
      `timestamp` INTEGER NOT NULL,
      `percent` INTEGER NOT NULL,
      FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
     )""")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_samples_sessionId` ON `session_samples` (`sessionId`)")
   }
  }
  val MIGRATION_4_5 = object : Migration(4, 5) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE `sessions` ADD COLUMN `chargeSource` TEXT")
    db.execSQL("ALTER TABLE `sessions` ADD COLUMN `chargerLabel` TEXT")
   }
  }
  val MIGRATION_3_4 = object : Migration(3, 4) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("""CREATE TABLE IF NOT EXISTS `telemetry_samples` (
      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      `sessionId` INTEGER NOT NULL,
      `timestamp` INTEGER NOT NULL,
      `elapsedMs` INTEGER NOT NULL,
      `mode` TEXT NOT NULL,
      `percent` INTEGER NOT NULL,
      `currentMa` REAL,
      `currentSource` TEXT,
      `temperatureC` REAL,
      `counterUah` INTEGER,
      FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
     )""")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_telemetry_samples_sessionId` ON `telemetry_samples` (`sessionId`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_telemetry_samples_timestamp` ON `telemetry_samples` (`timestamp`)")
   }
  }
  val MIGRATION_2_3 = object : Migration(2, 3) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE `session_samples` ADD COLUMN `currentMa` REAL")
    db.execSQL("ALTER TABLE `session_samples` ADD COLUMN `temperatureC` REAL")
    db.execSQL("ALTER TABLE `session_samples` ADD COLUMN `currentSource` TEXT")
   }
  }
 }
}
