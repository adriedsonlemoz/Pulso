package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class DischargeForecastTest {
 private val now=1_780_000_000_000L
 private fun session(end: Long = now-60_000, hours: Long=1, drop: Int=25, samples: Int=15,
  mode: String="DISCHARGING") = HistoricalDischarge(end-hours*3_600_000L,end,hours*3_600_000L,80,80-drop,samples,mode)

 @Test fun weightedAverageUsesObservedTimeNotMeanOfRates() {
  val sessions=listOf(session(drop=25),session(end=now-2*3_600_000L,hours=3,drop=30))
  val result=DischargeForecast.history(sessions,now)!!
  assertEquals((100.0*3_600_000/(55.0/4.0)).toLong(),result.fullBatteryMs)
 }
 @Test fun noChargingSessionsOrInvalidGauges() {
  assertNull(DischargeForecast.history(listOf(session(mode="CHARGING"),session(drop=-8)),now))
  assertNull(DischargeForecast.history(listOf(session(drop=0)),now))
  assertNull(DischargeForecast.history(listOf(session(samples=2)),now))
 }
 @Test fun recentAndLocalConsumptionYieldAnEndClock() {
  val a=DischargeForecast.history(listOf(session()),now)!!
  val forecast=DischargeForecast.prediction(20,-30.0,now-10_000,now,a)!!
  assertEquals("Consumo atual + sessões recentes",forecast.basis)
  assertEquals(((20*3_600_000.0)/(30*.70+25*.30)).toLong(),forecast.remainingMs)
  assertEquals(now-10_000+forecast.remainingMs,forecast.estimatedEndAtMs)
 }
 @Test fun staleReadingsAndUnsupportedValuesNeverInventEta() {
  val a=DischargeForecast.history(listOf(session()),now)!!
  assertNull(DischargeForecast.prediction(20,null,now-180_001,now,a))
  assertNull(DischargeForecast.prediction(0,-10.0,now,now,a))
  assertNull(DischargeForecast.prediction(null,-10.0,now,now,a))
  assertNull(DischargeForecast.prediction(10,null,now+1000,now,a))
 }
 @Test fun historicalFallbackAndNoHistoryUsesOnlyActualLiveRate() {
  val old=DischargeForecast.history(listOf(session(end=now-20*24*3_600_000L)),now)!!
  assertNull(old.recentPercentPerHour)
  assertEquals("Média histórica de descarga",DischargeForecast.prediction(50,null,now,now,old)!!.basis)
  assertEquals("Consumo atual",DischargeForecast.prediction(50,-20.0,now,now,null)!!.basis)
  assertNull(DischargeForecast.prediction(50,null,now,now,null))
 }
 @Test fun sessionDurationMustBeMeasuredAndPlausible() {
  val a=session().copy(observedDurationMs=6*3_600_000L)
  assertNull(DischargeForecast.history(listOf(a),now))
  assertNull(DischargeForecast.history(listOf(session().copy(endedAtMs=now+900_000)),now))
 }
}
