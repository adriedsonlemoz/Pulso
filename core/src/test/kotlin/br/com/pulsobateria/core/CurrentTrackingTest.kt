package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class CurrentTrackingTest {
 private fun read(t: CurrentTracker=CurrentTracker(), raw: Int?=1_500_000, avg: Int?=null,
  counter: Int?=null, time: Long=0, mode: Mode=Mode.CHARGING, wall: Long=time) =
  t.observe(raw,avg,counter,mode,time,null,null,wall)
 private fun train(t: CurrentTracker, raw: Int=-1_500_000): CurrentResult {
  var r=CurrentResult()
  for(i in 0..9) r=read(t,raw,counter=2_000_000+i*12_500,time=i*30_000L)
  return r
 }
 @Test fun positiveMicroamps() { assertEquals(1500.0,read().nowMa!!,0.001) }
 @Test fun negativeMicroamps() { assertEquals(-161.0,read(raw=-161_000,mode=Mode.DISCHARGING).ma!!,0.001) }
 @Test fun validSmallCurrentIsNotUnitAmbiguity() { assertEquals(1.6,read(raw=1600).ma!!,0.0001) }
 @Test fun zeroIsNotUnsupported() { assertEquals(0.0,read(raw=0).ma!!,0.0) }
 @Test fun sentinelsPreservedForDiagnosisButNotUsed() {
  val r=read(raw=Int.MIN_VALUE,avg=Int.MAX_VALUE)
  assertNull(r.ma); assertEquals(Int.MIN_VALUE,r.rawNow); assertEquals(Int.MAX_VALUE,r.rawAverage)
 }
 @Test fun outOfRangeIsAbsent() { assertNull(read(raw=30_000_001).ma) }
 @Test fun idleCanHaveNetDischarge() { assertEquals(-300.0,read(raw=-300_000,mode=Mode.IDLE).ma!!,0.001) }
 @Test fun fullCanHaveZero() { assertEquals(0.0,read(raw=0,mode=Mode.FULL).ma!!,0.0) }
 @Test fun unknownStateDoesNotInventValue() { assertNull(read(mode=Mode.UNKNOWN).ma) }
 @Test fun connectionStatusAloneNeverFlipsSignal() {
  val t=CurrentTracker()
  for(i in 0..10) assertEquals(-1500.0,read(t,raw=-1_500_000,time=i*30_000L).nowMa!!,0.001)
  assertNull(t.calibrationRecord("device"))
 }
 @Test fun realNetDischargeWhileChargingRemainsNegative() {
  val t=CurrentTracker()
  for(i in 0..9) assertEquals(-1500.0,read(t,raw=-1_500_000,counter=2_000_000-i*12_500,time=i*30_000L).nowMa!!,0.001)
  assertNull(t.calibrationRecord("device"))
 }
 @Test fun invertedSignalRequiresThreeDisjointWindows() {
  val t=CurrentTracker()
  for(i in 0..8) read(t,raw=-1_500_000,counter=2_000_000+i*12_500,time=i*30_000L)
  assertNull(t.calibrationRecord("device"))
  val r=read(t,raw=-1_500_000,counter=2_112_500,time=270_000)
  assertEquals(1500.0,r.nowMa!!,0.001)
  assertTrue(t.calibrationRecord("device")!!.endsWith("INVERTED_U"))
 }
 @Test fun milliampOemCanBeCorroborated() {
  val t=CurrentTracker(); assertEquals(1500.0,train(t,1500).nowMa!!,0.001)
  assertTrue(t.calibrationRecord("device")!!.endsWith(";M"))
 }
 @Test fun invertedMilliampOemCanBeCorroborated() {
  val t=CurrentTracker(); assertEquals(1500.0,train(t,-1500).nowMa!!,0.001)
  assertTrue(t.calibrationRecord("device")!!.endsWith("INVERTED_M"))
 }
 @Test fun arithmeticMeanCannotReplaceTimeIntegration() {
  // Área independente: 22.000+33.000+42.000 = 97.000 mA*s; ΔQ=26.944,44 µAh.
  val t=CurrentTracker()
  val times=longArrayOf(0,20_000,50_000,90_000)
  val raw=intArrayOf(-1_000_000,-1_200_000,-1_000_000,-1_100_000)
  val charge=intArrayOf(2_000_000,2_006_111,2_015_278,2_026_944)
  var r=CurrentResult()
  for(i in times.indices) r=read(t,raw[i],counter=charge[i],time=times[i])
  assertTrue(r.comparison.contains("1077.777")); assertTrue(r.comparison.contains("INVERTED_U"))
  assertNull(r.nowMa)
 }
 @Test fun intermittentInvalidSamplesBreakCalibrationEvidence() {
  val t=CurrentTracker()
  for(i in 0..3) read(t,raw=-1500,counter=2_000_000+i*12_500,time=i*30_000L)
  read(t,raw=null,counter=null,time=100_000)
  for(i in 0..6) read(t,raw=-1500,counter=2_100_000+i*12_500,time=120_000+i*30_000L)
  assertNull(t.calibrationRecord("device"))
 }
 @Test fun changingStateBreaksEvidence() {
  val t=CurrentTracker()
  for(i in 0..3) read(t,raw=-1500,counter=2_000_000+i*12_500,time=i*30_000L)
  read(t,raw=-1500,counter=2_037_500,time=100_000,mode=Mode.DISCHARGING)
  for(i in 0..6) read(t,raw=-1500,counter=2_100_000+i*12_500,time=120_000+i*30_000L)
  assertNull(t.calibrationRecord("device"))
 }
 @Test fun noCalibrationFromFlatCounter() {
  val t=CurrentTracker()
  for(i in 0..30) read(t,raw=1500,counter=2_000_000,time=i*30_000L)
  assertNull(t.calibrationRecord("device"))
 }
 @Test fun fluctuatingCurrentIsNotEvidenceOfWrongUnit() {
  val t=CurrentTracker()
  for(i in 0..30) read(t,raw=if(i%2==0) 1000 else 2000,counter=2_000_000+i*12_500,time=i*30_000L)
  assertNull(t.calibrationRecord("device"))
 }
 @Test fun bothAndroidSourcesAreRetained() {
  val r=read(raw=437_000,avg=540_000)
  assertEquals(437.0,r.nowMa!!,0.01); assertEquals(540.0,r.androidAverageMa!!,0.01)
  assertEquals(CurrentSource.NOW,r.source)
 }
 @Test fun hardwareAverageDoesNotBorrowInstantaneousCalibration() {
  val t=CurrentTracker(); train(t)
  assertEquals(-500.0,read(t,-1_500_000,-500_000,2_125_000,300_000).androidAverageMa!!,0.01)
 }
 @Test fun unavailableInstantaneousUsesAverage() {
  val r=read(raw=null,avg=-583_000,mode=Mode.DISCHARGING)
  assertEquals(CurrentSource.AVERAGE,r.source); assertEquals(-583.0,r.ma!!,0.01)
 }
 @Test fun noCapacityMeansNoFabricatedCurrent() {
  assertNull(CurrentTracker().observe(null,null,null,Mode.CHARGING,0,30.0,null).ma)
 }
 @Test fun explicitCapacityProducesIdentifiedEstimate() {
  val r=CurrentTracker().observe(null,null,null,Mode.DISCHARGING,0,-10.0,5000)
  assertEquals(-500.0,r.ma!!,0.01); assertEquals(CurrentSource.CAPACITY,r.source)
 }
 @Test fun counterBeatsCapacityFallback() {
  val t=CurrentTracker(); read(t,raw=null,counter=2_000_000)
  read(t,raw=null,counter=2_025_000,time=60_000)
  val r=t.observe(null,null,2_050_000,Mode.CHARGING,120_000,50.0,5000)
  assertEquals(1500.0,r.ma!!,0.01); assertEquals(CurrentSource.COUNTER,r.source)
 }
 @Test fun counterFormulaAndWindowAreDimensionallyCorrect() {
  val t=ChargeCounterTracker()
  t.observe(2_000_000,Mode.CHARGING,0); t.observe(2_025_000,Mode.CHARGING,60_000)
  assertEquals(1500.0,t.observe(2_050_000,Mode.CHARGING,120_000)!!,0.01)
  assertEquals(120_000L,t.windowMs)
 }
 @Test fun counterHandlesIrregularIntervals() {
  val t=ChargeCounterTracker()
  t.observe(2_000_000,Mode.CHARGING,0); t.observe(2_005_000,Mode.CHARGING,30_000)
  assertEquals(600.0,t.observe(2_020_000,Mode.CHARGING,120_000)!!,0.01)
 }
 @Test fun counterCanReportOppositeNetFlowToStatus() {
  val t=ChargeCounterTracker()
  t.observe(2_050_000,Mode.CHARGING,0); t.observe(2_025_000,Mode.CHARGING,60_000)
  assertEquals(-1500.0,t.observe(2_000_000,Mode.CHARGING,120_000)!!,0.01)
 }
 @Test fun counterResetIsNotIntegratedAsConsumption() {
  val t=ChargeCounterTracker()
  t.observe(2_000_000,Mode.DISCHARGING,0); t.observe(1_980_000,Mode.DISCHARGING,60_000)
  assertNull(t.observe(0,Mode.DISCHARGING,61_000))
 }
 @Test fun counterImplausibleStepCannotBeDilutedByLongWindow() {
  val t=ChargeCounterTracker()
  t.observe(2_000_000,Mode.CHARGING,0); t.observe(2_001_000,Mode.CHARGING,90_000)
  assertNull(t.observe(2_101_000,Mode.CHARGING,91_000))
 }
 @Test fun counterContradictoryDirectionStartsNewWindow() {
  val t=ChargeCounterTracker()
  t.observe(2_000_000,Mode.CHARGING,0); t.observe(2_025_000,Mode.CHARGING,60_000)
  assertNull(t.observe(2_000_000,Mode.CHARGING,120_000))
 }
 @Test fun counterGapsAndClockRegressionReset() {
  val t=ChargeCounterTracker()
  t.observe(2_000_000,Mode.CHARGING,0); t.observe(2_025_000,Mode.CHARGING,60_000)
  assertNull(t.observe(2_050_000,Mode.CHARGING,400_000))
  assertNull(t.observe(2_060_000,Mode.CHARGING,390_000))
 }
 @Test fun persistedCorrectionNeedsLiveRevalidation() {
  val t=CurrentTracker(); train(t); val r=CurrentTracker()
  assertTrue(r.loadCalibration(t.calibrationRecord("device"),300_000,"device"))
  assertNull(read(r,-1_500_000,counter=2_000_000,time=0,wall=300_000).nowMa)
  for(i in 1..2) read(r,-1_500_000,counter=2_000_000+i*12_500,time=i*30_000L,wall=300_000+i*30_000L)
  assertEquals(1500.0,read(r,-1_500_000,counter=2_037_500,time=90_000,wall=390_000).nowMa!!,0.01)
 }
 @Test fun calibrationRejectedForOldFormatFirmwareTimeAndMalformedCode() {
  val t=CurrentTracker(); train(t); val saved=t.calibrationRecord("device")!!; val r=CurrentTracker()
  assertFalse(r.loadCalibration("1000;1M:??",300_000,"device"))
  assertFalse(r.loadCalibration(saved,300_000,"other-firmware"))
  assertFalse(r.loadCalibration(saved,200_000,"device"))
  assertFalse(r.loadCalibration(saved,8L*24*3_600_000,"device"))
  assertFalse(r.loadCalibration("v2;device;1000;bogus",300_000,"device"))
 }
 @Test fun persistedExpiryIsCheckedDuringObservation() {
  val t=CurrentTracker(); train(t)
  read(t,-1_500_000,time=500_000,wall=8L*24*3_600_000)
  assertNull(t.calibrationRecord("device"))
 }
 @Test fun learnedCorrectionIsNotRefreshedByReuse() {
  val t=CurrentTracker(); train(t); val saved=t.calibrationRecord("device")
  read(t,-1_500_000,counter=2_125_000,time=300_000)
  assertEquals(saved,t.calibrationRecord("device"))
 }
 @Test fun savedCorrectionIsRevokedWhenLiveWindowProvesDefaultScale() {
  val t=CurrentTracker(); assertTrue(t.loadCalibration("v2;device;0;INVERTED_U",1000,"device"))
  var r=CurrentResult()
  for(i in 0..3) r=read(t,1_500_000,counter=2_000_000+i*12_500,time=i*30_000L,wall=1000+i*30_000L)
  assertEquals(1500.0,r.nowMa!!,0.01); assertNull(t.calibrationRecord("device"))
 }
}
