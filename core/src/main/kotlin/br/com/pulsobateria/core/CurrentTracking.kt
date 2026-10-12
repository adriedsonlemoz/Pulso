package br.com.pulsobateria.core

import java.util.ArrayDeque
import kotlin.math.abs

enum class CurrentSource(val label: String) {
 NOW("Android • instantânea"), AVERAGE("Android • média"),
 COUNTER("Contador de carga • média"), CAPACITY("Estimativa por %/h e capacidade"), NONE("Indisponível")
}

data class CurrentResult(
 val ma: Double? = null, val source: CurrentSource = CurrentSource.NONE,
 val note: String = "Aguardando leituras.",
 val rawNow: Int? = null, val rawAverage: Int? = null, val rawCounter: Int? = null,
 val counterMeanMa: Double? = null,
 val nowAdjustment: String = "Aguardando", val averageAdjustment: String = "Aguardando",
 val nowMa: Double? = null, val androidAverageMa: Double? = null,
 val counterWindowMs: Long? = null, val comparison: String = "Sem comparação temporal suficiente."
)

/** As propriedades Android declaram µA. Zero não é sentinela para targetSdk >= 28. */
internal fun validRawCurrent(raw: Int?): Boolean = raw != null && raw != Int.MIN_VALUE &&
 raw != Int.MAX_VALUE && abs(raw.toLong()) <= 30_000_000L
internal fun validCounter(raw: Int?): Boolean = raw != null && raw in 0..100_000_000

/** Média por diferença do contador; jamais compara seu sinal ao status de conexão. */
class ChargeCounterTracker {
 private data class Item(val time: Long, val value: Int)
 private val history = ArrayDeque<Item>()
 private var mode = Mode.UNKNOWN
 private var direction = 0
 var windowMs: Long? = null; private set
 fun observe(raw: Int?, state: Mode, now: Long): Double? {
  windowMs = null
  if (state == Mode.UNKNOWN || !validCounter(raw) || now < 0) { reset(); return null }
  if (mode != state || history.isNotEmpty() &&
   (now <= history.last().time || now-history.last().time > 180_000L)) reset()
  mode = state
  if (history.isNotEmpty()) {
   val dt = now-history.last().time
   val delta = raw!!.toLong()-history.last().value
   val nextDirection = delta.compareTo(0L)
   if (abs(delta*3_600.0/dt) > 30_000.0 || nextDirection != 0 && direction != 0 && nextDirection != direction) {
    reset(); mode = state
   } else if (nextDirection != 0) direction = nextDirection
  }
  history.addLast(Item(now,raw!!))
  while (history.size > 2 && now-history.first().time > 600_000L) history.removeFirst()
  if (history.size < 3) return null
  val elapsed = now-history.first().time
  val change = raw.toLong()-history.first().value
  if (elapsed !in 90_000L..600_000L || abs(change) < 1_000L) return null
  val ma = change*3_600.0/elapsed
  if (!ma.isFinite() || abs(ma) > 30_000.0) return null
  windowMs = elapsed
  return ma
 }
 private fun reset() { history.clear(); mode=Mode.UNKNOWN; direction=0; windowMs=null }
}

private enum class Transform(val factor: Double, val description: String) {
 U(0.001,"µA → mA; sinal Android"), INVERTED_U(-0.001,"µA → mA; sinal invertido corroborado pelo contador"),
 M(1.0,"mA do fabricante corroborado pelo contador"), INVERTED_M(-1.0,"mA e sinal invertido corroborados pelo contador")
}

/**
 * Hipótese de correção OEM, não calibração metrológica. Compara integral trapezoidal
 * de CURRENT_NOW e ΔCHARGE_COUNTER no MESMO intervalo. Três blocos não sobrepostos
 * de >=90 s, >=4 pontos, sinal estável e razão máxima/mínima <=1,25 são exigidos.
 * CURRENT_AVERAGE não entra aqui: a sua janela de hardware é desconhecida.
 */
