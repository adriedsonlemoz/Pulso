package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BatteryReportsTest {
 private val zone=ZoneId.of("America/Sao_Paulo")
 private val day=LocalDate.of(2026,10,13)
 private val base=day.atStartOfDay(zone).toInstant().toEpochMilli()
 private fun r(t:Long,p:Int,mode:String="DISCHARGING",id:Long=1L)=ReportReading(id,base+t,t,mode,p,35.0)
 @Test fun countsOnlyActualIntervals() {
  val week=BatteryReports.week(listOf(r(0,80),r(60_000,79),r(120_000,77)),day,zone)
  val today=week.days.last()
  assertEquals(3,today.dropPercent); assertEquals(120_000L,today.observedMs)
  assertEquals(3,week.dropPercent)
 }
 @Test fun ignoresLongGapsAndNewSessions() {
  val week=BatteryReports.week(listOf(r(0,80),r(190_000,70),r(200_000,60,id=2L)),day,zone)
  assertEquals(0,week.dropPercent);assertEquals(0L,week.observedMs)
 }
 @Test fun countsChargingAndTemperature() {
  val week=BatteryReports.week(listOf(r(0,40,"CHARGING"),r(60_000,42,"CHARGING")),day,zone)
  assertEquals(2,week.gainedPercent);assertEquals(1,week.chargingSessions)
  assertEquals(35.0,week.maxTemperatureC!!,0.001)
 }
 @Test fun neverAttributesCrossMidnightToOneDay() {
  val w=BatteryReports.week(listOf(r(-60_000,80),r(60_000,79)),day,zone)
  assertEquals(0,w.dropPercent); assertEquals(0L,w.observedMs)
 }
}
