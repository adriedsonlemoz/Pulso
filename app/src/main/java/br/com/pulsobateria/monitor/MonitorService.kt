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
import br.com.pulsobateria.MainActivity
import br.com.pulsobateria.PulsoApp
import br.com.pulsobateria.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MonitorService: Service() {
 private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
 private val app get() = application as PulsoApp
 private var started = false

 override fun onBind(intent: Intent?): IBinder? = null

 override fun onCreate() {
  super.onCreate()
  getSystemService(NotificationManager::class.java).createNotificationChannel(
   NotificationChannel("battery", "Monitoramento de bateria", NotificationManager.IMPORTANCE_LOW).apply {
    setSound(null,null)
    enableVibration(false)
   }
  )
 }

 override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
  if (intent?.action == "STOP") {
   app.scope.launch { app.settings.continuous(false) }
   stopForeground(STOP_FOREGROUND_REMOVE)
   stopSelf()
   return START_NOT_STICKY
  }
  if (started) return START_NOT_STICKY
  if (!NotificationManagerCompat.from(this).areNotificationsEnabled() ||
   (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) {
   fail("Permita as notificações para ativar o monitoramento.")
   return START_NOT_STICKY
  }
  if (getSystemService(NotificationManager::class.java).getNotificationChannel("battery")?.importance == NotificationManager.IMPORTANCE_NONE) {
   fail("Ative o canal Monitoramento de bateria nas notificações do Android.")
   return START_NOT_STICKY
  }
  try {
   ServiceCompat.startForeground(
    this,
    1,
    notification(BatteryNotificationText("Pulso Bateria", "Iniciando leitura da bateria…", "Iniciando leitura da bateria…")),
    if(Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
   )
  } catch (_: RuntimeException) {
   fail("O Android não permitiu iniciar o monitoramento. Tente novamente com o aplicativo aberto.")
   return START_NOT_STICKY
  }

  started = true
  app.repository.serviceActive.value = true
  app.repository.serviceError.value = null
  app.repository.acquire("service")
  app.scope.launch { app.settings.continuous(true) }

  scope.launch {
   combine(app.repository.reading, app.settings.flow) { reading, settings ->
    BatteryNotificationFormatter.format(
     percent = reading.percent,
     temperatureC = reading.temperature,
     fahrenheit = settings.fahrenheit,
     mode = reading.mode,
     remainingMs = reading.remaining,
     currentMa = reading.current,
     source = reading.currentInfo.source,
     counterMeanMa = reading.currentInfo.counterMeanMa,
     ratePercentPerHour = reading.rate,
     nowEpochMillis = reading.updated.takeIf { it > 0 },
     androidAverageMa = reading.currentInfo.androidAverageMa
    )
   }.collect { content ->
    try {
     getSystemService(NotificationManager::class.java).notify(1, notification(content))
    } catch (_: SecurityException) {
     fail("As notificações foram bloqueadas.")
    }
   }
  }
  return START_NOT_STICKY
 }

 private fun fail(message: String) {
  app.repository.serviceError.value = message
  app.scope.launch { app.settings.continuous(false) }
  stopSelf()
 }

 private fun notification(content: BatteryNotificationText): Notification {
  val open = PendingIntent.getActivity(
   this, 0, Intent(this, MainActivity::class.java),
   PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  )
  val stop = PendingIntent.getService(
   this, 1, Intent(this, MonitorService::class.java).setAction("STOP"),
   PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  )
  return NotificationCompat.Builder(this, "battery")
   .setSmallIcon(R.drawable.ic_notification)
   .setContentTitle(content.title)
   .setContentText(content.summary)
   .setStyle(NotificationCompat.BigTextStyle().bigText(content.expandedText))
   .setContentIntent(open)
   .setOnlyAlertOnce(true)
   .setSilent(true)
   .setOngoing(true)
   .setShowWhen(false)
   .addAction(0, "Parar", stop)
   .build()
 }

 override fun onDestroy() {
  scope.cancel()
  app.repository.serviceActive.value = false
  app.repository.release("service")
  stopForeground(STOP_FOREGROUND_REMOVE)
  super.onDestroy()
 }
}
