package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test
class SmartAlertsTest {
 @Test fun triggersOnlyForRelevantState() {
  val limits=AlertLimits()
  assertTrue(SmartAlerts.evaluate(19,Mode.CHARGING,35.0,null,600_000,limits).none { it.id=="low" })
  assertTrue(SmartAlerts.evaluate(19,Mode.DISCHARGING,35.0,null,600_000,limits).any { it.id=="low" })
  assertTrue(SmartAlerts.evaluate(80,Mode.CHARGING,35.0,null,600_000,limits).any { it.id=="target" })
 }
 @Test fun temperatureDoesNotInventReadings() {
  assertTrue(SmartAlerts.evaluate(50,Mode.UNKNOWN,null,null,0,AlertLimits()).isEmpty())
  assertTrue(SmartAlerts.evaluate(50,Mode.UNKNOWN,Double.NaN,null,0,AlertLimits()).isEmpty())
  assertTrue(SmartAlerts.evaluate(50,Mode.UNKNOWN,43.0,null,0,AlertLimits()).any { it.id=="heat" })
 }
 @Test fun drainNeedsObservedDurationAndCompatibleMode() {
  val limits=AlertLimits()
  assertTrue(SmartAlerts.evaluate(50,Mode.DISCHARGING,null,-21.0,60_000,limits).none { it.id=="drain" })
  assertTrue(SmartAlerts.evaluate(50,Mode.DISCHARGING,null,-21.0,300_000,limits).any { it.id=="drain" })
  assertTrue(SmartAlerts.evaluate(50,Mode.CHARGING,null,-21.0,300_000,limits).none { it.id=="drain" })
 }
 @Test fun limitsAreValidated() { try { AlertLimits(0);assertTrue(false) } catch(_: IllegalArgumentException) {} }
}
