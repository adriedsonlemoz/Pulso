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
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.pulsobateria.core.*
import br.com.pulsobateria.data.*
import br.com.pulsobateria.monitor.Reading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulsoScreen(onMonitor: (Boolean) -> Unit, openLabs: Boolean = false, vm: PulsoViewModel = viewModel()) {
 val settings by vm.settings.collectAsStateWithLifecycle()
 val reading by vm.reading.collectAsStateWithLifecycle()
 val sessions by vm.sessions.collectAsStateWithLifecycle()
 val dischargeHistory = remember(sessions,reading.updated) {
  DischargeForecast.history(sessions.map { HistoricalDischarge(it.started,it.ended,it.durationMs,it.initial,it.final,it.samples,it.mode) },System.currentTimeMillis())
 }
 val chargeHistory = remember(sessions,reading.updated,reading.source) {
  ChargeForecast.history(sessions.map { HistoricalCharge(it.started,it.ended,it.durationMs,it.initial,it.final,it.samples,it.mode,it.chargeSource) },
   System.currentTimeMillis(),reading.source)
 }
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
 val colors = if(dark) darkColorScheme(
  primary=Color(0xFF59B5FF),onPrimary=Color(0xFF002A44),
  primaryContainer=Color(0xFF163C57),onPrimaryContainer=Color(0xFFDDEFFF),
  secondary=Color(0xFF38E8C4),secondaryContainer=Color(0xFF124B43),onSecondaryContainer=Color(0xFFC7FFF0),
  background=Color.Black,surface=Color(0xFF101419),surfaceVariant=Color(0xFF17212B),
  onSurface=Color(0xFFF2F6FC),onSurfaceVariant=Color(0xFFB5C5D3),onBackground=Color.White,
  outlineVariant=Color(0xFF394957))
 else lightColorScheme(
  primary=Color(0xFF0061A7),onPrimary=Color.White,
  primaryContainer=Color(0xFFD9EDFC),onPrimaryContainer=Color(0xFF0E3F60),
  secondary=Color(0xFF006C59),secondaryContainer=Color(0xFFD3F4E9),onSecondaryContainer=Color(0xFF12483D),
  background=Color(0xFFF3F7FC),surface=Color.White,surfaceVariant=Color(0xFFE6EEF7),
  onSurface=Color(0xFF101C2A),onSurfaceVariant=Color(0xFF526375),onBackground=Color(0xFF101C2A),
  outlineVariant=Color(0xFFD6E1EB))
 var tab by rememberSaveable { mutableIntStateOf(if(openLabs) 3 else 0) }
 LaunchedEffect(openLabs) { if(openLabs) tab=3 }
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
    Text(when(detailPage) { "analysis" -> "Análise de medições"; "compare" -> "Comparar recargas"; "reports" -> "Relatórios"; else -> when(tab) { 0 -> "Pulso Bateria"; 1 -> "Suas sessões"; 2 -> "Uso da bateria"; 3 -> "Labs"; else -> "Configurações" } },fontWeight=FontWeight.Bold)
    Text(if(tab==0) "ENERGIA EM TEMPO REAL" else "PULSO • ALPHA",style=MaterialTheme.typography.labelSmall,color=colors.primary,letterSpacing=2.sp)
   } }, actions={ if(tab!=4 && detailPage.isEmpty()) IconButton(onClick={tab=4;detailPage=""}) { Icon(Icons.Default.Settings,"Configurações") } },
    colors=TopAppBarDefaults.topAppBarColors(containerColor=colors.background))
  },bottomBar={
   NavigationBar(containerColor=colors.surface) {
    listOf("Início","Sessões","Uso","Labs","Config.").forEachIndexed { index,label ->
     NavigationBarItem(selected=tab==index,onClick={tab=index;detailPage=""},icon={
      when(index) { 2 -> UsageTabIcon(); 3 -> LabsTabIcon(); else -> Icon(when(index){0->Icons.Default.Home;1->Icons.Default.DateRange;else->Icons.Default.Settings},label) }
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
     Mode.CHARGING -> ChargingScreen(reading,settings,telemetry,dischargeHistory,chargeHistory,Modifier.padding(padding),onDetails={detailPage="analysis"}) { limitations=true }
     Mode.DISCHARGING -> DischargingScreen(reading,settings,telemetry,dischargeHistory,chargeHistory,Modifier.padding(padding),onDetails={detailPage="analysis"}) { limitations=true }
     else -> MonitorScreen(reading,settings,telemetry,dischargeHistory,chargeHistory,Modifier.padding(padding),"Visão da bateria",onDetails={detailPage="analysis"},onLimitations={limitations=true})
    }
    1 -> History(sessions,settings,vm,Modifier.padding(padding),onCompare={detailPage="compare"},onReports={detailPage="reports"})
    2 -> AppUsageScreen(Modifier.padding(padding))
    3 -> LabsScreen(vm,Modifier.padding(padding),onMonitor)
    else -> SettingsPage(settings,reading,active,error,vm,onMonitor,{limitations=true},
     onExport={exportLauncher.launch("Pulso-Bateria-backup-${System.currentTimeMillis()}.json")},
     onImport={importLauncher.launch(arrayOf("application/json","application/octet-stream","*/*"))},
     modifier=Modifier.padding(padding))
   }
  }
  if(limitations) AlertDialog(onDismissRequest={limitations=false},confirmButton={TextButton(onClick={limitations=false}){Text("Entendi")}},title={Text("Sobre as medições")},text={
   Text("Os dados vêm do Android e dependem do sensor e do fabricante. Corrente positiva indica entrada líquida na bateria; negativa indica saída. O app usa µA e sinal Android. Ajustes da corrente instantânea exigem três janelas estáveis comparadas ao contador; isso não substitui calibração com instrumento. A média Android usa a janela desconhecida do hardware. Estar conectado não garante carga.\n\nA taxa local exige pelo menos 5 minutos e 2% de variação. A duração média com 100% usa só sessões de descarga; a duração equivalente de carga 0 a 100% usa apenas sessões carregando, inclusive parciais, com dados mínimos. Ambas combinam tendência atual e histórico quando disponíveis. Carregadores, fontes USB e energia solar podem apresentar taxas diferentes; perto de 100% a carga pode desacelerar. São estimativas, não garantias. A previsão do sistema só é aceita durante a carga quando compatível com a tendência medida.\n\nSem monitoramento contínuo, só registramos com o app visível. Pausas de até 90 segundos podem retomar a mesma sessão se estado e porcentagem permanecerem iguais; o tempo parado não entra na duração. Lacunas de coleta acima de 3 minutos criam outra sessão. O Android pode suspender o serviço. Não há coleta durante desligamento nem recuperação de intervalos perdidos. Temperatura é da bateria, não da CPU. Corrente não é potência nem capacidade em mAh.")
  })
 }
}

