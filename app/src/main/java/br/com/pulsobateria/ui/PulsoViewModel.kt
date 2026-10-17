package br.com.pulsobateria.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.pulsobateria.data.BackupData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.pulsobateria.PulsoApp
import br.com.pulsobateria.data.TelemetrySample
import br.com.pulsobateria.data.SessionSample
import br.com.pulsobateria.data.Settings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import br.com.pulsobateria.data.LabTest
import kotlinx.coroutines.launch

class PulsoViewModel(application: Application): AndroidViewModel(application) {
 private val app = application as PulsoApp
 val labCurrent = app.labs.current
 val labHistory = app.labs.history.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000), emptyList<LabTest>())
 var labError by mutableStateOf<String?>(null)
  private set
 fun startLab(sky: String, watts: Int?, minutes: Int?, screen: String,
  camera: Boolean, wifi: Boolean, recording: Boolean, notes: String, ensureMonitor: () -> Unit) =
  startLabInternal(ensureMonitor) { app.labs.start(sky,watts,minutes,screen,camera,wifi,recording,notes) }
 fun startDischargeLab(minutes: Int?, screen: String, camera: Boolean, wifi: Boolean,
  recording: Boolean, notes: String, ensureMonitor: () -> Unit) =
  startLabInternal(ensureMonitor) { app.labs.startDischarge(minutes,screen,camera,wifi,recording,notes) }
 private fun startLabInternal(ensureMonitor: () -> Unit, start: suspend () -> Unit) {
  labError = null
  app.repository.serviceError.value = null
  ensureMonitor()
  viewModelScope.launch {
   try {
    val result = withTimeoutOrNull(120_000L) {
     combine(app.repository.serviceActive, app.repository.serviceError) { active, error -> active to error }
      .first { (active, error) -> active || error != null }
    }
    if (result?.first != true) {
     labError = result?.second ?: "Monitoramento não iniciado; verifique a permissão de notificações."
     return@launch
    }
    start()
   } catch(e: Exception) { labError=e.message ?: "Não foi possível iniciar o teste." }
  }
 }
 fun deleteLab(id: Long) {
  viewModelScope.launch {
   try {
    labError = null
    if (!app.labs.deleteFinished(id)) labError = "Não foi possível excluir este teste."
   } catch (e: Exception) { labError = e.message ?: "Erro ao excluir o teste." }
  }
 }
 fun stopLab() { viewModelScope.launch { runCatching { app.labs.stop() }.onFailure { labError=it.message } } }
 fun changeLabSky(sky: String) { viewModelScope.launch { runCatching { app.labs.changeSky(sky) }.onFailure { labError=it.message } } }
 val reading = app.repository.reading
 fun sessionSamples(sessionId: Long): Flow<List<SessionSample>> = app.repository.sessionSamples(sessionId)
 fun sessionTelemetry(sessionId: Long): Flow<List<TelemetrySample>> = app.repository.sessionTelemetry(sessionId)
 fun setChargerLabel(id: Long, value: String) {
  val cleaned=value.trim().take(40).takeIf { it.isNotBlank() }
  viewModelScope.launch { app.repository.setChargerLabel(id,cleaned) }
 }
 val telemetry = app.repository.telemetry.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList<TelemetrySample>())
 val weeklyTelemetry = app.repository.weeklyTelemetry(System.currentTimeMillis()-8L*24*60*60*1000)
  .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList<TelemetrySample>())
 val sessions = app.repository.sessions.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val settings = app.settings.flow.stateIn(viewModelScope,SharingStarted.Eagerly,Settings())
 val active = app.repository.serviceActive
 val error = app.repository.serviceError
 var backupStatus by mutableStateOf("Nenhum backup exportado nesta sessão."); private set
 var backupBusy by mutableStateOf(false); private set
 var pendingBackup by mutableStateOf<BackupData?>(null); private set
 fun exportBackup(uri: Uri) { viewModelScope.launch {
  backupBusy=true; backupStatus="Exportando histórico…"
  try {
   val (sessions, points) = app.backup.export(uri)
   backupStatus="Backup salvo: $sessions sessões e $points pontos. Guarde o arquivo em lugar seguro."
  } catch (e: Exception) { backupStatus="Falha ao exportar: ${e.message ?: "erro desconhecido"}" }
  finally { backupBusy=false }
 } }
 fun inspectBackup(uri: Uri) { viewModelScope.launch {
  backupBusy=true; pendingBackup=null; backupStatus="Verificando arquivo e integridade…"
  try {
   pendingBackup=app.backup.inspect(uri)
   backupStatus="Backup válido. Confira os dados antes de confirmar a substituição."
  } catch(e: Exception) { backupStatus="Backup rejeitado: ${e.message ?: "erro desconhecido"}" }
  finally { backupBusy=false }
 } }
 fun cancelBackupRestore() { pendingBackup=null }
 fun restoreBackup() {
  if(backupBusy) return
  val backup=pendingBackup ?: return
  if(app.repository.serviceActive.value) {
   backupStatus="Desative primeiro o monitoramento contínuo nas Configurações para restaurar com segurança."
   return
  }
  viewModelScope.launch {
  backupBusy=true; backupStatus="Restaurando banco de dados…"
  var frozen=false
  try {
   app.repository.freezeForRestore(); frozen=true
   app.backup.restore(backup)
   pendingBackup=null
   backupStatus="Restaurado: ${backup.sessions.size} sessões e ${backup.samples.size} pontos. Monitoramento reiniciado sem simular leituras anteriores."
  } catch(e: Exception) {
   backupStatus="Erro ao restaurar: ${e.message ?: "erro desconhecido"}. Confira o histórico e as preferências."
  } finally {
   if(frozen) app.repository.resumeAfterRestore()
   backupBusy=false
  }
 } }
 fun theme(value: String) { viewModelScope.launch { app.settings.theme(value) } }
 fun startOnBoot(value: Boolean) { viewModelScope.launch { app.settings.startOnBoot(value) } }
 fun pairExternalLabs(packageName: String) { viewModelScope.launch {
  val signature = br.com.pulsobateria.integration.TrustedPartner.signerFingerprint(app, packageName)
  if (signature != null) app.settings.pairExternalLabs(packageName, signature)
 } }
 fun externalLabsEnabled(value: Boolean) { viewModelScope.launch {
  runCatching { app.settings.enableExternalLabs(value) }
 } }
 fun enableExternalPartner(packageName: String, value: Boolean) { viewModelScope.launch {
  runCatching { app.settings.enableExternalPartner(packageName, value) }
 } }
 fun removeExternalPartner(packageName: String) { viewModelScope.launch {
  app.settings.removeExternalPartner(packageName)
 } }
 fun clearExternalLabs() { viewModelScope.launch { app.settings.clearExternalLabs() } }
 fun interval(value: Int) { viewModelScope.launch { app.settings.interval(value) } }
 fun fahrenheit(value: Boolean) { viewModelScope.launch { app.settings.fahrenheit(value) } }
 fun alertsEnabled(value: Boolean) { viewModelScope.launch { app.settings.alertsEnabled(value) } }
 fun alertLow(value: Int) { viewModelScope.launch { app.settings.alertLow(value) } }
 fun alertTemp(value: Int) { viewModelScope.launch { app.settings.alertTemp(value) } }
 fun alertTarget(value: Int) { viewModelScope.launch { app.settings.alertTarget(value) } }
 fun alertDrain(value: Int) { viewModelScope.launch { app.settings.alertDrain(value) } }
 fun resetCalibration() { app.repository.resetCalibration() }
 fun capacityMah(value: Int?) { viewModelScope.launch { app.settings.capacityMah(value) } }
}
