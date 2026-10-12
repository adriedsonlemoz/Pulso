package br.com.pulsobateria.ui

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val pt = Locale.forLanguageTag("pt-BR")
fun currentText(ma: Double?): String = ma?.takeIf { it.isFinite() }?.let {
 val signal = if (it > 0) "+" else if (it < 0) "−" else ""
 val magnitude = if(abs(it) in 0.001..0.999999)
  String.format(pt,"%.3f",abs(it)).trimEnd('0').trimEnd(',')
 else String.format(pt,"%,d",abs(it).roundToInt())
 "$signal$magnitude mA"
} ?: "Indisponível"
fun currentTitleText(ma: Double?): String = if(ma==null) "— mA" else currentText(ma)
fun temperatureText(c: Double?, fahrenheit: Boolean): String = c?.let {
 String.format(pt,"%.1f °%s",if(fahrenheit) it*9/5+32 else it,if(fahrenheit) "F" else "C")
} ?: "Indisponível"
fun rateText(rate: Double?): String = rate?.let { String.format(pt,"%s%.1f %%/h",if(it>0) "+" else "−",abs(it)) } ?: "Calculando…"
fun durationText(ms: Long?): String {
 if (ms == null) return "Calculando…"
 val minutes = ms.coerceAtLeast(0)/60_000
 return when { minutes < 1 -> "< 1 min"; minutes < 60 -> "$minutes min"; else -> "${minutes/60} h ${minutes%60} min" }
}
