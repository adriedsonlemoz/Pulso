package br.com.pulsobateria.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
private fun dateCompact(time: Long): String = SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(Date(time))

@Composable
fun LabsScreen(vm: PulsoViewModel, modifier: Modifier = Modifier, onMonitor: (Boolean) -> Unit) {
 val active by vm.labCurrent.collectAsState()
 val history by vm.labHistory.collectAsState()
 var page by rememberSaveable { mutableStateOf("home") }
 var sky by rememberSaveable { mutableStateOf("nublado") }
 var panel by rememberSaveable { mutableStateOf("8") }
 var customWatts by rememberSaveable { mutableStateOf("") }
 var timer by rememberSaveable { mutableStateOf("120") }
 var customMinutes by rememberSaveable { mutableStateOf("") }
 var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
 var hadActive by rememberSaveable { mutableStateOf(false) }
 LaunchedEffect(active?.id) {
  if(active!=null) hadActive=true
  else if(hadActive) { page="home"; hadActive=false }
  while(active!=null) { now=System.currentTimeMillis(); delay(1000L) }
 }
 BackHandler(enabled=active==null && page!="home") { page="home" }
 val selectedWatts=when(panel) { "other" -> customWatts.toIntOrNull(); else -> panel.toIntOrNull() }
 val selectedMinutes=when(timer) { "manual" -> null; "other" -> customMinutes.toIntOrNull(); else -> timer.toIntOrNull() }
 val validWatts=selectedWatts!=null && selectedWatts in 1..200
 val validDuration=timer=="manual" || selectedMinutes!=null && selectedMinutes in 1..1440
 val completed=history.filter { it.status != "RUNNING" }
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  item {
   Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text("🔬 Labs",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    Text("Experimentos de bateria e energia, sem internet ou gravações.",
     style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  if(active!=null) {
   item { RunningLabCard(active!!,now,vm) }
   item { Text("Acompanhe o teste com a tela apagada. O resultado é salvo ao concluir.",
    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
  } else when(page) {
   "home" -> {
    item { LabMenuCard("☀️","Teste Solar","Meça períodos de carga e corrente média do painel.") { page="solar" } }
    item { LabMenuCard("🔋","Teste de Consumo","Descubra quantos pontos percentuais a bateria perde sem fonte externa.") { page="discharge" } }
    item {
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
      Text("Últimos testes",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
      TextButton(onClick={page="history"}) { Text("Ver todos ›") }
     }
    }
    if(vm.labError!=null) item { Text(vm.labError ?: "",color=MaterialTheme.colorScheme.error) }
    if(completed.isEmpty()) item { Text("Nenhum experimento concluído ainda.",style=MaterialTheme.typography.bodySmall,
     color=MaterialTheme.colorScheme.onSurfaceVariant) }
    items(completed.take(5), key={it.id}) { test -> LabResult(test,onDelete=vm::deleteLab) }
   }
   "solar", "discharge" -> {
    val solar=page=="solar"
    item {
     TextButton(onClick={page="home"}) { Text("‹ Voltar aos experimentos") }
     Text(if(solar) "☀️ Teste Solar" else "🔋 Teste de Consumo",style=MaterialTheme.typography.titleLarge,
      fontWeight=FontWeight.SemiBold)
     Text(if(solar) "Registre a carga fornecida pelo painel em diferentes condições do céu."
      else "Meça a queda da bateria sem carregador, power bank ou painel conectado.",
      style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if(solar) {
     item {
      LabPanel("Condições do céu") {
       Text("Como está o céu?",style=MaterialTheme.typography.bodyMedium)
       skyOptions.chunked(2).forEach { row ->
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()) {
         row.forEach { option ->
          Surface(onClick={sky=option.key},modifier=Modifier.weight(1f).height(110.dp),shape=RoundedCornerShape(18.dp),
           color=if(sky==option.key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
           border=BorderStroke(if(sky==option.key) 2.dp else 1.dp,
            if(sky==option.key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
           Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center,
            modifier=Modifier.padding(5.dp)) {
            Text(option.emoji,style=MaterialTheme.typography.headlineMedium)
            Text(option.title,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
            Text(option.subtitle,style=MaterialTheme.typography.labelSmall,
             color=MaterialTheme.colorScheme.onSurfaceVariant)
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
       if(panel=="other") OutlinedTextField(value=customWatts,onValueChange={customWatts=it.filter(Char::isDigit).take(3)},
        label={Text("Potência nominal (W)")},singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
      }
     }
    } else item {
     LabPanel("Sem alimentação externa") {
      Text("Desconecte o USB, carregador, power bank e painel solar antes de iniciar.",
       style=MaterialTheme.typography.bodyMedium)
      Text("Se uma fonte externa for detectada durante o teste, o relatório será marcado como interferido.",
       style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
      if(timer=="other") OutlinedTextField(value=customMinutes,
       onValueChange={customMinutes=it.filter(Char::isDigit).take(4)},
       label={Text("Duração em minutos (1 a 1440)")},singleLine=true,
       keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
      Text(if(timer=="manual") "Finalização manual." else "Finalização automática, relatório salvo e notificação.",
       style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
    }
    item {
     Button(onClick={if(solar) vm.startLab(sky,selectedWatts,selectedMinutes) { onMonitor(true) }
      else vm.startDischargeLab(selectedMinutes) { onMonitor(true) } },
      enabled=validDuration && (!solar || validWatts),modifier=Modifier.fillMaxWidth(),
      shape=RoundedCornerShape(16.dp)) { Text("▶  Iniciar teste") }
    }
    if(vm.labError!=null) item { Text(vm.labError ?: "",color=MaterialTheme.colorScheme.error) }
   }
   "history" -> {
    item { TextButton(onClick={page="home"}) { Text("‹ Voltar ao Labs") } }
    item { Text("Histórico de experimentos",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold) }
    if(vm.labError!=null) item { Text(vm.labError ?: "",color=MaterialTheme.colorScheme.error) }
    if(completed.isEmpty()) item { Text("Nenhum teste concluído ainda.") }
    items(completed,key={it.id}) { test -> LabResult(test,onDelete=vm::deleteLab) }
   }
  }
 }
}

@Composable
private fun LabMenuCard(emoji: String,title: String,description: String,onClick: () -> Unit) {
 Card(onClick=onClick,shape=RoundedCornerShape(20.dp),
  colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
  border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
  Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically,
   horizontalArrangement=Arrangement.spacedBy(14.dp)) {
   Text(emoji,style=MaterialTheme.typography.headlineMedium)
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   Text("›",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
 val discharge=test.kind=="DISCHARGE"
 LabPanel(if(discharge) "⏱️ Consumo em andamento" else "⏱️ Teste solar em andamento") {
  Text(duration(total),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
  if(test.deadlineAt != null) Text("Restante: ${duration((test.deadlineAt-now).coerceAtLeast(0))}")
  if(!discharge) Text("Céu: ${skyLabel(test.sky)} • Painel: ${test.panelWatts ?: "?"} W")
  else Text("Sem carregador • ${if(test.externalSeen) "Fonte externa detectada!" else "Nenhuma interferência detectada"}",
   color=if(test.externalSeen) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
  Text("Bateria inicial: ${test.initialPercent?.let { "$it%" } ?: "Sem leitura"}")
  if(discharge) {
   Text("Tempo confirmado sem fonte: ${duration(test.notChargingMs)}")
   Text("Queda observada: ${LabMath.dischargePoints(test.initialPercent,test.finalPercent,test.externalSeen)?.let { "$it pontos percentuais" } ?: "Indisponível"}")
  } else {
   Text("Carregando: ${duration(test.chargingMs)} • Sem carregar: ${duration(test.notChargingMs)}")
   val mean=LabMath.meanMa(test.weightedMaMs,test.validCurrentMs)
   Text("Média durante carga: ${mean?.let { "+${it.roundToInt()} mA" } ?: "Indisponível"}")
   Text("Mudou o tempo?",style=MaterialTheme.typography.labelLarge)
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()) {
    skyOptions.forEach { option -> LabChoice(option.emoji,test.sky==option.key,Modifier.weight(1f)) {
     vm.changeLabSky(option.key)
    } }
   }
  }
  val unknown=LabMath.unknownMs(total,test.chargingMs,test.notChargingMs)
  Text("Sem leitura confirmada: ${duration(unknown)}",style=MaterialTheme.typography.bodySmall,
   color=MaterialTheme.colorScheme.onSurfaceVariant)
  Text("Mantenha o monitoramento em primeiro plano para medir com a tela apagada.",
   style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  OutlinedButton(onClick={confirm=true},modifier=Modifier.fillMaxWidth()) { Text("Finalizar agora") }
 }
 if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Finalizar teste?")},
  text={Text("O relatório será salvo com o tempo efetivamente transcorrido.")},
  confirmButton={TextButton(onClick={confirm=false;vm.stopLab()}) { Text("Finalizar") }},
  dismissButton={TextButton(onClick={confirm=false}) { Text("Cancelar") }})
}

@Composable
private fun LabResult(test: LabTest, onDelete: (Long) -> Unit) {
 var expanded by rememberSaveable(test.id) { mutableStateOf(false) }
 var confirmDelete by rememberSaveable(test.id) { mutableStateOf(false) }
 val total=((test.endedAt ?: test.startedAt)-test.startedAt).coerceAtLeast(0)
 val missing=LabMath.unknownMs(total,test.chargingMs,test.notChargingMs)
 val discharge=test.kind=="DISCHARGE"
 val mean=LabMath.meanMa(test.weightedMaMs,test.validCurrentMs)
 val drop=LabMath.dischargePoints(test.initialPercent,test.finalPercent,test.externalSeen)
 Card(onClick={expanded=!expanded},shape=RoundedCornerShape(18.dp),
  colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
    Text(if(discharge) "🔋 Teste de Consumo" else "☀️ Teste Solar • ${test.panelWatts ?: "?"} W",fontWeight=FontWeight.SemiBold)
    Text(if(test.status=="COMPLETED") "Automático" else "Manual",style=MaterialTheme.typography.labelSmall)
   }
   Text("${date(test.startedAt)} • ${duration(total)}",style=MaterialTheme.typography.bodySmall,
    color=MaterialTheme.colorScheme.onSurfaceVariant)
   if(discharge) {
    Text(if(test.externalSeen) "⚠ Fonte externa detectada: resultado interferido"
     else "Queda: ${drop?.let { "$it pontos percentuais" } ?: "Não conclusiva"}",
     color=if(test.externalSeen) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    Text("Bateria: ${test.initialPercent?.let { "$it%" } ?: "?"} → ${test.finalPercent?.let { "$it%" } ?: "?"}",
     style=MaterialTheme.typography.bodySmall)
   } else {
    Text("${skyLabel(test.sky)} • Carga ${duration(test.chargingMs)} • Sem carga ${duration(test.notChargingMs)}",
     style=MaterialTheme.typography.bodySmall)
    if(!expanded) Text("Bateria: ${test.initialPercent?.let { "$it%" } ?: "?"} → ${test.finalPercent?.let { "$it%" } ?: "?"}",
     style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   if(expanded) {
    HorizontalDivider()
    if(discharge) {
     LabResultRow("Tempo confirmado sem fonte",duration(test.notChargingMs))
     LabResultRow("Fonte externa detectada",if(test.externalSeen) "Sim" else "Não nas leituras")
     LabResultRow("Taxa observada",if(drop!=null && total>0) "%.1f pontos/h".format(Locale.getDefault(),drop*3_600_000.0/total) else "Indisponível")
     val recent=test.lastReadingAt?.let { last -> test.endedAt?.let { end -> end-last in 0..90_000L } } ?: false
     if(!recent) Text("A última leitura pode ser anterior ao término. O percentual final não foi medido exatamente no prazo.",
      style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
    } else {
     LabResultRow("Corrente média durante carga",mean?.let { "+${it.roundToInt()} mA" } ?: "Indisponível")
     LabResultRow("Bateria", "${test.initialPercent?.let { "$it%" } ?: "?"} → ${test.finalPercent?.let { "$it%" } ?: "?"}")
     if(test.skyChanges.contains(';')) Text("Mudanças de céu: ${test.skyChanges.split(';').joinToString(" • ") {
      val parts=it.split(':',limit=2)
      if(parts.size==2) "${date(parts[0].toLongOrNull() ?: test.startedAt)} ${skyLabel(parts[1])}" else "?"
     }}",style=MaterialTheme.typography.bodySmall)
    }
    LabResultRow("Sem leitura / lacunas",duration(missing))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     LabDateCell("Início",dateCompact(test.startedAt),Modifier.weight(1f))
     LabDateCell("Fim",dateCompact(test.endedAt ?: test.startedAt),Modifier.weight(1f))
    }
   }
   if(missing>=total && total>0L) Text("Sem leituras confirmadas: resultado incompleto.",
    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
    horizontalArrangement=Arrangement.SpaceBetween) {
    TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "Recolher ↑" else "Ver detalhes ↓") }
    if(expanded) TextButton(onClick={confirmDelete=true},
     colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)) {
     Text("Excluir")
    }
   }
  }
 }
 if(confirmDelete) AlertDialog(onDismissRequest={confirmDelete=false},
  title={Text("Excluir este teste?")},
  text={Text("O resultado será apagado do histórico do Labs. Esta ação não pode ser desfeita.")},
  confirmButton={TextButton(onClick={confirmDelete=false; onDelete(test.id)},
   colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)) { Text("Excluir") }},
  dismissButton={TextButton(onClick={confirmDelete=false}) { Text("Cancelar") }})
}

@Composable
private fun LabDateCell(label: String, value: String, modifier: Modifier = Modifier) {
 Column(modifier,verticalArrangement=Arrangement.spacedBy(2.dp)) {
  Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Text(value,style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.Medium)
 }
}

@Composable
private fun LabResultRow(label: String,value: String) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
  Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,
   modifier=Modifier.weight(1f))
  Text(value,style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.Medium)
 }
}
