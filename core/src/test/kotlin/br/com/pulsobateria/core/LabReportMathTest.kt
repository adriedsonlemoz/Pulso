package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test
class LabReportMathTest {
 @Test fun coverageCountsOnlyObserved() {
  assertEquals(98,LabReportMath.coveragePercent(1_773_000L,0,1_740_000L))
  assertEquals(0,LabReportMath.coveragePercent(0,0,0))
  assertEquals(100,LabReportMath.coveragePercent(10,50,50))
  assertEquals(0,LabReportMath.coveragePercent(100,0,0))
 }
 @Test fun dischargeRateIsObservedPercentagePerHour() {
  assertEquals(8.12,LabReportMath.dischargeRate(4,1_773_000L)!!,0.02)
  assertNull(LabReportMath.dischargeRate(null,1_000L))
  assertNull(LabReportMath.dischargeRate(2,0))
 }
}
