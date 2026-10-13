package br.com.pulsobateria

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.com.pulsobateria.monitor.MonitorService
import br.com.pulsobateria.ui.PulsoScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity: ComponentActivity() {
 private val app get() = application as PulsoApp
 private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted) startMonitor() else app.repository.serviceError.value = "Permissão negada. O monitoramento funciona enquanto o app estiver aberto."
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  installSplashScreen()
  super.onCreate(savedInstanceState)
  enableEdgeToEdge()
  setContent { PulsoScreen(onMonitor = ::setMonitoring) }
 }
 override fun onStart() {
  super.onStart()
  app.repository.acquire("activity")
  // Tentar com Activity visível após encerramento pelo fabricante, respeitando a permissão.
  app.scope.launch {
   if (app.settings.flow.first().continuous && !app.repository.serviceActive.value) setMonitoring(true)
  }
 }
 override fun onStop() { if (!isChangingConfigurations) app.repository.release("activity"); super.onStop() }
 private fun setMonitoring(enabled: Boolean) {
  if (!enabled) {
   app.scope.launch {
    app.settings.continuous(false)
    // A ação STOP também desarma o reinício sticky do serviço.
    startService(Intent(this@MainActivity,MonitorService::class.java).setAction("STOP"))
   }
  } else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
   permission.launch(Manifest.permission.POST_NOTIFICATIONS)
  } else startMonitor()
 }
 private fun startMonitor() {
  if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
   app.repository.serviceError.value = "Notificações bloqueadas. Habilite-as nas configurações do Android e tente novamente."
   return
  }
  try { ContextCompat.startForegroundService(this,Intent(this,MonitorService::class.java)) }
  catch (_: RuntimeException) { app.repository.serviceError.value = "O Android bloqueou o serviço. Tente novamente com o app aberto." }
 }
}
