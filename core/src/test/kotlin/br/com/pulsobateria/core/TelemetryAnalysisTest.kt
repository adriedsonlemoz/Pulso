package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class TelemetryAnalysisTest {
 private fun reading(t:Long,q:Int?=null,i:Double?=null,src:String?="NOW",mode:String="CHARGING",sid:Long=1)=
  TelemetryDatum(sid,t,t,mode,50,i,src,35.0,q)
 @Test fun counterMeasuresHundredMilliampHourInSixMinutes() {
  val end=360_000L
  val result=TelemetryAnalysis.summarize(listOf(reading(0,1_000_000),reading(120_000,1_020_000),
   reading(240_000,1_050_000),reading(360_000,1_100_000)),end,20)
  assertEquals(100.0,result.deltaMah!!,0.01)
  assertEquals(1000.0,result.meanMa!!,0.01)
  assertEquals("Contador de carga",result.origin)
  assertEquals(30,result.coveragePercent)
 }
 @Test fun interpolatedCurrentIsNotInventedAcrossGaps() {
  val result=TelemetryAnalysis.summarize(listOf(reading(0,i=400.0),reading(180_000,i=600.0),
   reading(600_000,i=800.0)),600_000,20)
  assertEquals(25.0,result.deltaMah!!,0.01)
  assertEquals(500.0,result.meanMa!!,0.01)
  assertEquals(180_000L,result.coveredMs)
 }
 @Test fun percentEstimatesNotIntegratedAsMeasuredMah() {
  val result=TelemetryAnalysis.summarize(listOf(reading(0,i=1000.0,src="CAPACITY"),
   reading(60_000,i=1000.0,src="CAPACITY")),60_000,5)
  assertNull(result.deltaMah)
  assertEquals(60_000,result.observedMs)
  assertEquals(0,result.coveragePercent)
 }
 @Test fun differentSessionsDoNotConnect() {
  val result=TelemetryAnalysis.summarize(listOf(reading(0,1_000_000),reading(60_000,1_020_000,sid=2)),60_000,5)
  assertNull(result.meanMa)
  assertEquals(0,result.observedMs)
 }
 @Test fun invalidCounterFallsBackToCurrent() {
  val result=TelemetryAnalysis.summarize(listOf(reading(0,1_000_000,500.0),
   reading(60_000,200_000,500.0)),60_000,5)
  assertEquals(500.0,result.meanMa!!,0.001)
  assertEquals("Corrente observada",result.origin)
 }
 @Test fun signContradictionNotUsedForDischarge() {
  val result=TelemetryAnalysis.summarize(listOf(reading(0,1_000_000,300.0,mode="DISCHARGING"),
   reading(60_000,1_010_000,300.0,mode="DISCHARGING")),60_000,5)
  assertNull(result.meanMa)
 }
 @Test fun validNegativeDischarge() {
  val result=TelemetryAnalysis.summarize(listOf(reading(0,1_000_000,-600.0,mode="DISCHARGING"),
   reading(60_000,990_000,-600.0,mode="DISCHARGING")),60_000,5)
  assertEquals(-10.0,result.deltaMah!!,0.001)
 }
 @Test fun rejectsWallClockJumpAcrossElapsedTime() {
  val a=reading(0,1_000_000)
  val b=reading(60_000,1_010_000).copy(elapsedMs=20_000)
  assertNull(TelemetryAnalysis.summarize(listOf(a,b),60_000,5).meanMa)
 }
 @Test fun noDataNoCoverage() {
  val v=TelemetryAnalysis.summarize(emptyList(),100_000,60)
  assertEquals(0,v.coveragePercent); assertNull(v.deltaMah)
 }
}
