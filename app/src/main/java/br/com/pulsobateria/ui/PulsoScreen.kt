package br.com.pulsobateria.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.pulsobateria.BuildConfig
import br.com.pulsobateria.core.*
import br.com.pulsobateria.data.*
import br.com.pulsobateria.monitor.Reading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulsoScreen(onMonitor: (Boolean) -> Unit, vm: PulsoViewModel = viewModel()) {
 val settings by vm.settings.collectAsStateWithLifecycle()
 val reading by vm.reading.collectAsStateWithLifecycle()
 val sessions by vm.sessions.collectAsStateWithLifecycle()
 val telemetry by vm.telemetry.collectAsStateWithLifecycle()
 val weeklyTelemetry by vm.weeklyTelemetry.collectAsStateWithLifecycle()
 val active by vm.active.collectAsStateWithLifecycle()
 val error by vm.error.collectAsStateWithLifecycle()
 val dark = settings.theme == "dark" || (settings.theme == "system" && isSystemInDarkTheme())
 val view = LocalView.current
 SideEffect {
  (view.context as? Activity)?.window?.let { window ->
   WindowCompat.getInsetsController(window,view).apply {
    isAppearanceLightStatusBars = !dark
    isAppearanceLightNavigationBars = !dark
   }
  }
 }
 val colors = if (dark) darkColorScheme(primary=Color(0xFF59B5FF),secondary=Color(0xFF38E8C4),background=Color.Black,
  surface=Color(0xFF101419),surfaceVariant=Color(0xFF17212B),onSurface=Color(0xFFF2F6FC),onBackground=Color.White)
 else lightColorScheme(primary=Color(0xFF0061A7),secondary=Color(0xFF006C59),background=Color(0xFFF3F7FC),
  surface=Color.White,surfaceVariant=Color(0xFFE6EEF7),onSurface=Color(0xFF101C2A),onBackground=Color(0xFF101C2A))
 var tab by rememberSaveable { mutableIntStateOf(0) }
 var detailPage by rememberSaveable { mutableStateOf("") }
 var limitations by rememberSaveable { mutableStateOf(false) }
 val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
  if(uri!=null) vm.exportBackup(uri)
 }
 val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if(uri!=null) vm.inspectBackup(uri)
 }
 BackHandler(detailPage.isNotEmpty() || tab != 0) {
  if(detailPage.isNotEmpty()) detailPage="" else tab=0
 }
 MaterialTheme(colorScheme=colors) {
  Scaffold(containerColor=colors.background, topBar={
   TopAppBar(navigationIcon={ if(detailPage.isNotEmpty()) IconButton(onClick={detailPage=""}) { Icon(Icons.Default.ArrowBack,"Voltar") } },title={ Column {
    Text(when(detailPage) { "analysis" -> "Análise de medições"; "compare" -> "Comparar recargas"; "reports" -> "Relatórios"; else -> when(tab) { 0 -> "Pulso Bateria"; 1 -> "Suas sessões"; 2 -> "Uso da bateria"; else -> "Configurações" } },fontWeight=FontWeight.Bold)
    Text(if(tab==0) "ENERGIA EM TEMPO REAL" else "PULSO • ALPHA",style=MaterialTheme.typography.labelSmall,color=colors.primary,letterSpacing=2.sp)
   } }, actions={ if(tab!=3 && detailPage.isEmpty()) IconButton(onClick={tab=3;detailPage=""}) { Icon(Icons.Default.Settings,"Configurações") } },
    colors=TopAppBarDefaults.topAppBarColors(containerColor=colors.background))
  },bottomBar={
   NavigationBar(containerColor=colors.surface) {
    listOf("Início","Sessões","Uso","Config.").forEachIndexed { index,label ->
     NavigationBarItem(selected=tab==index,onClick={tab=index;detailPage=""},icon={
      if(index==2) UsageTabIcon() else Icon(when(index){0->Icons.Default.Home;1->Icons.Default.DateRange;else->Icons.Default.Settings},label)
     },label={Text(label)})
    }
   }
  }) { padding ->
   if(detailPage=="analysis") {
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     item { Text("Corrente, temperatura e bateria",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold) }
     item { Column(verticalArrangement=Arrangement.spacedBy(12.dp)) { TelemetryPanel(telemetry,System.currentTimeMillis()) } }
    }
   } else if(detailPage=="compare") {
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp)) {
     item { ChargeCompareCard(sessions,vm) }
    }
   } else if(detailPage=="reports") {
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp)) {
     item { BatteryReportsCard(weeklyTelemetry,alwaysExpanded=true) }
    }
   } else when(tab) {
    0 -> when(reading.mode) {
     Mode.CHARGING -> ChargingScreen(reading,settings,telemetry,Modifier.padding(padding),onDetails={detailPage="analysis"}) { limitations=true }
     Mode.DISCHARGING -> DischargingScreen(reading,settings,telemetry,Modifier.padding(padding),onDetails={detailPage="analysis"}) { limitations=true }
     else -> MonitorScreen(reading,settings,telemetry,Modifier.padding(padding),"Visão da bateria",onDetails={detailPage="analysis"},onLimitations={limitations=true})
    }
    1 -> History(sessions,settings,vm,Modifier.padding(padding),onCompare={detailPage="compare"},onReports={detailPage="reports"})
    2 -> AppUsageScreen(Modifier.padding(padding))
    else -> SettingsPage(settings,reading,active,error,vm,onMonitor,{limitations=true},
     onExport={exportLauncher.launch("Pulso-Bateria-backup-${System.currentTimeMillis()}.json")},
     onImport={importLauncher.launch(arrayOf("application/json","application/octet-stream","*/*"))},
     modifier=Modifier.padding(padding))
   }
  }
  if(limitations) AlertDialog(onDismissRequest={limitations=false},confirmButton={TextButton(onClick={limitations=false}){Text("Entendi")}},title={Text("Sobre as medições")},text={
   Text("Os dados vêm do Android e dependem do sensor e do fabricante. Corrente positiva indica entrada líquida na bateria; negativa indica saída. O app usa µA e sinal Android. Ajustes da corrente instantânea exigem três janelas estáveis comparadas ao contador; isso não substitui calibração com instrumento. A média Android usa a janela desconhecida do hardware. Estar conectado não garante carga.\n\nTaxa e autonomia exigem pelo menos 5 minutos e 2% de variação na escala da bateria. São estimativas, não garantias. A previsão do sistema só é aceita durante a carga quando compatível com a tendência medida.\n\nSem monitoramento contínuo, só registramos com o app visível. Pausas de até 90 segundos podem retomar a mesma sessão se estado e porcentagem permanecerem iguais; o tempo parado não entra na duração. Lacunas de coleta acima de 3 minutos criam outra sessão. O Android pode suspender o serviço. Não há coleta durante desligamento nem recuperação de intervalos perdidos. Temperatura é da bateria, não da CPU. Corrente não é potência nem capacidade em mAh.")
  })
 }
}

