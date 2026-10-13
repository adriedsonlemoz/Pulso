package br.com.pulsobateria.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Agregações com lacunas explícitas. O percentual representa diferença de nível, não mAh. */
data class ReportReading(val sessionId: Long,val timestamp: Long,val elapsedMs: Long,
 val mode: String,val percent: Int,val temperatureC: Double?)
data class DailyBatteryReport(val date: LocalDate,val observedMs: Long,val dischargeMs: Long,
 val chargeMs: Long,val dropPercent: Int,val gainedPercent: Int,
 val maxTemperatureC: Double?,val chargingSessions: Int)
data class WeekBatteryReport(val days: List<DailyBatteryReport>, val uniqueChargingSessions: Int) {
 val observedMs: Long get()=days.sumOf { it.observedMs }
 val dischargeMs: Long get()=days.sumOf { it.dischargeMs }
 val chargeMs: Long get()=days.sumOf { it.chargeMs }
 val dropPercent: Int get()=days.sumOf { it.dropPercent }
 val gainedPercent: Int get()=days.sumOf { it.gainedPercent }
 val chargingSessions: Int get()=uniqueChargingSessions
 val maxTemperatureC: Double? get()=days.mapNotNull { it.maxTemperatureC }.maxOrNull()
}
object BatteryReports {
 private const val MAX_GAP=180_000L
 /** Dias civis (inclui hoje). Não atribuir trechos atravessando meia-noite a qualquer dia. */
 fun week(input: List<ReportReading>,today: LocalDate,zone: ZoneId): WeekBatteryReport {
  val first=today.minusDays(6)
  data class Acc(var total: Long=0,var charge: Long=0,var discharge: Long=0,
   var drop: Int=0,var gained: Int=0,var maxTemp: Double?=null,val chargerIds: MutableSet<Long> = mutableSetOf())
  val map=(0L..6L).associate { first.plusDays(it) to Acc() }
  val sorted=input.asSequence().filter { it.sessionId>0 && it.percent in 0..100 && it.timestamp>=0 && it.elapsedMs>=0 }
   .sortedWith(compareBy<ReportReading> { it.sessionId }.thenBy { it.timestamp }).toList()
  for ((a,b) in sorted.zipWithNext()) {
   if(a.sessionId!=b.sessionId || a.mode!=b.mode) continue
   val dt=b.timestamp-a.timestamp
   val elapsed=b.elapsedMs-a.elapsedMs
   if(dt !in 1..MAX_GAP || elapsed !in 1..MAX_GAP || kotlin.math.abs(dt-elapsed)>5000) continue
   val dateA=Instant.ofEpochMilli(a.timestamp).atZone(zone).toLocalDate()
   val dateB=Instant.ofEpochMilli(b.timestamp).atZone(zone).toLocalDate()
   if(dateA!=dateB) continue
   val target=map[dateB] ?: continue
   when(b.mode) {
    "DISCHARGING" -> { if(b.percent>a.percent) continue; target.discharge+=dt;target.drop+=a.percent-b.percent }
    "CHARGING" -> { if(b.percent<a.percent) continue; target.charge+=dt;target.gained+=b.percent-a.percent;target.chargerIds+=a.sessionId }
    else -> continue
   }
   target.total+=dt
   for(t in listOf(a.temperatureC,b.temperatureC)) if(t!=null && t.isFinite() && t in -40.0..100.0)
    target.maxTemp=maxOf(target.maxTemp ?: t,t)
  }
  val uniqueChargingSessions=map.values.flatMap { it.chargerIds }.toSet().size
  return WeekBatteryReport((0L..6L).map { dateOffset ->
   val day=first.plusDays(dateOffset);val a=map.getValue(day)
   DailyBatteryReport(day,a.total,a.discharge,a.charge,a.drop,a.gained,a.maxTemp,a.chargerIds.size)
  }, uniqueChargingSessions)
 }
}
