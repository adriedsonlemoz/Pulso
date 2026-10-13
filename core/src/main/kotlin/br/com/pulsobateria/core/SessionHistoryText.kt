package br.com.pulsobateria.core

import java.util.Locale
import kotlin.math.abs

/** UI: valores em % representam diferença de níveis na escala 0..100 (não mAh). */
object SessionHistoryText {
 private val locale = Locale.forLanguageTag("pt-BR")

 fun variation(initial: Int, final: Int, mode: Mode): String {
  if (initial !in 0..100 || final !in 0..100) return "Variação indisponível"
  val delta = final - initial
  if (delta == 0) return "Sem variação"
  val amount = abs(delta)
  return when {
   mode == Mode.DISCHARGING && delta < 0 -> "Caiu $amount%"
   mode == Mode.CHARGING && delta > 0 -> "Carregou +$amount%"
   else -> "Variação: ${if(delta > 0) "+" else "−"}$amount%"
  }
 }

 fun pace(rate: Double?, mode: Mode, active: Boolean): String {
  if (rate == null || !rate.isFinite()) return if(active) "Calculando…" else "Dados insuficientes"
  val sign = if (mode == Mode.DISCHARGING && rate < 0 || mode == Mode.CHARGING && rate > 0) ""
   else if(rate > 0) "+" else if(rate < 0) "−" else ""
  return String.format(locale, "%s%.1f%%/h", sign, abs(rate))
 }

 /** Indício visual somente: nunca mesclar registros nem contar o tempo sem monitoramento. */
 fun possibleContinuation(previous: SessionBoundary, next: SessionBoundary): Boolean {
  val gap = next.started - previous.ended
  return previous.closed && previous.mode == next.mode &&
   previous.mode in setOf(Mode.CHARGING, Mode.DISCHARGING) &&
   previous.final == next.initial && previous.final in 0..100 &&
   previous.started <= previous.ended && next.started <= next.ended &&
   previous.reason !in setOf("Estado alterado", "Histórico restaurado") &&
   gap in 0L..180_000L
 }
}

/** Apenas dados dos resumos Room, sem inferência de amostras ou continuidade física. */
data class SessionBoundary(
 val mode: Mode, val started: Long, val ended: Long,
 val initial: Int, val final: Int, val closed: Boolean, val reason: String
)
