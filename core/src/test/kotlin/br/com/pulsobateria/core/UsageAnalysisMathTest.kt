package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class UsageAnalysisMathTest {
 private fun e(t: Long, pkg: String?, kind: UsageEventPoint.Kind)=UsageEventPoint(t,pkg,kind)
 @Test fun foregroundRespectScreenOffAndContext() {
  val t=UsageAnalysisMath.foreground(listOf(
   e(-2_000,null,UsageEventPoint.Kind.SCREEN_INTERACTIVE),e(-1_000,"youtube",UsageEventPoint.Kind.FOREGROUND),
   e(60_000,"youtube",UsageEventPoint.Kind.BACKGROUND),e(65_000,"chrome",UsageEventPoint.Kind.FOREGROUND),
   e(75_000,null,UsageEventPoint.Kind.SCREEN_NON_INTERACTIVE)
  ),0,120_000)
  assertEquals(75_000L-5_000L,t.observedForegroundMs)
  assertEquals("youtube",t.applications[0].packageName)
  assertEquals(60_000L,t.applications[0].durationMs)
  assertEquals(10_000L,t.applications[1].durationMs)
 }
 @Test fun withoutEventsCannotInventDuration() {
  assertTrue(UsageAnalysisMath.foreground(emptyList(),0,86_400_000L).applications.isEmpty())
 }
 @Test fun hourlyBinsDoNotDoubleCount() {
  val events=listOf(e(-1,null,UsageEventPoint.Kind.SCREEN_INTERACTIVE),e(0,"app",UsageEventPoint.Kind.FOREGROUND),e(5_400_000,"app",UsageEventPoint.Kind.BACKGROUND))
  val r=UsageAnalysisMath.foreground(events,0,7_200_000)
  assertEquals(listOf(3_600_000L,1_800_000L),r.hourlyForegroundMs)
 }
 @Test fun batteryDoesNotBridgeGapsOrSessions() {
  val p=listOf(ObservedBatteryPoint(1,0,80),ObservedBatteryPoint(1,60_000,79),ObservedBatteryPoint(1,400_000,60),ObservedBatteryPoint(2,410_000,59))
  val r=UsageAnalysisMath.battery(p,listOf(e(-1,null,UsageEventPoint.Kind.SCREEN_INTERACTIVE)),0,420_000)
  assertEquals(1,r.observedDrop)
  assertEquals(60_000L,r.onTimeMs)
 }
 @Test fun crossingScreenTransitionStaysUnclassified() {
  val p=listOf(ObservedBatteryPoint(1,1_000,80),ObservedBatteryPoint(1,61_000,79))
  val r=UsageAnalysisMath.battery(p,listOf(e(0,null,UsageEventPoint.Kind.SCREEN_INTERACTIVE),e(30_000,null,UsageEventPoint.Kind.SCREEN_NON_INTERACTIVE)),0,62_000)
  assertEquals(1,r.observedDrop)
  assertEquals(1,r.unclassifiedDrop)
  assertEquals(0,r.onDrop)
  assertEquals(0,r.offDrop)
 }
 @Test fun nonInteractiveCanBeClassified() {
  val p=listOf(ObservedBatteryPoint(5,0,80),ObservedBatteryPoint(5,60_000,79))
  val r=UsageAnalysisMath.battery(p,listOf(e(-1,null,UsageEventPoint.Kind.SCREEN_NON_INTERACTIVE)),0,61_000)
  assertEquals(1,r.offDrop)
  assertEquals(60_000L,r.offTimeMs)
 }
 @Test fun aChangedLevelCanAlsoCharge() {
  val p=listOf(ObservedBatteryPoint(3,0,20),ObservedBatteryPoint(3,60_000,22))
  val r=UsageAnalysisMath.battery(p,emptyList(),0,61_000)
  assertEquals(2,r.observedGain)
  assertEquals(0,r.observedDrop)
 }
}