/** Gráfico de barras na navegação, sem depender do pacote de ícones estendidos. */
@Composable
private fun UsageTabIcon() {
 val tint=androidx.compose.material3.LocalContentColor.current
 Canvas(Modifier.size(24.dp)) {
  val w=size.width
  val h=size.height
  drawRoundRect(tint,topLeft=Offset(w*.13f,h*.55f),size=Size(w*.18f,h*.34f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.035f))
  drawRoundRect(tint,topLeft=Offset(w*.41f,h*.26f),size=Size(w*.18f,h*.63f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.035f))
  drawRoundRect(tint,topLeft=Offset(w*.69f,h*.41f),size=Size(w*.18f,h*.48f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.035f))
 }
}

@Composable
private fun ChargingScreen(r: Reading, s: Settings, telemetry: List<TelemetrySample>, modifier: Modifier, onDetails: () -> Unit, onLimitations: () -> Unit) {
 MonitorScreen(r,s,telemetry,modifier,"Recarga",onDetails,onLimitations)
}
@Composable
private fun DischargingScreen(r: Reading, s: Settings, telemetry: List<TelemetrySample>, modifier: Modifier, onDetails: () -> Unit, onLimitations: () -> Unit) {
 MonitorScreen(r,s,telemetry,modifier,"Autonomia",onDetails,onLimitations)
}
@Composable
private fun MonitorScreen(r: Reading, s: Settings, telemetry: List<TelemetrySample>, modifier: Modifier, pageTitle: String, onDetails: () -> Unit, onLimitations: () -> Unit) {
 val isDark = MaterialTheme.colorScheme.background == Color.Black
 val accent by animateColorAsState(
  if(r.mode==Mode.CHARGING) (if(isDark) Color(0xFF38E8C4) else Color(0xFF00785D)) else MaterialTheme.colorScheme.primary,
  label="estado"
 )
 val progress by animateFloatAsState((r.percent ?: 0)/100f,tween(700),label="bateria")
 val charging = r.mode==Mode.CHARGING
 val measuring = charging || r.mode==Mode.DISCHARGING
 var metricDetails by remember { mutableStateOf<Pair<String,String>?>(null) }
 val currentLabel = if(charging) "Corrente" else "Consumo"
 val currentValue = r.current?.let { currentText(it).removeSuffix(" mA") } ?: "—"
 val currentUnit = if (r.currentInfo.source==CurrentSource.NONE && r.currentInfo.note.contains("Sinal em verificação")) "mA • sinal em análise" else when(r.currentInfo.source) {
  CurrentSource.CAPACITY -> "mA • estim."
  CurrentSource.COUNTER, CurrentSource.AVERAGE -> "mA • média"
  else -> "mA"
 }
 val currentDetails = "${r.currentInfo.source.label}. ${r.currentInfo.note} " +
  "Bruto CURRENT_NOW: ${r.currentInfo.rawNow?.toString() ?: "indisponível"}; " +
  "CURRENT_AVERAGE: ${r.currentInfo.rawAverage?.toString() ?: "indisponível"} " +
  "(unidade declarada µA); CHARGE_COUNTER: ${r.currentInfo.rawCounter?.toString() ?: "indisponível"} µAh. " +
  "O resultado representa a corrente líquida na bateria, não a corrente do carregador."
 val temperatureLabel = "Temperatura"
 val temperatureValue = r.temperature?.let {
  String.format(Locale.forLanguageTag("pt-BR"),"%.1f",if(s.fahrenheit) it*9/5+32 else it)
 } ?: "—"
 val temperatureDetails = if(r.temperature==null) "Sensor de temperatura da bateria indisponível. Não representa a temperatura da CPU."
 else "${temperatureText(r.temperature,s.fahrenheit)}. Medição do sensor da bateria, não da CPU."
 val rateLabel = if(charging) "Carga" else "Descarga"
 val rateValue = if(measuring) r.rate?.let {
  "${if(it>0) "+" else if(it<0) "−" else ""}${String.format(Locale.forLanguageTag("pt-BR"),"%.1f",abs(it))}"
 } ?: "—" else "—"
 val rateDetails = when {
  !measuring -> "Não há uma taxa de carga ou descarga para este estado da bateria."
  r.rate == null -> "Calculando: são necessários pelo menos 5 minutos, 3 leituras e variação de 2% na escala da bateria, sem lacunas ou direção contraditória."
  else -> "${rateText(r.rate)}. Variação na escala de 0 a 100% da bateria por hora. Não é corrente em mA."
 }
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item {
   Text(pageTitle,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=accent)
   Text(if(charging) "Acompanhe o tempo até completar a carga" else if(r.mode==Mode.DISCHARGING) "Veja quanto tempo de bateria ainda resta" else r.mode.label,style=MaterialTheme.typography.bodySmall)
  }
  item {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    Surface(color=accent.copy(alpha=.12f),shape=RoundedCornerShape(50)) { Text(r.mode.label,Modifier.padding(horizontal=12.dp,vertical=6.dp),color=accent,style=MaterialTheme.typography.labelLarge) }
    Text(r.source,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  item {
   Box(Modifier.fillMaxWidth().padding(vertical=4.dp),contentAlignment=Alignment.Center) {
    val track=MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.size(212.dp).semantics { contentDescription="Bateria ${r.percent?.toString() ?: "indisponível"} por cento" }) {
     val inset=8.dp.toPx()
     val arcSize=Size(size.width-2*inset,size.height-2*inset)
     drawArc(track,135f,270f,false,topLeft=Offset(inset,inset),size=arcSize,style=Stroke(12.dp.toPx(),cap=StrokeCap.Round))
     drawArc(accent,135f,270f*progress,false,topLeft=Offset(inset,inset),size=arcSize,style=Stroke(12.dp.toPx(),cap=StrokeCap.Round))
    }
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
     Text(r.percent?.let { "$it%" } ?: "—",fontSize=54.sp,fontWeight=FontWeight.Bold,letterSpacing=(-2).sp)
     Spacer(Modifier.height(6.dp))
     Text(if(r.updated==0L) "Aguardando leitura" else "${durationText(r.duration)} observados",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  item {
   Surface(shape=RoundedCornerShape(20.dp),color=accent.copy(alpha=.10f)) {
    Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
     Text(if(charging) "TEMPO ESTIMADO ATÉ 100%" else "AUTONOMIA ESTIMADA",style=MaterialTheme.typography.labelMedium,color=accent,letterSpacing=1.sp)
     Text(if(measuring) durationText(r.remaining) else "Indisponível",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
     if(r.remaining==null) Text("Aguardando dados suficientes",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  item {
   BoxWithConstraints(Modifier.fillMaxWidth()) {
    // Abaixo desse espaço útil, ou com texto ampliado, evita cortes e alvos de toque pequenos.
    val useThreeColumns = maxWidth >= 312.dp && LocalDensity.current.fontScale <= 1.2f
    if(useThreeColumns) {
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      MetricTile(currentLabel,currentValue,currentUnit,Modifier.weight(1f)) { metricDetails=currentLabel to currentDetails }
      MetricTile(temperatureLabel,temperatureValue,if(s.fahrenheit) "°F" else "°C",Modifier.weight(1f)) { metricDetails=temperatureLabel to temperatureDetails }
      MetricTile(rateLabel,rateValue,"%/h",Modifier.weight(1f)) { metricDetails=rateLabel to rateDetails }
     }
    } else {
     Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
      MetricTile(currentLabel,if(r.current==null) "—" else currentText(r.current),r.currentInfo.source.label,Modifier.fillMaxWidth()) { metricDetails=currentLabel to currentDetails }
      MetricTile(temperatureLabel,temperatureText(r.temperature,s.fahrenheit),"Sensor da bateria",Modifier.fillMaxWidth()) { metricDetails=temperatureLabel to temperatureDetails }
      MetricTile(rateLabel,if(measuring) rateText(r.rate) else "Indisponível","Variação da bateria por hora",Modifier.fillMaxWidth()) { metricDetails=rateLabel to rateDetails }
     }
    }
   }
  }
  item {
   val counter=r.currentInfo.counterMeanMa?.takeIf { matchesModeDirection(r.mode,it) }
   // Zero durante carga ou descarga não é, por si só, uma média útil.
   val androidAverage=r.currentInfo.androidAverageMa?.takeIf {
    matchesModeDirection(r.mode,it) && (it != 0.0 || !measuring)
   }
   val mean=counter ?: androidAverage
   val fromCounter=counter!=null
   var showDetails by remember { mutableStateOf(false) }
   SettingsCard("Corrente da bateria") {
    Text(if(charging) "Carga média observada" else if(r.mode==Mode.DISCHARGING) "Consumo médio observado" else "Corrente média observada",
     style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Text(mean?.let(::currentText) ?: "Aguardando média confiável",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
     Text(if(fromCounter) "Pelo contador da bateria" else if(androidAverage!=null) "Informada pelo Android" else "Dados insuficientes",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     SectionHelpButton("Corrente da bateria", "Valor líquido que entra ou sai da bateria, não a potência do cabo. Contador e Android podem usar períodos diferentes. Um zero não confirmado durante carga ou descarga é mostrado como ausência de leitura útil.")
    }
    TextButton(onClick={showDetails=!showDetails}) { Text(if(showDetails) "Ocultar detalhes" else "Detalhes da medição") }
    if(showDetails) {
     SessionDetailRow("Média do contador",counter?.let(::currentText) ?: "Sem leitura válida")
     SessionDetailRow("Média informada pelo Android",androidAverage?.let(::currentText) ?: "Sem leitura válida")

    }
   }
  }
  item { Chart(r.points,accent) }
  item { TelemetrySummaryCard(telemetry,System.currentTimeMillis(),onDetails) }
  if(r.error!=null) item { Text(r.error,color=MaterialTheme.colorScheme.error) }
  item { TextButton(onClick=onLimitations) { Icon(Icons.Default.Info,null); Spacer(Modifier.width(8.dp)); Text("Entenda as medições") } }
 }
 metricDetails?.let { (title, explanation) ->
  AlertDialog(onDismissRequest={metricDetails=null},title={Text(title)},text={Text(explanation)},confirmButton={TextButton(onClick={metricDetails=null}){Text("Fechar")}})
 }
}

@Composable
private fun MetricTile(label: String,value: String,unit: String,modifier: Modifier=Modifier,onClick: () -> Unit) {
 Card(onClick=onClick,modifier=modifier.heightIn(min=94.dp).semantics { contentDescription="$label: $value $unit. Toque para detalhes." },
  shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
   Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
   Text(value,fontSize=19.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
   Text(unit,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
  }
 }
}

@Composable
private fun Metric(label: String,value: String,detail: String,modifier: Modifier=Modifier) {
 Card(modifier,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text(label,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
   Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

@Composable
private fun Chart(points: List<Point>,accent: Color) {
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text("Pulso recente",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   val range=BatteryMath.chartRange(points)
   if(points.size<2 || range==null) {
    Text("Aguardando novas amostras…",Modifier.padding(vertical=20.dp),style=MaterialTheme.typography.bodySmall)
   } else {
    val lower=range.first
    val upper=range.last
    val difference=(upper-lower).coerceAtLeast(1)
    val elapsed=(points.last().elapsed-points.first().elapsed).coerceAtLeast(0L)
    val timeLabel=if(elapsed<60_000L) "há menos de 1 min" else "há ${elapsed/60_000L} min"
    Text("Últimos 30 min",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    val track=MaterialTheme.colorScheme.surfaceVariant
    Row(Modifier.fillMaxWidth().height(88.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     Column(Modifier.fillMaxHeight(),verticalArrangement=Arrangement.SpaceBetween) {
      Text("$upper%",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      Text("$lower%",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
     Canvas(Modifier.weight(1f).fillMaxHeight().semantics {
      contentDescription="Variação observada de ${points.first().percent}% para ${points.last().percent}%, entre $lower e $upper por cento"
     }) {
      for(i in 0..4) drawLine(track,Offset(0f,size.height*i/4),Offset(size.width,size.height*i/4),1.dp.toPx())
      val span=elapsed.coerceAtLeast(1L)
      val path=Path()
      points.forEachIndexed { i,p ->
       val x=((p.elapsed-points.first().elapsed).toDouble()/span*size.width).toFloat().coerceIn(0f,size.width)
       val y=size.height*(1-(p.percent-lower).toFloat()/difference)
       if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
      }
      drawPath(path,accent,style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
     }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("${points.first().percent}% • $timeLabel",style=MaterialTheme.typography.labelSmall)
     Text("${points.last().percent}% • agora",style=MaterialTheme.typography.labelSmall)
    }
   }
  }
 }
}
@Composable
private fun History(sessions: List<Session>, settings: Settings, vm: PulsoViewModel, modifier: Modifier, onCompare: () -> Unit, onReports: () -> Unit) {
 var filter by rememberSaveable { mutableIntStateOf(0) }
 var showInfo by rememberSaveable { mutableStateOf(false) }
 val filtered = remember(sessions,filter) {
  sessions.filter { filter==0 || it.mode==(if(filter==1) Mode.CHARGING.name else Mode.DISCHARGING.name) }
 }
 val locale = remember { Locale.forLanguageTag("pt-BR") }
 // Toda a lista, ANTES dos filtros, impede associar duas descargas separadas por carga.
 // A indicação é apenas uma hipótese, não faz junção física ou alteração no banco.
 val possibleResumptions = remember(sessions) {
  sessions.sortedWith(compareBy<Session> { it.started }.thenBy { it.id })
   .zipWithNext().filter { (before, after) ->
    SessionHistoryText.possibleContinuation(before.toBoundary(), after.toBoundary())
   }.associate { (before, after) -> after.id to (after.started-before.ended) }
 }
 // O DAO já retorna as sessões mais recentes primeiro. Nenhuma data é inventada.
 val groups = remember(filtered) {
  val day = SimpleDateFormat("yyyy-MM-dd",locale)
  filtered.groupBy { day.format(Date(it.started)) }
 }
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  item(key="history-header") {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
    Text("Histórico de bateria",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    IconButton(onClick={showInfo=true}) { Icon(Icons.Default.Info,"Como funcionam as sessões") }
   }
  }
  item(key="history-tools") {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick=onCompare,enabled=sessions.count { it.mode=="CHARGING" && it.closed && it.samples>=2 }>=2,modifier=Modifier.weight(1f)) {
     Icon(Icons.Default.DateRange,null,Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Comparar",maxLines=1)
    }
    OutlinedButton(onClick=onReports,modifier=Modifier.weight(1f)) {
     Icon(Icons.Default.Info,null,Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Relatórios",maxLines=1)
    }
   }
  }
  item(key="history-filters") {
   SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
    listOf("Todas","Carga","Descarga").forEachIndexed { index,label ->
     SegmentedButton(selected=filter==index,onClick={filter=index},shape=SegmentedButtonDefaults.itemShape(index,3),label={
      Text(label,style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
     })
    }
   }
  }
  if(filtered.isEmpty()) item(key="history-empty") {
   Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),shape=RoundedCornerShape(20.dp)) {
    Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text(if(filter==1) "Nenhuma carga registrada" else if(filter==2) "Nenhuma descarga registrada" else "Nenhuma sessão registrada",fontWeight=FontWeight.SemiBold)
     Text("As sessões aparecem depois de leituras reais, com o aplicativo aberto ou o monitoramento contínuo ativo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  groups.forEach { (dayKey, dailySessions) ->
   item(key="day-$dayKey") {
    val dayTitle = remember(dayKey) { SimpleDateFormat("dd 'de' MMMM 'de' yyyy",locale).format(Date(dailySessions.first().started)) }
    Text(dayTitle,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp,bottom=2.dp))
   }
   items(dailySessions,key={ "session-${it.id}" }) { session ->
    SessionHistoryCard(session,settings,vm,possibleResumptions[session.id])
   }
  }
 }
 if(showInfo) AlertDialog(onDismissRequest={showInfo=false},title={Text("Sobre as sessões")},text={
  Text("As sessões guardam leituras reais e são separadas por estado da bateria. Pausas curtas podem manter um registro, sem contar o tempo não observado. Após reinício do processo, encerramento do serviço ou lacuna prolongada, um novo registro é iniciado para não inventar medições. Quando duas sessões próximas têm mesmo estado e porcentagem na fronteira, mostramos uma possível retomada, mas preservamos os dois registros separados. Guardamos os 500 resumos mais recentes.\n\nA partir desta versão, também salvamos pontos para gráficos individuais, até 480 por sessão. Sessões antigas preservam seus resumos, mas não têm gráficos retroativos. Valores médios só aparecem quando os dados são suficientes.")
 },confirmButton={TextButton(onClick={showInfo=false}){Text("Entendi")}})
}

private fun Session.toBoundary(): SessionBoundary = SessionBoundary(
 mode=runCatching { Mode.valueOf(mode) }.getOrDefault(Mode.UNKNOWN),
 started=started,ended=ended,initial=initial,final=final,closed=closed,reason=reason
)

@Composable
private fun SessionHistoryCard(session: Session,settings: Settings,vm: PulsoViewModel,possibleGapMs: Long?) {
 var expanded by rememberSaveable(session.id) { mutableStateOf(false) }
 val mode = remember(session.mode) { runCatching { Mode.valueOf(session.mode) }.getOrDefault(Mode.UNKNOWN) }
 val change = SessionHistoryText.variation(session.initial,session.final,mode)
 val active = !session.closed
 val paused = active && session.reason.startsWith("Pausada")
 val stateText = when { paused -> "Pausada"; active -> "Em andamento"; else -> "Finalizada" }
 val accent = if(mode==Mode.CHARGING) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
 val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",Locale.forLanguageTag("pt-BR")) }
 val timeFormat = remember { SimpleDateFormat("HH:mm",Locale.forLanguageTag("pt-BR")) }
 val average = BatteryMath.sessionRate(session.initial,session.final,session.durationMs,session.samples)
 val rateLabel = SessionHistoryText.pace(average,mode,active)
 Card(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
    Text(mode.label,fontWeight=FontWeight.SemiBold,color=accent,style=MaterialTheme.typography.titleSmall)
    Surface(shape=RoundedCornerShape(50),color=accent.copy(alpha=.10f)) {
     Text(stateText,modifier=Modifier.padding(horizontal=9.dp,vertical=4.dp),style=MaterialTheme.typography.labelSmall,color=accent)
    }
   }
   BoxWithConstraints(Modifier.fillMaxWidth()) {
    val narrow = maxWidth < 288.dp || LocalDensity.current.fontScale > 1.2f
    if(narrow) {
     Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
      Text("${session.initial}% → ${session.final}%",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
      Text(change,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
      Text("${durationText(session.durationMs)} • ${timeFormat.format(Date(session.started))} → ${timeFormat.format(Date(session.ended))}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
    } else {
     Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
       Text("${session.initial}% → ${session.final}%",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
       Text(durationText(session.durationMs),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
       Text(change,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f))
       Spacer(Modifier.width(8.dp))
       Text("${timeFormat.format(Date(session.started))} → ${timeFormat.format(Date(session.ended))}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
     }
    }
   }
   if(possibleGapMs != null) {
    Text("Possível retomada • ${durationText(possibleGapMs)} sem observar",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
    Text("Ritmo médio: $rateLabel",style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
    Spacer(Modifier.width(8.dp))
    Text(if(expanded) "Ocultar detalhes  ▲" else "Ver detalhes  ▼",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,maxLines=1)
   }
   if(expanded) {
    HorizontalDivider(color=MaterialTheme.colorScheme.surfaceVariant)
    SessionDetailRow("Início",dateFormat.format(Date(session.started)))
    SessionDetailRow(if(session.closed) "Fim" else "Última leitura",dateFormat.format(Date(session.ended)))
    SessionDetailRow("Variação",change)
    SessionDetailRow("Duração observada",durationText(session.durationMs))
    SessionDetailRow("Ritmo médio",rateLabel)
    if(possibleGapMs != null) SessionDetailRow("Continuidade", "Possível retomada após ${durationText(possibleGapMs)} sem observação; registros separados")
    SessionDetailRow("Temperatura média (amostras)",temperatureText(BatteryMath.averageTemperature(session.temperatureSum,session.temperatureCount),settings.fahrenheit))
    SessionDetailRow("Amostras coletadas",session.samples.toString())
    SessionDetailRow("Motivo",session.reason)
    if(mode==Mode.CHARGING) {
     SessionDetailRow("Tipo de fonte",session.chargeSource ?: "Não registrado")
     session.chargerLabel?.let { SessionDetailRow("Nome do carregador (manual)",it) }
    }
    Text("Evolução da bateria",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=4.dp))
    val sampleFlow = remember(session.id) { vm.sessionSamples(session.id) }
    val savedSamples by sampleFlow.collectAsStateWithLifecycle(initialValue=emptyList())
    SessionHistoryChart(savedSamples,accent)
    savedSamples.lastOrNull { it.currentMa != null }?.let { measured ->
     SessionDetailRow("Última corrente salva",currentText(measured.currentMa))
     SessionDetailRow("Origem da corrente",measured.currentSource ?: "Indisponível")
    }
    savedSamples.lastOrNull { it.temperatureC != null }?.let { measured ->
     SessionDetailRow("Última temperatura salva",temperatureText(measured.temperatureC,settings.fahrenheit))
    }
    Text("Sessão #${session.id} • Pontos reais armazenados",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
 }
}

@Composable
private fun SessionDetailRow(label: String,value: String) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Top) {
  Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f))
  Spacer(Modifier.width(8.dp))
  Text(value,style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1.4f),textAlign=androidx.compose.ui.text.style.TextAlign.End)
 }
}

@Composable
private fun SessionHistoryChart(samples: List<SessionSample>,accent: Color) {
 val points = remember(samples) { samples.map { Point(it.timestamp,it.percent) } }
 val range = remember(points) { BatteryMath.chartRange(points) }
 when {
  points.isEmpty() || range==null -> Text("Aguardando pontos salvos. Sessões anteriores à alpha.6 possuem apenas o resumo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  else -> {
   val first=points.first()
   val last=points.last()
   val span=(last.elapsed-first.elapsed).coerceAtLeast(1L)
   val bottom=range.first
   val rangeSize=(range.last-bottom).coerceAtLeast(1)
   val grid=MaterialTheme.colorScheme.surfaceVariant
   Row(Modifier.fillMaxWidth().height(124.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    Column(Modifier.fillMaxHeight(),verticalArrangement=Arrangement.SpaceBetween) {
     Text("${range.last}%",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     Text("$bottom%",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Canvas(Modifier.weight(1f).fillMaxHeight().semantics { contentDescription="${points.size} amostras de ${first.percent} a ${last.percent} por cento, escala $bottom a ${range.last} por cento" }) {
     for (i in 0..4) drawLine(grid,Offset(0f,size.height*i/4),Offset(size.width,size.height*i/4),1.dp.toPx())
     fun xy(point: Point): Offset = Offset(
      ((point.elapsed-first.elapsed).toDouble()/span*size.width).toFloat().coerceIn(0f,size.width),
      size.height*(1-(point.percent-bottom).toFloat()/rangeSize)
     )
     points.zipWithNext().forEach { (a,b) ->
      if(BatteryMath.chartSamplesConnect(a,b)) drawLine(accent,xy(a),xy(b),strokeWidth=3.dp.toPx(),cap=StrokeCap.Round)
     }
     drawCircle(accent,3.dp.toPx(),xy(last))
     if(points.size==1) drawCircle(accent,3.dp.toPx(),xy(first))
    }
   }
   val time = remember { SimpleDateFormat("HH:mm",Locale.forLanguageTag("pt-BR")) }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
    Text(time.format(Date(first.elapsed)),style=MaterialTheme.typography.labelSmall)
    Text(time.format(Date(last.elapsed)),style=MaterialTheme.typography.labelSmall)
   }
   Text("${points.size} pontos salvos • intervalos sem observação não são conectados",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(s: Settings,r: Reading,active: Boolean,error: String?,vm: PulsoViewModel,
 onMonitor: (Boolean)->Unit,onLimitations: ()->Unit,onExport: ()->Unit,onImport: ()->Unit,modifier: Modifier) {
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { SettingsCard("Aparência") {
   val themes=listOf("system" to "Sistema","dark" to "AMOLED","light" to "Claro")
   SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
    themes.forEachIndexed { index,theme ->
     SegmentedButton(selected=s.theme==theme.first,onClick={vm.theme(theme.first)},shape=SegmentedButtonDefaults.itemShape(index,themes.size),label={
      Text(theme.second,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelMedium)
     })
    }
   }
  } }
  item { SettingsCard("Monitoramento contínuo") {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Text(if(active) "Notificação ativa" else if(s.continuous) "Monitoramento solicitado" else "Notificação desativada",Modifier.weight(1f))
    Switch(checked=s.continuous || active,onCheckedChange=onMonitor)
   }
   Text("Mantém as leituras e sessões com o app em segundo plano. Pode ser interrompido pelo Android. Sem bloqueio de suspensão da tela.",style=MaterialTheme.typography.bodySmall)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Text("Iniciar com o sistema",Modifier.weight(1f))
    Switch(checked=s.startOnBoot,onCheckedChange=vm::startOnBoot)
   }
   Text("Tenta retomar após reiniciar o celular, quando o monitoramento contínuo estiver habilitado e o Android permitir. Também tenta retomar após atualização.",style=MaterialTheme.typography.bodySmall)
   if(s.continuous && !active) {
    Text("Serviço interrompido ou aguardando Android. O monitoramento será tentado ao abrir o app.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
    TextButton(onClick={onMonitor(true)}) { Text("Tentar agora") }
   }
   if(error!=null) Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
  } }
  item { SettingsCard("Leituras e unidades") {
   BoxWithConstraints(Modifier.fillMaxWidth()) {
    val sideBySide=maxWidth>=280.dp && LocalDensity.current.fontScale<=1.2f
    if(sideBySide) {
     Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxWidth()) {
      Column(Modifier.weight(1f)) { IntervalPicker(s.interval,vm) }
      Column(Modifier.weight(1f)) { TemperaturePicker(s.fahrenheit,vm) }
     }
    } else {
     Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
      IntervalPicker(s.interval,vm)
      TemperaturePicker(s.fahrenheit,vm)
     }
    }
   }
   Text("30 s é o padrão. Eventos podem antecipar a leitura; o Android pode adiar atualizações durante a suspensão.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  } }
  item { SettingsCard("Alertas inteligentes") {
   val context=LocalContext.current
   var permissionHint by remember { mutableStateOf(false) }
   val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    permissionHint=!granted
    if(granted) vm.alertsEnabled(true)
   }
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
    Column(Modifier.weight(1f)) {
     Text("Avisar sobre alterações importantes",fontWeight=FontWeight.SemiBold)
     Text("Desativado por padrão",style=MaterialTheme.typography.bodySmall)
    }
    Switch(checked=s.alertsEnabled,onCheckedChange={ enable ->
     if(!enable) vm.alertsEnabled(false)
     else if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
     else if(NotificationManagerCompat.from(context).areNotificationsEnabled()) vm.alertsEnabled(true)
     else permissionHint=true
    })
   }
   if(permissionHint) Text("Permita notificações para o Pulso nas configurações do Android e tente novamente.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
   if(s.alertsEnabled) {
    AlertThresholdRow("Bateria baixa (%)",s.lowBatteryAlert,listOf(10,15,20,25,30),vm::alertLow)
    AlertThresholdRow("Temperatura máxima (°C)",s.hotBatteryAlert,listOf(40,43,45,48),vm::alertTemp)
    AlertThresholdRow("Meta de carga (%)",s.chargeTargetAlert,listOf(75,80,85,90),vm::alertTarget)
    AlertThresholdRow("Descarga rápida (%/h)",s.fastDrainAlert,listOf(15,20,25,30),vm::alertDrain)
   }
   Text("Avisos locais após duas leituras consecutivas, no máximo uma vez por episódio e com intervalo mínimo de 1 hora. Não interrompe carregamento, não usa Internet nem garante alerta se o Android suspender o monitoramento.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  } }
  item { SettingsCard("Estimativa de corrente (opcional)") {
   Text("Informe a capacidade ÚTIL aproximada da bateria, somente se souber. Sem esse valor, não estimamos mA a partir de %/h. A corrente estimada será indicada como tal.",style=MaterialTheme.typography.bodySmall)
   val presets=listOf(2500,3000,3500,4000,4500,5000,5500,6000,6500,7000)
   var capacityOption by rememberSaveable(s.capacityMah) {
    mutableStateOf(if(s.capacityMah != null && s.capacityMah in presets) s.capacityMah.toString() else "outro")
   }
   var capacityInput by rememberSaveable(s.capacityMah) { mutableStateOf(s.capacityMah?.toString() ?: "") }
   var capacityMenuExpanded by remember { mutableStateOf(false) }
   ExposedDropdownMenuBox(expanded=capacityMenuExpanded,onExpandedChange={capacityMenuExpanded=it}) {
    OutlinedTextField(value=if(capacityOption=="outro") "Digitar outro valor" else "${formatCapacityMah(capacityOption.toInt())} mAh",
     onValueChange={},readOnly=true,label={Text("Capacidade predefinida")},
     trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded=capacityMenuExpanded)},
     modifier=Modifier.menuAnchor().fillMaxWidth())
    ExposedDropdownMenu(expanded=capacityMenuExpanded,onDismissRequest={capacityMenuExpanded=false}) {
     presets.forEach { amount ->
      DropdownMenuItem(text={Text("${formatCapacityMah(amount)} mAh")},onClick={capacityOption=amount.toString();capacityMenuExpanded=false})
     }
     DropdownMenuItem(text={Text("Digitar outro valor…")},onClick={capacityOption="outro";capacityMenuExpanded=false})
    }
   }
   if(capacityOption=="outro") OutlinedTextField(
    value=capacityInput.toIntOrNull()?.let(::formatCapacityMah) ?: capacityInput,
    onValueChange={ value ->
     val digits=value.filter(Char::isDigit)
     if(digits.length<=5) capacityInput=digits
    },label={Text("Capacidade útil (mAh)")},placeholder={Text("Ex.: 5.000")},
    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,
    isError=capacityInput.toIntOrNull()?.let { it !in 500..30_000 } ?: false,
    modifier=Modifier.fillMaxWidth())
   val capacityValue = if(capacityOption=="outro") capacityInput.toIntOrNull() else capacityOption.toIntOrNull()
   val valid = capacityValue != null && capacityValue in 500..30_000
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    Button(onClick={vm.capacityMah(capacityValue)},enabled=valid && capacityValue!=s.capacityMah) { Text("Salvar") }
    TextButton(onClick={vm.capacityMah(null)},enabled=s.capacityMah!=null) { Text("Remover") }
   }
   Text("Valor salvo: ${s.capacityMah?.let { "${formatCapacityMah(it)} mAh" } ?: "não informado"}. Estimativas ignoram degradação e não equivalem a uma leitura de corrente.",style=MaterialTheme.typography.bodySmall)
  } }
  item { SettingsCard("Diagnóstico de corrente") {
   var expanded by rememberSaveable { mutableStateOf(false) }
   Text("Origem: ${r.currentInfo.source.label}",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
   Text(r.currentInfo.note,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "Ocultar dados brutos" else "Mostrar dados brutos") }
   if(expanded) {
    if(r.updated>0) SessionDetailRow("Horário da leitura",SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.forLanguageTag("pt-BR")).format(Date(r.updated)))
    SessionDetailRow("CURRENT_NOW",r.currentInfo.rawNow?.let { "$it (µA declarados)" } ?: "Indisponível")
    SessionDetailRow("CURRENT_AVERAGE",r.currentInfo.rawAverage?.let { "$it (µA declarados)" } ?: "Indisponível")
    SessionDetailRow("CHARGE_COUNTER",r.currentInfo.rawCounter?.let { "$it µAh" } ?: "Indisponível")
    SessionDetailRow("Média Android convertida",currentText(r.currentInfo.androidAverageMa))
    SessionDetailRow("Média pelo contador",if(r.currentInfo.counterMeanMa==null) "Indisponível" else if(matchesModeDirection(r.mode,r.currentInfo.counterMeanMa)) currentText(r.currentInfo.counterMeanMa) else "Sinal em verificação")
    SessionDetailRow("Calibração instantânea",r.currentInfo.nowAdjustment)
    SessionDetailRow("Calibração média",r.currentInfo.averageAdjustment)
    Text(r.currentInfo.comparison,style=MaterialTheme.typography.bodySmall)
    Text("Ajustes da instantânea: três blocos estáveis de pelo menos 90 s. Validade de até 7 dias, vinculada à instalação e ao firmware, com nova confirmação após pausas. Média Android não é recalibrada: sua janela é desconhecida. Sentinelas brutas permanecem visíveis para diagnóstico.",style=MaterialTheme.typography.bodySmall)
    TextButton(onClick={vm.resetCalibration()}) { Text("Recalibrar agora") }
   }
  } }
  item { SettingsCard("Backup e restauração") {
   Text("Exporte sessões, pontos dos gráficos e preferências em JSON local. O arquivo não é criptografado; guarde em lugar privado.",style=MaterialTheme.typography.bodySmall)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
    Button(onClick=onExport,enabled=!vm.backupBusy) { Text("Exportar") }
    OutlinedButton(onClick=onImport,enabled=!vm.backupBusy) { Text("Importar") }
   }
   if(vm.backupBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
   Text(vm.backupStatus,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text("Importar substitui todo o histórico atual. Desative o monitoramento contínuo antes de restaurar. Faça uma cópia antes.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
  } }
  item { SettingsCard("Sobre o Pulso") {
   Text("Pulso Bateria ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",fontWeight=FontWeight.SemiBold)
   Text("100% offline • Sem anúncios • Sem rastreamento",style=MaterialTheme.typography.bodySmall)
   Text("Histórico e preferências ficam neste aparelho. Desinstalar ou limpar os dados os remove. Exporte um backup manual antes de trocar a instalação.",style=MaterialTheme.typography.bodySmall)
   TextButton(onClick=onLimitations) { Text("Limitações das medições") }
  } }
 }
 vm.pendingBackup?.let { preview ->
  val date=SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.forLanguageTag("pt-BR")).format(Date(preview.exportedAt))
  AlertDialog(onDismissRequest={vm.cancelBackupRestore()},
   title={Text("Substituir histórico atual?")},
   text={Text("Backup de $date com ${preview.sessions.size} sessões e ${preview.samples.size} pontos. A restauração apagará os registros atuais e aplicará as configurações do arquivo. Não há mesclagem. Deseja continuar?")},
   confirmButton={TextButton(onClick={vm.restoreBackup()},enabled=!vm.backupBusy && !active) { Text("Substituir e restaurar") }},
   dismissButton={TextButton(onClick={vm.cancelBackupRestore()},enabled=!vm.backupBusy) { Text("Cancelar") }})
 }
}

@Composable
private fun IntervalPicker(interval: Int, vm: PulsoViewModel) {
 Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Text("Atualização • s",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  val intervals=listOf(15,30,60)
  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
   intervals.forEachIndexed { index,seconds ->
    SegmentedButton(selected=interval==seconds,onClick={vm.interval(seconds)},shape=SegmentedButtonDefaults.itemShape(index,intervals.size),label={
     Text("$seconds",style=MaterialTheme.typography.labelSmall,maxLines=1)
    })
   }
  }
 }
}

@Composable
private fun TemperaturePicker(fahrenheit: Boolean, vm: PulsoViewModel) {
 Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Text("Temperatura",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
   listOf(false,true).forEachIndexed { index,unit ->
    SegmentedButton(selected=fahrenheit==unit,onClick={vm.fahrenheit(unit)},shape=SegmentedButtonDefaults.itemShape(index,2),label={
     Text(if(unit) "°F" else "°C",style=MaterialTheme.typography.labelSmall)
    })
   }
  }
 }
}

@Composable
private fun SettingsCard(title: String,content: @Composable ColumnScope.()->Unit) {
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   content()
  }
 }
}

@Composable
private fun AlertThresholdRow(label: String,selected: Int,values: List<Int>,onChange: (Int)->Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
  Text(label,style=MaterialTheme.typography.labelMedium)
  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
   values.forEachIndexed { i,value ->
    SegmentedButton(selected=selected==value,onClick={onChange(value)},
     shape=SegmentedButtonDefaults.itemShape(i,values.size),label={ Text("$value",style=MaterialTheme.typography.labelSmall) })
   }
  }
 }
}
