package br.com.pulsobateria.core

import kotlin.math.abs
import kotlin.math.roundToInt

enum class Mode(val label: String) {
 CHARGING("Carregando"), DISCHARGING("Descarregando"), FULL("Cheia"),
 IDLE("Conectado, sem carregar"), UNKNOWN("Estado desconhecido")
}
data class Point(val elapsed: Long, val percent: Int)
object BatteryMath {
 const val PAUSE_GRACE_MS = 90_000L
 fun observedDuration(previous: Long, lastElapsed: Long, elapsed: Long, paused: Boolean, samples: Int): Long {
  val interval = elapsed-lastElapsed
  return previous.coerceAtLeast(0)+(if (!paused && samples > 0 && lastElapsed > 0 && interval in 1..180_000L) interval else 0)
 }
 fun canResumePause(previous: Mode, next: Mode, before: Int, after: Int, lastElapsed: Long, elapsed: Long, lastWall: Long, wall: Long): Boolean =
  previous != Mode.UNKNOWN && previous == next && before in 0..100 && before == after &&
   elapsed > lastElapsed && elapsed-lastElapsed <= PAUSE_GRACE_MS &&
   abs((wall-lastWall)-(elapsed-lastElapsed)) <= 60_000

 fun sessionRate(initial: Int, final: Int, durationMs: Long, samples: Int): Double? {
  if (initial !in 0..100 || final !in 0..100 || durationMs < 300_000 || samples < 3 || abs(final-initial) < 2) return null
  return ((final-initial)*3_600_000.0/durationMs).takeIf { abs(it) <= 200 }
 }
 fun averageTemperature(sum: Double, count: Int): Double? =
  if (count < 3 || !sum.isFinite()) null else (sum/count).takeIf { it in -40.0..100.0 }
 fun percent(level: Int, scale: Int): Int? =
  if (scale <= 0 || level < 0 || level > scale) null else (100.0 * level / scale).roundToInt()
 fun temperature(tenths: Int?): Double? = tenths?.takeIf { it in -400..1000 }?.div(10.0)
 // Preserve Android's net-current sign; contradictory OEM readings are not silently flipped.
 fun currentMa(microamps: Int, mode: Mode): Double? {
  if (microamps == Int.MIN_VALUE || microamps == Int.MAX_VALUE || abs(microamps.toLong()) > 30_000_000L) return null
  val value = microamps / 1000.0
  return when {
   mode == Mode.DISCHARGING && value > 0 -> null
   mode == Mode.CHARGING && value < 0 -> null
   mode == Mode.UNKNOWN -> null
   else -> value
  }
 }
 fun rate(points: List<Point>, mode: Mode): Double? {
  if (mode != Mode.CHARGING && mode != Mode.DISCHARGING) return null
  if (points.size < 3 || points.any { it.percent !in 0..100 }) return null
  if (points.zipWithNext().any { (a,b) -> b.elapsed <= a.elapsed || b.elapsed-a.elapsed > 180_000 }) return null
  val first = points.first(); val last = points.last()
  val duration = last.elapsed - first.elapsed
  val delta = last.percent - first.percent
  if (duration < 300_000 || abs(delta) < 2) return null
  if (mode == Mode.CHARGING && delta <= 0 || mode == Mode.DISCHARGING && delta >= 0) return null
  // Reject direction reversals: gauge recalibration must not look like reliable autonomy.
  if (points.zipWithNext().any { (a,b) -> if (mode == Mode.CHARGING) b.percent < a.percent else b.percent > a.percent }) return null
  return (delta * 3_600_000.0 / duration).takeIf { abs(it) <= 200 }
 }
 fun remainingMs(percent: Int?, rate: Double?, mode: Mode): Long? {
  if (percent == null || percent !in 0..100 || rate == null || !rate.isFinite() || rate == 0.0) return null
  val hours = when {
   mode == Mode.CHARGING && rate > 0 -> (100 - percent) / rate
   mode == Mode.DISCHARGING && rate < 0 -> percent / -rate
   else -> return null
  }
  return (hours * 3_600_000).toLong().takeIf { it in 0..604_800_000L }
 }
 fun startsNewSession(previous: Mode, next: Mode, lastElapsed: Long, elapsed: Long, lastWall: Long, wall: Long): Boolean =
  previous != next || elapsed <= lastElapsed || elapsed-lastElapsed > 180_000 || abs((wall-lastWall)-(elapsed-lastElapsed)) > 60_000
}
