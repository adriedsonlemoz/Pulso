package br.com.pulsobateria.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.pulsobateria.core.TelemetryAnalysis
import br.com.pulsobateria.core.TelemetryDatum
import br.com.pulsobateria.data.TelemetrySample
import java.util.Locale
import kotlin.math.abs

private val BR = Locale.forLanguageTag("pt-BR")
private fun signedMah(v: Double): String = String.format(BR,"%+.1f mAh",v)

/** Resumo da tela inicial. Metadados técnicos permanecem acessíveis em outra tela. */
@Composable
fun TelemetrySummaryCard(samples: List<TelemetrySample>, now: Long, onDetails: () -> Unit) {
 var minutes by remember { mutableIntStateOf(20) }
 val readings=remember(samples) { samples.map { TelemetryDatum(it.sessionId,it.timestamp,it.elapsedMs,it.mode,
  it.percent,it.currentMa,it.currentSource,it.temperatureC,it.counterUah) } }
 val result=remember(readings,now,minutes) { TelemetryAnalysis.summarize(readings,now,minutes) }
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Text("Consumo por período",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
    SectionHelpButton("Consumo por período","Corrente líquida na bateria, não no cabo ou por aplicativo. A carga em mAh só cobre intervalos válidos. Cobertura indica tempo efetivamente observado, não precisão do sensor.")
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    listOf(5,20,60).forEach { n ->
     FilterChip(selected=minutes==n,onClick={minutes=n},label={Text("$n min")},modifier=Modifier.weight(1f))
    }
   }
   BoxWithConstraints(Modifier.fillMaxWidth()) {
    val compactStack=maxWidth<310.dp || LocalDensity.current.fontScale>1.2f
    val current: @Composable () -> Unit = {
     Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
      Text("Corrente média",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      Text(result.meanMa?.let(::currentText) ?: "Sem média suficiente",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
     }
    }
    val coverage: @Composable () -> Unit = {
     Column(horizontalAlignment=if(compactStack) Alignment.Start else Alignment.End,verticalArrangement=Arrangement.spacedBy(3.dp)) {
      Text("Dados disponíveis",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      Text(if(result.meanMa==null) "Insuficientes" else if(result.coveragePercent<80) "Parciais" else "${result.coveragePercent}% do período",
       style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
     }
    }
    if(compactStack) {
     Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { current(); coverage() }
    } else {
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
      Box(Modifier.weight(1f)) { current() }
      coverage()
     }
    }
   }
   HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
     Text("Carga acumulada",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     Text(result.deltaMah?.let(::signedMah) ?: "Indisponível",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
    }
    TextButton(onClick=onDetails) {
     Text("Análise completa")
     Spacer(Modifier.width(3.dp))
     Text("›")
    }
   }
  }
 }
}

