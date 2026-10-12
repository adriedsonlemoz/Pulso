package br.com.pulsobateria.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import android.app.Activity
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
 var limitations by rememberSaveable { mutableStateOf(false) }
 BackHandler(tab != 0) { tab = 0 }
 MaterialTheme(colorScheme=colors) {
  Scaffold(containerColor=colors.background, topBar={
   TopAppBar(title={ Column {
    Text(if(tab==0) "Pulso Bateria" else if(tab==1) "Suas sessões" else "Configurações",fontWeight=FontWeight.Bold)
    Text(if(tab==0) "ENERGIA EM TEMPO REAL" else "PULSO • ALPHA",style=MaterialTheme.typography.labelSmall,color=colors.primary,letterSpacing=2.sp)
   } }, actions={ if(tab!=2) IconButton(onClick={tab=2}) { Icon(Icons.Default.Settings,"Configurações") } },
    colors=TopAppBarDefaults.topAppBarColors(containerColor=colors.background))
  },bottomBar={
   NavigationBar(containerColor=colors.surface) {
    listOf("Início","Sessões","Configurações").forEachIndexed { index,label ->
     NavigationBarItem(selected=tab==index,onClick={tab=index},icon={
      Icon(when(index){0->Icons.Default.Home;1->Icons.Default.DateRange;else->Icons.Default.Settings},label)
     },label={Text(label)})
    }
   }
  }) { padding ->
   when(tab) {
    0 -> when(reading.mode) {
     Mode.CHARGING -> ChargingScreen(reading,settings,Modifier.padding(padding)) { limitations=true }
     Mode.DISCHARGING -> DischargingScreen(reading,settings,Modifier.padding(padding)) { limitations=true }
     else -> MonitorScreen(reading,settings,Modifier.padding(padding),"Visão da bateria",onLimitations={limitations=true})
    }
    1 -> History(sessions,settings,Modifier.padding(padding))
    else -> SettingsPage(settings,active,error,vm,onMonitor,{limitations=true},Modifier.padding(padding))
   }
  }
  if(limitations) AlertDialog(onDismissRequest={limitations=false},confirmButton={TextButton(onClick={limitations=false}){Text("Entendi")}},title={Text("Sobre as medições")},text={
   Text("Os dados vêm do Android e dependem do sensor e do fabricante. Corrente positiva indica entrada na bateria; negativa indica saída. Leituras com sinal contraditório ficam indisponíveis. Estar conectado não garante carga.\n\nTaxa e autonomia exigem pelo menos 5 minutos e 2 pontos percentuais de variação consistente. São estimativas, não garantias. A previsão do sistema tem prioridade durante a carga.\n\nSem monitoramento contínuo, só registramos com o app visível. Pausas de até 90 segundos podem retomar a mesma sessão se estado e porcentagem permanecerem iguais; o tempo parado não entra na duração. Lacunas de coleta acima de 3 minutos criam outra sessão. O Android pode suspender o serviço. Não há coleta durante desligamento nem recuperação de intervalos perdidos. Temperatura é da bateria, não da CPU. Corrente não é potência nem capacidade em mAh.")
  })
 }
}