private class CurrentProbe {
 private data class Sample(val time: Long, val raw: Int, val counter: Int)
 private var first: Sample? = null
 private var last: Sample? = null
 private var area = 0.0
 private var count = 0
 private var minMagnitude = 0.0
 private var maxMagnitude = 0.0
 private var direction = 0
 private var mode = Mode.UNKNOWN
 private var candidate: Transform? = null
 private var confirmations = 0
 private var correction: Transform? = null
 private var confirmedAt: Long? = null
 private var revalidated = false
 var comparison = "Sem comparação temporal suficiente."; private set
 companion object { const val TTL = 7L*24*3_600_000 }

 fun code(): String? = correction?.let { "$confirmedAt;${it.name}" }
 fun load(timestamp: Long, transform: String) {
  correction = Transform.valueOf(transform); confirmedAt=timestamp; revalidated=false
 }
 fun breakObservation() {
  first=null; last=null; area=0.0; count=0; direction=0
  candidate=null; confirmations=0; revalidated=false
 }
 private fun begin(sample: Sample) {
  first=sample; last=sample; area=0.0; count=1; direction=0
  minMagnitude=abs(sample.raw.toDouble()); maxMagnitude=minMagnitude
 }
 fun observe(raw: Int?, counter: Int?, state: Mode, time: Long, wall: Long): Pair<Double?,String> {
  confirmedAt?.let { if (wall < it || wall-it > TTL) {
   correction=null; confirmedAt=null; breakObservation()
  } }
  if (state != mode) { breakObservation(); mode=state }
  if (!validRawCurrent(raw) || state == Mode.UNKNOWN || time < 0) {
   breakObservation(); comparison="Leitura inválida: comparação interrompida."
   return null to "Leitura ausente/inválida ou estado desconhecido"
  }
  if (!validCounter(counter)) {
   breakObservation(); comparison="Contador indisponível; unidade e sinal seguem o contrato Android."
  } else {
   val sample=Sample(time,raw!!,counter!!)
   val previous=last
   if (previous == null) begin(sample)
   else {
    val dt=time-previous.time
    val delta=counter.toLong()-previous.counter
    val stepDirection=delta.compareTo(0L)
    val discontinuity=dt !in 1..180_000L ||
     abs(delta*3_600.0/dt.coerceAtLeast(1)) > 30_000.0 ||
     (direction != 0 && stepDirection != 0 && direction != stepDirection) ||
     raw.toLong()*previous.raw < 0L
    if (discontinuity) {
     breakObservation(); begin(sample); comparison="Lacuna, inversão ou salto do contador: evidência reiniciada."
    } else {
     if (stepDirection != 0) direction=stepDirection
     area += (previous.raw.toDouble()+raw)/2.0*dt
     count++; last=sample
     minMagnitude=minOf(minMagnitude,abs(raw.toDouble()))
     maxMagnitude=maxOf(maxMagnitude,abs(raw.toDouble()))
     val start=first!!
     val duration=time-start.time
     if (duration >= 90_000L && count >= 4) {
      val counterMa=(counter.toLong()-start.counter)*3_600.0/duration
      val rawMean=area/duration
      val stable=minMagnitude > 0.0 && maxMagnitude/minMagnitude <= 1.25
      val matches=if (stable && abs(counter.toLong()-start.counter) >= 1_000L && abs(counterMa) >= 10.0)
       Transform.entries.filter { rawMean*it.factor/counterMa in 0.75..1.25 && maxMagnitude*abs(it.factor) <= 30_000.0 }
      else emptyList()
      val match=matches.singleOrNull()
      comparison="Janela ${duration/1000} s; integral Android: ${rawMean/1000.0} mA; contador: $counterMa mA. " +
       if (match==null) "Sem evidência estável para ajuste." else "Hipótese ${match.name}."
      if (match==null) {
       candidate=null; confirmations=0
       // Uma janela inconclusiva não prova mudança de escala; suspender correção até nova evidência.
       revalidated=false
      } else {
       if (candidate != match) { candidate=match; confirmations=0 }
       confirmations++
       if (correction != null && correction != match) {
        correction=null; confirmedAt=null; revalidated=false
       }
       if (match == correction) revalidated=true
       if (confirmations >= 3 && match != Transform.U && correction != match) {
        correction=match; confirmedAt=wall; revalidated=true
       }
      }
      begin(sample) // Apenas a fronteira é compartilhada, nunca a duração dos blocos.
     }
    }
   }
  }
  if (correction != null && !revalidated) return null to "Ajuste salvo suspenso; aguardando confirmação temporal neste período"
  if (candidate != null && candidate != Transform.U && correction == null) {
   return null to "Possível desvio OEM; $confirmations de 3 blocos temporais, sem aplicar ajuste prematuro"
  }
  val transform=correction ?: Transform.U
  val ma=raw!!.toDouble()*transform.factor
  if (abs(ma) > 30_000.0) return null to "Fora do limite defensivo de 30 A"
  val warning=when {
   raw==0 -> "; zero informado, resolução do sensor não verificada"
   state==Mode.CHARGING && ma<0 || state==Mode.DISCHARGING && ma>0 -> "; sinal difere do status, investigar fabricante/instante da leitura"
   else -> ""
  }
  return ma to (transform.description+warning)
 }
}

