package br.com.pulsobateria.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.pulsobateria.PulsoApp
import br.com.pulsobateria.usage.AppUsageEntry
import br.com.pulsobateria.usage.UsageInsights
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/** Quarta aba: correlação temporal, NÃO atribuição de mAh por processo. */
@Composable
fun AppUsageScreen(modifier: Modifier = Modifier) {
 val context=LocalContext.current
 val reader=remember(context) { (context.applicationContext as PulsoApp).usageInsights }
 var refreshKey by rememberSaveable { mutableIntStateOf(0) }
 var granted by remember { mutableStateOf(reader.hasPermission()) }
 var loading by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<String?>(null) }
 var data by remember { mutableStateOf<UsageInsights?>(null) }
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshKey++ }
 LaunchedEffect(refreshKey) {
  granted=reader.hasPermission()
  data=null
  error=null
  if(granted) {
   loading=true
   try { data=reader.query() }
   catch(e: CancellationException) { throw e }
   catch(e: Exception) { error=e.message ?: "Não foi possível ler as estatísticas do Android." }
   finally { loading=false }
  }
 }
 val extra=MaterialTheme.colorScheme.onSurfaceVariant
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item {
   Text("Consumo e atividade",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
   Text("Últimas 24 horas • dados locais",style=MaterialTheme.typography.bodySmall,color=extra)
  }
  if(!granted) {
   item {
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
     Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Text("Autorizar acesso ao uso",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
      Text("O Android exige uma autorização especial para mostrar quanto tempo cada aplicativo ficou em primeiro plano. Esse acesso é opcional; o Pulso continua funcionando sem ele.")
      Text("Os dados não saem do aparelho e não usamos permissão de acessibilidade nem acesso à internet.",style=MaterialTheme.typography.bodySmall,color=extra)
      Button(onClick={launcher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))}) { Text("Abrir acesso ao uso") }
      OutlinedButton(onClick={refreshKey++}) { Text("Verificar permissão") }
     }
    }
   }
  } else {
   item {
    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("Estatísticas do Android",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
     TextButton(onClick={refreshKey++},enabled=!loading) { Text("Atualizar") }
    }
   }
   if(loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
   if(error!=null) item { Text(error ?: "Erro desconhecido",color=MaterialTheme.colorScheme.error) }
   val summary=data
   if(summary!=null) {
    item { BatteryUsageCard(summary) }
    item { ActivityHoursCard(summary) }
    item {
     Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
      Text("Aplicativos em primeiro plano",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
      Text("Tempo aproximado por eventos de uso, não corrente nem mAh por aplicativo.",style=MaterialTheme.typography.bodySmall,color=extra)
     }
    }
    if(summary.apps.isEmpty()) item {
     Card { Text("Nenhum período de uso identificável nas últimas 24 horas. Os eventos podem estar incompletos ou indisponíveis no Android.",Modifier.padding(16.dp)) }
    } else {
     val top=summary.apps.take(20)
     items(top,key={it.packageName}) { app -> AppTimeCard(app,summary.apps.first().durationMs) }
     if(summary.apps.size>20) item { Text("Exibindo 20 de ${summary.apps.size} aplicativos.",style=MaterialTheme.typography.bodySmall,color=extra) }
    }
    item {
     Text("Como interpretar: a queda da bateria é do aparelho inteiro. Tela, rede, sistema e serviços em segundo plano também consomem energia. Usar um aplicativo durante uma queda não prova que ele a causou. Dados anteriores à instalação do Pulso não têm amostras locais da bateria; o histórico de uso do Android é temporário.",style=MaterialTheme.typography.bodySmall,color=extra)
    }
   }
  }
 }
}

