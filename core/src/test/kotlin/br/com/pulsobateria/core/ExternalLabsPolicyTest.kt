package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class ExternalLabsPolicyTest {
 private val certA = "a".repeat(64)
 private val certB = "b".repeat(64)

 @Test fun severalAppsAreIndependentlyAuthorized() {
  val apps = ExternalLabsPolicy.pair(ExternalLabsPolicy.pair(emptyList(), "br.app.vigia", certA), "br.app.camera", certB)
  assertEquals(2, apps.size)
  assertNotNull(ExternalLabsPolicy.authorized(true, apps, listOf("br.app.vigia"), certA))
  assertNotNull(ExternalLabsPolicy.authorized(true, apps, listOf("br.app.camera"), certB))
  assertNull(ExternalLabsPolicy.authorized(false, apps, listOf("br.app.vigia"), certA))
  assertNull(ExternalLabsPolicy.authorized(true, apps, listOf("br.app.vigia"), certB))
  assertNull(ExternalLabsPolicy.authorized(true, apps, listOf("br.app.vigia", "br.app.camera"), certA))
 }

 @Test fun onePartnerCannotStopOrReadTheOthersTest() {
  val apps = ExternalLabsPolicy.pair(ExternalLabsPolicy.pair(emptyList(), "br.app.vigia", certA), "br.app.camera", certB)
  val vigia = ExternalLabsPolicy.remember(apps, "br.app.vigia", 21L)
  val both = ExternalLabsPolicy.remember(vigia, "br.app.camera", 33L)
  assertTrue(ExternalLabsPolicy.owns(both.first { it.packageName == "br.app.vigia" }, 21L))
  assertFalse(ExternalLabsPolicy.owns(both.first { it.packageName == "br.app.camera" }, 21L))
  assertTrue(ExternalLabsPolicy.owns(both.first { it.packageName == "br.app.camera" }, 33L))
  assertFalse(ExternalLabsPolicy.owns(both.first { it.packageName == "br.app.vigia" }, 33L))
 }

 @Test fun revokeAndSignatureChangeAreIndependent() {
  val initial = ExternalLabsPolicy.remember(ExternalLabsPolicy.pair(emptyList(), "br.app.vigia", certA), "br.app.vigia", 25L)
  val unchanged = ExternalLabsPolicy.pair(initial, "br.app.vigia", certA)
  assertEquals(25L, unchanged.first().lastTestId)
  val changed = ExternalLabsPolicy.pair(initial, "br.app.vigia", certB)
  assertNull(changed.first().lastTestId)
  val off = ExternalLabsPolicy.enabled(initial, "br.app.vigia", false)
  assertNull(ExternalLabsPolicy.authorized(true, off, listOf("br.app.vigia"), certA))
  assertNotNull(ExternalLabsPolicy.authorized(true, initial, listOf("br.app.vigia"), certA))
 }

 @Test fun validatesInputAndLimit() {
  assertFalse(ExternalLabsPolicy.validIdentity("bad", certA))
  assertFalse(ExternalLabsPolicy.validIdentity("br.app.ok", "invalid"))
  var apps = emptyList<ExternalLabsClient>()
  repeat(ExternalLabsPolicy.MAX_CLIENTS) { apps = ExternalLabsPolicy.pair(apps, "br.client.app$it", certA) }
  assertEquals(20, apps.size)
  try { ExternalLabsPolicy.pair(apps, "br.client.overflow", certA); fail("should reject beyond limit") }
  catch (_: IllegalArgumentException) { }
 }
}
