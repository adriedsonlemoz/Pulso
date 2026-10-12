package br.com.pulsobateria

import android.app.Application
import androidx.room.Room
import br.com.pulsobateria.data.*
import br.com.pulsobateria.monitor.BatteryRepository
import kotlinx.coroutines.*

class PulsoApp: Application() {
 val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
 lateinit var settings: SettingsStore; private set
 lateinit var repository: BatteryRepository; private set
 override fun onCreate() {
  super.onCreate()
  settings = SettingsStore(this)
  val database = Room.databaseBuilder(this, BatteryDatabase::class.java, "pulso.db").build()
  repository = BatteryRepository(this, database.sessions(), settings, scope)
 }
}
