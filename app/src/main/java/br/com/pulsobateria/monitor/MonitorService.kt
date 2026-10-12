package br.com.pulsobateria.monitor

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import br.com.pulsobateria.*
import br.com.pulsobateria.ui.*
import br.com.pulsobateria.core.Mode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect

class MonitorService: Service() {
 private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
 private val app get() = application as PulsoApp
 private var started = false
 override fun onBind(intent: Intent?): IBinder? = null
 override fun onCreate() {
  super.onCreate()
  getSystemService(NotificationManager::class.java).createNotificationChannel(
   NotificationChannel("battery", "Monitoramento de bateria", NotificationManager.IMPORTANCE_LOW).apply { setSound(null,null); enableVibration(false) })
 }
 override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
  if (intent?.action == "STOP") {
   app.scope.launch { app.settings.continuous(false) }
   stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY
  }
  if (started) return START_NOT_STICKY
  if (!NotificationManagerCompat.from(this).areNotificationsEnabled() || (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) {
   fail("Permita as notificações para ativar o monitoramento."); return START_NOT_STICKY
  }
  if (getSystemService(NotificationManager::class.java).getNotificationChannel("battery")?.importance == NotificationManager.IMPORTANCE_NONE) {
   fail("Ative o canal Monitoramento de bateria nas notificações do Android."); return START_NOT_STICKY
  }
  try {
   ServiceCompat.startForeground(this,1,notification("Iniciando leitura da bateria…"),
    if(Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
  } catch (_: RuntimeException) {
   fail("O Android não permitiu iniciar o monitoramento. Tente novamente com o aplicativo aberto."); return START_NOT_STICKY
  }
  started = true; app.repository.serviceActive.value = true; app.repository.serviceError.value = null
  app.repository.acquire("service")
  app.scope.launch { app.settings.continuous(true) }
  scope.launch {
   combine(app.repository.reading,app.settings.flow) { r,s ->
    val estimating = r.mode == Mode.CHARGING || r.mode == Mode.DISCHARGING
    val rate = if (estimating) rateText(r.rate) else "Taxa indisponível"
    val eta = if (estimating) durationText(r.remaining) else "Previsão indisponível"
    "${r.percent?.let { "$it%" } ?: "—"} • ${r.mode.label}\n${currentText(r.current)} • ${temperatureText(r.temperature,s.fahrenheit)}\n$rate • $eta"
   }.collect { text ->
    try { getSystemService(NotificationManager::class.java).notify(1,notification(text)) }
    catch (_: SecurityException) { fail("As notificações foram bloqueadas.") }
   }
  }
  return START_NOT_STICKY
 }
 private fun fail(message: String) {
  app.repository.serviceError.value = message
  app.scope.launch { app.settings.continuous(false) }
  stopSelf()
 }
 private fun notification(text: String): Notification {
  val open = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val stop = PendingIntent.getService(this,1,Intent(this,MonitorService::class.java).setAction("STOP"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return NotificationCompat.Builder(this,"battery").setSmallIcon(R.drawable.ic_notification)
   .setContentTitle("Pulso Bateria").setContentText(text.lineSequence().first())
   .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open)
   .setOnlyAlertOnce(true).setSilent(true).setOngoing(true).setShowWhen(false)
   .addAction(0,"Parar",stop).build()
 }
 override fun onDestroy() {
  scope.cancel(); app.repository.serviceActive.value = false
  app.repository.release("service"); stopForeground(STOP_FOREGROUND_REMOVE)
  super.onDestroy()
 }
}
