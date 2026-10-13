#!/usr/bin/env python3
"""Execute the actual migration/retention SQL from Store.kt using SQLite.
This is not an Android Room schema validation or an instrumented migration test.
"""
from pathlib import Path
import re
import sqlite3
import unittest

ROOT=Path(__file__).resolve().parents[1]
STORE=(ROOT/'app/src/main/java/br/com/pulsobateria/data/Store.kt').read_text()
FIXTURE=(ROOT/'app/src/androidTest/java/br/com/pulsobateria/data/RoomMigrationTest.kt').read_text()
CREATE_SESSION=re.search(r'legacy.execSQL\("""(CREATE TABLE IF NOT EXISTS `sessions`.*?)"""\)',FIXTURE,re.S)[1]

def migration(db, start):
 section=STORE.split(f'val MIGRATION_{start}_{start+1} =',1)[1].split('val MIGRATION_',1)[0]
 sql=re.findall(r'db.execSQL\((?:"""(.*?)"""|"([^"\n]*)")\)',section,re.S)
 for triple,single in sql: db.execute(triple or single)

def schema(db, version):
 db.execute('PRAGMA foreign_keys=ON')
 db.execute(CREATE_SESSION)
 db.execute("INSERT INTO sessions VALUES (1,'CHARGING',1000,2000,1000,40,41,30.0,1,2,1,'Teste')")
 if version>=2:
  migration(db,1)
  db.execute('INSERT INTO session_samples(id,sessionId,timestamp,percent) VALUES(1,1,1000,40)')
 if version>=3:
  migration(db,2)
  db.execute("UPDATE session_samples SET currentMa=437.0,temperatureC=38.9,currentSource='NOW'")
 db.commit()

class StorageSQLTest(unittest.TestCase):
 def test_v1_to_v3_preserves_session(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,1); before=db.execute('SELECT * FROM sessions').fetchall()
   migration(db,1); migration(db,2)
   self.assertEqual(before,db.execute('SELECT * FROM sessions').fetchall())
   self.assertEqual([],db.execute('PRAGMA foreign_key_check').fetchall())
 def test_v2_to_v3_preserves_points_without_fabricating_current(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,2); migration(db,2)
   self.assertEqual((1,1,1000,40,None,None,None),db.execute('SELECT * FROM session_samples').fetchone())
 def test_v3_values_remain_after_adding_telemetry(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,3)
   self.assertEqual(437.0,db.execute('SELECT currentMa FROM session_samples').fetchone()[0])
   self.assertIn('version = 5',STORE)
 def test_foreign_key_cascades(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,3); db.execute('DELETE FROM sessions WHERE id=1')
   self.assertEqual(0,db.execute('SELECT COUNT(*) FROM session_samples').fetchone()[0])
 def test_orphan_is_rejected(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,3)
   with self.assertRaises(sqlite3.IntegrityError):
    db.execute('INSERT INTO session_samples(sessionId,timestamp,percent) VALUES(99,1000,50)')
 def test_retention_keeps_first_and_last_479(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,3)
   db.executemany('INSERT INTO session_samples(sessionId,timestamp,percent) VALUES(1,?,50)',[(i,) for i in range(2,601)])
   sql=re.search(r'@Query\("""(DELETE FROM session_samples.*?)"""\)',STORE,re.S)[1]
   db.execute(sql,{'sessionId':1})
   ids=[r[0] for r in db.execute('SELECT id FROM session_samples ORDER BY id')]
   self.assertEqual([1]+list(range(122,601)),ids)
 def test_summary_retention_and_cascade(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,3)
   for n in range(2,502):
    db.execute("INSERT INTO sessions VALUES (?,'CHARGING',1000,2000,1000,40,41,30.0,1,2,1,'Teste')",(n,))
   sql=re.search(r'@Query\("(DELETE FROM sessions WHERE id NOT IN.*?)"\)',STORE)[1]
   db.execute(sql)
   self.assertEqual(500,db.execute('SELECT COUNT(*) FROM sessions').fetchone()[0])
   self.assertEqual(0,db.execute('SELECT COUNT(*) FROM session_samples').fetchone()[0])
 def test_v3_to_v4_preserves_old_history(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,3)
   before=db.execute('SELECT * FROM sessions').fetchall()
   points=db.execute('SELECT * FROM session_samples').fetchall()
   migration(db,3)
   self.assertEqual(before,db.execute('SELECT * FROM sessions').fetchall())
   self.assertEqual(points,db.execute('SELECT * FROM session_samples').fetchall())
   self.assertEqual([],db.execute('SELECT * FROM telemetry_samples').fetchall())
   self.assertEqual([],db.execute('PRAGMA foreign_key_check').fetchall())
 def test_v4_to_v5_preserves_sessions_and_telemetry(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,3); migration(db,3)
   db.execute("INSERT INTO telemetry_samples (sessionId,timestamp,elapsedMs,mode,percent) VALUES(1,1500,1500,'CHARGING',41)")
   before=db.execute('SELECT * FROM telemetry_samples').fetchall()
   migration(db,4)
   self.assertEqual(before,db.execute('SELECT * FROM telemetry_samples').fetchall())
   self.assertEqual((None,None),db.execute('SELECT chargeSource,chargerLabel FROM sessions WHERE id=1').fetchone())
   db.execute("UPDATE sessions SET chargeSource='USB',chargerLabel='Carregador de casa' WHERE id=1")
   self.assertEqual(('USB','Carregador de casa'),db.execute('SELECT chargeSource,chargerLabel FROM sessions').fetchone())
   self.assertEqual([],db.execute('PRAGMA foreign_key_check').fetchall())
 def test_v1_to_v5_retains_old_history(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,1)
   for v in range(1,5): migration(db,v)
   self.assertEqual((41,None,None),db.execute('SELECT final,chargeSource,chargerLabel FROM sessions').fetchone())
   self.assertEqual([],db.execute('SELECT * FROM session_samples').fetchall())

 def test_telemetry_retention_and_cascade(self):
  with sqlite3.connect(':memory:') as db:
   schema(db,3); migration(db,3)
   db.executemany("INSERT INTO telemetry_samples(sessionId,timestamp,elapsedMs,mode,percent) VALUES(1,?,?,'CHARGING',50)",[(i,i) for i in range(1500)])
   sql=re.search(r'@Query\("(DELETE FROM telemetry_samples WHERE sessionId.*?)"\)',STORE)[1]
   db.execute(sql,{'sessionId':1})
   self.assertEqual(1440,db.execute('SELECT COUNT(*) FROM telemetry_samples').fetchone()[0])
   db.execute('DELETE FROM sessions WHERE id=1')
   self.assertEqual(0,db.execute('SELECT COUNT(*) FROM telemetry_samples').fetchone()[0])
 def test_transaction_rolls_back_both_changes(self):
  db=sqlite3.connect(':memory:'); schema(db,3)
  try:
   with self.assertRaises(sqlite3.IntegrityError):
    with db:
     db.execute('UPDATE sessions SET final=90 WHERE id=1')
     db.execute('INSERT INTO session_samples(id,sessionId,timestamp,percent) VALUES(1,1,1100,90)')
   self.assertEqual(41,db.execute('SELECT final FROM sessions').fetchone()[0])
   self.assertEqual(1,db.execute('SELECT COUNT(*) FROM session_samples').fetchone()[0])
  finally: db.close()

if __name__=='__main__': unittest.main(verbosity=2)
