package br.com.pulsobateria.core

import kotlin.math.abs

/** Apenas intervalos entre duas leituras reais, sem interpolar lacunas ou mudanças de estado. */
object LabMath {
 const val MAX_INTERVAL_MS = 90_000L
 data class Interval(val chargingMs: Long = 0, val notChargingMs: Long = 0,
                     val weightedMaMs: Double = 0.0, val validCurrentMs: Long = 0)
 fun classify(previousMode: String, nextMode: String, previousMa: Double?, nextMa: Double?,
              previousSource: String?, nextSource: String?, elapsedDeltaMs: Long,
              wallDeltaMs: Long): Interval {
  if (elapsedDeltaMs !in 1..MAX_INTERVAL_MS || wallDeltaMs <= 0 ||
      abs(wallDeltaMs-elapsedDeltaMs) > 10_000L || previousMode != nextMode) return Interval()
  if (previousMode == "CHARGING") {
   val valid = listOf("NOW", "AVERAGE", "COUNTER")
   if (previousSource in valid && nextSource in valid && previousMa != null && nextMa != null &&
       previousMa.isFinite() && nextMa.isFinite() && previousMa > 0 && nextMa > 0 &&
       previousMa <= 30_000 && nextMa <= 30_000) {
    return Interval(chargingMs=elapsedDeltaMs,
     weightedMaMs=(previousMa+nextMa)/2.0*elapsedDeltaMs, validCurrentMs=elapsedDeltaMs)
   }
   return Interval(chargingMs=elapsedDeltaMs)
  }
  if (previousMode in listOf("DISCHARGING", "IDLE", "FULL")) return Interval(notChargingMs=elapsedDeltaMs)
  return Interval()
 }
 /** Descarga apenas com duas leituras consecutivas comprovadamente desconectadas. */
 fun classifyUnplugged(previousMode: String, nextMode: String,
                      previousPlugged: Boolean?, nextPlugged: Boolean?,
                      elapsedDeltaMs: Long, wallDeltaMs: Long): Interval {
  if (previousPlugged != false || nextPlugged != false ||
      previousMode != nextMode || previousMode !in setOf("DISCHARGING", "FULL") ||
      elapsedDeltaMs !in 1..MAX_INTERVAL_MS || wallDeltaMs <= 0 ||
      abs(wallDeltaMs-elapsedDeltaMs) > 10_000L) return Interval()
  return Interval(notChargingMs=elapsedDeltaMs)
 }

 /** P.p. de queda, não mAh. Testes com fonte externa não são chamados de consumo puro. */
 fun dischargePoints(initial: Int?, final: Int?, externalSeen: Boolean): Int? {
  if (externalSeen || initial == null || final == null || initial !in 0..100 || final !in 0..100)
   return null
  return (initial-final).takeIf { it>=0 }
 }

 fun meanMa(weightedMaMs: Double, validCurrentMs: Long): Double? =
  if (validCurrentMs > 0 && weightedMaMs.isFinite() && weightedMaMs > 0)
   (weightedMaMs/validCurrentMs).takeIf { it in 0.0..30_000.0 } else null
 fun unknownMs(totalMs: Long, chargingMs: Long, notChargingMs: Long): Long =
  (totalMs.coerceAtLeast(0) - chargingMs.coerceAtLeast(0) - notChargingMs.coerceAtLeast(0)).coerceAtLeast(0)
}