@Composable
private fun ChargingScreen(r: Reading, s: Settings, modifier: Modifier, onLimitations: () -> Unit) {
 MonitorScreen(r,s,modifier,"Recarga",onLimitations)
}
@Composable
private fun DischargingScreen(r: Reading, s: Settings, modifier: Modifier, onLimitations: () -> Unit) {
 MonitorScreen(r,s,modifier,"Autonomia",onLimitations)
}
@Composable
private fun MonitorScreen(r: Reading, s: Settings, modifier: Modifier, pageTitle: String, onLimitations: () -> Unit) {
 val isDark = MaterialTheme.colorScheme.background == Color.Black
 val accent by animateColorAsState(if(r.mode==Mode.CHARGING) (if(isDark) Color(0xFF38E8C4) else Color(0xFF00785D)) else MaterialTheme.colorScheme.primary,label="estado")
 val progress by animateFloatAsState((r.percent ?: 0)/100f,tween(700),label="bateria")
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item {
   Text(pageTitle,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=accent)
   Text(if(r.mode==Mode.CHARGING) "Acompanhe o tempo até completar a carga" else if(r.mode==Mode.DISCHARGING) "Veja quanto tempo de bateria ainda resta" else r.mode.label,style=MaterialTheme.typography.bodySmall)
  }
  item {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    Surface(color=accent.copy(alpha=.12f),shape=RoundedCornerShape(50)) { Text(r.mode.label,Modifier.padding(horizontal=14.dp,vertical=8.dp),color=accent,style=MaterialTheme.typography.labelLarge) }
    Text(r.source,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  item {
   Box(Modifier.fillMaxWidth().padding(vertical=12.dp),contentAlignment=Alignment.Center) {
    val track=MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.size(240.dp).semantics { contentDescription="Bateria ${r.percent?.toString() ?: "indisponível"} por cento" }) {
     val inset=8.dp.toPx()
     val arcSize=Size(size.width-2*inset,size.height-2*inset)
     drawArc(track,135f,270f,false,topLeft=Offset(inset,inset),size=arcSize,style=Stroke(13.dp.toPx(),cap=StrokeCap.Round))
     drawArc(accent,135f,270f*progress,false,topLeft=Offset(inset,inset),size=arcSize,style=Stroke(13.dp.toPx(),cap=StrokeCap.Round))
    }
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
     Text("${r.percent?.toString() ?: "—"}%",fontSize=58.sp,fontWeight=FontWeight.Bold,letterSpacing=(-2).sp)
     Text(temperatureText(r.temperature,s.fahrenheit),color=accent,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
     Spacer(Modifier.height(12.dp))
     Text(if(r.updated==0L) "Aguardando leitura" else "${durationText(r.duration)} observados",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  item {
   Surface(shape=RoundedCornerShape(24.dp),color=accent.copy(alpha=.10f)) {
    Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Text(if(r.mode==Mode.CHARGING) "PREVISÃO ATÉ 100%" else "AUTONOMIA ESTIMADA",style=MaterialTheme.typography.labelMedium,color=accent,letterSpacing=1.sp)
     Text(if(r.mode==Mode.CHARGING || r.mode==Mode.DISCHARGING) durationText(r.remaining) else "Indisponível",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
     Text(if(r.systemEstimate) "Previsão fornecida pelo Android" else "Baseada na variação recente da bateria",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
   Metric(if(r.mode==Mode.CHARGING) "Corrente de carga" else "Consumo instantâneo",currentText(r.current),if(r.currentConflict) "Sinal inconsistente" else "Instantânea • bateria",Modifier.weight(1f))
   Metric("Temperatura",temperatureText(r.temperature,s.fahrenheit),"Sensor da bateria",Modifier.weight(1f))
  } }
  item { Metric(if(r.mode==Mode.CHARGING) "Velocidade de carga" else "Ritmo de descarga",if(r.mode==Mode.CHARGING || r.mode==Mode.DISCHARGING) rateText(r.rate) else "Indisponível","%/h • pontos percentuais por hora",Modifier.fillMaxWidth()) }
  item { Chart(r.points,accent) }
  if(r.error!=null) item { Text(r.error,color=MaterialTheme.colorScheme.error) }
  item { TextButton(onClick=onLimitations) { Icon(Icons.Default.Info,null); Spacer(Modifier.width(8.dp)); Text("Entenda as medições") } }
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
  Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("Pulso recente",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text("Últimos 30 minutos observados • escala 0–100%",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   if(points.size<2) Text("Aguardando novas amostras…",Modifier.padding(vertical=24.dp)) else {
    val track=MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(100.dp).semantics { contentDescription="Evolução da bateria: ${points.first().percent}% a ${points.last().percent}%" }) {
     for(i in 0..4) drawLine(track,Offset(0f,size.height*i/4),Offset(size.width,size.height*i/4),1.dp.toPx())
     val span=(points.last().elapsed-points.first().elapsed).coerceAtLeast(1)
     val path=Path()
     points.forEachIndexed { i,p ->
      val x=((p.elapsed-points.first().elapsed).toDouble()/span*size.width).toFloat()
      val y=size.height*(1-p.percent/100f)
      if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
     }
     drawPath(path,accent,style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("${points.first().percent}%",style=MaterialTheme.typography.labelSmall)
     Text("${points.last().percent}% • agora",style=MaterialTheme.typography.labelSmall)
    }
   }
  }
 }
}
@Composable
private fun History(sessions: List<Session>,settings: Settings,modifier: Modifier) {
 var filter by rememberSaveable { mutableIntStateOf(0) }
 val filtered=sessions.filter { filter==0 || it.mode==(if(filter==1) Mode.CHARGING.name else Mode.DISCHARGING.name) }
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { Text("Sessões com amostras reais. Pausas curtas podem retomar o mesmo registro; guardamos as 500 mais recentes.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
  item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Todas","Carga","Descarga").forEachIndexed { i,label -> FilterChip(selected=filter==i,onClick={filter=i},label={Text(label)}) } } }
  if(filtered.isEmpty()) item { Metric("Nenhuma sessão por aqui","Seu histórico começa agora","Use o app ou ative o monitoramento contínuo.",Modifier.fillMaxWidth()) }
  items(filtered,key={it.id}) { session ->
   val mode=runCatching { Mode.valueOf(session.mode) }.getOrDefault(Mode.UNKNOWN)
   val date=remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",Locale.forLanguageTag("pt-BR")) }
   Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
    Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text(mode.label,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
     Text("${session.initial}% → ${session.final}%",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
     val delta=session.final-session.initial
     Text("${if(delta>0) "+" else ""}$delta p.p. • ${durationText(session.durationMs)}",style=MaterialTheme.typography.bodyMedium)
     Text("Início: ${date.format(Date(session.started))}\n${if(session.closed) "Fim" else "Última leitura"}: ${date.format(Date(session.ended))}",style=MaterialTheme.typography.bodySmall)
     val average=BatteryMath.sessionRate(session.initial,session.final,session.durationMs,session.samples)
     val rateLabel=if(average==null && (session.closed || mode==Mode.FULL || mode==Mode.IDLE)) "Indisponível" else rateText(average)
     Text("Média: $rateLabel • ${temperatureText(BatteryMath.averageTemperature(session.temperatureSum,session.temperatureCount),settings.fahrenheit)}",style=MaterialTheme.typography.bodySmall)
     Text("Sessão #${session.id} • ${session.reason}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
 }
}
@Composable
private fun SettingsPage(s: Settings,active: Boolean,error: String?,vm: PulsoViewModel,onMonitor: (Boolean)->Unit,onLimitations: ()->Unit,modifier: Modifier) {
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item { SettingsCard("Aparência") {
   listOf("system" to "Sistema","dark" to "Escuro AMOLED","light" to "Claro").forEach { (value,label) ->
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { RadioButton(selected=s.theme==value,onClick={vm.theme(value)}); TextButton(onClick={vm.theme(value)}) { Text(label) } }
   }
  } }
  item { SettingsCard("Monitoramento contínuo") {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Text(if(active) "Notificação ativa" else "Notificação desativada",Modifier.weight(1f))
    Switch(checked=active,onCheckedChange=onMonitor)
   }
   Text("Mantém as leituras e sessões com o app em segundo plano. Pode ser interrompido pelo Android. Sem bloqueio de suspensão da tela.",style=MaterialTheme.typography.bodySmall)
   if(s.continuous && !active) Text("Monitoramento interrompido. Ative novamente para retomar.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
   if(error!=null) Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
  } }
  item { SettingsCard("Intervalo de atualização") {
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(15,30,60).forEach { value -> FilterChip(selected=s.interval==value,onClick={vm.interval(value)},label={Text("$value s")}) } }
   Text("30 s é o padrão. Eventos de bateria podem antecipar a leitura. Durante suspensão, o Android pode adiar as atualizações.",style=MaterialTheme.typography.bodySmall)
  } }
  item { SettingsCard("Unidade de temperatura") {
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    FilterChip(selected=!s.fahrenheit,onClick={vm.fahrenheit(false)},label={Text("Celsius • °C")})
    FilterChip(selected=s.fahrenheit,onClick={vm.fahrenheit(true)},label={Text("Fahrenheit • °F")})
   }
  } }
  item { SettingsCard("Sobre o Pulso") {
   Text("Pulso Bateria ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",fontWeight=FontWeight.SemiBold)
   Text("100% offline • Sem anúncios • Sem rastreamento",style=MaterialTheme.typography.bodySmall)
   Text("Histórico e preferências ficam neste aparelho. Desinstalar ou limpar os dados remove essas informações.",style=MaterialTheme.typography.bodySmall)
   TextButton(onClick=onLimitations) { Text("Limitações das medições") }
  } }
 }
}
@Composable
private fun SettingsCard(title: String,content: @Composable ColumnScope.()->Unit) {
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   content()
  }
 }
}