@Composable
private fun BatteryUsageCard(s: UsageInsights) {
 val c=MaterialTheme.colorScheme.onSurfaceVariant
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text("Bateria observada",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
    Column { Text("Queda registrada",style=MaterialTheme.typography.labelMedium,color=c)
     Text("${s.battery.observedDrop} pontos",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold) }
    Column { Text("Recarga registrada",style=MaterialTheme.typography.labelMedium,color=c)
     Text("+${s.battery.observedGain} pontos",style=MaterialTheme.typography.titleLarge) }
   }
   HorizontalDivider()
   Text("Tela ligada: ${s.battery.onDrop} pontos de queda • ${usageDuration(s.battery.onTimeMs)} observados",style=MaterialTheme.typography.bodyMedium)
   Text("Tela desligada: ${s.battery.offDrop} pontos de queda • ${usageDuration(s.battery.offTimeMs)} observados",style=MaterialTheme.typography.bodyMedium)
   val unclassified=s.battery.unclassifiedDrop
   if(unclassified>0) Text("Sem classificação de tela: $unclassified pontos",style=MaterialTheme.typography.bodySmall,color=c)
   Text("Cobertura de intervalos válidos: ${usageDuration(s.battery.observedTimeMs)} • ${s.sampleCount} amostras salvas. Não extrapolamos períodos sem leitura ou acima de 3 min entre amostras.",style=MaterialTheme.typography.bodySmall,color=c)
  }
 }
}

@Composable
private fun ActivityHoursCard(s: UsageInsights) {
 val secondary=MaterialTheme.colorScheme.onSurfaceVariant
 val primary=MaterialTheme.colorScheme.primary
 // Faixas consecutivas de duas horas contadas para trás desde o início da janela.
 val blocks=(0 until 12).map { i ->
  val a=s.hourlyMs.getOrElse(2*i) { 0L }
  val b=s.hourlyMs.getOrElse(2*i+1) { 0L }
  a+b
 }
 val peak=max(1L,blocks.maxOrNull() ?: 0L)
 val fmt=remember { SimpleDateFormat("HH'h'",Locale.forLanguageTag("pt-BR")) }
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text("Atividade por horário",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text("Minutos de aplicativos em primeiro plano, por intervalos de 2 horas",style=MaterialTheme.typography.bodySmall,color=secondary)
   Row(Modifier.fillMaxWidth().height(142.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
    blocks.forEachIndexed { i, ms ->
     Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom) {
      Box(Modifier.fillMaxWidth().height((if(ms==0L) 0f else max(3f,116f*ms.toFloat()/peak.toFloat())).dp)
       .background(primary.copy(alpha=.8f),RoundedCornerShape(topStart=4.dp,topEnd=4.dp)))
      Spacer(Modifier.height(4.dp))
      Text(if(i%3==0) fmt.format(Date(s.from+i*7_200_000L)) else "",style=MaterialTheme.typography.labelSmall,color=secondary,maxLines=1)
     }
    }
   }
   Text("Tempo total identificado: ${usageDuration(s.foregroundMs)}. Ausência de eventos não significa ausência de consumo.",style=MaterialTheme.typography.bodySmall,color=secondary)
  }
 }
}

@Composable
private fun AppTimeCard(app: AppUsageEntry, maxDuration: Long) {
 val color=MaterialTheme.colorScheme.primary
 Card(shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=11.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    Text(app.displayName,modifier=Modifier.weight(1f),fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
    Text(usageDuration(app.durationMs),style=MaterialTheme.typography.labelMedium)
   }
   Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(50))) {
    Box(Modifier.fillMaxWidth((app.durationMs.toFloat()/max(1L,maxDuration)).coerceIn(0f,1f)).fillMaxHeight()
     .background(color,RoundedCornerShape(50)))
   }
   if(app.displayName==app.packageName) Text(app.packageName,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
  }
 }
}

private fun usageDuration(millis: Long): String {
 if(millis<=0) return "0 min"
 val minutes=millis/60_000L
 if(minutes==0L) return "< 1 min"
 return if(minutes>=60) "${minutes/60}h${(minutes%60).toString().padStart(2,'0')}" else "$minutes min"
}
