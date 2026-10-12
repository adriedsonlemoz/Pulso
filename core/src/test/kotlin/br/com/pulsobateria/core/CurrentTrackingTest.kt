package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class CurrentTrackingTest {
 @Test fun signedInstantaneousIsShownInMa() {
  val t=CurrentTracker()
  val r=t.observe(1_400_000,null,null,Mode.CHARGING,1_000,null,null)
  assertEquals(CurrentSource.NOW,r.source); assertEquals(1400.0,r.ma!!,0.001)
 }
 @Test fun negativeDischargeRemainsNegative() {
  val r=CurrentTracker().observe(-380_000,null,null,Mode.DISCHARGING,1_000,null,null)
  assertEquals(-380.0,r.ma!!,0.001)
 }
 @Test fun unsupportedAndSentinelsAreNotRealZero() {
  val r=CurrentTracker().observe(Int.MIN_VALUE,Int.MAX_VALUE,Int.MIN_VALUE,Mode.CHARGING,1_000,null,null)
  assertEquals(CurrentSource.NONE,r.source); assertNull(r.ma)
 }
 @Test fun inverseSensorIsNotFlippedPrematurely() {
  val t=CurrentTracker()
  repeat(3) { i -> assertNull(t.observe(-1_600_000,null,null,Mode.CHARGING,10_000+i*30_000L,null,null).ma) }
  val r=t.observe(-1_600_000,null,null,Mode.CHARGING,100_000L,null,null)
  assertEquals(1600.0,r.ma!!,0.001)
  assertTrue(r.note.contains("invertida"))
 }
 @Test fun tinyRawUnitWithoutReferenceIsAmbiguous() {
  val t=CurrentTracker()
  val r=t.observe(1_600,null,null,Mode.CHARGING,1_000,null,null)
  assertEquals(CurrentSource.NONE,r.source)
 }
 @Test fun averageIsUsedWhenNowUnavailable() {
  val r=CurrentTracker().observe(Int.MIN_VALUE,-700_000,null,Mode.DISCHARGING,5_000,null,null)
  assertEquals(CurrentSource.AVERAGE,r.source); assertEquals(-700.0,r.ma!!,0.001)
 }
 @Test fun counterUsesMicroampHoursPerObservedTime() {
  val t=ChargeCounterTracker()
  assertNull(t.observe(2_000_000,Mode.CHARGING,0))
  assertNull(t.observe(2_025_000,Mode.CHARGING,60_000))
  assertEquals(1500.0,t.observe(2_050_000,Mode.CHARGING,120_000)!!,0.01)
 }
 @Test fun counterDischargeSignPreserved() {
  val t=ChargeCounterTracker()
  t.observe(2_050_000,Mode.DISCHARGING,0)
  t.observe(2_025_000,Mode.DISCHARGING,60_000)
  assertEquals(-1500.0,t.observe(2_000_000,Mode.DISCHARGING,120_000)!!,0.01)
 }
 @Test fun counterBackwardsClockAndGapsDiscardHistory() {
  val t=ChargeCounterTracker()
  t.observe(2_000_000,Mode.CHARGING,0)
  t.observe(2_025_000,Mode.CHARGING,60_000)
  assertNull(t.observe(2_050_000,Mode.CHARGING,400_000))
  assertNull(t.observe(2_075_000,Mode.CHARGING,390_000))
 }
 @Test fun counterContradictoryChargeDoesNotProduceCurrent() {
  val t=ChargeCounterTracker()
  t.observe(2_000_000,Mode.CHARGING,0)
  t.observe(1_950_000,Mode.CHARGING,60_000)
  assertNull(t.observe(1_900_000,Mode.CHARGING,120_000))
 }
 @Test fun smallRawMilliampUnitIsCorroboratedByCounter() {
  val t=CurrentTracker()
  var r=CurrentResult()
  for(i in 0..6) {
   r=t.observe(1_500,null,2_000_000+i*25_000,Mode.CHARGING,i*60_000L,null,null)
  }
  assertEquals(CurrentSource.NOW,r.source)
  assertEquals(1500.0,r.ma!!,0.01)
  assertTrue(r.note.contains("mA informado"))
 }
 @Test fun zeroCurrentDoesNotHideRealCounterRate() {
  val t=CurrentTracker()
  t.observe(0,null,2_000_000,Mode.CHARGING,0,null,null)
  t.observe(0,null,2_025_000,Mode.CHARGING,60_000,null,null)
  val r=t.observe(0,null,2_050_000,Mode.CHARGING,120_000,null,null)
  assertEquals(CurrentSource.COUNTER,r.source); assertEquals(1500.0,r.ma!!,0.01)
 }
 @Test fun invertedSmallRawCanBeRecalibratedWithCounter() {
  val t=CurrentTracker()
  var r=CurrentResult()
  for(i in 0..6) r=t.observe(-1500,null,2_000_000+i*25_000,Mode.CHARGING,i*60_000L,null,null)
  assertEquals(CurrentSource.NOW,r.source)
  assertEquals(1500.0,r.ma!!,0.01)
  assertTrue(r.note.contains("invertida"))
 }
 @Test fun capacityIsNotAssumedWithoutUserInput() {
  assertNull(BatteryMath.estimatedCurrentMa(32.4,null,Mode.CHARGING))
  assertNull(BatteryMath.estimatedCurrentMa(32.4,100,Mode.CHARGING))
 }
 @Test fun capacityEstimateIsClearlySigned() {
  assertEquals(1620.0,BatteryMath.estimatedCurrentMa(32.4,5000,Mode.CHARGING)!!,0.01)
  assertEquals(-500.0,BatteryMath.estimatedCurrentMa(-10.0,5000,Mode.DISCHARGING)!!,0.01)
 }
 @Test fun capacityFallbackRequiresValidatedRate() {
  assertEquals(CurrentSource.NONE,CurrentTracker().observe(null,null,null,Mode.CHARGING,1000,null,5000).source)
  val r=CurrentTracker().observe(null,null,null,Mode.CHARGING,2000,32.4,5000)
  assertEquals(CurrentSource.CAPACITY,r.source); assertEquals(1620.0,r.ma!!,0.01)
 }
 @Test fun capacityCannotOverrideActualInstantaneous() {
  val r=CurrentTracker().observe(1_250_000,null,null,Mode.CHARGING,1000,32.4,5000)
  assertEquals(CurrentSource.NOW,r.source); assertEquals(1250.0,r.ma!!,0.01)
 }
 @Test fun counterAverageBeatsCapacityExtrapolation() {
  val t=CurrentTracker()
  for(i in 0..1) t.observe(null,null,2_000_000+i*25_000,Mode.CHARGING,i*60_000L,null,null)
  val r=t.observe(null,null,2_050_000,Mode.CHARGING,120_000,32.4,5000)
  assertEquals(CurrentSource.COUNTER,r.source); assertEquals(1500.0,r.ma!!,0.01)
 }
 @Test fun confirmedCalibrationCanBeReusedBySameDevice() {
  val t=CurrentTracker()
  for(i in 0..6) t.observe(-1500,null,2_000_000+i*25_000,Mode.CHARGING,i*60_000L,null,null)
  val code=t.calibrationCode()
  assertTrue(code.contains("1M"))
  val restored=CurrentTracker()
  assertTrue(restored.loadCalibration("1000000;$code",1_001_000L))
  val r=restored.observe(-1500,null,null,Mode.CHARGING,1_000L,null,null)
  assertEquals(1500.0,r.ma!!,0.01)
 }
 @Test fun oldCalibrationAndMalformedValuesAreRejected() {
  val t=CurrentTracker()
  assertFalse(t.loadCalibration("broken",1000L))
  assertFalse(t.loadCalibration("0;1M:??",8L*24*3600*1000))
  assertEquals("",t.calibrationCode())
 }
}
