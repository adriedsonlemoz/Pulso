package br.com.pulsobateria.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.pulsobateria.BuildConfig
import br.com.pulsobateria.core.matchesModeDirection
import br.com.pulsobateria.data.Settings
import br.com.pulsobateria.monitor.Reading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Menu de preferências compacto: abre somente os controles da categoria escolhida. */
@Composable
internal fun SettingsPage(
 s: Settings, r: Reading, active: Boolean, error: String?, vm: PulsoViewModel,
 onMonitor: (Boolean) -> Unit, onLimitations: () -> Unit,
 onExport: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier
) {
 var section by rememberSaveable { mutableStateOf("home") }
 val scrollState=rememberLazyListState()
 LaunchedEffect(section) { scrollState.scrollToItem(0) }
 BackHandler(enabled=section!="home") { section="home" }
 val context=LocalContext.current
 var permissionHint by rememberSaveable { mutableStateOf(false) }
 val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  permissionHint=!granted
  if(granted) vm.alertsEnabled(true)
 }
 val title=when(section) {
  "appearance" -> "Aparência"; "monitor" -> "Monitoramento"
  "reading" -> "Leituras e unidades"; "alerts" -> "Alertas inteligentes"
  "capacity" -> "Estimativa de corrente"; "diagnostic" -> "Diagnóstico de corrente"
  "backup" -> "Backup e restauração"; "integration" -> "Integração com aplicativos"; "about" -> "Sobre o Pulso"
  else -> "Configurações"
 }
 LazyColumn(modifier.fillMaxSize(),state=scrollState,contentPadding=PaddingValues(start=16.dp,end=16.dp,top=12.dp,bottom=30.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  if(section=="home") {
   item {
    Column(Modifier.padding(horizontal=2.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
     Text("Personalize seu Pulso",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
     Text("Tudo organizado por assunto. Toque para ajustar.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
   item {
    SettingsGroup("PREFERÊNCIAS") {
     SettingsMenuItem(Icons.Default.Settings,"Aparência",when(s.theme){"dark"->"AMOLED";"light"->"Claro";else->"Sistema"}) { section="appearance" }
     SettingsMenuDivider()
     SettingsMenuItem(Icons.Default.DateRange,"Leituras e unidades","A cada ${s.interval} s  •  ${if(s.fahrenheit) "°F" else "°C"}") { section="reading" }
    }
   }
   item {
    SettingsGroup("CONTROLE E SEGURANÇA") {
     SettingsMenuItem(Icons.Default.Home,"Monitoramento",if(active) "Ativo em segundo plano" else if(s.continuous) "Aguardando Android" else "Desativado") { section="monitor" }
     SettingsMenuDivider()
     SettingsMenuItem(Icons.Default.Info,"Alertas inteligentes",if(s.alertsEnabled) "Ativados" else "Desativados") { section="alerts" }
     SettingsMenuDivider()
     SettingsMenuItem(Icons.Default.Share,"Integração com aplicativos",if(s.externalLabsEnabled) "API Labs autorizada" else "API Labs desativada") { section="integration" }
    }
   }
   item {
    SettingsGroup("BATERIA E DIAGNÓSTICO") {
     SettingsMenuItem(Icons.Default.Build,"Estimativa de corrente",s.capacityMah?.let { "Capacidade útil: ${formatCapacityMah(it)} mAh" } ?: "Capacidade não informada") { section="capacity" }
     SettingsMenuDivider()
     SettingsMenuItem(Icons.Default.Info,"Diagnóstico de corrente","Fonte: ${r.currentInfo.source.label}") { section="diagnostic" }
    }
   }
   item {
    SettingsGroup("DADOS E APLICATIVO") {
     SettingsMenuItem(Icons.Default.Share,"Backup e restauração","Exportar ou importar dados locais") { section="backup" }
     SettingsMenuDivider()
     SettingsMenuItem(Icons.Default.Info,"Sobre o Pulso","Versão ${BuildConfig.VERSION_NAME}") { section="about" }
    }
   }
   item {
    Text("Dados locais • Sem internet • Sem rastreamento",Modifier.fillMaxWidth().padding(vertical=4.dp),style=MaterialTheme.typography.labelSmall,
     color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  } else {
   item {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
     TextButton(onClick={section="home"},contentPadding=PaddingValues(horizontal=4.dp,vertical=4.dp)) {
      Icon(Icons.Default.ArrowBack,contentDescription=null,modifier=Modifier.size(18.dp))
      Spacer(Modifier.width(7.dp)); Text("Configurações")
     }
     Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=4.dp))
    }
   }
   when(section) {
    "appearance" -> {
     item { SettingsPanel("Tema do aplicativo") {
      SettingsPills(listOf("system" to "Sistema","dark" to "AMOLED","light" to "Claro"),s.theme,vm::theme)
      Text("A aparência escolhida vale para todas as abas. AMOLED usa fundo preto.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     } }
    }
    "monitor" -> {
     item { SettingsPanel("Em segundo plano") {
      SettingsToggleRow("Monitoramento contínuo",if(active) "Serviço ativo" else if(s.continuous) "Aguardando Android" else "Desativado",s.continuous || active,onMonitor)
      HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))
      SettingsToggleRow("Iniciar com o sistema","Tentar retomar após reinicialização",s.startOnBoot,vm::startOnBoot)
      SettingsExpandableHelp("Como funciona") {
       Text("Mantém leituras e sessões em segundo plano quando autorizado. O Android pode suspender o serviço; não há bloqueio permanente da tela. A retomada automática depende das restrições do aparelho.",style=MaterialTheme.typography.bodySmall)
      }
      if(s.continuous && !active) {
       Text("Monitoramento solicitado, mas o serviço não está ativo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
       OutlinedButton(onClick={onMonitor(true)}) { Text("Tentar agora") }
      }
      if(error!=null) Text(error,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
     } }
    }
    "reading" -> {
     item { SettingsPanel("Intervalo de atualização") {
      SettingsPills(listOf(15 to "15 s",30 to "30 s",60 to "60 s"),s.interval,vm::interval)
      Text("30 s é o padrão. Eventos podem antecipar leituras; o Android pode atrasá-las durante a suspensão.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     } }
     item { SettingsPanel("Unidade de temperatura") {
      SettingsPills(listOf(false to "Celsius (°C)",true to "Fahrenheit (°F)"),s.fahrenheit,vm::fahrenheit)
     } }
    }
    "alerts" -> {
     item { SettingsPanel("Avisos importantes") {
      SettingsToggleRow("Ativar alertas","Desativados por padrão",s.alertsEnabled) { enable ->
       if(!enable) vm.alertsEnabled(false)
       else if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
       else if(NotificationManagerCompat.from(context).areNotificationsEnabled()) vm.alertsEnabled(true)
       else permissionHint=true
      }
      if(permissionHint) Text("Autorize as notificações do Pulso nas configurações do Android.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
     } }
     if(s.alertsEnabled) {
      item { SettingsPanel("Limites dos alertas") {
       SettingsThreshold("Bateria baixa", "%",s.lowBatteryAlert,listOf(10,15,20,25,30),vm::alertLow)
       HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))
       SettingsThreshold("Temperatura máxima", "°C",s.hotBatteryAlert,listOf(40,43,45,48),vm::alertTemp)
       HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))
       SettingsThreshold("Meta de carga", "%",s.chargeTargetAlert,listOf(75,80,85,90),vm::alertTarget)
       HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))
       SettingsThreshold("Descarga rápida", "%/h",s.fastDrainAlert,listOf(15,20,25,30),vm::alertDrain)
      } }
     }
     item { SettingsPanel("Sobre os avisos") {
      Text("Os avisos exigem duas leituras consecutivas e têm intervalo mínimo de 1 hora por episódio. Não interrompem o carregamento e podem atrasar caso o Android suspenda o monitoramento.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     } }
    }
    "capacity" -> {
     item { SettingsPanel("Capacidade útil aproximada") {
      Text("Informe apenas se souber. Esse valor permite estimativas de corrente por %/h; não altera a leitura real do Android.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      val presets=listOf(2500,3000,3500,4000,4500,5000,5500,6000,6500,7000)
      var option by rememberSaveable(s.capacityMah) { mutableStateOf(if(s.capacityMah != null && s.capacityMah in presets) s.capacityMah.toString() else "outro") }
      var input by rememberSaveable(s.capacityMah) { mutableStateOf(s.capacityMah?.toString() ?: "") }
      var expanded by remember { mutableStateOf(false) }
      Box {
       OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)) {
        Text(if(option=="outro") "Digitar capacidade personalizada" else "${formatCapacityMah(option.toInt())} mAh",modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
        Icon(Icons.Default.KeyboardArrowDown,contentDescription=null)
       }
       DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
        presets.forEach { n -> DropdownMenuItem(text={Text("${formatCapacityMah(n)} mAh")},onClick={option=n.toString();expanded=false}) }
        DropdownMenuItem(text={Text("Outro valor…")},onClick={option="outro";expanded=false})
       }
      }
      if(option=="outro") OutlinedTextField(value=input.toIntOrNull()?.let(::formatCapacityMah) ?: input,
       onValueChange={ value -> val digits=value.filter(Char::isDigit);if(digits.length<=5) input=digits },
       label={Text("Capacidade útil (mAh)")},singleLine=true,
       keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
       isError=input.toIntOrNull()?.let { it !in 500..30_000 } ?: false,modifier=Modifier.fillMaxWidth())
      val value=if(option=="outro") input.toIntOrNull() else option.toIntOrNull()
      val valid=value!=null && value in 500..30_000
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
       Button(onClick={vm.capacityMah(value)},enabled=valid && value!=s.capacityMah) { Text("Salvar") }
       TextButton(onClick={vm.capacityMah(null)},enabled=s.capacityMah!=null) { Text("Remover") }
      }
      Text("Salvo: ${s.capacityMah?.let { "${formatCapacityMah(it)} mAh" } ?: "não informado"}",style=MaterialTheme.typography.labelMedium)
      Text("Estimativas não consideram degradação e não equivalem a uma medição de corrente.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     } }
    }
    "diagnostic" -> {
     item { SettingsPanel("Fonte de medição") {
      Text(r.currentInfo.source.label,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.titleMedium)
      Text(r.currentInfo.note,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     } }
     item { SettingsPanel("Dados técnicos") {
      var expanded by rememberSaveable { mutableStateOf(false) }
      OutlinedButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth()) {
       Text(if(expanded) "Ocultar leituras brutas" else "Mostrar leituras brutas")
      }
      if(expanded) {
       if(r.updated>0) SettingsValueRow("Leitura",SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.forLanguageTag("pt-BR")).format(Date(r.updated)))
       SettingsValueRow("CURRENT_NOW",r.currentInfo.rawNow?.let { "$it µA (Android)" } ?: "Indisponível")
       SettingsValueRow("CURRENT_AVERAGE",r.currentInfo.rawAverage?.let { "$it µA (Android)" } ?: "Indisponível")
       SettingsValueRow("CHARGE_COUNTER",r.currentInfo.rawCounter?.let { "$it µAh" } ?: "Indisponível")
       SettingsValueRow("Média Android",currentText(r.currentInfo.androidAverageMa))
       SettingsValueRow("Média pelo contador",if(matchesModeDirection(r.mode,r.currentInfo.counterMeanMa)) currentText(r.currentInfo.counterMeanMa) else if(r.currentInfo.counterMeanMa==null) "Indisponível" else "Sinal em verificação")
       SettingsValueRow("Ajuste instantâneo",r.currentInfo.nowAdjustment)
       SettingsValueRow("Ajuste da média",r.currentInfo.averageAdjustment)
       Text(r.currentInfo.comparison,style=MaterialTheme.typography.bodySmall)
       SettingsExpandableHelp("Limitações da calibração") {
        Text("Ajustes da instantânea exigem três blocos estáveis de ao menos 90 s, válidos até 7 dias e vinculados à instalação e firmware. A janela da média Android é desconhecida; sentinelas brutas permanecem visíveis.",style=MaterialTheme.typography.bodySmall)
       }
       TextButton(onClick={vm.resetCalibration()}) { Text("Recalibrar agora") }
      }
     } }
    }
    "integration" -> {
     item { SettingsPanel("Controle dos testes Labs") {
      Text("Autorize um aplicativo instalado, como o Vigia IA, a iniciar, consultar e encerrar somente os testes criados por ele. Funciona offline e não ativa sozinho o monitoramento.",style=MaterialTheme.typography.bodySmall)
      var chooser by remember { mutableStateOf(false) }
      var search by remember { mutableStateOf("") }
      val pm = context.packageManager
      val installed = remember(chooser) {
       if (!chooser) emptyList() else pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
        .mapNotNull { result ->
         val packageName = result.activityInfo?.packageName ?: return@mapNotNull null
         if(packageName == context.packageName) null
         else (result.loadLabel(pm)?.toString() ?: packageName) to packageName
        }.distinctBy { it.second }.sortedBy { it.first.lowercase(Locale.ROOT) }
      }
      Text("Aplicativo autorizado: ${s.externalLabsPackage ?: "nenhum"}",style=MaterialTheme.typography.bodySmall)
      OutlinedButton(onClick={chooser=true},modifier=Modifier.fillMaxWidth()) { Text("Selecionar aplicativo instalado") }
      if(chooser) AlertDialog(onDismissRequest={chooser=false},title={Text("Autorizar aplicativo")},text={
       Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value=search,onValueChange={search=it.take(50)},singleLine=true,
         label={Text("Pesquisar aplicativo")},modifier=Modifier.fillMaxWidth())
        LazyColumn(Modifier.heightIn(max=320.dp)) {
         items(installed.filter { it.first.contains(search,true) || it.second.contains(search,true) }) { entry ->
          TextButton(onClick={vm.pairExternalLabs(entry.second);chooser=false},modifier=Modifier.fillMaxWidth()) {
           Column(Modifier.fillMaxWidth()) {
            Text(entry.first,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(entry.second,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
           }
          }
         }
        }
       }
      },confirmButton={TextButton(onClick={chooser=false}) { Text("Fechar") }})
      SettingsToggleRow("Permitir controle externo","Exige seleção de aplicativo e assinatura válida",s.externalLabsEnabled,vm::externalLabsEnabled)
      if(s.externalLabsEnabled && s.externalLabsPackage!=null) Text("Autorizado: ${s.externalLabsPackage}",style=MaterialTheme.typography.labelSmall)
      TextButton(onClick={vm.clearExternalLabs()},enabled=s.externalLabsPackage!=null) { Text("Revogar autorização") }
      Text("A autorização é local e desativada por padrão. Não é incluída em backups. Se a assinatura do outro aplicativo mudar, selecione-o novamente. Um teste manual não pode ser encerrado pela API.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     } }
    }
    "backup" -> {
     item { SettingsPanel("Backup local em JSON") {
      Text("Exporte sessões, gráficos, experimentos Labs e preferências. O arquivo não é criptografado: guarde em local privado.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
       Button(onClick=onExport,enabled=!vm.backupBusy) { Text("Exportar") }
       OutlinedButton(onClick=onImport,enabled=!vm.backupBusy) { Text("Importar") }
      }
      if(vm.backupBusy) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
      if(vm.backupStatus.isNotBlank()) Text(vm.backupStatus,style=MaterialTheme.typography.bodySmall)
     } }
     item { SettingsPanel("Atenção ao restaurar") {
      Text("Importar substitui todo o histórico atual. Desative o monitoramento contínuo e faça uma cópia de segurança antes de restaurar.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
     } }
    }
    "about" -> {
     item { SettingsPanel("Pulso Bateria") {
      Text("Versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",fontWeight=FontWeight.SemiBold)
      Text("100% offline • Sem anúncios • Sem rastreamento",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      Text("Histórico e preferências ficam somente neste aparelho. Desinstalar ou limpar os dados pode apagá-los. Exporte um backup antes de trocar a instalação.",style=MaterialTheme.typography.bodySmall)
      OutlinedButton(onClick=onLimitations) { Text("Limitações das medições") }
     } }
    }
   }
  }
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
private fun SettingsGroup(label: String,content: @Composable ColumnScope.() -> Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
  Text(label,modifier=Modifier.padding(start=6.dp),style=MaterialTheme.typography.labelSmall,
   color=MaterialTheme.colorScheme.primary,letterSpacing=1.2.sp,fontWeight=FontWeight.Bold)
  Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,
   border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))) {
   Column(content=content)
  }
 }
}

@Composable
private fun SettingsMenuItem(icon: ImageVector,title: String,subtitle: String,onClick: ()->Unit) {
 Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=14.dp,vertical=13.dp),
  verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
  Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.55f)) {
   Box(Modifier.size(42.dp),contentAlignment=Alignment.Center) {
    Icon(icon,contentDescription=null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(22.dp))
   }
  }
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
   Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
   Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines=2,overflow=TextOverflow.Ellipsis)
  }
  Icon(Icons.Default.KeyboardArrowRight,contentDescription=null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}

@Composable
private fun SettingsMenuDivider() {
 HorizontalDivider(modifier=Modifier.padding(start=68.dp,end=14.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))
}

@Composable
private fun SettingsPanel(title: String,content: @Composable ColumnScope.()->Unit) {
 Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,
  border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
   Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
   content()
  }
 }
}

@Composable
private fun SettingsToggleRow(title:String,subtitle:String,checked:Boolean,onChange:(Boolean)->Unit) {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
   Text(title,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Medium)
   Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Switch(checked=checked,onCheckedChange=onChange)
 }
}

/** Botões independentes com fundo e borda de seleção; evita barras segmentadas muito largas. */
@Composable
private fun <T> SettingsPills(options: List<Pair<T,String>>, selected:T,onSelected:(T)->Unit) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
  options.forEach { (value,label) ->
   val isSelected=selected==value
   Surface(onClick={onSelected(value)},modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics { stateDescription=if(isSelected) "Selecionado" else "Não selecionado" },
    shape=RoundedCornerShape(12.dp),
    color=if(isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    border=BorderStroke(if(isSelected) 1.5.dp else 1.dp,
     if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
    Box(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=11.dp),contentAlignment=Alignment.Center) {
     Text(label,style=MaterialTheme.typography.labelMedium,fontWeight=if(isSelected) FontWeight.Bold else FontWeight.Normal,
      color=if(isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
      maxLines=2,overflow=TextOverflow.Ellipsis)
    }
   }
  }
 }
}

@Composable
private fun SettingsThreshold(title:String,unit:String,selected:Int,options:List<Int>,onSelect:(Int)->Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
   Text(title,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
   Text("$selected $unit",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
  }
  SettingsPills(options.map { it to it.toString() },selected,onSelect)
 }
}

@Composable
private fun SettingsExpandableHelp(title: String,content: @Composable ColumnScope.()->Unit) {
 var expanded by rememberSaveable { mutableStateOf(false) }
 TextButton(onClick={expanded=!expanded},contentPadding=PaddingValues(horizontal=0.dp,vertical=4.dp)) {
  Text(if(expanded) "Ocultar: $title" else title)
  Spacer(Modifier.width(4.dp))
  Icon(if(expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,contentDescription=null)
 }
 if(expanded) Column(verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
}

@Composable
private fun SettingsValueRow(title: String,value: String) {
 Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(3.dp)) {
  Text(title,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Text(value,style=MaterialTheme.typography.bodyMedium)
 }
}
