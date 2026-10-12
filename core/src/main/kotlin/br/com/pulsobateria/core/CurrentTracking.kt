package br.com.pulsobateria.core

import java.util.ArrayDeque
import kotlin.math.abs

/** A origem importa: mA de uma propriedade, de um contador e de %/h não são equivalentes. */
enum class CurrentSource(val label: String) {
 NOW("Android • instantânea"), AVERAGE("Android • média"),
 COUNTER("Contador de carga • média"), CAPACITY("Estimativa por %/h e capacidade"),
 NONE("Indisponível")
}

data class CurrentResult(
 val ma: Double? = null, val source: CurrentSource = CurrentSource.NONE,
 val note: String = "Aguardando leituras confiáveis.",
 val rawNow: Int? = null, val rawAverage: Int? = null, val rawCounter: Int? = null,
 val counterMeanMa: Double? = null,
 val nowAdjustment: String = "Aguardando", val averageAdjustment: String = "Aguardando"
)

/** Estima a corrente MÉDIA pela variação real de BATTERY_PROPERTY_CHARGE_COUNTER (µAh). */
class ChargeCounterTracker {
 private data class Item(val time: Long, val value: Int)
 private val history = ArrayDeque<Item>()
 private var mode: Mode = Mode.UNKNOWN

 fun observe(raw: Int?, state: Mode, now: Long): Double? {
  if (state != Mode.CHARGING && state != Mode.DISCHARGING) { reset(); return null }
  if (raw == null || raw <= 0 || raw == Int.MAX_VALUE || raw > 100_000_000 || now < 0) { reset(); return null }
  if (mode != state || history.isNotEmpty() && (now <= history.last().time || now-history.last().time > 180_000L)) reset()
  mode = state
  if (history.isNotEmpty()) {
   val delta = raw.toLong() - history.last().value.toLong()
   if (state == Mode.CHARGING && delta < 0 || state == Mode.DISCHARGING && delta > 0) reset().also { mode=state }
  }
  history.addLast(Item(now,raw))
  // Janela limitada a 10 min: sem extrapolar lacunas, sem confundir taxa instantânea e média.
  while(history.size > 2 && now-history.first().time > 600_000L) history.removeFirst()
  if (history.size < 3) return null
  val elapsed = history.last().time-history.first().time
  if (elapsed < 90_000L || elapsed > 600_000L) return null
  val change = history.last().value.toLong()-history.first().value.toLong()
  if (abs(change) < 1_000L) return null
  val ma = change*3_600.0/elapsed
  return ma.takeIf { it.isFinite() && abs(it) in 10.0..30_000.0 &&
   (state == Mode.CHARGING && it > 0 || state == Mode.DISCHARGING && it < 0) }
 }
 private fun reset() { history.clear(); mode=Mode.UNKNOWN }
}

private enum class CurrentUnit(val divisor: Double, val label: String) {
 MICROAMPS(1_000.0,"µA → mA"), MILLIAMPS(1.0,"mA informado pelo fabricante")
}
private data class ProbeResult(val ma: Double?, val note: String)

/** Observação em memória; somente decisões confirmadas podem ser persistidas por DataStore. */
private class CurrentProbe {
 private var flipped = false
 private var polarityConfirmed = false
 private var unitConfirmed = false
 private var polarityCandidate: Boolean? = null
 private var polarityCount = 0
 private var polarityStart = 0L
 private var polarityLast = 0L
 private var lastMode = Mode.UNKNOWN
 private var unit: CurrentUnit? = null
 private var unitCandidate: CurrentUnit? = null
 private var unitCount = 0
 private var unitStart = 0L
 private var unitLast = 0L

