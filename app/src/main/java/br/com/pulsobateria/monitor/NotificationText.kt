package br.com.pulsobateria.monitor

import br.com.pulsobateria.core.CurrentSource
import br.com.pulsobateria.core.Mode
import br.com.pulsobateria.core.matchesModeDirection
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

/** Representação compacta + expandida; não modifica a origem ou o valor de nenhuma leitura. */
data class BatteryNotificationText(val title: String, val summary: String, val expandedText: String)

object BatteryNotificationFormatter {
 /** Apenas converte um ETA já validado em horário civil; não melhora sua precisão. */
 internal fun predictedChargeTime(remainingMs: Long, nowEpochMillis: Long?, timeZone: ZoneId): String {
  val maxEta = 7L * 24L * 60L * 60L * 1_000L
  if (remainingMs !in 0L..maxEta || nowEpochMillis == null || nowEpochMillis <= 0 ||
   nowEpochMillis > Long.MAX_VALUE - remainingMs - 299_999L) {
   return if (remainingMs in 0L..maxEta) "Até 100%: ${durationText(remainingMs)}" else "Carregando"
  }
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

 private fun usableCurrent(value: Double?, mode: Mode): Double? = value?.takeIf {
  it.isFinite() && abs(it) <= 30_000.0 && mode != Mode.UNKNOWN && matchesModeDirection(mode, it)
 }

 /** Não repetir no painel expandido uma média praticamente igual à exibida no título. */
 private fun isDistinct(value: Double, displayed: Double?): Boolean =
  displayed == null || abs(value - displayed) > maxOf(20.0, abs(displayed) * 0.05)

 fun format(
  percent: Int?, temperatureC: Double?, fahrenheit: Boolean, mode: Mode,
  remainingMs: Long?, currentMa: Double?, source: CurrentSource,
  counterMeanMa: Double?, ratePercentPerHour: Double?,
  nowEpochMillis: Long? = null, timeZone: ZoneId = ZoneId.systemDefault(),
  androidAverageMa: Double? = null, directionWarning: Boolean = false,
  hotThresholdC: Int = 43
 ): BatteryNotificationText {
  val safePercent = percent?.takeIf { it in 0..100 }
  val safeTemp = temperatureC?.takeIf { it.isFinite() && it in -40.0..100.0 }
  val hot = safeTemp != null && hotThresholdC in 35..55 && safeTemp >= hotThresholdC

  // Zero é uma leitura possível, mas não confirma ausência de corrente. Durante carga/descarga,
  // não apresentá-lo como medição operacional. Valor bruto continua no diagnóstico do app.
  val mainCurrent = if (source == CurrentSource.NONE) null else usableCurrent(currentMa, mode)
  val counter = usableCurrent(counterMeanMa, mode)?.takeUnless { it == 0.0 }
  val androidMean = usableCurrent(androidAverageMa, mode)?.takeUnless { it == 0.0 }
  val usableMain = mainCurrent?.takeUnless { it == 0.0 }
  // Se a leitura principal for zero/ausente, dar preferência a uma média válida do contador.
  // A origem é explicitada no texto expandido. Não altera nem recalibra corrente.
  val displayed = usableMain ?: counter ?: androidMean
  val displayedSource = when {
   usableMain != null -> source
   counter != null -> CurrentSource.COUNTER
   androidMean != null -> CurrentSource.AVERAGE
   else -> CurrentSource.NONE
  }
  val titleParts = mutableListOf(safePercent?.let { "$it%" } ?: "—")
  titleParts += safeTemp?.let { temperatureText(it, fahrenheit) } ?: "— °${if (fahrenheit) "F" else "C"}"
  displayed?.let { titleParts += currentText(it) }

  val status = when (mode) {
   Mode.CHARGING -> when {
    // 100% por si só não significa que o sistema confirmou fim do carregamento.
    safePercent == 100 -> "Carregando • 100%"
    remainingMs != null && remainingMs >= 0 -> predictedChargeTime(remainingMs, nowEpochMillis, timeZone)
    else -> "Carregando"
   }
   Mode.DISCHARGING -> if (remainingMs != null && remainingMs >= 0)
    "Autonomia: ${durationText(remainingMs)}" else "Descarregando"
   Mode.FULL -> "Bateria cheia"
   Mode.IDLE -> "Carga pausada"
   Mode.UNKNOWN -> "Aguardando leitura"
  }

  // Resumo de uma linha. Um limiar excedido deve continuar visível sem expandir a notificação.
  val summary = if (hot) "Temperatura elevada" else status
  val lines = mutableListOf<String>()
  lines += status
  if (hot) lines += "Temperatura acima do limite (${hotThresholdC} °C)"
  when (displayedSource) {
   CurrentSource.COUNTER -> lines += "Corrente: média pelo contador"
   CurrentSource.AVERAGE -> lines += "Corrente: média do Android"
   CurrentSource.CAPACITY -> lines += "Corrente: estimativa por capacidade"
   // Fonte instantânea já aparece com mA no cabeçalho. Não duplicar a leitura.
   CurrentSource.NOW, CurrentSource.NONE -> Unit
  }

  // Médias de fontes distintas só aparecem se acrescentarem informação; zero de hardware
  // durante movimento de carga não aparece como média útil. Continuam no diagnóstico do app.
  if (displayedSource != CurrentSource.COUNTER && counter != null && isDistinct(counter,displayed))
   lines += "Média pelo contador: ${currentText(counter)}"
  if (displayedSource != CurrentSource.AVERAGE && androidMean != null &&
   isDistinct(androidMean,displayed) && isDistinct(androidMean,counter))
   lines += "Média do Android: ${currentText(androidMean)}"

  // Um zero em FULL/IDLE pode ser uma leitura legítima, mas não equivale à ausência de consumo.
  if (displayed == null && mainCurrent == 0.0 && (mode == Mode.FULL || mode == Mode.IDLE))
   lines += "Sensor informou 0 mA (valor instantâneo pode variar)"
  val validRate = ratePercentPerHour?.takeIf { it.isFinite() && abs(it) <= 200.0 &&
   (mode == Mode.CHARGING && it > 0 || mode == Mode.DISCHARGING && it < 0) }
  if (validRate != null && remainingMs == null) lines += "Ritmo: ${rateText(validRate)}"
  if (directionWarning) lines += "Sinal da corrente em verificação"

  return BatteryNotificationText(titleParts.joinToString(" • "),summary,lines.joinToString("\n"))
 }
}
