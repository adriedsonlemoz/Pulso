package br.com.pulsobateria.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.*
import java.io.IOException

private val Context.preferences by preferencesDataStore("settings")
data class Settings(val theme: String = "system", val interval: Int = 30, val fahrenheit: Boolean = false, val continuous: Boolean = false, val capacityMah: Int? = null)
class SettingsStore(private val context: Context) {
 private val theme = stringPreferencesKey("theme")
 private val interval = intPreferencesKey("interval")
 private val fahrenheit = booleanPreferencesKey("fahrenheit")
 private val continuous = booleanPreferencesKey("continuous")
 private val capacity = intPreferencesKey("capacity_mah")
 val flow = context.preferences.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
  Settings(it[theme] ?: "system", (it[interval] ?: 30).coerceIn(15,60), it[fahrenheit] ?: false, it[continuous] ?: false,
   it[capacity]?.takeIf { mah -> mah in 500..30_000 })
 }
 suspend fun theme(value: String) { context.preferences.edit { it[theme] = value } }
 suspend fun interval(value: Int) { context.preferences.edit { it[interval] = value.coerceIn(15,60) } }
 suspend fun fahrenheit(value: Boolean) { context.preferences.edit { it[fahrenheit] = value } }
 suspend fun continuous(value: Boolean) { context.preferences.edit { it[continuous] = value } }
 suspend fun capacityMah(value: Int?) { require(value == null || value in 500..30_000); context.preferences.edit { if (value == null) it.remove(capacity) else it[capacity] = value } }
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
 val sessionId: Long, val timestamp: Long, val percent: Int
)

@Dao
interface SessionDao {
 @Query("SELECT * FROM sessions ORDER BY id DESC LIMIT 500") fun observe(): Flow<List<Session>>
 @Query("UPDATE sessions SET closed = 1, reason = 'Observação interrompida' WHERE closed = 0") suspend fun closeInterrupted()
 @Query("SELECT * FROM session_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
 fun observeSamples(sessionId: Long): Flow<List<SessionSample>>
 @Insert suspend fun insertSample(sample: SessionSample)
 // Manter o primeiro ponto e os 479 mais recentes evita crescimento sem limite por sessão.
 @Query("""DELETE FROM session_samples WHERE sessionId = :sessionId
 AND id != (SELECT MIN(id) FROM session_samples WHERE sessionId = :sessionId)
 AND id NOT IN (SELECT id FROM session_samples WHERE sessionId = :sessionId ORDER BY id DESC LIMIT 479)""")
 suspend fun trimSamples(sessionId: Long)
 @Insert suspend fun insert(session: Session): Long
 @Update suspend fun update(session: Session)
 @Query("DELETE FROM sessions WHERE id NOT IN (SELECT id FROM sessions ORDER BY id DESC LIMIT 500)") suspend fun trim()
}
@Database(entities = [Session::class, SessionSample::class], version = 2, exportSchema = true)
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
 }
}
