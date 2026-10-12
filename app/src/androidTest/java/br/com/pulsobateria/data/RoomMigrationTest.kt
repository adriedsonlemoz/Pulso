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
   if(from==2) {
    legacy.execSQL("""CREATE TABLE IF NOT EXISTS `session_samples` (
     `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
     `sessionId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `percent` INTEGER NOT NULL,
     FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
    )""")
    legacy.execSQL("CREATE INDEX `index_session_samples_sessionId` ON `session_samples` (`sessionId`)")
    legacy.execSQL("INSERT INTO session_samples (id,sessionId,timestamp,percent) VALUES (1,1,1000,40)")
   }
   legacy.version=from
  } finally { legacy.close() }
  val db=Room.databaseBuilder(context,BatteryDatabase::class.java,name)
   .addMigrations(BatteryDatabase.MIGRATION_1_2,BatteryDatabase.MIGRATION_2_3).build()
  try {
   val restored=db.sessions().exportSessions()
   assertEquals(1,restored.size)
   assertEquals(41,restored.single().final)
   assertEquals("Teste",restored.single().reason)
   val points=db.sessions().observeSamples(1).first()
   assertEquals(if(from==2) 1 else 0,points.size)
   assertTrue(points.all { it.currentMa==null && it.temperatureC==null && it.currentSource==null })
  } finally {
   db.close()
   context.deleteDatabase(name)
  }
 }
}
