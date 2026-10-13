package br.com.pulsobateria.monitor

import br.com.pulsobateria.core.CurrentSource
import br.com.pulsobateria.core.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationTextTest {
 private val zone = ZoneId.of("America/Sao_Paulo")
 private val now = ZonedDateTime.of(2026,10,12,14,19,0,0,zone).toInstant().toEpochMilli()
 private fun format(
  mode: Mode = Mode.CHARGING, current: Double? = 1018.0,
  source: CurrentSource = CurrentSource.NOW, counter: Double? = null,
  android: Double? = null, rate: Double? = 25.4, percent: Int? = 86,
  temp: Double? = 41.3, limit: Int = 43, remaining: Long? = 25 * 60_000L
 ) = BatteryNotificationFormatter.format(percent,temp,false,mode,remaining,current,
  source,counter,rate,now,zone,android,hotThresholdC=limit)

 @Test fun compactTitleDoesNotRepeatCurrentInExpandedText() {
  val n = format(counter=945.0)
  assertEquals("86% • 41,3 °C • +1.018 mA",n.title)
  assertEquals("100% previsto: ~14h45",n.summary)
  assertEquals("100% previsto: ~14h45\nMédia pelo contador: +945 mA",n.expandedText)
  assertFalse(n.expandedText.contains("+1.018 mA"))
 }
 @Test fun similarMeanIsOmittedToAvoidVisualDuplication() {
  val n = format(counter=1020.0)
  assertFalse(n.expandedText.contains("1.020 mA"))
 }
 @Test fun independentAndroidMeanIsShownWhenMateriallyDifferent() {
  val n = format(current=437.0,android=540.0,remaining=null)
  assertTrue(n.title.endsWith("+437 mA"))
  assertTrue(n.expandedText.contains("Média do Android: +540 mA"))
 }
 @Test fun hardwareZeroMeanIsNotPresentedAsUsefulDuringCharge() {
  val n = format(current=531.0, counter=423.0, android=0.0)
  assertFalse(n.expandedText.contains("0 mA"))
  assertTrue(n.expandedText.contains("Média pelo contador: +423 mA"))
 }
 @Test fun counterMeanReplacesUnconfirmedZeroButIsLabeledAsMean() {
  val n = format(current=0.0,counter=423.0)
  assertTrue(n.title.endsWith("+423 mA"))
  assertTrue(n.expandedText.contains("Corrente: média pelo contador"))
  assertFalse(n.expandedText.contains("0 mA"))
 }
 @Test fun zeroDuringChargingWithoutCounterIsHiddenNotClaimedAsNoConsumption() {
  val n = format(current=0.0,counter=null,android=0.0)
  assertEquals("86% • 41,3 °C",n.title)
  assertFalse(n.expandedText.contains("0 mA"))
 }
 @Test fun zeroInFullStateIsLabeledAsReadingNotClaimedAsAbsentConsumption() {
  val n = format(mode=Mode.FULL, current=0.0,percent=100)
  assertEquals("Bateria cheia",n.summary)
  assertEquals("100% • 41,3 °C",n.title)
  assertTrue(n.expandedText.contains("Sensor informou 0 mA"))
 }
 @Test fun hundredWhileStillChargingIsNotDeclaredFull() {
  val n = format(percent=100,current=0.0)
  assertEquals("Carregando • 100%",n.summary)
  assertFalse(n.expandedText.contains("Bateria cheia"))
  assertFalse(n.expandedText.contains("previsto"))
 }
 @Test fun idleReportsPausedState() {
  val n = format(mode=Mode.IDLE,current=null,source=CurrentSource.NONE,remaining=null)
  assertEquals("Carga pausada",n.summary)
 }
 @Test fun contradictoryPositiveCurrentDuringDischargeIsNotExposed() {
  val n = format(mode=Mode.DISCHARGING,current=400.0,rate=10.0)
  assertFalse(n.title.contains("400 mA"))
  assertFalse(n.expandedText.contains("400 mA"))
  assertFalse(n.expandedText.contains("Ritmo"))
  assertEquals("Autonomia: 25 min",n.summary)
 }
 @Test fun dischargeKeepsNegativeCurrentAndEstimateOfRuntime() {
  val n = format(mode=Mode.DISCHARGING,current=-450.0,rate=-10.0)
  assertTrue(n.title.endsWith("−450 mA"))
  assertEquals("Autonomia: 25 min",n.summary)
  assertFalse(n.expandedText.contains("Ritmo:"))
 }
 @Test fun estimatedCurrentHasClearSourceDescription() {
  val n = format(current=1620.0,source=CurrentSource.CAPACITY)
  assertTrue(n.expandedText.contains("Corrente: estimativa por capacidade"))
 }
 @Test fun averageCurrentHasClearSourceDescription() {
  val n = format(current=945.0,source=CurrentSource.AVERAGE)
  assertTrue(n.expandedText.contains("Corrente: média do Android"))
 }
 @Test fun noCurrentShowsStatusWithoutFictitiousReading() {
  val n = format(current=null,source=CurrentSource.NONE,rate=null)
  assertEquals("86% • 41,3 °C",n.title)
  assertEquals("100% previsto: ~14h45",n.expandedText)
 }
 @Test fun unknownReadingsRemainUnavailable() {
  val n = format(mode=Mode.UNKNOWN,current=null,source=CurrentSource.NONE,temp=null,remaining=null)
  assertEquals("86% • — °C",n.title)
  assertEquals("Aguardando leitura",n.summary)
 }
 @Test fun smallValidCurrentKeepsPrecision() {
  val n = format(current=0.4)
  assertTrue(n.title.endsWith("+0,4 mA"))
 }
 @Test fun temperatureWarningIsVisibleWithoutExpansion() {
  val n = format(temp=43.0)
  assertEquals("Temperatura elevada",n.summary)
  assertTrue(n.expandedText.contains("acima do limite (43 °C)"))
  assertTrue(n.expandedText.contains("100% previsto: ~14h45"))
 }
 @Test fun customTemperatureThresholdIsRespected() {
  assertEquals("Temperatura elevada",format(temp=41.0,limit=40).summary)
  assertEquals("100% previsto: ~14h45",format(temp=41.0,limit=43).summary)
 }
 @Test fun invalidTemperatureDoesNotTriggerWarning() {
  assertEquals("100% previsto: ~14h45",format(temp=Double.NaN,limit=40).summary)
  assertEquals("100% previsto: ~14h45",format(temp=41.0,limit=100).summary)
 }
 @Test fun midnightPredictionIsLabeledTomorrow() {
  val late = ZonedDateTime.of(2026,10,12,23,50,0,0,zone).toInstant().toEpochMilli()
  val n = BatteryNotificationFormatter.format(92,35.0,false,Mode.CHARGING,25*60_000L,
   800.0,CurrentSource.NOW,null,15.0,late,zone)
  assertEquals("100% previsto: ~00h15 amanhã",n.summary)
 }
 @Test fun twoDaysLaterPredictionShowsDate() {
  val eta = 2L * 24L * 60L * 60_000L
  val n = BatteryNotificationFormatter.format(40,35.0,false,Mode.CHARGING,eta,
   100.0,CurrentSource.NOW,null,4.0,now,zone)
  assertEquals("100% previsto: ~14h20 em 14/10",n.summary)
 }
 @Test fun etaIsNotInventedWhenMissing() {
  assertEquals("Carregando",format(remaining=null).summary)
 }
 @Test fun whenClockIsMissingOnlyDurationCanBeShown() {
  val n = BatteryNotificationFormatter.format(86,41.3,false,Mode.CHARGING,25*60_000L,
   1000.0,CurrentSource.NOW,null,25.0,null,zone)
  assertEquals("Até 100%: 25 min",n.summary)
 }
 @Test fun roundingNeverPredictsPastClockTime() {
  val at=ZonedDateTime.of(2026,10,12,14,21,0,0,zone).toInstant().toEpochMilli()
  assertEquals("100% previsto: ~14h25",BatteryNotificationFormatter.predictedChargeTime(30_000,at,zone))
 }
 @Test fun directionWarningKeepsDiagnosticAvailable() {
  val n=BatteryNotificationFormatter.format(75,39.0,false,Mode.DISCHARGING,null,null,
   CurrentSource.NONE,null,null,directionWarning=true)
  assertTrue(n.expandedText.contains("Sinal da corrente em verificação"))
 }
}
