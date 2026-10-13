package br.com.pulsobateria.ui

import android.content.Intent
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageScreen(modifier: Modifier = Modifier) {
 val context=LocalContext.current
 val reader=remember(context) { (context.applicationContext as PulsoApp).usageInsights }
 var refreshKey by rememberSaveable { mutableIntStateOf(0) }
 var section by rememberSaveable { mutableIntStateOf(0) }
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
  if(!granted) {
   item {
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
     Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Text("Autorizar acesso ao uso",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
      Text("Para mostrar atividade de aplicativos, autorize o acesso às estatísticas do Android. É opcional e os dados permanecem no telefone.",style=MaterialTheme.typography.bodySmall)
      Button(onClick={launcher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))}) { Text("Abrir acesso ao uso") }
      TextButton(onClick={refreshKey++}) { Text("Verificar permissão") }
     }
    }
   }
  } else {
   item {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
     Text("Últimas 24 horas",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
     TextButton(onClick={refreshKey++},enabled=!loading) { Text("Atualizar") }
    }
   }
   item {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
     listOf("Resumo","Aplicativos","Noite").forEachIndexed { i,label ->
      SegmentedButton(selected=section==i,onClick={section=i},shape=SegmentedButtonDefaults.itemShape(i,3),label={Text(label,maxLines=1,overflow=TextOverflow.Ellipsis)})
     }
    }
   }
   if(loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
   if(error!=null) item { Text(error ?: "Não foi possível atualizar",color=MaterialTheme.colorScheme.error) }
   val summary=data
   if(summary!=null) when(section) {
    0 -> item { BatteryUsageCard(summary) }
    1 -> {
     item { ActivityHoursCard(summary) }
     item {
      Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
       Text("Aplicativos mais usados",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
       SectionHelpButton("Atividade dos aplicativos","As barras mostram tempo em primeiro plano, não corrente ou mAh atribuídos a cada aplicativo. Rede, tela e sistema também consomem energia.")
      }
     }
     if(summary.apps.isEmpty()) item {
      Card { Text("Ainda não há dados suficientes de uso nesta janela.",Modifier.padding(16.dp)) }
     } else {
      items(summary.apps.take(20),key={it.packageName}) { app -> AppTimeCard(app,summary.apps.first().durationMs) }
      if(summary.apps.size>20) item { Text("Exibindo os 20 mais utilizados.",style=MaterialTheme.typography.labelSmall,color=extra) }
     }
    }
    else -> item { NightConsumptionCard(summary) }
   }
  }
 }
}

@Composable
private fun BatteryUsageCard(s: UsageInsights) {
 val c=MaterialTheme.colorScheme.onSurfaceVariant
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Text("Variação da bateria",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
    SectionHelpButton("Variação da bateria","Os valores somam quedas e recargas em vários ciclos e podem ultrapassar 100%. Uma queda de 80% para 60% aparece como 'Caiu 20%'. Não representam mAh nem consumo isolado de aplicativos. Lacunas não são contadas.")
   }
   if(s.battery.observedTimeMs==0L) Text("Ainda não há leituras suficientes.") else {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Column { Text("Caiu (acumulado)",style=MaterialTheme.typography.labelMedium,color=c)
      Text("${s.battery.observedDrop}%",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold) }
     Column { Text("Carregou (acumulado)",style=MaterialTheme.typography.labelMedium,color=c)
      Text("+${s.battery.observedGain}%",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold) }
    }
    HorizontalDivider()
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("Tela ligada",style=MaterialTheme.typography.bodyMedium)
     Text("Caiu ${s.battery.onDrop}%",fontWeight=FontWeight.Medium)
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("Tela apagada",style=MaterialTheme.typography.bodyMedium)
     Text("Caiu ${s.battery.offDrop}%",fontWeight=FontWeight.Medium)
    }
    if(s.battery.unclassifiedDrop>0) Text("Tela sem estado conhecido: caiu ${s.battery.unclassifiedDrop}%",style=MaterialTheme.typography.labelSmall,color=c)
    Text("Monitorado: ${usageDuration(s.battery.observedTimeMs)}",style=MaterialTheme.typography.labelMedium,color=c)
   }
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
   Text("Uso identificado: ${usageDuration(s.foregroundMs)}",style=MaterialTheme.typography.labelMedium,color=secondary)
  }
 }
}