/** Valores reais da telemetria Room. Gráficos não unem sessões ou lacunas >3 minutos. */
@Composable
fun TelemetryPanel(samples: List<TelemetrySample>, now: Long) {
 var selectedMinutes by remember { mutableIntStateOf(20) }
 var metric by remember { mutableStateOf("Corrente") }
 var graphMinutes by remember { mutableIntStateOf(60) }
 val readings=remember(samples) { samples.map { TelemetryDatum(it.sessionId,it.timestamp,it.elapsedMs,it.mode,
  it.percent,it.currentMa,it.currentSource,it.temperatureC,it.counterUah) } }
 val window=remember(readings,now,selectedMinutes) { TelemetryAnalysis.summarize(readings,now,selectedMinutes) }
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text("Médias por período",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.End) {
    SectionHelpButton("Médias por período","A corrente é líquida na bateria, não no cabo USB. Cobertura elétrica é tempo com medidas válidas, não precisão do sensor. mAh parciais não representam toda a janela.")
   }
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
    listOf(5,20,60).forEach { n ->
     FilterChip(selected=selectedMinutes==n,onClick={selectedMinutes=n},label={Text("$n min")},modifier=Modifier.weight(1f))
    }
   }
   Text(window.meanMa?.let(::currentText) ?: "Aguardando leituras suficientes",
    style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
   Text(window.deltaMah?.let { "Carga líquida: ${signedMah(it)}" } ?: "Carga acumulada: indisponível",
    style=MaterialTheme.typography.bodyMedium)
   Text("Cobertura elétrica: ${window.coveragePercent}% • Tempo monitorado: ${window.observedPercent}%",
    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   LinearProgressIndicator(progress={window.coveragePercent/100f},modifier=Modifier.fillMaxWidth())
   Text(window.origin?.let { "Origem: $it • ${window.intervalCount} intervalos válidos" }
    ?: "Sem uma sequência de corrente ou contador confiável no período.",
    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)

  }
 }
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text("Gráfico de medições",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()) {
    listOf("Corrente","Temperatura","Bateria").forEach { item ->
     FilterChip(selected=metric==item,onClick={metric=item},label={Text(item,style=MaterialTheme.typography.labelSmall)},modifier=Modifier.weight(1f))
    }
   }
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    FilterChip(selected=graphMinutes==60,onClick={graphMinutes=60},label={Text("1 hora")})
    FilterChip(selected=graphMinutes==360,onClick={graphMinutes=360},label={Text("6 horas")})
   }
   val data=remember(readings,now,graphMinutes) {
    readings.filter { it.timestamp in (now-graphMinutes*60_000L)..now }
     .sortedWith(compareBy<TelemetryDatum> { it.timestamp }.thenBy { it.elapsedMs })
   }
   val relevant=data.mapNotNull { p ->
    val value=when(metric) { "Corrente" -> p.currentMa?.takeIf { it.isFinite() && (p.source == "NOW" || p.source == "AVERAGE" || p.source == "COUNTER") }
     "Temperatura" -> p.temperatureC?.takeIf { it.isFinite() }
     else -> p.percent.toDouble() }
    value?.let { p to it }
   }
   if(relevant.size<2) {
    Text("Ainda não há duas medições disponíveis para esse gráfico.",modifier=Modifier.padding(vertical=24.dp),
     style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   } else {
    val bottom=relevant.minOf { it.second }
    val top=relevant.maxOf { it.second }
    val padding=(top-bottom).coerceAtLeast(1.0)*0.12
    val low=bottom-padding; val high=top+padding
    val graphColor=MaterialTheme.colorScheme.primary
    val gridColor=MaterialTheme.colorScheme.surfaceVariant
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     Column(Modifier.height(140.dp),verticalArrangement=Arrangement.SpaceBetween) {
      Text(String.format(BR,"%.1f",top),style=MaterialTheme.typography.labelSmall)
      Text(String.format(BR,"%.1f",bottom),style=MaterialTheme.typography.labelSmall)
     }
     Canvas(Modifier.weight(1f).height(140.dp)) {
      for(y in 0..3) drawLine(gridColor,Offset(0f,size.height*y/3f),Offset(size.width,size.height*y/3f),1.dp.toPx())
      val originTime=now-graphMinutes*60_000L
      val span=graphMinutes*60_000L
      val path=Path()
      var previous: TelemetryDatum?=null
      relevant.forEach { (reading,v) ->
       val x=((reading.timestamp-originTime).toFloat()/span*size.width).coerceIn(0f,size.width)
       val y=(size.height*(1f-(v-low).toFloat()/(high-low).toFloat())).coerceIn(0f,size.height)
       val prev=previous
       val connected=prev!=null && prev.sessionId==reading.sessionId && prev.mode==reading.mode &&
        reading.timestamp-prev.timestamp in 1..TelemetryAnalysis.MAX_GAP_MS &&
        reading.elapsedMs-prev.elapsedMs in 1..TelemetryAnalysis.MAX_GAP_MS &&
        abs((reading.timestamp-prev.timestamp)-(reading.elapsedMs-prev.elapsedMs))<=5000L
       if(connected) path.lineTo(x,y) else path.moveTo(x,y)
       previous=reading
      }
      drawPath(path,graphColor,style=Stroke(2.5.dp.toPx(),cap=StrokeCap.Round))
     }
    }
    Text("${relevant.size} leituras • ${when(metric) { "Corrente" -> "mA"; "Temperatura" -> "°C"; else -> "%" }} • lacunas não conectadas",
     style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
 }
}
