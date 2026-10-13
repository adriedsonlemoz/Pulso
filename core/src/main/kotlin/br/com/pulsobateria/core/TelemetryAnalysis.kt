package br.com.pulsobateria.core

import kotlin.math.abs

/** Leituras reais já registradas. Nenhuma estimativa por capacidade é promovida a medição. */
data class TelemetryDatum(
 val sessionId: Long, val timestamp: Long, val elapsedMs: Long, val mode: String,
 val percent: Int, val currentMa: Double?, val source: String?,
 val temperatureC: Double?, val counterUah: Int?
)

data class TelemetryWindow(
 val minutes: Int, val observedMs: Long, val coveredMs: Long,
 val meanMa: Double?, val deltaMah: Double?, val origin: String?, val intervalCount: Int
) {
 val coveragePercent: Int get() = (100.0 * coveredMs / (minutes * 60_000L)).toInt().coerceIn(0,100)
 val observedPercent: Int get() = (100.0 * observedMs / (minutes * 60_000L)).toInt().coerceIn(0,100)
}

object TelemetryAnalysis {
 const val MAX_GAP_MS = 180_000L
 const val MAX_CURRENT_MA = 30_000.0
 /** Sem misturar sessões, lacunas, timestamps inválidos, fontes ou polaridades incompatíveis. */
 fun summarize(input: List<TelemetryDatum>, now: Long, minutes: Int): TelemetryWindow {
  require(minutes in setOf(5,20,60))
  val duration = minutes * 60_000L
  val start = now-duration
  val points = input.asSequence().filter { it.timestamp in start..now && it.percent in 0..100 }
   .sortedWith(compareBy<TelemetryDatum> { it.timestamp }.thenBy { it.elapsedMs }).toList()
  var observed=0L
  var counterMs=0L
  var counterMah=0.0
  var currentMs=0L
  var currentMah=0.0
  var intervalsCounter=0
  var intervalsCurrent=0
  for (i in 1 until points.size) {
   val a=points[i-1]; val b=points[i]
   val dt=b.timestamp-a.timestamp
   val elapsed=b.elapsedMs-a.elapsedMs
   if(a.sessionId<=0 || a.sessionId!=b.sessionId || a.mode!=b.mode ||
     a.mode !in listOf("CHARGING","DISCHARGING") ||
     dt !in 1..MAX_GAP_MS || elapsed !in 1..MAX_GAP_MS || abs(dt-elapsed)>5000) continue
   observed+=dt
   val sign=if(a.mode=="CHARGING") 1 else -1
   val qa=a.counterUah; val qb=b.counterUah
   if(qa!=null && qb!=null && qa in 0..100_000_000 && qb in 0..100_000_000) {
    val delta=qb.toLong()-qa.toLong()
    val avgMa=delta*3600.0/dt
    if(delta!=0L && avgMa.isFinite() && abs(avgMa) in 10.0..MAX_CURRENT_MA && avgMa*sign>0) {
     counterMah+=delta/1000.0
     counterMs+=dt; intervalsCounter++
    }
   }
   // Só integrar fontes físicas ou médias informadas. CAPACITY é estimativa por %/h.
   if(a.source in setOf("NOW","AVERAGE") && b.source==a.source) {
    val ca=a.currentMa; val cb=b.currentMa
    if(ca!=null && cb!=null && ca.isFinite() && cb.isFinite() &&
      abs(ca)<=MAX_CURRENT_MA && abs(cb)<=MAX_CURRENT_MA && ca*sign>0 && cb*sign>0) {
     currentMah+=(ca+cb)/2.0 * dt/3_600_000.0
     currentMs+=dt; intervalsCurrent++
    }
   }
  }
  // Seleciona uma única fonte. Não duplica o mesmo intervalo e não mistura integrais.
  val chooseCounter=counterMs>0L && counterMs>=currentMs/2
  val covered=if(chooseCounter) counterMs else currentMs
  val mah=if(chooseCounter) counterMah else currentMah
  val intervals=if(chooseCounter) intervalsCounter else intervalsCurrent
  return TelemetryWindow(minutes,observed.coerceAtMost(duration),covered.coerceAtMost(duration),
   if(covered>0) mah*3_600_000.0/covered else null,
   if(covered>0) mah else null,
   if(covered==0L) null else if(chooseCounter) "Contador de carga" else "Corrente observada",
   intervals)
 }
}
