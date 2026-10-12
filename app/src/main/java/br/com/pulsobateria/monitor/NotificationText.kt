package br.com.pulsobateria.monitor

import br.com.pulsobateria.core.CurrentSource
import br.com.pulsobateria.core.Mode
import br.com.pulsobateria.ui.currentText
import br.com.pulsobateria.ui.durationText
import br.com.pulsobateria.ui.rateText
import br.com.pulsobateria.ui.temperatureText
import kotlin.math.abs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Textos da notificação; não cria nem altera medições. Testável sem Android. */
data class BatteryNotificationText(val title: String, val summary: String, val expandedText: String)

object BatteryNotificationFormatter {
 /** Apenas converte um ETA já validado em horário civil; não melhora sua precisão. */
 internal fun predictedChargeTime(remainingMs: Long, nowEpochMillis: Long?, timeZone: ZoneId): String {
  val maxEta = 7L * 24L * 60L * 60L * 1_000L
  if (remainingMs !in 0L..maxEta || nowEpochMillis == null || nowEpochMillis <= 0 ||
   nowEpochMillis > Long.MAX_VALUE - remainingMs - 299_999L) {
   return if (remainingMs in 0L..maxEta) "Até 100%: ${durationText(remainingMs)}" else "Carregando"
  }
  // Arredondar para cima evita indicar um horário passado para ETAs curtos.
  val predictedMs = nowEpochMillis + remainingMs
  val roundedMs = ((predictedMs + 299_999L) / 300_000L) * 300_000L
  val now = Instant.ofEpochMilli(nowEpochMillis).atZone(timeZone)
  val target = Instant.ofEpochMilli(roundedMs).atZone(timeZone)
  val hour = target.format(DateTimeFormatter.ofPattern("HH'h'mm", Locale.forLanguageTag("pt-BR")))
  val dayGap = ChronoUnit.DAYS.between(now.toLocalDate(), target.toLocalDate())
  val dayLabel = when {
   dayGap == 1L -> " amanhã"
   dayGap > 1L -> " em ${target.format(DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR")))}"
   else -> ""
  }
  return "100% previsto: ~${hour}${dayLabel}"
 }
 fun format(
  percent: Int?, temperatureC: Double?, fahrenheit: Boolean, mode: Mode,
  remainingMs: Long?, currentMa: Double?, source: CurrentSource,
  counterMeanMa: Double?, ratePercentPerHour: Double?,
  nowEpochMillis: Long? = null, timeZone: ZoneId = ZoneId.systemDefault(),
  androidAverageMa: Double? = null
 ): BatteryNotificationText {
  val titleParts = mutableListOf(percent?.takeIf { it in 0..100 }?.let { "$it%" } ?: "—")
  titleParts += temperatureC?.takeIf { it.isFinite() && it in -40.0..100.0 }
   ?.let { temperatureText(it, fahrenheit) } ?: "— °${if (fahrenheit) "F" else "C"}"

  val validCurrent = currentMa?.takeIf { it.isFinite() && abs(it) <= 30_000.0 &&
   mode != Mode.UNKNOWN && source != CurrentSource.NONE }
  if (validCurrent != null) titleParts += currentText(validCurrent)

  val status = when (mode) {
   Mode.CHARGING -> when {
    percent == 100 -> "Bateria em 100%"
    remainingMs != null && remainingMs >= 0 -> predictedChargeTime(remainingMs, nowEpochMillis, timeZone)
    else -> "Carregando"
   }
   Mode.DISCHARGING -> if (remainingMs != null && remainingMs >= 0) "Autonomia: ${durationText(remainingMs)}" else "Descarregando"
   Mode.FULL -> "Bateria cheia"
   Mode.IDLE -> "Carga pausada"
   Mode.UNKNOWN -> "Aguardando leitura"
  }

  // Não repetir "Carregando" acima de "Carregando: +... mA" quando ainda não há previsão.
  val lines = mutableListOf<String>()
  if (validCurrent == null || status != "Carregando" && status != "Descarregando") lines += status
  if (validCurrent != null) {
   val action = if (validCurrent > 0) "Carregando" else if(validCurrent < 0) "Gastando" else "Corrente líquida"
   val currentLabel = when (source) {
    CurrentSource.NOW -> action
    CurrentSource.AVERAGE, CurrentSource.COUNTER -> "$action (média)"
    CurrentSource.CAPACITY -> "$action (estimativa)"
    CurrentSource.NONE -> action
   }
   lines += "$currentLabel: ${currentText(validCurrent)}"

  }
  // Fontes diferentes não viram a mesma média; igualdade numérica não as torna redundantes.
  if(source != CurrentSource.AVERAGE) androidAverageMa?.takeIf {
   it.isFinite() && abs(it)<=30_000.0 && mode!=Mode.UNKNOWN
  }?.let { lines += "Média Android: ${currentText(it)}" }
  if(source != CurrentSource.COUNTER) counterMeanMa?.takeIf {
   it.isFinite() && abs(it)<=30_000.0 && mode!=Mode.UNKNOWN
  }?.let { lines += "Média contador: ${currentText(it)}" }

  val validRate = ratePercentPerHour?.takeIf { it.isFinite() && abs(it) <= 200.0 &&
   (mode == Mode.CHARGING && it > 0 || mode == Mode.DISCHARGING && it < 0) }
  if (validRate != null) lines += "Ritmo: ${rateText(validRate)}"

  return BatteryNotificationText(titleParts.joinToString(" • "), status, lines.joinToString("\n"))
 }
}