 /** Serializa somente decisões confirmadas. A referência de carga e as amostras nunca são persistidas. */
 fun calibrationCode(): String {
  val sign = if (polarityConfirmed) (if (flipped) "1" else "0") else "?"
  val scale = if (!unitConfirmed) "?" else when(unit) {
   CurrentUnit.MICROAMPS -> "U"; CurrentUnit.MILLIAMPS -> "M"; else -> "?"
  }
  return "$sign$scale"
 }
 fun loadCalibration(code: String) {
  if(code.length!=2) return
  if(code[0] in "01") { flipped=code[0]=='1'; polarityConfirmed=true }
  when(code[1]) {
   'U' -> { unit=CurrentUnit.MICROAMPS; unitConfirmed=true }
   'M' -> { unit=CurrentUnit.MILLIAMPS; unitConfirmed=true }
  }
 }

 fun observe(raw: Int?, mode: Mode, now: Long, referenceMa: Double?): ProbeResult {
  if (mode != Mode.CHARGING && mode != Mode.DISCHARGING || raw == null ||
   raw == Int.MIN_VALUE || raw == Int.MAX_VALUE || abs(raw.toLong()) > 30_000_000L || now < 0) {
   return ProbeResult(null,"Leitura ausente/inválida ou bateria sem carga/descarga")
  }
  if (lastMode != mode || now <= polarityLast || now-polarityLast > 180_000L) {
   polarityCandidate=null; polarityCount=0
  }
  lastMode=mode
  val expectedSign = if(mode==Mode.CHARGING) 1 else -1
  val rawSign = raw.compareTo(0)
  if (rawSign != 0) {
   val proposedFlip = rawSign != expectedSign
   if (polarityCandidate != proposedFlip) { polarityCandidate=proposedFlip; polarityCount=0; polarityStart=now }
   polarityCount++
   polarityLast=now
   if (polarityCount >= 4 && now-polarityStart >= 60_000L) {
    flipped=proposedFlip; polarityConfirmed=true
   }
  }
  // Muitos aparelhos devolvem zero como substituto de indisponível. Durante carga/descarga,
  // preferir o contador independente à falsa medição instantânea de 0 mA.
  if (raw == 0) return ProbeResult(null,"Zero informado pelo sensor; não confirmado como corrente real")
  if (rawSign*(if(flipped) -1 else 1) != expectedSign) {
   return ProbeResult(null,"Polaridade contraditória; aguardando 4 amostras em ≥60 s para calibrar")
  }
  // Acima de 30 mil, a escala mA seria >30 A: usamos a unidade µA documentada.
  // Abaixo disso as escalas µA e mA se sobrepõem e exigem referência independente.
  val magnitude = abs(raw.toLong())
  if (magnitude > 30_000L) {
   unit=CurrentUnit.MICROAMPS; unitConfirmed=true
  }
  if (magnitude <= 30_000L && referenceMa != null && abs(referenceMa) >= 50.0) {
   val normalizedReference=abs(referenceMa)
   val ratioUa = magnitude/1_000.0/normalizedReference
   val ratioMa = magnitude.toDouble()/normalizedReference
   val uaMatches = ratioUa in 0.25..4.0
   val maMatches = ratioMa in 0.25..4.0
   val candidate = when {
    uaMatches && !maMatches -> CurrentUnit.MICROAMPS
    maMatches && !uaMatches -> CurrentUnit.MILLIAMPS
    else -> null
   }
   if (candidate == null) { unitCandidate=null; unitCount=0 }
   else {
    if (unitCandidate != candidate || now <= unitLast || now-unitLast > 180_000L) {
     unitCandidate=candidate; unitCount=0; unitStart=now
    }
    unitCount++; unitLast=now
    if(unitCount >= 3 && now-unitStart >= 60_000L) {
     unit=candidate; unitConfirmed=true
    }
   }
  }
  // Uma calibração anterior só é usada se não contradizer uma referência disponível.
  if (referenceMa != null && unit != null && abs(referenceMa) >= 50) {
   val presumed=abs(raw.toDouble() / unit!!.divisor)
   val proportion=presumed / abs(referenceMa)
   if(proportion !in 0.1..10.0) { unit=null; unitConfirmed=false }
  }
  val decidedUnit = unit ?: return ProbeResult(null,"Unidade ambígua: aguardando contador de carga para distinguir µA de mA")
  // Se a unidade calibrada era mA e o bruto muda de escala abruptamente, não usar resultado absurdo.
  val value = raw.toDouble()/decidedUnit.divisor*(if(flipped) -1 else 1)
  if (!value.isFinite() || abs(value) > 30_000) return ProbeResult(null,"Leitura fora do limite de 30 A")
  return ProbeResult(value, "${decidedUnit.label}; ${if(flipped) "polaridade invertida após amostras consistentes" else "polaridade Android"}")
 }
}

