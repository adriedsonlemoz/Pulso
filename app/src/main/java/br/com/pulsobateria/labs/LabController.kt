package br.com.pulsobateria.labs

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import br.com.pulsobateria.MainActivity
import br.com.pulsobateria.R
import br.com.pulsobateria.core.LabMath
import br.com.pulsobateria.data.LabDao
import br.com.pulsobateria.data.LabTest
import br.com.pulsobateria.monitor.BatteryRepository
import br.com.pulsobateria.monitor.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Não cria um segundo foreground service: consome o mesmo monitoramento do app. */
class LabController(
 private val context: Context, private val dao: LabDao,
 private val repository: BatteryRepository, private val scope: CoroutineScope
) {
 private val mutex = Mutex()
 private val ready = CompletableDeferred<Unit>()
 private val mutable = MutableStateFlow<LabTest?>(null)
 val current = mutable.asStateFlow()
 val history = dao.observe()

 init {
  scope.launch {
   try {
    mutex.withLock { mutable.value = withContext(Dispatchers.IO) { dao.active() } }
   } finally { ready.complete(Unit) }
  }
  scope.launch {
   ready.await()
   repository.reading.collect { reading ->
    if (reading.updated > 0L) runCatching { observe(reading) }
   }
  }
  scope.launch {
   ready.await()
   while (isActive) {
    runCatching { completeIfExpired() }
    // Não depende da aba aberta. Alarmes exatos/wakelocks não são usados.
    delay(15_000L)
   }
  }
 }

 suspend fun start(sky: String, panelWatts: Int?, minutes: Int?) {
  require(sky in listOf("forte", "parcial", "nublado", "chuva"))
  require(panelWatts == null || panelWatts in 1..200)
  startInternal("SOLAR", sky, panelWatts, minutes)
 }

 /** Teste de descarga: sem fonte de energia externa no início. */
 suspend fun startDischarge(minutes: Int?) = startInternal("DISCHARGE", "", null, minutes)

 private suspend fun startInternal(kind: String, sky: String, panelWatts: Int?, minutes: Int?) {
  require(kind in setOf("SOLAR", "DISCHARGE"))
  require(minutes == null || minutes in 1..1440)
  ready.await()
  mutex.withLock {
   check(mutable.value == null) { "Já existe um teste em andamento." }
   check(repository.serviceActive.value) { "Ative o monitoramento em primeiro plano para testar com a tela apagada." }
   val now = System.currentTimeMillis()
   val reading = repository.reading.value.takeIf { it.updated in (now-90_000L)..now }
   if (kind == "DISCHARGE") {
    check(reading?.plugged == false && reading?.percent != null &&
     reading?.mode?.name in setOf("DISCHARGING", "FULL")) {
     "Desconecte o painel, carregador ou power bank e aguarde uma leitura recente da bateria."
    }
   }
   val targetMs = minutes?.toLong()?.times(60_000L)
   val test = LabTest(kind=kind, sky=sky,skyChanges=if(kind=="SOLAR") "$now:$sky" else "",
    panelWatts=panelWatts,targetMs=targetMs,startedAt=now,deadlineAt=targetMs?.let { now+it },
    initialPercent=reading?.percent, finalPercent=reading?.percent,
    lastReadingAt=null,lastElapsedMs=null,lastMode=null,lastCurrentMa=null,
    lastCurrentSource=null,externalSeen=false,lastPlugged=null)
   val id = withContext(Dispatchers.IO) { dao.insert(test).also { dao.trim() } }
   mutable.value = test.copy(id=id)
  }
 }

 suspend fun changeSky(sky: String) {
  require(sky in listOf("forte", "parcial", "nublado", "chuva"))
  ready.await()
  mutex.withLock {
   val test = mutable.value ?: return@withLock
   if (test.kind != "SOLAR") return@withLock
   if (test.sky == sky) return@withLock
   val now = System.currentTimeMillis()
   val updated = test.copy(sky=sky,skyChanges=(test.skyChanges+";$now:$sky").takeLast(3000))
   withContext(Dispatchers.IO) { dao.update(updated) }
   mutable.value = updated
  }
 }

 /** Exclui apenas resultados finalizados, sem tocar no teste ativo nem nos demais registros. */
 suspend fun deleteFinished(id: Long): Boolean {
  require(id > 0L)
  ready.await()
  return mutex.withLock {
   if(mutable.value?.id == id) return@withLock false
   withContext(Dispatchers.IO) { dao.deleteFinished(id) > 0 }
  }
 }

 suspend fun stop() {
  ready.await()
  mutex.withLock {
   val test = mutable.value ?: return@withLock
   val now=System.currentTimeMillis()
   val due=test.deadlineAt?.let { now>=it }==true
   finishLocked(test,if(due) test.deadlineAt!! else now,if(due) "COMPLETED" else "STOPPED")
  }
 }

 private suspend fun completeIfExpired() {
  mutex.withLock {
   val test = mutable.value ?: return@withLock
   val deadline = test.deadlineAt ?: return@withLock
   if (System.currentTimeMillis() >= deadline) finishLocked(test,deadline,"COMPLETED")
  }
 }

 private suspend fun observe(reading: Reading) {
  mutex.withLock {
   var test = mutable.value ?: return@withLock
   // Emissões antigas não podem atualizar nem prolongar um experimento.
   if (reading.updated <= (test.lastReadingAt ?: 0L) || reading.updated < test.startedAt) return@withLock
   if (test.deadlineAt != null && reading.updated >= test.deadlineAt) {
    finishLocked(test,test.deadlineAt,"COMPLETED")
    return@withLock
   }
   val elapsed = SystemClock.elapsedRealtime()
   val oldWall = test.lastReadingAt
   val oldElapsed = test.lastElapsedMs
   val interval = if(oldWall != null && oldElapsed != null && test.lastMode != null) {
    if(test.kind=="DISCHARGE") LabMath.classifyUnplugged(test.lastMode!!, reading.mode.name,
     test.lastPlugged, reading.plugged, elapsed-oldElapsed,reading.updated-oldWall)
    else LabMath.classify(test.lastMode!!,reading.mode.name,test.lastCurrentMa,reading.current,
     test.lastCurrentSource,reading.currentInfo.source.name, elapsed-oldElapsed,reading.updated-oldWall)
   } else LabMath.Interval()
   test = test.copy(
    initialPercent=test.initialPercent ?: reading.percent,
    finalPercent=reading.percent ?: test.finalPercent,
    chargingMs=test.chargingMs+interval.chargingMs,
    notChargingMs=test.notChargingMs+interval.notChargingMs,
    weightedMaMs=test.weightedMaMs+interval.weightedMaMs,
    validCurrentMs=test.validCurrentMs+interval.validCurrentMs,
    lastReadingAt=reading.updated, lastElapsedMs=elapsed,
    lastMode=reading.mode.name,lastCurrentMa=reading.current,
    lastCurrentSource=reading.currentInfo.source.name,
    lastPlugged=reading.plugged,
    externalSeen=test.externalSeen || (test.kind=="DISCHARGE" && reading.plugged==true))
   withContext(Dispatchers.IO) { dao.update(test) }
   mutable.value=test
  }
 }

 private suspend fun finishLocked(test: LabTest, endedAt: Long, status: String) {
  // Duração usa relógio civil para incluir períodos sem observação, sem atribuí-los a carga.
  val end = endedAt.coerceAtLeast(test.startedAt)
  val recent = repository.reading.value.takeIf { it.updated in test.startedAt..end &&
   it.updated >= (test.lastReadingAt ?: 0L) && it.updated >= end-90_000L }
  val saved = test.copy(endedAt=end,status=status,
   finalPercent=recent?.percent ?: test.finalPercent,
   lastReadingAt=recent?.updated ?: test.lastReadingAt,
   externalSeen=test.externalSeen || (test.kind=="DISCHARGE" && recent?.plugged==true))
  withContext(Dispatchers.IO) { dao.update(saved) }
  mutable.value=null
  if (status=="COMPLETED") notifyFinished(saved)
 }

 private fun notifyFinished(test: LabTest) {
  if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("labs","Conclusão de testes Labs",NotificationManager.IMPORTANCE_DEFAULT))
  val open=PendingIntent.getActivity(context,8,Intent(context,MainActivity::class.java).apply {
   putExtra("open_labs",true)
   flags=Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
  },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val total=(test.endedAt ?: test.startedAt)-test.startedAt
  val unknown=LabMath.unknownMs(total,test.chargingMs,test.notChargingMs)
  val content=if(test.kind=="DISCHARGE") {
   val drop=LabMath.dischargePoints(test.initialPercent,test.finalPercent,test.externalSeen)
   when {
    test.externalSeen -> "Fonte externa detectada; consulte o relatório (teste interferido)."
    drop!=null -> "Queda observada: $drop pontos percentuais • Sem leitura: ${unknown/60_000} min"
    else -> "Descarga não conclusiva; consulte o relatório."
   }
  } else "Carga: ${test.chargingMs/60_000} min • Sem carga: ${test.notChargingMs/60_000} min • Sem leitura: ${unknown/60_000} min"
  val title=if(test.kind=="DISCHARGE") "Labs: teste de consumo concluído" else "Labs: teste solar concluído"
  manager.notify(20_000+(test.id%10_000).toInt(),NotificationCompat.Builder(context,"labs")
   .setSmallIcon(R.drawable.ic_notification).setContentTitle(title)
   .setContentText(content).setStyle(NotificationCompat.BigTextStyle().bigText(content))
   .setContentIntent(open).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build())
 }
}
