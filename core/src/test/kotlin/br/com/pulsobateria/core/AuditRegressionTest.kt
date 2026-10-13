package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

/** Mesmos casos executados contra a alpha.11 para reproduzir defeitos, sem depender de novas APIs. */
class AuditRegressionTest {
 @Test fun smallOfficialMicroampReadingIsUsable() {
  assertEquals(1.6,CurrentTracker().observe(1600,null,null,Mode.CHARGING,1000,null,null).ma ?: Double.NaN,0.001)
 }
 @Test fun netDischargeIsNotFlippedByChargingStatusAlone() {
  val t=CurrentTracker(); var r=CurrentResult()
  for(i in 0..4) r=t.observe(-1_500_000,null,null,Mode.CHARGING,i*30_000L,null,null)
  // O sinal original não pode ser invertido pelo status de conexão.
  assertEquals(-1_500_000,r.rawNow)
  assertEquals(-1500.0,r.nowMa ?: Double.NaN,0.001)
  // Política alpha.13+: corrente contrária ao status só aparece no diagnóstico.
  assertNull(r.ma)
  assertEquals(CurrentSource.NONE,r.source)
  assertTrue(r.note.contains("Sinal em verificação"))
  assertNull(t.calibrationRecord("device"))
 }
 @Test fun zeroIsAReadingRatherThanASentinel() {
  assertEquals(0.0,CurrentTracker().observe(0,null,null,Mode.CHARGING,1000,null,null).ma ?: Double.NaN,0.0)
 }
 @Test fun fullBatteryCanReportCurrent() {
  assertEquals(-100.0,CurrentTracker().observe(-100_000,null,null,Mode.FULL,1000,null,null).ma ?: Double.NaN,0.01)
 }
 @Test fun currentConversionMustNotImposeConnectionSign() {
  assertEquals(-100.0,BatteryMath.currentMa(-100_000,Mode.CHARGING) ?: Double.NaN,0.01)
 }
 @Test fun chargeCounterCanExposeNetDischargeWhileConnected() {
  val t=ChargeCounterTracker()
  t.observe(2_050_000,Mode.CHARGING,0); t.observe(2_025_000,Mode.CHARGING,60_000)
  assertEquals(-1500.0,t.observe(2_000_000,Mode.CHARGING,120_000) ?: Double.NaN,0.01)
 }
 @Test fun implausibleCounterStepMustNotBeDiluted() {
  val t=ChargeCounterTracker()
  t.observe(2_000_000,Mode.CHARGING,0); t.observe(2_001_000,Mode.CHARGING,90_000)
  assertNull(t.observe(2_101_000,Mode.CHARGING,91_000))
 }
 @Test fun instantaneousDropIsNotComparedToHistoricalMeanToChangeUnits() {
  val t=CurrentTracker()
  for(i in 0..4) t.observe(1_500_000,null,2_000_000+i*12_500,Mode.CHARGING,i*30_000L,null,null)
  val r=t.observe(10_000,null,2_062_500,Mode.CHARGING,150_000,null,null)
  assertEquals(CurrentSource.NOW,r.source)
  assertEquals(10.0,r.ma ?: Double.NaN,0.01)
 }
}