/** Ícone simples de frasco para Labs, sem a dependência material-icons-extended. */
@Composable
private fun LabsTabIcon() {
 val tint=androidx.compose.material3.LocalContentColor.current
 Canvas(Modifier.size(24.dp)) {
  val w=size.width; val h=size.height
  val path=Path().apply {
   moveTo(w*.40f,h*.12f); lineTo(w*.60f,h*.12f)
   moveTo(w*.45f,h*.12f); lineTo(w*.45f,h*.47f)
   lineTo(w*.19f,h*.83f); quadraticTo(w*.16f,h*.92f,w*.30f,h*.92f)
   lineTo(w*.70f,h*.92f); quadraticTo(w*.84f,h*.92f,w*.81f,h*.83f)
   lineTo(w*.55f,h*.47f); lineTo(w*.55f,h*.12f)
  }
  drawPath(path,tint,style=Stroke(width=w*.075f,cap=StrokeCap.Round))
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

/** Arredonda a previsão para cima aos próximos cinco minutos, sem alterar a estimativa física. */
private fun dischargeEndText(estimatedEnd: Long, measuredAt: Long): String {
 val step = 300_000L
 val rounded = estimatedEnd + (step - estimatedEnd % step) % step
 val fmt = SimpleDateFormat("HH:mm",Locale.forLanguageTag("pt-BR"))
 val today = SimpleDateFormat("yyyy-MM-dd",Locale.forLanguageTag("pt-BR"))
 val date = Date(rounded)
 val base = Date(measuredAt)
 val tomorrow = Calendar.getInstance().apply { time=base; add(Calendar.DAY_OF_YEAR,1) }.time
 val label = when(today.format(date)) {
  today.format(base) -> "Hoje"
  today.format(tomorrow) -> "Amanhã"
  else -> SimpleDateFormat("dd/MM",Locale.forLanguageTag("pt-BR")).format(date)
 }
 return "$label, às ${fmt.format(date)}"
}

@Composable
private fun ChargingScreen(r: Reading, s: Settings, telemetry: List<TelemetrySample>, autonomy: HistoricalAutonomy?, chargeHistory: HistoricalCharging?, modifier: Modifier, onDetails: () -> Unit, onLimitations: () -> Unit) {
 MonitorScreen(r,s,telemetry,autonomy,chargeHistory,modifier,"Recarga",onDetails,onLimitations)
}
@Composable
private fun DischargingScreen(r: Reading, s: Settings, telemetry: List<TelemetrySample>, autonomy: HistoricalAutonomy?, chargeHistory: HistoricalCharging?, modifier: Modifier, onDetails: () -> Unit, onLimitations: () -> Unit) {
 MonitorScreen(r,s,telemetry,autonomy,chargeHistory,modifier,"Autonomia",onDetails,onLimitations)
}
@Composable
private fun MonitorScreen(r: Reading, s: Settings, telemetry: List<TelemetrySample>, autonomy: HistoricalAutonomy?, chargeHistory: HistoricalCharging?, modifier: Modifier, pageTitle: String, onDetails: () -> Unit, onLimitations: () -> Unit) {
 val isDark = MaterialTheme.colorScheme.background == Color.Black
 val accent by animateColorAsState(
  if(r.mode==Mode.CHARGING) (if(isDark) Color(0xFF38E8C4) else Color(0xFF00785D)) else MaterialTheme.colorScheme.primary,
  label="estado"
 )
 val progress by animateFloatAsState((r.percent ?: 0)/100f,tween(700),label="bateria")
 val charging = r.mode==Mode.CHARGING
 val measuring = charging || r.mode==Mode.DISCHARGING
 val dischargeEta = remember(r.percent,r.rate,r.updated,r.mode,autonomy) {
  if (r.mode==Mode.DISCHARGING) DischargeForecast.prediction(r.percent,r.rate,r.updated,System.currentTimeMillis(),autonomy) else null
 }
 val chargeEta = remember(r.percent,r.rate,r.remaining,r.updated,r.mode,chargeHistory) {
  if (r.mode != Mode.CHARGING || r.percent == null || r.percent !in 0..99) null
  else ChargeForecast.prediction(r.percent,r.rate,r.updated,System.currentTimeMillis(),chargeHistory)
   ?: r.remaining?.takeIf { it in 1L..604_800_000L && r.updated > 0L &&
    System.currentTimeMillis() - r.updated in 0L..180_000L && r.updated <= Long.MAX_VALUE-it }
    ?.let { ChargeEta(it,r.updated+it,if(r.systemEstimate) "Previsão local validada do Android" else "Tendência de carga atual") }
 }
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
   CompactHomeHeaderCard(
    pageTitle=pageTitle,
    subtitle=if(charging) "Acompanhe o tempo até completar a carga" else if(r.mode==Mode.DISCHARGING) "Veja quanto tempo de bateria ainda resta" else r.mode.label,
    modeLabel=r.mode.label,
    source=r.source,
    percent=r.percent,
    observedText=if(r.updated==0L) "Aguardando leitura" else "${durationText(r.duration)} observados",
    accent=accent,
    progress=progress
   )
  }
  item {
   CompactForecastCard(
    charging=charging,
    accent=accent,
    headline=if(charging) {
     if(r.percent==100) "100% no indicador" else chargeEta?.let { dischargeEndText(it.estimatedFullAtMs,r.updated) } ?: "Calculando…"
    } else {
     dischargeEta?.let { dischargeEndText(it.estimatedEndAtMs,r.updated) } ?: "Calculando…"
    },
    supporting=if(charging) {
     chargeEta?.let { "Até 100%: ${durationText(it.remainingMs)}" }
      ?: if(r.percent==100) "Android ainda indica carregando" else "Aguardando histórico ou taxa de recarga válida"
    } else {
     dischargeEta?.let { "Tempo restante: ${durationText(it.remainingMs)}" }
      ?: if(r.mode!=Mode.DISCHARGING) "Previsão disponível quando descarregando" else "Aguardando dados de descarga ou leitura recente"
    },
    basis=if(charging) chargeEta?.let { it.basis+" • horário aproximado" } else dischargeEta?.let { it.basis+" • horário aproximado" },
    averageTitle=if(charging) "Média equivalente de 0 a 100%" else "Média com 100%",
    averageValue=if(charging) chargeHistory?.let { durationText(it.fullChargeMs) } ?: "Dados insuficientes" else autonomy?.let { durationText(it.fullBatteryMs) } ?: "Dados insuficientes",
    averageDescription=if(charging) {
     chargeHistory?.let { "${it.sessionsUsed} sessões válidas de carregamento; extrapolação de recargas parciais" }
      ?: "Exige 30 min observados e ganho de 4% em sessões válidas de carga."
    } else {
     autonomy?.let { "${it.sessionsUsed} sessões válidas de descarga no histórico disponível" }
      ?: "Exige ao menos 30 min observados e 4% consumidos em sessões válidas."
    },
    statLabel=if(charging) "Tempo restante" else "Tempo restante",
    statValue=if(charging) chargeEta?.let { durationText(it.remainingMs) } ?: "—" else dischargeEta?.let { durationText(it.remainingMs) } ?: "—"
   )
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
   // As mesmas fontes e os mesmos filtros conservadores da alpha.30.
   val counter=r.currentInfo.counterMeanMa?.takeIf { matchesModeDirection(r.mode,it) }
   val androidAverage=r.currentInfo.androidAverageMa?.takeIf {
    matchesModeDirection(r.mode,it) && (it != 0.0 || !measuring)
   }
   CompactCurrentCard(
    title=if(charging) "Carga média observada" else if(r.mode==Mode.DISCHARGING) "Consumo médio observado" else "Corrente média observada",
    value=(counter ?: androidAverage)?.let(::currentText) ?: "Aguardando média confiável",
    source=if(counter!=null) "Pelo contador da bateria" else if(androidAverage!=null) "Informada pelo Android" else "Dados insuficientes",
    counter=counter,
    androidAverage=androidAverage
   )
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
private fun CompactHomeHeaderCard(
 pageTitle: String,
 subtitle: String,
 modeLabel: String,
 source: String,
 percent: Int?,
 observedText: String,
 accent: Color,
 progress: Float,
) {
 ElevatedCard(shape=RoundedCornerShape(24.dp),colors=CardDefaults.elevatedCardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Top) {
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
     Text(pageTitle,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=accent)
     Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    StatusPill(modeLabel,accent)
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
    CompactBatteryGauge(percent,progress,accent,Modifier.size(114.dp))
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
     SmallInfoChip("Fonte",source)
     SmallInfoChip("Status",observedText)
     Text(
      "Resumo compacto com a carga atual e o tempo realmente observado nesta sessão.",
      style=MaterialTheme.typography.bodySmall,
      color=MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines=3,
      overflow=TextOverflow.Ellipsis
     )
    }
   }
  }
 }
}

