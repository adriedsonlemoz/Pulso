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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Context.preferences by preferencesDataStore("settings")
data class Settings(val theme: String = "system", val interval: Int = 30, val fahrenheit: Boolean = false, val continuous: Boolean = false, val capacityMah: Int? = null, val calibration: String? = null, val startOnBoot: Boolean = false)
class SettingsStore(private val context: Context) {
 private val theme = stringPreferencesKey("theme")
 private val interval = intPreferencesKey("interval")
 private val fahrenheit = booleanPreferencesKey("fahrenheit")
 private val continuous = booleanPreferencesKey("continuous")
 private val startOnBoot = booleanPreferencesKey("start_on_boot")
 private val capacity = intPreferencesKey("capacity_mah")
 private val calibration = stringPreferencesKey("current_calibration")
 val flow = context.preferences.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
  Settings(it[theme] ?: "system", (it[interval] ?: 30).coerceIn(15,60), it[fahrenheit] ?: false, it[continuous] ?: false,
   it[capacity]?.takeIf { mah -> mah in 500..30_000 }, it[calibration], it[startOnBoot] ?: false)
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
 val reason: String = "Em observação"
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

@Dao
interface SessionDao {
 @Query("SELECT * FROM sessions ORDER BY id DESC LIMIT 500") fun observe(): Flow<List<Session>>
 @Query("UPDATE sessions SET closed = 1, reason = 'Observação interrompida' WHERE closed = 0") suspend fun closeInterrupted()
 @Query("SELECT * FROM session_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
 fun observeSamples(sessionId: Long): Flow<List<SessionSample>>
 /** Resumo, amostra e retenção são confirmados juntos, ou revertidos juntos. */
 @Transaction
 suspend fun saveObservation(session: Session, sample: SessionSample?): Session {
  val stored=if(session.id==0L) session.copy(id=insert(session)) else session.also { update(it) }
  if(sample!=null) { insertSample(sample.copy(sessionId=stored.id)); trimSamples(stored.id) }
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
@Database(entities = [Session::class, SessionSample::class], version = 3, exportSchema = true)
abstract class BatteryDatabase: RoomDatabase() {
 abstract fun sessions(): SessionDao
 companion object {
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
  val MIGRATION_2_3 = object : Migration(2, 3) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE `session_samples` ADD COLUMN `currentMa` REAL")
    db.execSQL("ALTER TABLE `session_samples` ADD COLUMN `temperatureC` REAL")
    db.execSQL("ALTER TABLE `session_samples` ADD COLUMN `currentSource` TEXT")
   }
  }
 }
}
