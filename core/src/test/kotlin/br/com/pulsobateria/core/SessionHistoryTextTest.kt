package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class SessionHistoryTextTest {
 @Test fun changesUseHumanReadableUnits() {
  assertEquals("27 pontos consumidos", SessionHistoryText.variation(67,40,Mode.DISCHARGING))
  assertEquals("30 pontos adicionados", SessionHistoryText.variation(70,100,Mode.CHARGING))
  assertEquals("1 ponto consumido", SessionHistoryText.variation(68,67,Mode.DISCHARGING))
  assertEquals("Sem variação", SessionHistoryText.variation(40,40,Mode.DISCHARGING))
  assertEquals("Variação: +2 pontos", SessionHistoryText.variation(40,42,Mode.DISCHARGING))
  assertEquals("Variação indisponível", SessionHistoryText.variation(-1,40,Mode.DISCHARGING))
 }
 @Test fun paceSeparatesDirectionFromMagnitude() {
  assertEquals("17,0 pontos/h", SessionHistoryText.pace(-17.0,Mode.DISCHARGING,false))
  assertEquals("27,7 pontos/h", SessionHistoryText.pace(27.7,Mode.CHARGING,false))
  assertEquals("Calculando…", SessionHistoryText.pace(null,Mode.DISCHARGING,true))
  assertEquals("Dados insuficientes", SessionHistoryText.pace(null,Mode.DISCHARGING,false))
  assertEquals("+5,0 pontos/h", SessionHistoryText.pace(5.0,Mode.DISCHARGING,false))
 }
 private fun session(start: Long, end: Long, initial: Int, final: Int, reason: String = "Observação interrompida", mode: Mode = Mode.DISCHARGING, closed: Boolean = true) =
  SessionBoundary(mode,start,end,initial,final,closed,reason)
 @Test fun nearbyMatchingSessionsAreOnlyPossibleContinuations() {
  val first = session(1000,100_000,80,67)
  assertTrue(SessionHistoryText.possibleContinuation(first,session(160_000,200_000,67,65)))
  assertFalse(SessionHistoryText.possibleContinuation(first,session(280_001,300_000,67,65)))
  assertFalse(SessionHistoryText.possibleContinuation(first,session(160_000,200_000,66,65)))
  assertFalse(SessionHistoryText.possibleContinuation(first,session(160_000,200_000,67,68,mode=Mode.CHARGING)))
  assertFalse(SessionHistoryText.possibleContinuation(first.copy(closed=false),session(160_000,200_000,67,65)))
  assertFalse(SessionHistoryText.possibleContinuation(first.copy(reason="Estado alterado"),session(160_000,200_000,67,65)))
  assertFalse(SessionHistoryText.possibleContinuation(first,session(99_999,150_000,67,65)))
 }
}
