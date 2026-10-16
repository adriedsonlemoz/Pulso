package br.com.pulsobateria.core

/** Apenas resumos reais, inclusive recargas parciais. Nunca reconstrói tempo não observado. */
data class HistoricalCharge(
 val startedAtMs: Long, val endedAtMs: Long, val observedDurationMs: Long,
 val initialPercent: Int, val finalPercent: Int, val samples: Int,
 val mode: String, val source: String?
)

data class HistoricalCharging(
 val fullChargeMs: Long, val percentPerHour: Double, val sessionsUsed: Int,
 val recentPercentPerHour: Double?, val recentSessionsUsed: Int,
 val matchingSourcePercentPerHour: Double?, val matchingSourceSessionsUsed: Int,
 val highBatteryPercentPerHour: Double?, val matchedCurrentSource: Boolean
)

data class ChargeEta(val remainingMs: Long, val estimatedFullAtMs: Long, val basis: String)

/** Projeção estatística da recarga, distinta da autonomia em descarga. */
object ChargeForecast {
 private const val HOUR_MS = 3_600_000L
 private const val MIN_HISTORY_MS = 30 * 60_000L
 private const val MIN_RECENT_MS = 15 * 60_000L
 private const val RECENT_WINDOW_MS = 7 * 24 * HOUR_MS
 private const val MAX_FORECAST_MS = 7 * 24 * HOUR_MS
 private const val MAX_SESSION_MS = 7 * 24 * HOUR_MS
 private const val MAX_READING_AGE_MS = 180_000L

 private data class Valid(val end: Long, val duration: Long, val added: Int, val start: Int, val finish: Int, val source: String?)
 private data class Average(val rate: Double, val fullMs: Long)
 private fun average(sessions: List<Valid>, minMs: Long, minGain: Int): Average? {
  val duration = sessions.sumOf { it.duration }
  val gained = sessions.sumOf { it.added.toLong() }
  if (duration < minMs || gained < minGain) return null
  val rate = gained * HOUR_MS.toDouble() / duration
  val fullMs = (100.0 * HOUR_MS / rate).toLong()
  return if (rate.isFinite() && rate in 0.1..200.0 && fullMs in 1..MAX_FORECAST_MS)
   Average(rate, fullMs) else null
 }

 fun history(sessions: List<HistoricalCharge>, nowMs: Long, currentSource: String?): HistoricalCharging? {
  if (nowMs <= 0L) return null
  val valid = sessions.asSequence().filter { s ->
   s.mode == Mode.CHARGING.name && s.startedAtMs >= 0L && s.endedAtMs >= s.startedAtMs &&
    s.endedAtMs <= nowMs + 60_000L && s.observedDurationMs in 300_000L..MAX_SESSION_MS &&
    s.observedDurationMs <= s.endedAtMs - s.startedAtMs + 60_000L &&
    BatteryMath.sessionRate(s.initialPercent,s.finalPercent,s.observedDurationMs,s.samples)?.let { it > 0.0 } == true
  }.map { Valid(it.endedAtMs,it.observedDurationMs,it.finalPercent-it.initialPercent,it.initialPercent,it.finalPercent,it.source) }
   .sortedByDescending { it.end }.take(500).toList()
  val all = average(valid,MIN_HISTORY_MS,4) ?: return null
  val recent = valid.filter { it.end >= nowMs - RECENT_WINDOW_MS }.take(8)
  val recentAverage = average(recent,MIN_RECENT_MS,4)
  // USB, tomada e sem fio são categorias do Android, NÃO identificação de carregadores.
  val matching = valid.takeIf { currentSource in setOf("USB","Tomada","Sem fio","Fonte externa") }
   ?.filter { it.source == currentSource }.orEmpty()
  val matchingRecent = matching.filter { it.end >= nowMs - RECENT_WINDOW_MS }.take(8)
  val sourceAverage = average(matchingRecent,MIN_RECENT_MS,4) ?: average(matching,MIN_HISTORY_MS,4)
  // A etapa final frequentemente é mais lenta. Usar somente sessões observadas inteiramente
  // em 80..100%, sem atribuir proporções de sessões que cruzam 80%.
  val highSource = matching.filter { it.start >= 80 && it.finish <= 100 }
  val highAll = valid.filter { it.start >= 80 && it.finish <= 100 }
  val highAverage = average(highSource,10 * 60_000L,3) ?: average(highAll,10 * 60_000L,3)
  return HistoricalCharging(all.fullMs,all.rate,valid.size,recentAverage?.rate,
   if(recentAverage==null) 0 else recent.size,sourceAverage?.rate,
   if(sourceAverage==null) 0 else matching.size,highAverage?.rate,sourceAverage!=null)
 }

 /** Sempre exige status de carga na chamada, percentual/leitura recentes e taxas comprovadas. */
 fun prediction(percent: Int?, liveRate: Double?, readAtMs: Long, nowMs: Long,
  history: HistoricalCharging?): ChargeEta? {
  if (percent == null || percent !in 0..99 || readAtMs <= 0L || nowMs < readAtMs ||
   nowMs-readAtMs > MAX_READING_AGE_MS) return null
  val live = liveRate?.takeIf { it.isFinite() && it in 0.1..200.0 }
  val historical = history?.matchingSourcePercentPerHour ?: history?.recentPercentPerHour ?: history?.percentPerHour
  val base = when {
   live != null && historical != null -> .70 * live + .30 * historical
   live != null -> live
   else -> historical ?: return null
  }
  if (!base.isFinite() || base !in 0.1..200.0) return null
  // Usar uma fase 80–100% apenas quando há dados dela, não forçar desaceleração inventada.
  val high = history?.highBatteryPercentPerHour
  val remainingHours = when {
   percent < 80 && high != null -> (80-percent)/base + 20.0/high
   percent >= 80 && high != null -> {
    val phaseRate = if(live!=null) .70 * live + .30 * high else high
    (100-percent)/phaseRate
   }
   else -> (100-percent)/base
  }
  if (!remainingHours.isFinite() || remainingHours <= 0.0) return null
  val remainingMs = (remainingHours * HOUR_MS).toLong()
  if (remainingMs !in 1..MAX_FORECAST_MS || readAtMs > Long.MAX_VALUE-remainingMs) return null
  val basis = when {
   history?.matchingSourcePercentPerHour != null && live != null -> "Carga atual + histórico do tipo de fonte"
   history?.matchingSourcePercentPerHour != null -> "Recargas do mesmo tipo de fonte"
   live != null && historical != null -> "Carga atual + histórico"
   live != null -> "Carga atual"
   history?.recentPercentPerHour != null -> "Recargas recentes"
   else -> "Histórico de recargas"
  }
  return ChargeEta(remainingMs,readAtMs+remainingMs,
   if(high!=null) "$basis • fase 80–100% observada" else basis)
 }
}