class CurrentTracker {
 private val currentNow=CurrentProbe()
 private val counter=ChargeCounterTracker()
 fun calibrationRecord(deviceKey: String): String? = currentNow.code()?.let { "v2;$deviceKey;$it" }
 /** Rejeita o formato alpha.11, outro dispositivo/firmware, códigos malformados e prazo vencido. */
 fun loadCalibration(saved: String?, wallMillis: Long, deviceKey: String): Boolean {
  val p=saved?.split(';') ?: return false
  if (deviceKey.isBlank() || p.size != 4 || p[0] != "v2" || p[1] != deviceKey) return false
  val timestamp=p[2].toLongOrNull() ?: return false
  if (timestamp < 0 || wallMillis < timestamp || wallMillis-timestamp > CurrentProbe.TTL ||
   p[3] !in setOf("INVERTED_U","M","INVERTED_M")) return false
  currentNow.load(timestamp,p[3]); return true
 }
 fun observe(rawNow: Int?, rawAverage: Int?, rawCounter: Int?, mode: Mode,
  elapsed: Long, ratePercentHour: Double?, capacityMah: Int?, wallMillis: Long = elapsed): CurrentResult {
  val mean=counter.observe(rawCounter,mode,elapsed)
  val (now,adjustment)=currentNow.observe(rawNow,rawCounter,mode,elapsed,wallMillis)
  // Janela de hardware desconhecida: não corrigir escala/sinal usando uma janela inventada.
  val avg=rawAverage?.takeIf { validRawCurrent(it) && mode != Mode.UNKNOWN }?.div(1000.0)
  val avgNote=if(avg==null) "Média Android indisponível" else "µA → mA; sinal Android; janela definida pelo hardware"
  val estimate=BatteryMath.estimatedCurrentMa(ratePercentHour,capacityMah,mode)
  val source=when {
   now != null -> CurrentSource.NOW; avg != null -> CurrentSource.AVERAGE
   mean != null -> CurrentSource.COUNTER; estimate != null -> CurrentSource.CAPACITY
   else -> CurrentSource.NONE
  }
  val value=when(source) {
   CurrentSource.NOW -> now; CurrentSource.AVERAGE -> avg; CurrentSource.COUNTER -> mean
   CurrentSource.CAPACITY -> estimate; CurrentSource.NONE -> null
  }
  val note=when(source) {
   CurrentSource.NOW -> "Corrente líquida na bateria. $adjustment. Não é corrente USB."
   CurrentSource.AVERAGE -> "$avgNote. Não representa leitura instantânea."
   CurrentSource.COUNTER -> "Média calculada por ΔµAh/Δtempo; pode sofrer ajustes do medidor do fabricante."
   CurrentSource.CAPACITY -> "Estimativa por %/h e capacidade informada; não é leitura de sensor."
   CurrentSource.NONE -> "Sem leitura utilizável. $adjustment. $avgNote."
  }
  // Preservar sentinelas no diagnóstico permite distinguir ausência real de zero.
  return CurrentResult(value,source,note,rawNow,rawAverage,rawCounter,mean,adjustment,avgNote,
   now,avg,counter.windowMs,currentNow.comparison)
 }
}
