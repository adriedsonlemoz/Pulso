import br.com.pulsobateria.core.ExternalLabsClient
import br.com.pulsobateria.core.ExternalLabsPolicy

fun main() {
 val a = "a".repeat(64); val b = "b".repeat(64)
 var clients = ExternalLabsPolicy.pair(emptyList(), "br.app.vigia", a)
 clients = ExternalLabsPolicy.pair(clients, "br.app.camera", b)
 check(clients.size == 2)
 check(ExternalLabsPolicy.authorized(true,clients,listOf("br.app.vigia"),a)!=null)
 check(ExternalLabsPolicy.authorized(true,clients,listOf("br.app.camera"),b)!=null)
 check(ExternalLabsPolicy.authorized(true,clients,listOf("br.app.vigia"),b)==null)
 check(ExternalLabsPolicy.authorized(false,clients,listOf("br.app.vigia"),a)==null)
 check(ExternalLabsPolicy.authorized(true,clients,listOf("br.app.vigia","br.app.camera"),a)==null)
 clients = ExternalLabsPolicy.remember(clients,"br.app.vigia",100L)
 clients = ExternalLabsPolicy.remember(clients,"br.app.camera",200L)
 check(ExternalLabsPolicy.owns(clients.single{it.packageName=="br.app.vigia"},100L))
 check(!ExternalLabsPolicy.owns(clients.single{it.packageName=="br.app.camera"},100L))
 clients = ExternalLabsPolicy.enabled(clients,"br.app.camera",false)
 check(ExternalLabsPolicy.authorized(true,clients,listOf("br.app.camera"),b)==null)
 check(ExternalLabsPolicy.authorized(true,clients,listOf("br.app.vigia"),a)!=null)
 val rotated=ExternalLabsPolicy.pair(clients,"br.app.vigia",b)
 check(rotated.single{it.packageName=="br.app.vigia"}.lastTestId==null)
 check(ExternalLabsPolicy.pair(clients,"br.app.vigia",a).single{it.packageName=="br.app.vigia"}.lastTestId==100L)
 var max=emptyList<ExternalLabsClient>()
 repeat(20){max=ExternalLabsPolicy.pair(max,"br.partner.app$it",a)}
 check(max.size == 20)
 check(runCatching{ExternalLabsPolicy.pair(max,"br.partner.app20",a)}.isFailure)
 check(!ExternalLabsPolicy.validIdentity("invalido",a))
 println("PASS: 16 invariantes em execucao Kotlin/JVM - multiapp, isolamento, revogacao, UID, assinatura, limite")
}
