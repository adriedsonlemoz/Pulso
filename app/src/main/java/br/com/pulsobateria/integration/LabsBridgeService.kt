package br.com.pulsobateria.integration

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import br.com.pulsobateria.PulsoApp
import br.com.pulsobateria.data.LabTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

/**
 * Serviço vinculado, sem FGS próprio, wake lock ou rede. Contrato AIDL v1.
 * Chamadas são síncronas e devem ser feitas fora da UI no aplicativo cliente.
 * O Binder verifica o UID REAL a cada transação, não nomes declarados no Bundle.
 */
class LabsBridgeService : Service() {
 companion object { const val ACTION_BIND = "br.com.pulsobateria.LABS_API_V1" }
 private val app get() = application as PulsoApp
 private fun respond(code: String, message: String = "") = Bundle().apply {
  putBoolean("ok", code == "OK")
  putString("code", code)
  putString("message", message)
  putInt("protocolVersion", 1)
 }

 /** Retorna apenas o pacote pareado, após verificar autorização local, UID e assinatura. */
 private suspend fun caller(uid: Int): String? {
  val s = app.settings.flow.first()
  val name = s.externalLabsPackage ?: return null
  val pinned = s.externalLabsCert ?: return null
  if (!s.externalLabsEnabled || uid == android.os.Process.myUid()) return null
  if (packageManager.getPackagesForUid(uid)?.contains(name) != true) return null
  val current = TrustedPartner.signerFingerprint(this, name) ?: return null
  return name.takeIf { current == pinned }
 }
 private fun <T> perform(operation: suspend (String) -> T, rejected: () -> T): T {
  // UID deve ser capturado NA thread da transação Binder, antes de trocar dispatcher.
  val callerUid = Binder.getCallingUid()
  return runBlocking(Dispatchers.IO) {
   val trusted = caller(callerUid) ?: return@runBlocking rejected()
   try { operation(trusted) } catch (_: Exception) { rejected() }
  }
 }

 private fun data(test: LabTest) = Bundle().apply {
  putLong("testId", test.id)
  putString("kind", test.kind)
  putString("status", test.status)
  putLong("startedAt", test.startedAt)
  putLong("deadlineAt", test.deadlineAt ?: -1L)
  putLong("endedAt", test.endedAt ?: -1L)
  val elapsed = ((test.endedAt ?: System.currentTimeMillis()) - test.startedAt).coerceAtLeast(0L)
  putLong("durationMs", elapsed)
  putLong("unobservedMs", (elapsed - test.chargingMs - test.notChargingMs).coerceAtLeast(0L))
  putString("sky", test.sky)
  if (test.panelWatts != null) putInt("panelWatts", test.panelWatts)
  putString("screenCondition", test.screenCondition)
  putBoolean("cameraReported", test.cameraReported)
  putBoolean("wifiReported", test.wifiReported)
  putBoolean("recordingReported", test.recordingReported)
  putInt("initialPercent", test.initialPercent ?: -1)
  putInt("finalPercent", test.finalPercent ?: -1)
  putLong("chargingObservedMs", test.chargingMs)
  putLong("notChargingObservedMs", test.notChargingMs)
  putLong("validCurrentMs", test.validCurrentMs)
  if (test.validCurrentMs > 0) putDouble("meanCurrentMa", test.weightedMaMs / test.validCurrentMs)
  putBoolean("externalPowerDetected", test.externalSeen)
 }

