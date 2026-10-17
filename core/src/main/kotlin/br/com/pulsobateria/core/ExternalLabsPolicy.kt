package br.com.pulsobateria.core

/** Identidade explícita do aplicativo: pacote + conjunto de assinaturas + último teste próprio. */
data class ExternalLabsClient(
 val packageName: String,
 val signer: String,
 val enabled: Boolean = true,
 val lastTestId: Long? = null
)

/** Regras locais puras. O UID REAL é capturado pelo Binder antes de chamar authorized(). */
object ExternalLabsPolicy {
 const val MAX_CLIENTS = 20
 private val packagePattern = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")
 private val certificatePattern = Regex("[0-9a-f]{64}(,[0-9a-f]{64})*")

 fun validIdentity(packageName: String, signer: String): Boolean =
  packageName.matches(packagePattern) && signer.matches(certificatePattern)

 fun pair(clients: List<ExternalLabsClient>, name: String, signer: String): List<ExternalLabsClient> {
  require(validIdentity(name, signer))
  val prior = clients.firstOrNull { it.packageName == name }
  require(prior != null || clients.size < MAX_CLIENTS)
  // Certificado alterado nunca herda propriedade de testes do certificado anterior.
  val updated = ExternalLabsClient(name, signer, enabled = true,
   lastTestId = prior?.lastTestId?.takeIf { prior.signer == signer })
  return (clients.filterNot { it.packageName == name } + updated).sortedBy { it.packageName }
 }
 fun enabled(clients: List<ExternalLabsClient>, name: String, value: Boolean): List<ExternalLabsClient> {
  require(clients.any { it.packageName == name })
  return clients.map { if (it.packageName == name) it.copy(enabled=value) else it }
 }
 fun remember(clients: List<ExternalLabsClient>, name: String, id: Long): List<ExternalLabsClient> {
  require(id > 0L)
  require(clients.any { it.packageName == name && it.enabled })
  return clients.map { if (it.packageName == name) it.copy(lastTestId=id) else it }
 }
 fun authorized(globalEnabled: Boolean, clients: List<ExternalLabsClient>, uidPackages: List<String>?, signer: String?): ExternalLabsClient? {
  if (!globalEnabled || uidPackages?.size != 1 || signer == null) return null
  return clients.firstOrNull { it.packageName == uidPackages.single() && it.enabled && it.signer == signer }
 }
 fun owns(partner: ExternalLabsClient, id: Long): Boolean = id > 0 && partner.lastTestId == id
}
