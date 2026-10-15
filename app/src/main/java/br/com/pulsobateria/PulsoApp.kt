package br.com.pulsobateria

import android.app.Application
import androidx.room.Room
import br.com.pulsobateria.data.*
import br.com.pulsobateria.monitor.BatteryRepository
import br.com.pulsobateria.labs.LabController
import br.com.pulsobateria.monitor.SmartAlertDispatcher
import br.com.pulsobateria.usage.UsageInsightsReader
import kotlinx.coroutines.*

class PulsoApp: Application() {
 val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
 lateinit var settings: SettingsStore; private set
 lateinit var repository: BatteryRepository; private set
 lateinit var backup: BackupManager; private set
 lateinit var usageInsights: UsageInsightsReader; private set
 lateinit var labs: LabController; private set
 override fun onCreate() {
  super.onCreate()
  settings = SettingsStore(this)
  val database = Room.databaseBuilder(this, BatteryDatabase::class.java, "pulso.db").addMigrations(BatteryDatabase.MIGRATION_1_2, BatteryDatabase.MIGRATION_2_3, BatteryDatabase.MIGRATION_3_4, BatteryDatabase.MIGRATION_4_5, BatteryDatabase.MIGRATION_5_6, BatteryDatabase.MIGRATION_6_7).build()
  backup = BackupManager(this, database, settings)
  repository = BatteryRepository(this, database.sessions(), settings, scope)
  usageInsights = UsageInsightsReader(this, database.sessions())
  labs = LabController(this, database.labs(), repository, scope)
  SmartAlertDispatcher(this, repository, settings, scope).start()
 }
}
