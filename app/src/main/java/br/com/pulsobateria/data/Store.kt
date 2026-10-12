package br.com.pulsobateria.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.*
import java.io.IOException

private val Context.preferences by preferencesDataStore("settings")
data class Settings(val theme: String = "system", val interval: Int = 30, val fahrenheit: Boolean = false, val continuous: Boolean = false)
class SettingsStore(private val context: Context) {
 private val theme = stringPreferencesKey("theme")
 private val interval = intPreferencesKey("interval")
 private val fahrenheit = booleanPreferencesKey("fahrenheit")
 private val continuous = booleanPreferencesKey("continuous")
 val flow = context.preferences.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
  Settings(it[theme] ?: "system", (it[interval] ?: 30).coerceIn(15,60), it[fahrenheit] ?: false, it[continuous] ?: false)
 }
 suspend fun theme(value: String) { context.preferences.edit { it[theme] = value } }
 suspend fun interval(value: Int) { context.preferences.edit { it[interval] = value.coerceIn(15,60) } }
 suspend fun fahrenheit(value: Boolean) { context.preferences.edit { it[fahrenheit] = value } }
 suspend fun continuous(value: Boolean) { context.preferences.edit { it[continuous] = value } }
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
@Dao
interface SessionDao {
 @Query("SELECT * FROM sessions ORDER BY id DESC LIMIT 500") fun observe(): Flow<List<Session>>
 @Query("UPDATE sessions SET closed = 1, reason = 'Observação interrompida' WHERE closed = 0") suspend fun closeInterrupted()
 @Insert suspend fun insert(session: Session): Long
 @Update suspend fun update(session: Session)
 @Query("DELETE FROM sessions WHERE id NOT IN (SELECT id FROM sessions ORDER BY id DESC LIMIT 500)") suspend fun trim()
}
@Database(entities = [Session::class], version = 1, exportSchema = true)
abstract class BatteryDatabase: RoomDatabase() { abstract fun sessions(): SessionDao }
