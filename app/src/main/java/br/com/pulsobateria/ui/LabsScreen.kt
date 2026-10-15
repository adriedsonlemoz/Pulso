package br.com.pulsobateria.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.pulsobateria.core.LabMath
import br.com.pulsobateria.data.LabTest
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class SkyOption(val key: String, val emoji: String, val title: String, val subtitle: String)
private val skyOptions = listOf(
 SkyOption("forte","☀️","Sol forte","Céu limpo"),
 SkyOption("parcial","🌤️","Entre nuvens","Sol intermitente"),
 SkyOption("nublado","☁️","Nublado","Sem sol direto"),
 SkyOption("chuva","🌧️","Chuva","Céu chuvoso")
)
private fun skyLabel(key: String) = skyOptions.firstOrNull { it.key==key }?.title ?: "Não informado"
private fun duration(ms: Long): String {
 val seconds=ms.coerceAtLeast(0)/1000L
 return "%02d:%02d:%02d".format(Locale.getDefault(),seconds/3600,(seconds/60)%60,seconds%60)
}
private fun date(time: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(time))

@Composable
fun LabsScreen(vm: PulsoViewModel, modifier: Modifier = Modifier, onMonitor: (Boolean) -> Unit) {
 val active by vm.labCurrent.collectAsState()
 val history by vm.labHistory.collectAsState()
 var sky by rememberSaveable { mutableStateOf("nublado") }
 var panel by rememberSaveable { mutableStateOf("8") }
 var customWatts by rememberSaveable { mutableStateOf("") }
 var timer by rememberSaveable { mutableStateOf("120") }
 var customMinutes by rememberSaveable { mutableStateOf("") }
 var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
 LaunchedEffect(active?.id) {
  while(true) { now = System.currentTimeMillis(); delay(1000L) }
 }
 val selectedWatts=when(panel) { "other" -> customWatts.toIntOrNull(); else -> panel.toIntOrNull() }
 val selectedMinutes=when(timer) { "manual" -> null; "other" -> customMinutes.toIntOrNull(); else -> timer.toIntOrNull() }
 val validWatts = selectedWatts != null && selectedWatts in 1..200
 val validDuration = timer == "manual" || selectedMinutes != null && selectedMinutes in 1..1440
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  item {
   Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text("🔬 Laboratório de bateria",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    Text("Teste seu painel solar sem internet e sem gravações.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  if(active != null) {
   item { RunningLabCard(active!!,now,vm) }
  } else {
   item {
    LabPanel("Condições do céu") {
     Text("Como está o céu?",style=MaterialTheme.typography.bodyMedium)
     skyOptions.chunked(2).forEach { row ->
      Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()) {
       row.forEach { item ->
        Surface(onClick={sky=item.key},modifier=Modifier.weight(1f).height(110.dp),shape=RoundedCornerShape(18.dp),
         color=if(sky==item.key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
         border=BorderStroke(if(sky==item.key) 2.dp else 1.dp,if(sky==item.key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
         Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center,modifier=Modifier.padding(5.dp)) {
          Text(item.emoji,style=MaterialTheme.typography.headlineMedium)
          Text(item.title,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
          Text(item.subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
         }
        }
       }
      }
     }
    }
   }
   item {
    LabPanel("Painel solar") {
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      listOf("8" to "8 W","20" to "20 W","other" to "Outro").forEach { (value,title) ->
       LabChoice(title,panel==value,Modifier.weight(1f)) { panel=value }
      }
     }
     if (panel=="other") OutlinedTextField(value=customWatts,onValueChange={customWatts=it.filter(Char::isDigit).take(3)},
      label={Text("Potência nominal (W)")},singleLine=true,
      keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
    }
   }
   item {
    LabPanel("Duração do teste") {
     listOf(listOf("30" to "30 min","60" to "1 hora","120" to "2 horas"),
      listOf("240" to "4 horas","manual" to "Manual","other" to "Outro")).forEach { row ->
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
       row.forEach { (value,title) -> LabChoice(title,timer==value,Modifier.weight(1f)) { timer=value } }
      }
     }
     if(timer=="other") OutlinedTextField(value=customMinutes,onValueChange={customMinutes=it.filter(Char::isDigit).take(4)},
      label={Text("Duração em minutos (1 a 1440)")},singleLine=true,
      keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
     Text(if(timer=="manual") "Finalização manual." else "O teste termina automaticamente e salva o relatório.",
      style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
   item {
    Button(onClick={vm.startLab(sky,selectedWatts,selectedMinutes) { onMonitor(true) }},
     enabled=validWatts && validDuration,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) {
     Text("▶  Iniciar teste")
    }
   }
  }
  if(vm.labError!=null) item {
   Text(vm.labError ?: "",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodyMedium)
  }
  item {
   Text("Histórico de experimentos",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
  }
  if(history.none { it.status != "RUNNING" }) item {
   Text("Nenhum teste concluído ainda.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  items(history.filter { it.status != "RUNNING" },key={it.id}) { test -> LabResult(test) }
  item {
   Text("A corrente média é líquida na bateria, não a potência do painel. Mudanças de estado, intervalos perdidos e leituras duvidosas ficam em 'Sem leitura'. O Android pode atrasar a conclusão se suspender o processo.",
    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

@Composable
private fun LabChoice(label: String, selected: Boolean, modifier: Modifier=Modifier, onClick: () -> Unit) {
 Surface(onClick=onClick,modifier=modifier.heightIn(min=52.dp),shape=RoundedCornerShape(13.dp),
  border=BorderStroke(if(selected) 2.dp else 1.dp,if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
  color=if(selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
  Box(contentAlignment=Alignment.Center,modifier=Modifier.padding(horizontal=4.dp,vertical=8.dp)) {
   Text(label,style=MaterialTheme.typography.labelMedium,fontWeight=if(selected) FontWeight.Bold else FontWeight.Normal)
  }
 }
}

@Composable
private fun LabPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
 Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
  elevation=CardDefaults.cardElevation(defaultElevation=1.dp)) {
  Column(modifier=Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
   content()
  }
 }
}

@Composable
private fun RunningLabCard(test: LabTest,now: Long,vm: PulsoViewModel) {
 val total=(now-test.startedAt).coerceAtLeast(0).let { if(test.targetMs!=null) it.coerceAtMost(test.targetMs) else it }
 var confirm by remember { mutableStateOf(false) }
 LabPanel("⏱️ Teste em andamento") {
  Text(duration(total),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
  if(test.deadlineAt != null) Text("Restante: ${duration((test.deadlineAt-now).coerceAtLeast(0))}",style=MaterialTheme.typography.bodyMedium)
  Text("Céu: ${skyLabel(test.sky)} • Painel: ${test.panelWatts ?: "?"} W",style=MaterialTheme.typography.bodyMedium)
  Text("Bateria inicial: ${test.initialPercent?.let { "$it%" } ?: "Sem leitura"}",style=MaterialTheme.typography.bodySmall)
  Text("Carregando: ${duration(test.chargingMs)}  •  Sem carregar: ${duration(test.notChargingMs)}")
  val mean=LabMath.meanMa(test.weightedMaMs,test.validCurrentMs)
  Text("Média durante carga: ${mean?.let { "+${it.roundToInt()} mA" } ?: "Indisponível"}")
  val unknown=LabMath.unknownMs(total,test.chargingMs,test.notChargingMs)
  Text("Sem leitura confirmada: ${duration(unknown)}",style=MaterialTheme.typography.bodySmall,
   color=MaterialTheme.colorScheme.onSurfaceVariant)
  Text("Mudou o tempo?",style=MaterialTheme.typography.labelLarge)
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()) {
   skyOptions.forEach { option -> LabChoice(option.emoji,test.sky==option.key,Modifier.weight(1f)) { vm.changeLabSky(option.key) } }
  }
  Text("O monitoramento em primeiro plano deve permanecer ativo para coletar com a tela apagada.",
   style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  OutlinedButton(onClick={confirm=true},modifier=Modifier.fillMaxWidth()) { Text("Finalizar agora") }
 }
 if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Finalizar teste?")},
  text={Text("O relatório será salvo com o tempo efetivamente transcorrido.")},
  confirmButton={TextButton(onClick={confirm=false;vm.stopLab()}) { Text("Finalizar") }},
  dismissButton={TextButton(onClick={confirm=false}) { Text("Cancelar") }})
}

@Composable
private fun LabResult(test: LabTest) {
 var expanded by rememberSaveable(test.id) { mutableStateOf(false) }
 val total=((test.endedAt ?: test.startedAt)-test.startedAt).coerceAtLeast(0)
 val missing=LabMath.unknownMs(total,test.chargingMs,test.notChargingMs)
 val mean=LabMath.meanMa(test.weightedMaMs,test.validCurrentMs)
 Card(onClick={expanded=!expanded},shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
    Text("${skyOptions.firstOrNull { it.key==test.sky }?.emoji ?: "☀️"}  ${test.panelWatts ?: "?"} W • ${skyLabel(test.sky)}",fontWeight=FontWeight.SemiBold)
    Text(if(test.status=="COMPLETED") "Automático" else "Manual",style=MaterialTheme.typography.labelSmall)
   }
   Text("${date(test.startedAt)} • ${duration(total)}",style=MaterialTheme.typography.bodySmall,
    color=MaterialTheme.colorScheme.onSurfaceVariant)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    val covered=test.chargingMs+test.notChargingMs
    if(covered>0) {
     Box(Modifier.weight((test.chargingMs.toFloat()/covered).coerceAtLeast(.001f)).height(8.dp)
      .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)))
     Box(Modifier.weight((test.notChargingMs.toFloat()/covered).coerceAtLeast(.001f)).height(8.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
    }
   }
   Text("Carga ${duration(test.chargingMs)} • Sem carga ${duration(test.notChargingMs)}",style=MaterialTheme.typography.bodySmall)
   if(expanded) {
    HorizontalDivider()
    LabResultRow("Sem leitura / lacunas",duration(missing))
    LabResultRow("Corrente média válida",mean?.let { "+${it.roundToInt()} mA" } ?: "Indisponível")
    LabResultRow("Bateria", "${test.initialPercent?.let { "$it%" } ?: "?"} → ${test.finalPercent?.let { "$it%" } ?: "?"}")
    LabResultRow("Início",date(test.startedAt))
    LabResultRow("Fim",date(test.endedAt ?: test.startedAt))
    if(test.skyChanges.contains(';')) Text("Mudanças de céu: ${test.skyChanges.split(';').joinToString(" • ") {
     val parts=it.split(':',limit=2)
     if(parts.size==2) "${date(parts[0].toLongOrNull() ?: test.startedAt)} ${skyLabel(parts[1])}" else "?"
    }}",style=MaterialTheme.typography.bodySmall)
   }
   Text(if(expanded) "Toque para recolher" else "Toque para ver detalhes",style=MaterialTheme.typography.labelSmall,
    color=MaterialTheme.colorScheme.primary)
  }
 }
}

@Composable
private fun LabResultRow(label: String,value: String) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
  Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Text(value,style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.Medium)
 }
}
