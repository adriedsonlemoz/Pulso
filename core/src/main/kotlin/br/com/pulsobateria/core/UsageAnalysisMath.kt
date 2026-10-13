package br.com.pulsobateria.core

import kotlin.math.min

/** Estatísticas locais de utilização. Nenhuma duração é atribuída a um app sem evento de uso. */
data class UsageEventPoint(val timestamp: Long, val packageName: String?, val type: Kind) {
 enum class Kind { FOREGROUND, BACKGROUND, SCREEN_INTERACTIVE, SCREEN_NON_INTERACTIVE }
}
data class AppForegroundTime(val packageName: String, val durationMs: Long)
data class UsageTimeline(val applications: List<AppForegroundTime>, val hourlyForegroundMs: List<Long>, val observedForegroundMs: Long)
data class ObservedBatteryPoint(val sessionId: Long, val timestamp: Long, val percent: Int)
data class ScreenBatteryStats(
 val observedDrop: Int, val observedGain: Int, val onDrop: Int, val offDrop: Int,
 val onTimeMs: Long, val offTimeMs: Long, val observedTimeMs: Long,
 val unclassifiedDrop: Int
)

object UsageAnalysisMath {
 private const val MAX_SAMPLE_GAP_MS = 180_000L
 private const val HOUR_MS = 3_600_000L

 /** Evento anterior ao início pode estabelecer contexto. Desconhecido permanece desconhecido. */
 fun foreground(events: List<UsageEventPoint>, start: Long, end: Long): UsageTimeline {
  if (start >= end) return UsageTimeline(emptyList(), emptyList(), 0L)
  val buckets=MutableList(((end-start-1)/HOUR_MS+1).toInt().coerceAtMost(744)) { 0L }
  val usage=linkedMapOf<String,Long>()
  var active: String?=null
  var interactive: Boolean?=null
  var previous=start
  fun addSegment(from: Long, to: Long) {
   if (to<=from || interactive==false) return
   val name=active ?: return
   usage[name]=(usage[name] ?: 0L)+(to-from)
   var pos=from
   while(pos<to) {
    val index=((pos-start)/HOUR_MS).toInt()
    if(index !in buckets.indices) break
    val next=min(to,start+(index+1)*HOUR_MS)
    buckets[index]+=next-pos
    pos=next
   }
  }
  for(e in events.sortedBy { it.timestamp }) {
   if(e.timestamp > end) break
   if(e.timestamp < start) {
    when(e.type) {
     UsageEventPoint.Kind.FOREGROUND -> active=e.packageName?.takeIf { it.isNotBlank() }
     UsageEventPoint.Kind.BACKGROUND -> if(active==e.packageName) active=null
     UsageEventPoint.Kind.SCREEN_INTERACTIVE -> interactive=true
     UsageEventPoint.Kind.SCREEN_NON_INTERACTIVE -> { interactive=false; active=null }
    }
    continue
   }
   addSegment(previous,e.timestamp)
   previous=e.timestamp
   when(e.type) {
    UsageEventPoint.Kind.FOREGROUND -> active=e.packageName?.takeIf { it.isNotBlank() }
    UsageEventPoint.Kind.BACKGROUND -> if(active==e.packageName) active=null
    UsageEventPoint.Kind.SCREEN_INTERACTIVE -> interactive=true
    UsageEventPoint.Kind.SCREEN_NON_INTERACTIVE -> { interactive=false; active=null }
   }
  }
  addSegment(previous,end)
  val apps=usage.map { AppForegroundTime(it.key,it.value) }.sortedByDescending { it.durationMs }
  return UsageTimeline(apps,buckets,apps.sumOf { it.durationMs })
 }

 /**
  * Só classifica uma mudança percentual quando duas amostras CONSECUTIVAS da mesma
  * sessão têm intervalo <= 3 min. Mudança de tela no meio do intervalo => classe incerta.
  * É correlação temporal da descarga total, nunca consumo elétrico por processo.
  */
 fun battery(points: List<ObservedBatteryPoint>, events: List<UsageEventPoint>, start: Long, end: Long): ScreenBatteryStats {
  var totalDrop=0; var totalGain=0; var onDrop=0; var offDrop=0; var unknownDrop=0
  var onMs=0L; var offMs=0L; var observedMs=0L
  val screen=events.filter { it.type==UsageEventPoint.Kind.SCREEN_INTERACTIVE || it.type==UsageEventPoint.Kind.SCREEN_NON_INTERACTIVE }.sortedBy { it.timestamp }
  val history=points.filter { it.timestamp in start until end && it.percent in 0..100 }.sortedWith(compareBy<ObservedBatteryPoint> { it.sessionId }.thenBy { it.timestamp })
  for(pair in history.zipWithNext()) {
   val a=pair.first; val b=pair.second
   val dt=b.timestamp-a.timestamp
   if(a.sessionId!=b.sessionId || dt !in 1..MAX_SAMPLE_GAP_MS) continue
   observedMs+=dt
   val d=b.percent-a.percent
   if(d>0) totalGain+=d
   if(d<0) totalDrop-=d
   val before=screen.lastOrNull { it.timestamp<=a.timestamp }
   val crossed=screen.any { it.timestamp>a.timestamp && it.timestamp<b.timestamp }
   val classification=if(crossed) null else before?.type
   when(classification) {
    UsageEventPoint.Kind.SCREEN_INTERACTIVE -> { onMs+=dt; if(d<0) onDrop-=d }
    UsageEventPoint.Kind.SCREEN_NON_INTERACTIVE -> { offMs+=dt; if(d<0) offDrop-=d }
    else -> if(d<0) unknownDrop-=d
   }
  }
  return ScreenBatteryStats(totalDrop,totalGain,onDrop,offDrop,onMs,offMs,observedMs,unknownDrop)
 }
}
