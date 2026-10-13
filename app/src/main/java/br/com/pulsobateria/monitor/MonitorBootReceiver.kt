package br.com.pulsobateria.monitor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.pulsobateria.PulsoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Retomada opt-in após boot e atualização. Não contorna restrições de execução do Android. */
class MonitorBootReceiver: BroadcastReceiver() {
 override fun onReceive(context: Context, intent: Intent) {
  val action=intent.action
  if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
  val pending=goAsync()
  val app=context.applicationContext as PulsoApp
  CoroutineScope(Dispatchers.IO).launch {
   try {
    val settings=withTimeoutOrNull(7000L) { app.settings.flow.first() } ?: return@launch
    if (!settings.continuous) return@launch
    if (action == Intent.ACTION_BOOT_COMPLETED && !settings.startOnBoot) return@launch
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return@launch
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@launch
    try {
     ContextCompat.startForegroundService(context,Intent(context,MonitorService::class.java))
    } catch (_: RuntimeException) {
     // Fabricantes e versões Android podem proibir o início a partir deste broadcast.
     // A preferência permanece para nova tentativa com o app aberto.
    }
   } finally { pending.finish() }
  }
 }
}
