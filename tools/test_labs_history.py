#!/usr/bin/env python3
"""Labs history delete / UI static contracts and executable SQLite guard tests.
Does not replace a compiled Android/Compose integration test.
"""
from pathlib import Path
import sqlite3

root = Path(__file__).resolve().parents[1]
screen = (root/'app/src/main/java/br/com/pulsobateria/ui/LabsScreen.kt').read_text()
store = (root/'app/src/main/java/br/com/pulsobateria/data/Store.kt').read_text()
ctrl = (root/'app/src/main/java/br/com/pulsobateria/labs/LabController.kt').read_text()
vm = (root/'app/src/main/java/br/com/pulsobateria/ui/PulsoViewModel.kt').read_text()

checks = {
 "Date fields share one row": 'LabDateCell("Início"' in screen and 'LabDateCell("Fim"' in screen and 'Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp))' in screen,
 "Actions paired in footer": all(fragment in screen for fragment in ('Text("Recolher ↑")','Text("Copiar")','Text("Excluir")','onClick={confirmDelete=true}')),
 "Deletion requires confirmation": 'if(confirmDelete) AlertDialog(' in screen and 'onDelete(test.id)' in screen,
 "No delete on running card": 'onDelete' not in screen.split('private fun RunningLabCard(',1)[1].split('private fun LabResult(',1)[0],
 "Both recent and full history support deletion": screen.count('LabResult(test,onDelete=vm::deleteLab)')==2,
 "Missing observation warning": 'intervalos sem leitura não foram classificados' in screen and 'LabCoverage(total,confirmed)' in screen,
 "Solar preview includes battery": 'Text("Bateria: ${percentText(test.initialPercent)}' in screen,
 "SQL row-level non-running guard": 'DELETE FROM lab_tests WHERE id = :id AND status != \'RUNNING\'' in store,
 "Controller waits for initialization": 'suspend fun deleteFinished(id: Long): Boolean' in ctrl and 'ready.await()' in ctrl.split('suspend fun deleteFinished',1)[1].split('suspend fun stop',1)[0],
 "Controller checks active record": 'if(mutable.value?.id == id) return@withLock false' in ctrl,
 "UI invokes ViewModel": 'fun deleteLab(id: Long)' in vm and 'app.labs.deleteFinished(id)' in vm,
 "Copy exports the measured fields": 'reportText(test)' in screen and 'setPrimaryClip' in screen,
 "Thermal summary uses real values": all(x in screen for x in ('temperatureText(test.initialTemperatureC)','temperatureText(test.finalTemperatureC)','temperatureText(test.maxTemperatureC)')),
 "Room schema migration 7→8": '@Database(entities = [Session::class, SessionSample::class, TelemetrySample::class, LabTest::class], version = 8' in store,
}
for title, ok in checks.items():
 print(('OK' if ok else 'FAIL')+' — '+title)
assert all(checks.values()), 'Labs UI / deletion contract mismatch'

with sqlite3.connect(':memory:') as db:
 db.execute('CREATE TABLE lab_tests(id INTEGER PRIMARY KEY,kind TEXT NOT NULL,status TEXT NOT NULL)')
 db.executemany('INSERT INTO lab_tests(id,kind,status) VALUES(?,?,?)', [(1,'SOLAR','STOPPED'),(2,'DISCHARGE','COMPLETED'),(3,'SOLAR','RUNNING')])
 query="DELETE FROM lab_tests WHERE id = ? AND status != 'RUNNING'"
 for rid,expected in [(3,0),(2,1),(200,0),(1,1)]:
  result=db.execute(query,(rid,))
  assert result.rowcount==expected,(rid,result.rowcount)
  print('OK — delete id',rid,'affected',result.rowcount)
 assert db.execute('SELECT id FROM lab_tests').fetchall()==[(3,)]
 print('OK — running record preserved; only chosen completed IDs removed')
print('LABS HISTORY TESTS PASSED')
