package br.com.pulsobateria.core

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

/** Janela civil da última noite local: 22h às 7h, limitada ao instante atual. */
data class NightWindow(val start: Long, val end: Long, val expectedEnd: Long) {
 val durationMs: Long get() = (end-start).coerceAtLeast(0L)
 val isComplete: Boolean get() = end >= expectedEnd
}

data class NightAnalysis(val window: NightWindow, val battery: ScreenBatteryStats) {
 val offDropRatePercentHour: Double? get() =
  if (battery.offTimeMs>=20*60_000 && battery.offDrop>=2) battery.offDrop*3_600_000.0/battery.offTimeMs else null
 val coveragePercent: Int get() =
  if(window.durationMs<=0) 0 else (battery.observedTimeMs*100/window.durationMs).toInt().coerceIn(0,100)
}

object NightConsumptionAnalysis {
 fun window(now: Long, timeZone: TimeZone = TimeZone.getDefault()): NightWindow {
  val calendar=Calendar.getInstance(timeZone).apply { timeInMillis=now }
  val currentHour=calendar.get(Calendar.HOUR_OF_DAY)
  val start=Calendar.getInstance(timeZone).apply {
   timeInMillis=now
   if(currentHour<22) add(Calendar.DAY_OF_MONTH,-1)
   set(Calendar.HOUR_OF_DAY,22); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
  }
  val expectedEnd=Calendar.getInstance(timeZone).apply {
   timeInMillis=start.timeInMillis
   add(Calendar.DAY_OF_MONTH,1)
   set(Calendar.HOUR_OF_DAY,7); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
  }.timeInMillis
  return NightWindow(start.timeInMillis,minOf(now,expectedEnd),expectedEnd)
 }
 fun analyze(now: Long, points: List<ObservedBatteryPoint>, events: List<UsageEventPoint>, tz: TimeZone = TimeZone.getDefault()): NightAnalysis {
  val period=window(now,tz)
  return NightAnalysis(period,UsageAnalysisMath.battery(points,events,period.start,period.end+1))
 }
}

/** Comparação só na mesma faixa percentual efetivamente atravessada por duas sessões. */
data class ChargeWindowStat(
 val fromPercent: Int, val toPercent: Int, val observedMs: Long,
 val averageTemperatureC: Double?, val addedMah: Double?, val source: String?
) {
 val pacePercentHour: Double get()=(toPercent-fromPercent)*3_600_000.0/observedMs
}
data class ChargeComparison(val first: ChargeWindowStat?, val second: ChargeWindowStat?, val message: String) {
 val comparable: Boolean get() = first!=null && second!=null
}

object ChargingComparison {
 private const val MAX_GAP_MS=180_000L
 fun compare(first: List<TelemetryDatum>, second: List<TelemetryDatum>): ChargeComparison {
  fun sanitize(input: List<TelemetryDatum>): List<TelemetryDatum> = input.asSequence()
   .filter { it.mode=="CHARGING" && it.percent in 0..100 && it.sessionId>0L }
   .sortedWith(compareBy<TelemetryDatum> { it.timestamp }.thenBy { it.elapsedMs }).toList()
  val a=sanitize(first);val b=sanitize(second)
  if(a.size<2 || b.size<2) return ChargeComparison(null,null,"As duas recargas precisam de leituras técnicas reais. Registros anteriores à alpha.18 podem não ter dados.")
  // Só comparar percentuais efetivamente presentes NAS DUAS sessões. Não interpolar horários.
  val common=a.map { it.percent }.toSet().intersect(b.map { it.percent }.toSet()).sorted()
  if(common.isEmpty()) return ChargeComparison(null,null,"As recargas não têm uma faixa percentual observada em comum.")
  // Encontrar a maior faixa com dados contínuos em ambas as sessões.
  for(loIndex in common.indices) {
   val low=common[loIndex]
   for(highIndex in common.lastIndex downTo loIndex+1) {
    val high=common[highIndex]
    if(high-low<5) break
    val sa=extract(a,low,high)
    val sb=extract(b,low,high)
    if(sa!=null && sb!=null) return ChargeComparison(sa,sb,"Comparação entre ${low}% e ${high}%, com leituras efetivas nos dois registros.")
   }
  }
  return ChargeComparison(null,null,"Sem faixa comum de pelo menos 5% com monitoramento contínuo nas duas recargas. Não é seguro comparar velocidades.")
 }
 private fun extract(data: List<TelemetryDatum>, low: Int, high: Int): ChargeWindowStat? {
  for(start in data.indices) {
   if(data[start].percent!=low) continue
   for(end in start+1 until data.size) {
    if(data[end].percent>high) break
    if(data[end].percent!=high) continue
    val section=data.subList(start,end+1)
    if(section.any { it.sessionId!=section[0].sessionId || it.percent !in low..high }) continue
    val intervals=section.zipWithNext()
    if(intervals.any { (a,b) ->
     val dt=b.timestamp-a.timestamp; val de=b.elapsedMs-a.elapsedMs
     dt !in 1..MAX_GAP_MS || de !in 1..MAX_GAP_MS || abs(dt-de)>5_000 || b.percent<a.percent
    }) continue
    val duration=section.last().timestamp-section.first().timestamp
    if(duration<=0L) continue
    val temperature=section.mapNotNull { it.temperatureC?.takeIf { t -> t.isFinite() && t in -40.0..100.0 } }
     .takeIf { it.isNotEmpty() }?.average()
    val qa=section.first().counterUah; val qb=section.last().counterUah
    // Contador só pode representar mAh quando todo o trecho é crescente e plausível.
    val counterValid=intervals.all { (a,b) ->
     val left=a.counterUah;val right=b.counterUah
     val dt=b.timestamp-a.timestamp
     left!=null && right!=null && right>left &&
      ((right-left)*3600.0/dt) in 10.0..30_000.0
    }
    val delta=if(counterValid && qa!=null && qb!=null && qb>qa) (qb-qa).toDouble()/1000.0 else null
    val average=delta?.times(3_600_000.0)?.div(duration)
    val validDelta=delta?.takeIf { average!=null && average in 10.0..30_000.0 }
    return ChargeWindowStat(low,high,duration,temperature,validDelta,
     if(validDelta!=null) "Contador de carga" else null)
   }
  }
  return null
 }
}