 private val remote = object : ILabsRemote.Stub() {
  override fun startTest(request: Bundle?): Bundle = perform({ caller ->
   try {
    if (request == null) return@perform respond("BAD_REQUEST", "Requisição vazia.")
    // Só o cliente autenticado chega aqui; todos os campos recebidos são validados.
    val kind = request.getString("kind") ?: "DISCHARGE"
    val minutes = request.getInt("minutes", 60)
    val screen = request.getString("screen") ?: "NAO_INFORMADA"
    val camera = request.getBoolean("camera", false)
    val wifi = request.getBoolean("wifi", false)
    val recording = request.getBoolean("recording", false)
    val notes = request.getString("notes") ?: ""
    if (kind !in setOf("DISCHARGE", "SOLAR") || minutes !in 1..1440 ||
     screen !in setOf("NAO_INFORMADA", "ACESA", "APAGADA") ||
     notes.length > 100 || notes.any { Character.isISOControl(it) })
     return@perform respond("BAD_REQUEST", "Parâmetros inválidos.")
    if (app.labs.snapshot() != null) return@perform respond("BUSY", "Já existe um teste em andamento.")
    if (!app.repository.serviceActive.value) return@perform respond("MONITOR_OFF", "Ative o monitoramento no Pulso Bateria antes do teste.")
    val annotation = "App: $caller" + if (notes.isBlank()) "" else " | $notes"
    val limitedNotes = annotation.take(160)
    if (kind == "DISCHARGE") {
     app.labs.startDischarge(minutes, screen, camera, wifi, recording, limitedNotes)
    } else {
     val sky = request.getString("sky") ?: return@perform respond("BAD_REQUEST", "Condição do céu obrigatória para Solar.")
     val watts = if (request.containsKey("panelWatts")) request.getInt("panelWatts") else null
     app.labs.start(sky, watts, minutes, screen, camera, wifi, recording, limitedNotes)
    }
    val test = app.labs.snapshot() ?: return@perform respond("ERROR", "Teste criado, mas status indisponível.")
    app.settings.rememberExternalLab(test.id)
    respond("OK").apply { putAll(data(test)) }
   } catch (e: IllegalArgumentException) { respond("BAD_REQUEST", e.message.orEmpty()) }
   catch (e: IllegalStateException) { respond("NOT_READY", e.message.orEmpty()) }
   catch (e: Exception) { respond("ERROR", "Falha ao iniciar o experimento.") }
  }, { respond("UNAUTHORIZED", "Aplicativo não autorizado.") })

  override fun stopTest(testId: Long): Bundle = perform({ _ ->
   try {
    val s = app.settings.flow.first()
    if (testId <= 0L || s.externalLabsLastId != testId) return@perform respond("NOT_OWNER", "Teste não pertence ao cliente pareado.")
    val current = app.labs.snapshot()
    if (current?.id != testId) return@perform respond("NOT_RUNNING", "Teste não está em execução.")
    app.labs.stop()
    respond("OK").apply { putLong("testId", testId) }
   } catch (e: Exception) { respond("ERROR", "Falha ao finalizar o teste.") }
  }, { respond("UNAUTHORIZED", "Aplicativo não autorizado.") })

  override fun getStatus(): Bundle = perform({ _ ->
   val current = app.labs.snapshot()
   val ownedId = app.settings.flow.first().externalLabsLastId
   when {
    current == null -> respond("OK").apply { putBoolean("running", false) }
    current.id != ownedId -> respond("OTHER_TEST_ACTIVE", "Há um experimento manual ativo.").apply { putBoolean("running", false) }
    else -> respond("OK").apply { putBoolean("running", true); putAll(data(current)) }
   }
  }, { respond("UNAUTHORIZED", "Aplicativo não autorizado.") })

  override fun getReport(testId: Long): Bundle = perform({ _ ->
   if (testId <= 0L || app.settings.flow.first().externalLabsLastId != testId)
    return@perform respond("NOT_OWNER", "Relatório não pertence ao cliente pareado.")
   val test = app.labs.report(testId) ?: return@perform respond("NOT_FOUND", "Relatório indisponível.")
   respond("OK").apply { putAll(data(test)) }
  }, { respond("UNAUTHORIZED", "Aplicativo não autorizado.") })
 }

 override fun onBind(intent: Intent?): IBinder? =
  remote.takeIf { intent?.action == ACTION_BIND }
}
