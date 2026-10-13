package br.com.pulsobateria.monitor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.pulsobateria.MainActivity
import br.com.pulsobateria.R
import br.com.pulsobateria.core.AlertLimits
import br.com.pulsobateria.core.SmartAlerts
import br.com.pulsobateria.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Alertas opt-in por episódio, com trava persistente e intervalo mínimo de 1 h. */
class SmartAlertDispatcher(
 private val context: Context, private val repository: BatteryRepository,
 private val settings: SettingsStore, private val scope: CoroutineScope
) {
 private val prefs=context.getSharedPreferences("smart_alert_cooldowns",Context.MODE_PRIVATE)
 private val channel="battery_warnings"
 private val ids=mapOf("low" to 101,"heat" to 102,"target" to 103,"drain" to 104)
 private val consecutive=mutableMapOf<String,Int>()
 private var previousReadingAt=0L
 fun start() {
  context.getSystemService(NotificationManager::class.java).createNotificationChannel(
   NotificationChannel(channel,"Alertas de bateria",NotificationManager.IMPORTANCE_DEFAULT))
  scope.launch {
   combine(repository.reading,settings.flow) { reading,options -> reading to options }.collect { (r,s) ->
    if(r.updated<=0 || r.updated<=previousReadingAt) return@collect
    previousReadingAt=r.updated
    if(!s.alertsEnabled) { consecutive.clear();return@collect }
    val current=SmartAlerts.evaluate(r.percent,r.mode,r.temperature,r.rate,r.duration,
     AlertLimits(s.lowBatteryAlert,s.hotBatteryAlert,s.chargeTargetAlert,s.fastDrainAlert))
    val names=current.map { it.id }.toSet()
    ids.keys.forEach { id ->
     if(id !in names) {
      consecutive.remove(id)
      if(prefs.getBoolean("active_$id",false)) prefs.edit().putBoolean("active_$id",false).apply()
     }
    }
    if(!NotificationManagerCompat.from(context).areNotificationsEnabled() ||
     (Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)) return@collect
    current.forEach { candidate ->
     val count=(consecutive[candidate.id] ?: 0)+1
     consecutive[candidate.id]=count
     if(count<2 || prefs.getBoolean("active_${candidate.id}",false)) return@forEach
     val last=prefs.getLong("last_${candidate.id}",0L)
     val now=System.currentTimeMillis()
     if(last>0 && now-last in 0 until 60*60*1000L) return@forEach
     try {
      val launch=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),
       PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
      val notification=NotificationCompat.Builder(context,channel)
       .setSmallIcon(R.drawable.ic_notification).setContentTitle(candidate.title)
       .setContentText(candidate.description).setStyle(NotificationCompat.BigTextStyle().bigText(candidate.description))
       .setContentIntent(launch).setAutoCancel(true).build()
      context.getSystemService(NotificationManager::class.java).notify(ids.getValue(candidate.id),notification)
      prefs.edit().putBoolean("active_${candidate.id}",true).putLong("last_${candidate.id}",now).apply()
     } catch(_: SecurityException) { /* permissão removida: não marcar como entregue */ }
    }
   }
  }
 }
}
