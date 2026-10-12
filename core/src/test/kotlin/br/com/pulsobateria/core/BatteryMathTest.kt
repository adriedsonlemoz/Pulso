package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class BatteryMathTest {
 private fun points(start: Int=50, sign: Int=1) = (0..10).map { Point(it*60_000L,start+sign*(it/2)) }
 @Test fun percentScalesCorrectly() { assertEquals(50,BatteryMath.percent(100,200)) }
 @Test fun invalidPercentageIsAbsent() { assertNull(BatteryMath.percent(-1,100)); assertNull(BatteryMath.percent(10,0)); assertNull(BatteryMath.percent(101,100)) }
 @Test fun temperatureUsesTenths() { assertEquals(37.8,BatteryMath.temperature(378)!!,0.001); assertEquals(0.0,BatteryMath.temperature(0)!!,0.001) }
 @Test fun absentTemperatureIsNotZero() { assertNull(BatteryMath.temperature(null)); assertNull(BatteryMath.temperature(9999)) }
 @Test fun currentUsesMicroamps() { assertEquals(1234.0,BatteryMath.currentMa(1_234_000,Mode.CHARGING)!!,0.001) }
 @Test fun dischargeSignPreserved() { assertEquals(-500.0,BatteryMath.currentMa(-500_000,Mode.DISCHARGING)!!,0.001) }
 @Test fun currentSentinelsRejected() { assertNull(BatteryMath.currentMa(Int.MIN_VALUE,Mode.CHARGING)); assertNull(BatteryMath.currentMa(Int.MAX_VALUE,Mode.CHARGING)) }
 @Test fun contradictoryCurrentRejected() { assertNull(BatteryMath.currentMa(500_000,Mode.DISCHARGING)); assertNull(BatteryMath.currentMa(-500_000,Mode.CHARGING)) }
 @Test fun zeroCurrentIsValid() { assertEquals(0.0,BatteryMath.currentMa(0,Mode.FULL)!!,0.001) }
 @Test fun extremeCurrentRejected() { assertNull(BatteryMath.currentMa(31_000_000,Mode.CHARGING)) }
 @Test fun chargingRate() { assertEquals(30.0,BatteryMath.rate(points(),Mode.CHARGING)!!,0.001) }
 @Test fun dischargingRate() { assertEquals(-30.0,BatteryMath.rate(points(sign=-1),Mode.DISCHARGING)!!,0.001) }
 @Test fun insufficientDurationRejected() { assertNull(BatteryMath.rate(points().take(4),Mode.CHARGING)) }
 @Test fun flatGaugeNotEstimated() { assertNull(BatteryMath.rate((0..10).map{Point(it*60_000L,50)},Mode.CHARGING)) }
 @Test fun reverseTrendRejected() { assertNull(BatteryMath.rate(points(),Mode.DISCHARGING)) }
 @Test fun recalibrationRejected() {
  val p=points().toMutableList(); p[5]=Point(300_000,49)
  assertNull(BatteryMath.rate(p,Mode.CHARGING))
 }
 @Test fun duplicateTimestampRejected() { val p=points().toMutableList(); p[2]=p[1]; assertNull(BatteryMath.rate(p,Mode.CHARGING)) }
 @Test fun gapRejected() { assertNull(BatteryMath.rate(listOf(Point(0,50),Point(60_000,51),Point(600_000,55)),Mode.CHARGING)) }
 @Test fun chargeEta() { assertEquals(3_600_000L,BatteryMath.remainingMs(70,30.0,Mode.CHARGING)) }
 @Test fun dischargeEta() { assertEquals(7_200_000L,BatteryMath.remainingMs(60,-30.0,Mode.DISCHARGING)) }
 @Test fun invalidEtaRejected() { assertNull(BatteryMath.remainingMs(50,0.0,Mode.CHARGING)); assertNull(BatteryMath.remainingMs(50,Double.NaN,Mode.CHARGING)); assertNull(BatteryMath.remainingMs(50,20.0,Mode.DISCHARGING)); assertNull(BatteryMath.remainingMs(null,20.0,Mode.CHARGING)) }
 @Test fun fullIdleHaveNoRate() { assertNull(BatteryMath.rate(points(),Mode.FULL)); assertNull(BatteryMath.rate(points(),Mode.IDLE)) }
 @Test fun transitionStartsSession() { assertTrue(BatteryMath.startsNewSession(Mode.CHARGING,Mode.DISCHARGING,1000,2000,1000,2000)) }
 @Test fun sleepGapStartsSession() { assertTrue(BatteryMath.startsNewSession(Mode.CHARGING,Mode.CHARGING,1000,300_000,1000,300_000)) }
 @Test fun clockChangeStartsSession() { assertTrue(BatteryMath.startsNewSession(Mode.CHARGING,Mode.CHARGING,1000,2000,1000,100_000)) }
 @Test fun ordinarySampleKeepsSession() { assertFalse(BatteryMath.startsNewSession(Mode.CHARGING,Mode.CHARGING,1000,31_000,1000,31_000)) }
 @Test fun monotonicClockResetStartsSession() { assertTrue(BatteryMath.startsNewSession(Mode.CHARGING,Mode.CHARGING,1000,0,1000,2000)) }
 @Test fun sessionNetRate() { assertEquals(-12.0,BatteryMath.sessionRate(80,74,1_800_000,61)!!,0.001) }
 @Test fun shortSessionHasNoRate() { assertNull(BatteryMath.sessionRate(80,74,60_000,2)) }
 @Test fun temperatureNeedsThreeSamples() { assertNull(BatteryMath.averageTemperature(70.0,2)); assertEquals(35.0,BatteryMath.averageTemperature(105.0,3)!!,0.001) }
 @Test fun invalidTemperatureAverageRejected() { assertNull(BatteryMath.averageTemperature(Double.NaN,3)); assertNull(BatteryMath.averageTemperature(1000.0,3)) }
}
