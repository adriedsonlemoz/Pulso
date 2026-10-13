package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class CurrentDirectionTest {
 @Test fun negativeDuringDischargeIsUsable() {
  assertTrue(matchesModeDirection(Mode.DISCHARGING,-251.0))
  assertTrue(matchesModeDirection(Mode.CHARGING,437.0))
 }
 @Test fun contradictoryValuesAreNotUsable() {
  assertFalse(matchesModeDirection(Mode.DISCHARGING,251.0))
  assertFalse(matchesModeDirection(Mode.CHARGING,-503.0))
 }
 @Test fun zeroAndFullDoNotInventDirection() {
  assertTrue(matchesModeDirection(Mode.DISCHARGING,0.0))
  assertTrue(matchesModeDirection(Mode.FULL,10.0))
  assertFalse(matchesModeDirection(Mode.CHARGING,Double.NaN))
  assertFalse(matchesModeDirection(Mode.CHARGING,null))
 }
 @Test fun contradictoryNowFallsBackToMatchingAverage() {
  val result=CurrentTracker().observe(251000,-500000,null,Mode.DISCHARGING,1000L,null,null)
  assertEquals(CurrentSource.AVERAGE,result.source)
  assertEquals(-500.0,result.ma!!,0.001)
  assertTrue(result.note.contains("Sinal em verificação"))
  assertEquals(251000,result.rawNow)
 }
 @Test fun zeroHardwareAverageDoesNotMaskContradictoryInstantReading() {
  val result=CurrentTracker().observe(251000,0,null,Mode.DISCHARGING,1000L,null,null)
  assertNull(result.ma)
  assertEquals(CurrentSource.NONE,result.source)
  assertTrue(result.note.contains("Sinal em verificação"))
 }
 @Test fun contradictionWithoutValidAlternativeStaysUnavailable() {
  val result=CurrentTracker().observe(251000,Int.MIN_VALUE,null,Mode.DISCHARGING,1000L,null,null)
  assertEquals(CurrentSource.NONE,result.source)
  assertNull(result.ma)
  assertTrue(result.note.contains("Sinal em verificação"))
  assertEquals(251000,result.rawNow)
 }
}
