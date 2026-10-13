package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NightAndChargingAnalysisTest {
 private val utc=TimeZone.getTimeZone("UTC")
 private fun date(day:Int,hour:Int):Long=Calendar.getInstance(utc).apply {
  set(2026,Calendar.OCTOBER,day,hour,0,0);set(Calendar.MILLISECOND,0)
 }.timeInMillis
 @Test fun janelaCompletaDepoisDasSete() {
  val w=NightConsumptionAnalysis.window(date(13,12),utc)
  assertEquals(date(12,22),w.start);assertEquals(date(13,7),w.end);assertTrue(w.isComplete)
 }
 @Test fun janelaParcialAposAsVinteDuas() {
  val w=NightConsumptionAnalysis.window(date(13,23),utc)
  assertEquals(date(13,22),w.start);assertEquals(date(13,23),w.end);assertFalse(w.isComplete)
 }
 @Test fun janelaParcialAntesDasSete() {
  val w=NightConsumptionAnalysis.window(date(13,3),utc)
  assertEquals(date(12,22),w.start);assertEquals(date(13,3),w.end);assertFalse(w.isComplete)
 }
 @Test fun noiteIgnoraLacunasEOcorreciaDesconhecida() {
  val start=date(12,22)
  val events=listOf(UsageEventPoint(start-1000,null,UsageEventPoint.Kind.SCREEN_NON_INTERACTIVE))
  val points=listOf(ObservedBatteryPoint(1,start+1000,80),ObservedBatteryPoint(1,start+61_000,79),
   ObservedBatteryPoint(1,start+600_000,78),ObservedBatteryPoint(2,start+660_000,77))
  val s=NightConsumptionAnalysis.analyze(date(13,7),points,events,utc)
  assertEquals(1,s.battery.offDrop);assertEquals(60_000L,s.battery.offTimeMs)
  assertNull(s.offDropRatePercentHour) // Sem amostras suficientes para ritmo noturno
 }
 private fun trace(id:Long,start:Long,begin:Int,end:Int,gap:Long=60_000):List<TelemetryDatum> =
  (begin..end).mapIndexed { index,p -> TelemetryDatum(id,start+index*gap,1000+index*gap,"CHARGING",p,
   900.0,"NOW",31.0+index*0.1,2_000_000+index*12_000) }
 @Test fun duasRecargasNaMesmaFaixa() {
  val c=ChargingComparison.compare(trace(1,10000,20,35),trace(2,20000,25,40))
  assertTrue(c.comparable);assertEquals(25,c.first!!.fromPercent);assertEquals(35,c.second!!.toPercent)
  assertEquals(600_000L,c.first!!.observedMs);assertEquals(120.0,c.first!!.addedMah!!,0.01)
 }
 @Test fun naoInterpolaFaixasNaoCompartilhadas() {
  val c=ChargingComparison.compare(trace(1,10000,10,20),trace(2,20000,60,80))
  assertFalse(c.comparable)
 }
 @Test fun naoComparaComLacuna() {
  val a=trace(1,10000,20,28).mapIndexed { i,v -> if(i>4) v.copy(timestamp=v.timestamp+400_000,elapsedMs=v.elapsedMs+400_000) else v }
  assertFalse(ChargingComparison.compare(a,trace(2,20000,20,28)).comparable)
 }
 @Test fun contadorQueRegredeNaoProduzMah() {
  val invalid=trace(1,10000,20,30).mapIndexed { index,p -> if(index==5) p.copy(counterUah=1_000_000) else p }
  val result=ChargingComparison.compare(invalid,trace(2,20000,20,30))
  assertTrue(result.comparable)
  assertNull(result.first!!.addedMah)
 }
 @Test fun semTelemetriaNaoGeraVelocidade() {
  assertFalse(ChargingComparison.compare(emptyList(),trace(2,20000,30,40)).comparable)
 }
}
