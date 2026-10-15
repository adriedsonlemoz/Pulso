package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class LabMathTest {
 @Test fun tempoCarregandoEComMedia() {
  val result=LabMath.classify("CHARGING","CHARGING",50.0,150.0,"NOW","COUNTER",60_000,60_000)
  assertEquals(60_000,result.chargingMs)
  assertEquals(0,result.notChargingMs)
  assertEquals(60_000,result.validCurrentMs)
  assertEquals(100.0,LabMath.meanMa(result.weightedMaMs,result.validCurrentMs)!!,0.001)
 }
 @Test fun tempoSemCarga() {
  assertEquals(30_000,LabMath.classify("DISCHARGING","DISCHARGING",-100.0,-100.0,"NOW","NOW",30_000,30_000).notChargingMs)
  assertEquals(30_000,LabMath.classify("FULL","FULL",0.0,0.0,"NONE","NONE",30_000,30_000).notChargingMs)
 }
 @Test fun transicaoFicaSemLeitura() {
  assertEquals(LabMath.Interval(),LabMath.classify("CHARGING","DISCHARGING",100.0,-10.0,"NOW","NOW",30_000,30_000))
  assertEquals(30_000,LabMath.unknownMs(120_000,60_000,30_000))
 }
 @Test fun lacunasERebootNaoInventamTempo() {
  assertEquals(LabMath.Interval(),LabMath.classify("CHARGING","CHARGING",100.0,100.0,"NOW","NOW",180_000,180_000))
  assertEquals(LabMath.Interval(),LabMath.classify("CHARGING","CHARGING",100.0,100.0,"NOW","NOW",-10,10))
  assertEquals(LabMath.Interval(),LabMath.classify("CHARGING","CHARGING",100.0,100.0,"NOW","NOW",30_000,15_000))
 }
 @Test fun correnteAusenteOuEstimativaNaoVirarMedia() {
  val r=LabMath.classify("CHARGING","CHARGING",100.0,300.0,"CAPACITY","CAPACITY",60_000,60_000)
  assertEquals(60_000,r.chargingMs)
  assertEquals(0,r.validCurrentMs)
  assertNull(LabMath.meanMa(r.weightedMaMs,r.validCurrentMs))
 }
 @Test fun descargaSoContaIntervalosSemFonteExterna() {
  assertEquals(60_000,LabMath.classifyUnplugged("DISCHARGING","DISCHARGING",false,false,60_000,60_000).notChargingMs)
  assertEquals(LabMath.Interval(),LabMath.classifyUnplugged("DISCHARGING","DISCHARGING",false,true,60_000,60_000))
  assertEquals(LabMath.Interval(),LabMath.classifyUnplugged("DISCHARGING","DISCHARGING",null,false,60_000,60_000))
  assertEquals(LabMath.Interval(),LabMath.classifyUnplugged("DISCHARGING","DISCHARGING",false,false,180_000,180_000))
 }
 @Test fun quedaEmPontosPercentuaisRejeitaFonteOuAumento() {
  assertEquals(6,LabMath.dischargePoints(80,74,false))
  assertNull(LabMath.dischargePoints(80,74,true))
  assertNull(LabMath.dischargePoints(74,80,false))
  assertNull(LabMath.dischargePoints(null,74,false))
 }
 @Test fun sinalContraditorioNaoEntraNaMedia() {
  val r=LabMath.classify("CHARGING","CHARGING",-100.0,300.0,"NOW","NOW",60_000,60_000)
  assertEquals(60_000,r.chargingMs)
  assertNull(LabMath.meanMa(r.weightedMaMs,r.validCurrentMs))
 }
}