/** Ícone e nome do pacote instalado; há um símbolo neutro quando o Android restringe acesso. */
@Composable
private fun AppTimeCard(app: AppUsageEntry, maxDuration: Long) {
 val color=MaterialTheme.colorScheme.primary
 val context=LocalContext.current
 val iconBitmap=remember(app.packageName) {
  try { context.packageManager.getApplicationIcon(app.packageName).toBitmap(64,64).asImageBitmap() }
  catch (_: Exception) { null }
 }
 val identified=app.displayName!=app.packageName
 Card(shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    if(iconBitmap!=null) {
     Image(bitmap=iconBitmap,contentDescription="Ícone de ${if(identified) app.displayName else "aplicativo"}",modifier=Modifier.size(40.dp))
    } else {
     Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center) {
      // Ícone genérico, sem sugerir identidade não verificada.
      Text("▦",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
    }
    Column(Modifier.weight(1f)) {
     Text(if(identified) app.displayName else "Aplicativo sem nome disponível",fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis)
     if(!identified) Text(app.packageName,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
    Text(usageDuration(app.durationMs),style=MaterialTheme.typography.labelMedium)
   }
   Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(50))) {
    Box(Modifier.fillMaxWidth((app.durationMs.toFloat()/max(1L,maxDuration)).coerceIn(0f,1f)).fillMaxHeight()
     .background(color,RoundedCornerShape(50)))
   }
  }
 }
}

private fun usageDuration(millis: Long): String {
 if(millis<=0) return "0 min"
 val minutes=millis/60_000L
 if(minutes==0L) return "< 1 min"
 return if(minutes>=60) "${minutes/60}h${(minutes%60).toString().padStart(2,'0')}" else "$minutes min"
}


/** A análise noturna é do aparelho inteiro. Sem eventos de tela não há atribuição. */
@Composable
private fun NightConsumptionCard(insights: UsageInsights) {
 val night=insights.night
 val stats=night.battery
 val muted=MaterialTheme.colorScheme.onSurfaceVariant
 val formatter=remember { SimpleDateFormat("dd/MM 'às' HH:mm",Locale.forLanguageTag("pt-BR")) }
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Text("Consumo noturno",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
    SectionHelpButton("Consumo noturno","Mostra as variações da bateria do telefone durante períodos realmente monitorados. Não mede consumo por aplicativo. A classificação da tela depende da permissão de uso e pode ficar incompleta; períodos sem leituras não são somados.")
   }
   Text("${formatter.format(Date(night.window.start))} até ${formatter.format(Date(night.window.end))}",style=MaterialTheme.typography.labelMedium,color=muted)
   if(stats.observedTimeMs==0L) Text("Ainda não há medições contínuas nesta noite.") else {
    Text("Caiu ${stats.observedDrop}%",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.SemiBold)
    if(stats.observedGain>0) Text("Carregou +${stats.observedGain}%",style=MaterialTheme.typography.bodyMedium,color=muted)
    HorizontalDivider()
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("Tela apagada")
     Text("Caiu ${stats.offDrop}%",fontWeight=FontWeight.Medium)
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("Tela ligada")
     Text("Caiu ${stats.onDrop}%",fontWeight=FontWeight.Medium)
    }
    if(stats.unclassifiedDrop>0) Text("Tela sem classificação: caiu ${stats.unclassifiedDrop}%",style=MaterialTheme.typography.labelSmall,color=muted)
    Text("Cobertura: ${night.coveragePercent}%${if(night.coveragePercent<80) " • dados parciais" else ""}",style=MaterialTheme.typography.labelMedium,color=muted)
    LinearProgressIndicator(progress={night.coveragePercent/100f},modifier=Modifier.fillMaxWidth())
   }
  }
 }
}
