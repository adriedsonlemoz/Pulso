package br.com.pulsobateria.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.pulsobateria.core.ChargeComparison
import br.com.pulsobateria.core.ChargeWindowStat
import br.com.pulsobateria.core.ChargingComparison
import br.com.pulsobateria.core.TelemetryDatum
import br.com.pulsobateria.data.Session
import br.com.pulsobateria.data.TelemetrySample
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun TelemetrySample.datum()=TelemetryDatum(sessionId,timestamp,elapsedMs,mode,percent,currentMa,currentSource,temperatureC,counterUah)

/** Comparação sem identificação automática de cabo/modelo: fonte Android + rótulo manual opcional. */
@Composable
fun ChargeCompareCard(chargingSessions: List<Session>, vm: PulsoViewModel) {
 val sessions=chargingSessions.filter { it.mode=="CHARGING" && it.closed && it.samples>=2 }.take(20)
 if(sessions.size<2) return
 var firstChoice by rememberSaveable { mutableLongStateOf(0L) }
 var secondChoice by rememberSaveable { mutableLongStateOf(0L) }
 val first=sessions.find { it.id==firstChoice } ?: sessions.first()
 val second=sessions.find { it.id==secondChoice && it.id!=first.id } ?: sessions.firstOrNull { it.id!=first.id } ?: return
 val a by remember(first.id) { vm.sessionTelemetry(first.id) }.collectAsStateWithLifecycle(initialValue=emptyList())
 val b by remember(second.id) { vm.sessionTelemetry(second.id) }.collectAsStateWithLifecycle(initialValue=emptyList())
 val comparison=remember(a,b) { ChargingComparison.compare(a.map { it.datum() },b.map { it.datum() }) }
 val muted=MaterialTheme.colorScheme.onSurfaceVariant
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text("Comparar recargas",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text("Selecione duas sessões e identifique o carregador, se souber. O Android não informa o modelo do cabo ou da fonte.",style=MaterialTheme.typography.bodySmall,color=muted)
   ChargerChooser("Recarga 1",first,sessions,second.id,{firstChoice=it})
   ChargerChooser("Recarga 2",second,sessions,first.id,{secondChoice=it})
   if(comparison.comparable) {
    Text(comparison.message,style=MaterialTheme.typography.bodySmall,color=muted)
    HorizontalDivider()
    val firstStats=comparison.first!!;val secondStats=comparison.second!!
    ChargeMeasureRow(first,firstStats)
    HorizontalDivider()
    ChargeMeasureRow(second,secondStats)
    val diff=firstStats.pacePercentHour-secondStats.pacePercentHour
    Text("Na faixa comparável, a recarga ${if(diff>=0) "1" else "2"} foi ${String.format(Locale.forLanguageTag("pt-BR"),"%.1f",kotlin.math.abs(diff))}%/h mais rápida.",style=MaterialTheme.typography.bodySmall)
   } else {
    Text(comparison.message,style=MaterialTheme.typography.bodySmall,color=muted)
    Text("Os percentuais totais e a duração das sessões continuam disponíveis nos cartões abaixo. Não comparamos velocidades de faixas diferentes.",style=MaterialTheme.typography.bodySmall,color=muted)
   }
   HorizontalDivider()
   Text("Nomear carregadores (opcional)",fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.titleSmall)
   ChargerLabelEditor(first,vm)
   ChargerLabelEditor(second,vm)
   Text("As temperaturas são médias das amostras da faixa comparável; mAh só aparecem quando há contador de carga válido. Uma taxa em %/h não comprova potência elétrica do carregador. Trocar de cabo sem alterar o tipo de conexão não é detectado automaticamente.",style=MaterialTheme.typography.bodySmall,color=muted)
  }
 }
}

@Composable
private fun ChargerChooser(label: String, selected: Session, sessions: List<Session>, otherId: Long, onSelect: (Long)->Unit) {
 var expanded by remember { mutableStateOf(false) }
 val dateFormat=remember { SimpleDateFormat("dd/MM HH:mm",Locale.forLanguageTag("pt-BR")) }
 Column {
  Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Box {
   OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {
    Text("${dateFormat.format(Date(selected.started))} • ${selected.chargerLabel ?: selected.chargeSource ?: "Fonte não registrada"} • ${selected.initial}%→${selected.final}%",maxLines=2)
   }
   DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
    sessions.filter { it.id!=otherId }.forEach { session ->
     DropdownMenuItem(text={Text("${dateFormat.format(Date(session.started))} • ${session.chargerLabel ?: session.chargeSource ?: "Fonte não registrada"} • ${session.initial}%→${session.final}%")},
      onClick={onSelect(session.id);expanded=false})
    }
   }
  }
 }
}

@Composable
private fun ChargeMeasureRow(session: Session, data: ChargeWindowStat) {
 val locale=Locale.forLanguageTag("pt-BR")
 Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
  Text(session.chargerLabel ?: session.chargeSource ?: "Recarga #${session.id}",fontWeight=FontWeight.SemiBold)
  Text("Tempo observado: ${data.observedMs/60_000} min • ${String.format(locale,"%.1f",data.pacePercentHour)}%/h")
  Text("Temperatura média: ${data.averageTemperatureC?.let { String.format(locale,"%.1f °C",it) } ?: "Sem medição"}")
  Text("Carga adicionada: ${data.addedMah?.let { String.format(locale,"%.0f mAh",it) } ?: "Sem contador válido"}")
 }
}

@Composable
private fun ChargerLabelEditor(session: Session, vm: PulsoViewModel) {
 var value by rememberSaveable(session.id) { mutableStateOf(session.chargerLabel.orEmpty()) }
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  OutlinedTextField(value=value,onValueChange={if(it.length<=40 && it.none { ch -> Character.isISOControl(ch) }) value=it},
   label={Text("Recarga #${session.id}")},placeholder={Text("Ex.: Carregador de casa")},
   singleLine=true,modifier=Modifier.weight(1f))
  TextButton(onClick={vm.setChargerLabel(session.id,value)},enabled=value.trim()!=session.chargerLabel.orEmpty()) { Text("Salvar") }
 }
}
