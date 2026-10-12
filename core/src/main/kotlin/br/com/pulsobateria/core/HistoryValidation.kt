package br.com.pulsobateria.core

/** Invariantes do histórico observado, também usadas antes de aceitar um backup. */
object HistoryValidation {
 // Duração monotônica não pode ser limitada pela diferença de relógios civis (ajustes/NTP).
 fun validSession(start: Long, end: Long, duration: Long, samples: Int, temperatureCount: Int, temperatureSum: Double): Boolean =
  start >= 0 && end >= start && duration >= 0 && samples >= 0 &&
   temperatureCount in 0..samples && temperatureSum.isFinite() &&
   temperatureSum in -40.0*temperatureCount..100.0*temperatureCount
 fun sampleWithinSession(timestamp: Long, start: Long, end: Long): Boolean = timestamp in start..end
}
