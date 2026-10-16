package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class ChargeForecastTest {
 private val now=1_780_000_000_000L
 private fun sample(end: Long=now-60_000, hours: Long=1, gain: Int=25, initial: Int=20,
   mode: String="CHARGING", source: String?="USB", samples: Int=15) =
  HistoricalCharge(end-hours*3_600_000L,end,hours*3_600_000L,initial,initial+gain,samples,mode,source)

 @Test fun averageUsesTotalGainsAndObservedDurations() {
  val history=ChargeForecast.history(listOf(sample(gain=25),sample(end=now-2*3_600_000,hours=3,gain=30)),now,"USB")!!
  assertEquals((100.0*3_600_000/(55.0/4)).toLong(),history.fullChargeMs)
  assertEquals(2,history.sessionsUsed)
 }
 @Test fun dischargeIdleAndInvalidRatesExcluded() {
  assertNull(ChargeForecast.history(listOf(sample(mode="DISCHARGING"),sample(gain=-4)),now,"USB"))
  assertNull(ChargeForecast.history(listOf(sample(samples=1)),now,"USB"))
  assertNull(ChargeForecast.history(listOf(sample(initial=99,gain=2)),now,"USB"))
 }
 @Test fun sourceSpecificHistoryIsPreferred() {
  val h=ChargeForecast.history(listOf(sample(gain=20),sample(end=now-2*3_600_000,gain=50,source="Tomada")),now,"USB")!!
  assertEquals(20.0,h.matchingSourcePercentPerHour!!,0.001)
  assertEquals(1,h.matchingSourceSessionsUsed)
  val eta=ChargeForecast.prediction(50,null,now,now,h)!!
  assertEquals(2*3_600_000L+1_800_000L,eta.remainingMs)
  assertEquals("Recargas do mesmo tipo de fonte",eta.basis)
 }
 @Test fun liveCurrentCombinesWithRecentHistorical() {
  val h=ChargeForecast.history(listOf(sample()),now,"USB")!!
  val eta=ChargeForecast.prediction(50,35.0,now-1_000,now,h)!!
  assertEquals((50*3_600_000.0/(35*.70+25*.30)).toLong(),eta.remainingMs)
  assertEquals(now-1_000+eta.remainingMs,eta.estimatedFullAtMs)
 }
 @Test fun finalPhaseOnlyUsesActualHighPercentSessions() {
  val h=ChargeForecast.history(listOf(sample(),sample(end=now-2*3_600_000,initial=85,gain=5)),now,"USB")!!
  assertEquals(5.0,h.highBatteryPercentPerHour!!,0.001)
  val eta=ChargeForecast.prediction(90,null,now,now,h)!!
  assertEquals(2*3_600_000L,eta.remainingMs)
  val earlier=ChargeForecast.prediction(50,null,now,now,h)!!
  assertEquals((30*3_600_000.0/15.0 + 20*3_600_000.0/5.0).toLong(),earlier.remainingMs)
 }
 @Test fun invalidStaleOrZeroPercentGuarded() {
  val h=ChargeForecast.history(listOf(sample()),now,"USB")!!
  assertNull(ChargeForecast.prediction(100,25.0,now,now,h))
  assertNull(ChargeForecast.prediction(50,null,now-180_001,now,h))
  assertNull(ChargeForecast.prediction(null,25.0,now,now,h))
  assertNull(ChargeForecast.prediction(50,25.0,now+1000,now,h))
  assertNull(ChargeForecast.prediction(50,Double.NaN,now,now,null))
  assertNotNull(ChargeForecast.prediction(50,25.0,now,now,null))
 }
 @Test fun overlongSessionAndFutureSessionsExcluded() {
  assertNull(ChargeForecast.history(listOf(sample().copy(observedDurationMs=8*3_600_000)),now,"USB"))
  assertNull(ChargeForecast.history(listOf(sample(end=now+20*60_000)),now,"USB"))
 }
}
