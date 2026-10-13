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
import kotlinx.coroutines.launch

class PulsoViewModel(application: Application): AndroidViewModel(application) {
 private val app = application as PulsoApp
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