@Composable
private fun CompactForecastCard(
 charging: Boolean,
 accent: Color,
 headline: String,
 supporting: String,
 basis: String?,
 averageTitle: String,
 averageValue: String,
 averageDescription: String,
 statLabel: String,
 statValue: String,
) {
 Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=accent.copy(alpha=.10f))) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text(if(charging) "RECARGA ESTIMADA" else "AUTONOMIA ESTIMADA",style=MaterialTheme.typography.labelMedium,color=accent,letterSpacing=1.sp)
   Text(headline,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
   Text(supporting,style=MaterialTheme.typography.bodyMedium)
   basis?.let {
    Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    ForecastMiniCard(statLabel,statValue,Modifier.weight(1f))
    ForecastMiniCard(averageTitle,averageValue,Modifier.weight(1f))
   }
   Text(averageDescription,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

@Composable
private fun ForecastMiniCard(label: String,value: String,modifier: Modifier=Modifier) {
 Surface(modifier=modifier,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.92f)) {
  Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=11.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
   Text(label.uppercase(Locale.forLanguageTag("pt-BR")),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
   Text(value,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
  }
 }
}

@Composable
private fun StatusPill(text: String,accent: Color) {
 Surface(color=accent.copy(alpha=.12f),shape=RoundedCornerShape(50)) {
  Text(text,Modifier.padding(horizontal=12.dp,vertical=6.dp),color=accent,style=MaterialTheme.typography.labelLarge)
 }
}

@Composable
private fun SmallInfoChip(label: String,value: String) {
 Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.65f)) {
  Column(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
   Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text(value,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis)
  }
 }
}

