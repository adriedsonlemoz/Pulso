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
 private val saoPaulo = ZoneId.of("America/Sao_Paulo")
 private val fixedNow = ZonedDateTime.of(2026, 10, 12, 14, 19, 0, 0, saoPaulo).toInstant().toEpochMilli()
 private fun format(mode: Mode = Mode.CHARGING, current: Double? = 1018.0,
                    source: CurrentSource = CurrentSource.NOW, mean: Double? = null,
                    rate: Double? = 25.4, percent: Int? = 86) =
  BatteryNotificationFormatter.format(percent, 41.3, false, mode, 25*60_000L, current, source, mean, rate, fixedNow, saoPaulo)

 @Test fun loadingLabelsAndMeanWhenMeasuredIndependently() {
  val n = format(mean=945.0)
  assertEquals("86% • 41,3 °C • +1.018 mA",n.title)
  assertEquals("100% previsto: ~14h45",n.summary)
  assertEquals("100% previsto: ~14h45\nCarregando: +1.018 mA\nMédia: +945 mA\nRitmo: +25,4 %/h",n.expandedText)
 }
 @Test fun absentMeanDoesNotShowPlaceholder() {
  val n = format()
  assertFalse(n.expandedText.contains("Média"))
  assertFalse(n.expandedText.contains("Indisponível"))
 }
 @Test fun similarMeanIsNotRepeated() {
  val n = format(mean=1020.0)
  assertFalse(n.expandedText.contains("Média"))
 }
 @Test fun dischargeUsesNegativeSpendingLabel() {
  val n = format(mode=Mode.DISCHARGING, current=-450.0, rate=-10.0)
  assertTrue(n.title.endsWith("−450 mA"))
  assertTrue(n.expandedText.contains("Gastando: −450 mA"))
  assertTrue(n.expandedText.contains("Ritmo: −10,0 %/h"))
 }
 @Test fun meanIsIdentifiedWithoutImplyingInstantaneousMeasurement() {
  val n = format(current=945.0,source=CurrentSource.AVERAGE,mean=945.0)
  assertTrue(n.expandedText.contains("Carregando (média): +945 mA"))
  assertFalse(n.expandedText.contains("\nMédia:"))
 }
 @Test fun estimatedCurrentIsMarkedAsEstimate() {
  val n = format(current=1620.0,source=CurrentSource.CAPACITY)
  assertTrue(n.expandedText.contains("Carregando (estimativa): +1.620 mA"))
 }
 @Test fun noCurrentHidesUnavailableLineButKeepsValidRate() {
  val n = format(current=null,source=CurrentSource.NONE)
  assertEquals("86% • 41,3 °C",n.title)
  assertFalse(n.expandedText.contains("Corrente"))
  assertFalse(n.expandedText.contains("Indisponível"))
  assertTrue(n.expandedText.contains("Ritmo: +25,4 %/h"))
 }
 @Test fun withoutEtaNoDuplicateChargingLabelInExpandedBody() {
  val n = BatteryNotificationFormatter.format(55,38.0,false,Mode.CHARGING,null,1000.0,CurrentSource.NOW,null,null)
  assertEquals("Carregando",n.summary)
  assertEquals("Carregando: +1.000 mA",n.expandedText)
 }
 @Test fun tinyCurrentWouldRoundToZeroAndIsOmitted() {
  val n = format(current=0.4)
  assertEquals("86% • 41,3 °C",n.title)
  assertFalse(n.expandedText.contains("mA"))
 }
 @Test fun emptyReadingsLeaveOnlyStatus() {
  val n = format(current=null,source=CurrentSource.NONE,rate=null)
  assertEquals("100% previsto: ~14h45",n.expandedText)
 }
 @Test fun chargingAtHundredDoesNotPredictZeroMinutes() {
  val n = format(percent=100)
  assertEquals("Bateria em 100%",n.summary)
  assertFalse(n.expandedText.contains("Até 100%"))
 }
 @Test fun invalidSignIsNotShownAsConsumption() {
  val n = format(mode=Mode.DISCHARGING,current=400.0,rate=10.0)
  assertFalse(n.title.contains("400 mA"))
  assertFalse(n.expandedText.contains("Gastando:"))
  assertFalse(n.expandedText.contains("Ritmo:"))
 }
 @Test fun idleDoesNotReportCurrentEvenIfStrayValueIsPresent() {
  val n = format(mode=Mode.IDLE)
  assertFalse(n.title.contains("mA"))
  assertEquals("Carga pausada",n.expandedText)
 }
 @Test fun midnightChangeIsLabeledTomorrow() {
  val late = ZonedDateTime.of(2026,10,12,23,50,0,0,saoPaulo).toInstant().toEpochMilli()
  val n = BatteryNotificationFormatter.format(92,35.0,false,Mode.CHARGING,25*60_000L,
   800.0,CurrentSource.NOW,null,15.0,late,saoPaulo)
  assertEquals("100% previsto: ~00h15 amanhã", n.summary)
 }
 @Test fun longPredictionLabelsDate() {
  val eta = 2L * 24L * 60L * 60_000L
  val n = BatteryNotificationFormatter.format(40,35.0,false,Mode.CHARGING,eta,
   100.0,CurrentSource.NOW,null,4.0,fixedNow,saoPaulo)
  assertEquals("100% previsto: ~14h20 em 14/10", n.summary)
 }
 @Test fun clockTimeNotDisplayedWithoutValidatedEta() {
  val n = BatteryNotificationFormatter.format(55,35.0,false,Mode.CHARGING,null,
   1000.0,CurrentSource.NOW,null,null,fixedNow,saoPaulo)
  assertEquals("Carregando",n.summary)
 }
 @Test fun clockTimeNotDisplayedAtOneHundredPercent() {
  val n = BatteryNotificationFormatter.format(100,35.0,false,Mode.CHARGING,0,
   1000.0,CurrentSource.NOW,null,null,fixedNow,saoPaulo)
  assertEquals("Bateria em 100%",n.summary)
 }
 @Test fun unavailableWallClockFallsBackToDurationWithoutInventingTime() {
  val n = BatteryNotificationFormatter.format(86,41.3,false,Mode.CHARGING,25*60_000L,
   1000.0,CurrentSource.NOW,null,25.0,null,saoPaulo)
  assertEquals("Até 100%: 25 min",n.summary)
 }
 @Test fun dischargingRetainsDurationNotFullChargeClock() {
  val n = format(mode=Mode.DISCHARGING,current=-450.0,rate=-10.0)
  assertEquals("Autonomia: 25 min",n.summary)
 }
 @Test fun fiveMinuteRoundingIsApproximate() {
  val n = BatteryNotificationFormatter.format(86,41.3,false,Mode.CHARGING,23*60_000L,
   1000.0,CurrentSource.NOW,null,25.0,fixedNow,saoPaulo)
  assertEquals("100% previsto: ~14h40",n.summary)
 }
 @Test fun unavailableTemperatureAndValueUseCompactPlaceholders() {
  val n = BatteryNotificationFormatter.format(55,null,false,Mode.UNKNOWN,null,null,CurrentSource.NONE,null,null)
  assertEquals("55% • — °C",n.title)
  assertEquals("Aguardando leitura",n.expandedText)
 }
}
