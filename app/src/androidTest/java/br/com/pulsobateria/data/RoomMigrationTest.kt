package br.com.pulsobateria.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumentado: rodar em emulador/dispositivo via :app:connectedDebugAndroidTest. */
@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {
 @Test fun migracaoDaVersao1PreservaResumo() = checkMigration(1)
 @Test fun migracaoDaVersao2PreservaResumoEGrafico() = checkMigration(2)
 @Test fun migracaoDaVersao3PreservaMedicoes() = checkMigration(3)
 @Test fun migracaoDaVersao4PreservaTelemetria() = checkMigration(4)
 @Test fun migracaoDaVersao5PreservaFonte() = checkMigration(5)

 private fun checkMigration(from: Int) = runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="migration-test-$from.db"
  context.deleteDatabase(name)
  val file=context.getDatabasePath(name)
  file.parentFile?.mkdirs()
  val legacy=SQLiteDatabase.openOrCreateDatabase(file,null)
  try {
   legacy.execSQL("""CREATE TABLE IF NOT EXISTS `sessions` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `mode` TEXT NOT NULL, `started` INTEGER NOT NULL, `ended` INTEGER NOT NULL,
    `durationMs` INTEGER NOT NULL, `initial` INTEGER NOT NULL, `final` INTEGER NOT NULL,
    `temperatureSum` REAL NOT NULL, `temperatureCount` INTEGER NOT NULL,
    `samples` INTEGER NOT NULL, `closed` INTEGER NOT NULL, `reason` TEXT NOT NULL
   )""")
   legacy.execSQL("INSERT INTO sessions (id,mode,started,ended,durationMs,initial,final,temperatureSum,temperatureCount,samples,closed,reason) VALUES (1,'CHARGING',1000,2000,1000,40,41,30.0,1,2,1,'Teste')")
   if(from>=2) {
    legacy.execSQL("""CREATE TABLE IF NOT EXISTS `session_samples` (
     `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
     `sessionId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `percent` INTEGER NOT NULL,
     FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
    )""")
    legacy.execSQL("CREATE INDEX `index_session_samples_sessionId` ON `session_samples` (`sessionId`)")
    legacy.execSQL("INSERT INTO session_samples (id,sessionId,timestamp,percent) VALUES (1,1,1000,40)")
   }
   if(from>=3) {
    legacy.execSQL("ALTER TABLE `session_samples` ADD COLUMN `currentMa` REAL")
    legacy.execSQL("ALTER TABLE `session_samples` ADD COLUMN `temperatureC` REAL")
    legacy.execSQL("ALTER TABLE `session_samples` ADD COLUMN `currentSource` TEXT")
    legacy.execSQL("UPDATE session_samples SET currentMa=437.0,temperatureC=38.9,currentSource='NOW'")
   }
   if(from>=4) {
    legacy.execSQL("""CREATE TABLE IF NOT EXISTS `telemetry_samples` (
     `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
     `sessionId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL,
     `elapsedMs` INTEGER NOT NULL, `mode` TEXT NOT NULL, `percent` INTEGER NOT NULL,
     `currentMa` REAL, `currentSource` TEXT, `temperatureC` REAL, `counterUah` INTEGER,
     FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
    )""")
    legacy.execSQL("CREATE INDEX `index_telemetry_samples_sessionId` ON `telemetry_samples` (`sessionId`)")
    legacy.execSQL("CREATE INDEX `index_telemetry_samples_timestamp` ON `telemetry_samples` (`timestamp`)")
   }
   if(from>=5) {
    legacy.execSQL("ALTER TABLE sessions ADD COLUMN chargeSource TEXT")
    legacy.execSQL("ALTER TABLE sessions ADD COLUMN chargerLabel TEXT")
   }
   legacy.version=from
  } finally { legacy.close() }
  val db=Room.databaseBuilder(context,BatteryDatabase::class.java,name)
   .addMigrations(BatteryDatabase.MIGRATION_1_2,BatteryDatabase.MIGRATION_2_3,BatteryDatabase.MIGRATION_3_4,BatteryDatabase.MIGRATION_4_5,BatteryDatabase.MIGRATION_5_6).build()
  try {
   assertTrue(db.sessions().exportTelemetry().isEmpty())
   assertTrue(db.labs().exportAll().isEmpty())
   val restored=db.sessions().exportSessions()
   assertEquals(1,restored.size)
   assertEquals(41,restored.single().final)
   assertEquals("Teste",restored.single().reason)
   assertNull(restored.single().chargeSource)
   assertNull(restored.single().chargerLabel)
   val points=db.sessions().observeSamples(1).first()
   assertEquals(if(from>=2) 1 else 0,points.size)
   if(from>=3) {
    assertEquals(437.0,points.single().currentMa!!,0.0)
    assertEquals(38.9,points.single().temperatureC!!,0.0)
    assertEquals("NOW",points.single().currentSource)
   } else assertTrue(points.all { it.currentMa==null && it.temperatureC==null && it.currentSource==null })
  } finally {
   db.close()
   context.deleteDatabase(name)
  }
 }
 @Test fun falhaNaAmostraReverteResumoNaMesmaTransacao() = runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val db=Room.inMemoryDatabaseBuilder(context,BatteryDatabase::class.java).build()
  try {
   val dao=db.sessions()
   val original=dao.saveObservation(Session(mode="CHARGING",started=1000,ended=1000,durationMs=0,
    initial=40,final=40,temperatureSum=30.0,temperatureCount=1,samples=1),
    SessionSample(id=1,sessionId=0,timestamp=1000,percent=40))
   val failure=runCatching {
    dao.saveObservation(original.copy(ended=2000,final=41),
     SessionSample(id=1,sessionId=original.id,timestamp=2000,percent=41))
   }.exceptionOrNull()
   assertNotNull(failure)
   assertEquals(40,dao.exportSessions().single().final)
   assertEquals(40,dao.exportSamples().single().percent)
  } finally { db.close() }
 }
 @Test fun telemetriaPersistidaEExcluidaEmCascata() = runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val db=Room.inMemoryDatabaseBuilder(context,BatteryDatabase::class.java).build()
  try {
   val dao=db.sessions()
   val session=dao.saveObservation(Session(mode="DISCHARGING",started=1000,ended=1000,durationMs=0,
    initial=60,final=60,temperatureSum=36.0,temperatureCount=1,samples=1),null,
    TelemetrySample(sessionId=0,timestamp=1000,elapsedMs=1000,mode="DISCHARGING",percent=60,
     currentMa=-350.0,currentSource="NOW",temperatureC=36.0,counterUah=2_500_000))
   assertEquals(1,dao.exportTelemetry().size)
   assertEquals(session.id,dao.exportTelemetry().single().sessionId)
   dao.clearSessions()
   assertTrue(dao.exportTelemetry().isEmpty())
  } finally { db.close() }
 }

}