@Composable
private fun CompactBatteryGauge(percent: Int?,progress: Float,accent: Color,modifier: Modifier=Modifier) {
 Box(modifier.semantics { contentDescription="Bateria ${percent?.toString() ?: "indisponível"} por cento" },contentAlignment=Alignment.Center) {
  val track=MaterialTheme.colorScheme.surfaceVariant
  Canvas(Modifier.fillMaxSize()) {
   val stroke=10.dp.toPx()
   val inset=stroke/2
   drawCircle(track,radius=size.minDimension/2-stroke/2,style=Stroke(width=stroke))
   drawArc(accent,-90f,360f*progress,false,topLeft=Offset(inset,inset),size=Size(size.width-stroke,size.height-stroke),style=Stroke(width=stroke,cap=StrokeCap.Round))
  }
  Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
   Text(percent?.let { "$it%" } ?: "—",fontSize=28.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
   Text("bateria",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

/** Resumo técnico compacto. Ajuda e fontes independentes continuam disponíveis. */
@Composable
private fun CompactCurrentCard(
 title: String,
 value: String,
 source: String,
 counter: Double?,
 androidAverage: Double?
) {
 var showDetails by rememberSaveable { mutableStateOf(false) }
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Text("Corrente da bateria",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
    SectionHelpButton("Corrente da bateria", "Valor líquido que entra ou sai da bateria, não a potência do cabo. Contador e Android podem usar períodos diferentes. Um zero não confirmado durante carga ou descarga é mostrado como ausência de leitura útil.")
   }
   Text(title,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text(value,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
   Text(source,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   TextButton(onClick={showDetails=!showDetails},contentPadding=PaddingValues(horizontal=0.dp,vertical=0.dp)) {
    Text(if(showDetails) "Ocultar detalhes" else "Detalhes da medição")
    Spacer(Modifier.width(4.dp))
    Icon(if(showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,contentDescription=null,modifier=Modifier.size(18.dp))
   }
   if(showDetails) {
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    SessionDetailRow("Média do contador",counter?.let(::currentText) ?: "Sem leitura válida")
    SessionDetailRow("Média informada pelo Android",androidAverage?.let(::currentText) ?: "Sem leitura válida")
   }
  }
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
  Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    Text("Pulso recente",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
    Text("Últimos 30 min",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   val range=BatteryMath.chartRange(points)
   if(points.size<2 || range==null) {
    Text("Aguardando novas amostras…",Modifier.padding(vertical=20.dp),style=MaterialTheme.typography.bodySmall)
   } else {
    val lower=range.first
    val upper=range.last
    val difference=(upper-lower).coerceAtLeast(1)
    val elapsed=(points.last().elapsed-points.first().elapsed).coerceAtLeast(0L)
    val timeLabel=if(elapsed<60_000L) "há menos de 1 min" else "há ${elapsed/60_000L} min"
    val track=MaterialTheme.colorScheme.surfaceVariant
    Row(Modifier.fillMaxWidth().height(76.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun SettingsCard(title: String,content: @Composable ColumnScope.()->Unit) {
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   content()
  }
 }
}
