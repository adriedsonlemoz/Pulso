package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class SessionHistoryGroupingTest {
 private fun segment(start: Long, end: Long, initial: Int, final: Int,
  mode: Mode = Mode.DISCHARGING, closed: Boolean = true,
  reason: String = "Observação interrompida", source: String? = null,
  charger: String? = null): SessionBoundary =
  SessionBoundary(mode,start,end,initial,final,closed,reason,source,charger)

 @Test fun joinsTheShownExampleAndKeepsIndicesForOriginals() {
  val a=segment(1_000,3_001_000,100,96)
  val b=segment(3_001_000,4_501_000,96,88,closed=false)
  assertEquals(listOf(listOf(0,1)),SessionHistoryText.groupConsecutive(listOf(a,b)))
 }
 @Test fun joinsSmallGapWithoutTreatingItAsObserved() {
  val a=segment(1_000,301_000,65,62)
  val b=segment(481_000,781_000,62,60)
  assertEquals(listOf(listOf(0,1)),SessionHistoryText.groupConsecutive(listOf(a,b)))
  assertEquals(180_000,b.started-a.ended)
 }
 @Test fun neverJoinsAcrossARealDifferentState() {
  val a=segment(1_000,61_000,70,65)
  val full=segment(61_000,121_000,100,100,mode=Mode.FULL)
  val b=segment(121_000,181_000,65,63)
  assertEquals(listOf(listOf(0),listOf(1),listOf(2)),SessionHistoryText.groupConsecutive(listOf(a,full,b)))
 }
 @Test fun refusesMismatchedBoundaryAndGapBeyondThreeMinutes() {
  val a=segment(1_000,100_000,80,70)
  assertEquals(2,SessionHistoryText.groupConsecutive(listOf(a,segment(100_000,200_000,69,68))).size)
  assertEquals(2,SessionHistoryText.groupConsecutive(listOf(a,segment(280_001,350_000,70,68))).size)
 }
 @Test fun refusesDifferentChargeSourcesAndManualNames() {
  val a=segment(1_000,100_000,10,20,mode=Mode.CHARGING,source="USB",charger="Solar")
  val changedSource=segment(100_000,160_000,20,30,mode=Mode.CHARGING,source="Tomada",charger="Solar")
  assertEquals(2,SessionHistoryText.groupConsecutive(listOf(a,changedSource)).size)
  assertFalse(SessionHistoryText.possibleContinuation(a,changedSource))
  assertEquals(2,SessionHistoryText.groupConsecutive(listOf(a,segment(100_000,160_000,20,30,mode=Mode.CHARGING,source="USB",charger="Outro"))).size)
 }
 @Test fun onlyKnownInterruptionReasonsCanGroup() {
  val a=segment(1_000,100_000,80,70)
  val b=segment(100_000,160_000,70,68)
  for (reason in listOf("Estado alterado","Fonte de carga alterada","Histórico restaurado","Leitura indisponível")) {
   assertEquals(reason,2,SessionHistoryText.groupConsecutive(listOf(a.copy(reason=reason),b)).size)
  }
  assertEquals(2,SessionHistoryText.groupConsecutive(listOf(a.copy(closed=false),b)).size)
 }
 @Test fun groupingKeepsMultipleAdjacentPiecesButNotAnUnrelatedOne() {
  val a=segment(1_000,100_000,85,80)
  val b=segment(100_000,220_000,80,78)
  val c=segment(220_000,280_000,78,75)
  val d=segment(280_000,300_000,75,75,mode=Mode.IDLE)
  assertEquals(listOf(listOf(0,1,2),listOf(3)), SessionHistoryText.groupConsecutive(listOf(a,b,c,d)))
  assertTrue(SessionHistoryText.groupConsecutive(emptyList()).isEmpty())
 }
}