class CurrentTracker {
 private val currentNow = CurrentProbe()
 private val currentAverage = CurrentProbe()
 private val counter = ChargeCounterTracker()
 /** Código sem timestamp; útil para evitar regravar DataStore a cada leitura. */
 fun calibrationCode(): String {
  val a=currentNow.calibrationCode()
  val b=currentAverage.calibrationCode()
  return if(a=="??" && b=="??") "" else "$a:$b"
 }
 /** Calibração específica deste aparelho e válida somente por 7 dias. */
 fun loadCalibration(saved: String?, wallMillis: Long): Boolean {
  val parts=saved?.split(';') ?: return false
  if(parts.size!=2) return false
  val whenSaved=parts[0].toLongOrNull() ?: return false
  if(whenSaved<0 || wallMillis<whenSaved || wallMillis-whenSaved>7L*24*3600*1000) return false
  val codes=parts[1].split(':')
  if(codes.size!=2 || codes.any { it.length!=2 }) return false
  currentNow.loadCalibration(codes[0]); currentAverage.loadCalibration(codes[1])
  return true
 }

 fun observe(
  rawNow: Int?, rawAverage: Int?, rawCounter: Int?, mode: Mode,
  elapsed: Long, ratePercentHour: Double?, capacityMah: Int?
 ): CurrentResult {
  val mean = counter.observe(rawCounter,mode,elapsed)
  val now = currentNow.observe(rawNow,mode,elapsed,mean)
  val avg = currentAverage.observe(rawAverage,mode,elapsed,mean)
  val estimate = BatteryMath.estimatedCurrentMa(ratePercentHour,capacityMah,mode)
  val source = when {
   now.ma != null -> CurrentSource.NOW
   avg.ma != null -> CurrentSource.AVERAGE
   mean != null -> CurrentSource.COUNTER
   estimate != null -> CurrentSource.CAPACITY
   else -> CurrentSource.NONE
  }
  val value = when(source) {
   CurrentSource.NOW -> now.ma
   CurrentSource.AVERAGE -> avg.ma
   CurrentSource.COUNTER -> mean
   CurrentSource.CAPACITY -> estimate
   CurrentSource.NONE -> null
  }
  val note = when(source) {
   CurrentSource.NOW -> "Corrente líquida na bateria. ${now.note}. Não é a corrente de entrada USB."
   CurrentSource.AVERAGE -> "Média disponibilizada pelo Android. ${avg.note}. Não é instantânea."
   CurrentSource.COUNTER -> "Média estimada da variação de carga armazenada (µAh) em ≥90 s; depende do contador do fabricante."
   CurrentSource.CAPACITY -> "Aproximação a partir de %/h e capacidade útil informada; não é medida pelo Android."
   CurrentSource.NONE -> "Sem corrente confiável: ${now.note}; ${avg.note}. A taxa em %/h pode estar disponível separadamente."
  }
  return CurrentResult(value,source,note,rawNow?.takeUnless { it==Int.MIN_VALUE || it==Int.MAX_VALUE },
   rawAverage?.takeUnless { it==Int.MIN_VALUE || it==Int.MAX_VALUE },
   rawCounter?.takeUnless { it==Int.MIN_VALUE || it==Int.MAX_VALUE },mean,now.note,avg.note)
 }
}
