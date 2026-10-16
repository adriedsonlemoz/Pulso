package br.com.pulsobateria.core

/** Resumo persistido de uma sessão: nenhuma lacuna é reconstruída. */
data class HistoricalDischarge(
 val startedAtMs: Long, val endedAtMs: Long, val observedDurationMs: Long,
 val initialPercent: Int, val finalPercent: Int, val samples: Int, val mode: String
)

data class HistoricalAutonomy(
 val fullBatteryMs: Long, val percentPerHour: Double, val sessionsUsed: Int,
 val recentPercentPerHour: Double?, val recentSessionsUsed: Int
)

data class DischargeEta(val remainingMs: Long, val estimatedEndAtMs: Long, val basis: String)

/** Histórico das descargas reais disponíveis (até 500 resumos retidos pelo Room). */
object DischargeForecast {
 private const val HOUR_MS = 3_600_000L
 private const val MIN_HISTORY_MS = 30 * 60_000L
 private const val MIN_RECENT_MS = 15 * 60_000L
 private const val RECENT_WINDOW_MS = 7 * 24 * HOUR_MS
 private const val MAX_FORECAST_MS = 7 * 24 * HOUR_MS
 private const val MAX_READING_AGE_MS = 180_000L
 private const val MAX_SESSION_MS = 7 * 24 * HOUR_MS

 private data class ValidSession(val endedAtMs: Long, val durationMs: Long, val drop: Int)
 private data class Average(val rate: Double, val fullMs: Long)

 private fun average(sessions: List<ValidSession>, minDurationMs: Long, minDrop: Int): Average? {
  val duration = sessions.sumOf { it.durationMs }
  val drop = sessions.sumOf { it.drop.toLong() }
  if (duration < minDurationMs || drop < minDrop) return null
  // Razão dos totais: média das taxas ponderada pelo tempo observado.
  val rate = drop.toDouble() * HOUR_MS / duration
  val full = (100.0 * HOUR_MS / rate).toLong()
  return if (rate.isFinite() && rate in 0.1..200.0 && full in 1..MAX_FORECAST_MS) Average(rate, full) else null
 }

 fun history(sessions: List<HistoricalDischarge>, nowMs: Long): HistoricalAutonomy? {
  if (nowMs <= 0L) return null
  val valid = sessions.asSequence().filter { s ->
   s.mode == Mode.DISCHARGING.name && s.startedAtMs >= 0 && s.endedAtMs >= s.startedAtMs &&
    s.endedAtMs <= nowMs + 60_000L && s.observedDurationMs in 300_000L..MAX_SESSION_MS &&
    s.observedDurationMs <= s.endedAtMs - s.startedAtMs + 60_000L &&
    BatteryMath.sessionRate(s.initialPercent,s.finalPercent,s.observedDurationMs,s.samples)?.let { it < 0.0 } == true
  }.map { ValidSession(it.endedAtMs,it.observedDurationMs,it.initialPercent-it.finalPercent) }
   .sortedByDescending { it.endedAtMs }.take(500).toList()
  val whole = average(valid,MIN_HISTORY_MS,4) ?: return null
  val recentSessions = valid.filter { it.endedAtMs >= nowMs-RECENT_WINDOW_MS }.take(8)
  val recent = average(recentSessions,MIN_RECENT_MS,4)
  return HistoricalAutonomy(whole.fullMs,whole.rate,valid.size,recent?.rate,if(recent==null) 0 else recentSessions.size)
 }

 /** Use a leitura atual recente; combine o consumo ao vivo com o histórico, sem inventar dados. */
 fun prediction(percent: Int?, liveRate: Double?, readAtMs: Long, nowMs: Long,
  history: HistoricalAutonomy?): DischargeEta? {
  if (percent == null || percent !in 1..100 || readAtMs <= 0L || nowMs < readAtMs ||
   nowMs-readAtMs > MAX_READING_AGE_MS) return null
  val live = liveRate?.takeIf { it.isFinite() && it in -200.0..-0.1 }?.let { -it }
  val historical = history?.recentPercentPerHour ?: history?.percentPerHour
  val rate = when {
   live != null && historical != null -> 0.70*live+0.30*historical
   live != null -> live
   else -> historical ?: return null
  }
  if (!rate.isFinite() || rate !in 0.1..200.0) return null
  val remainingMs = (percent * HOUR_MS / rate).toLong()
  if (remainingMs !in 1..MAX_FORECAST_MS || readAtMs > Long.MAX_VALUE-remainingMs) return null
  val basis = when {
   live != null && history?.recentPercentPerHour != null -> "Consumo atual + sessões recentes"
   live != null && historical != null -> "Consumo atual + média histórica"
   live != null -> "Consumo atual"
   history?.recentPercentPerHour != null -> "Últimas sessões de descarga"
   else -> "Média histórica de descarga"
  }
  return DischargeEta(remainingMs,readAtMs+remainingMs,basis)
 }
}
