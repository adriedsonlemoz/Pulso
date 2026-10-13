package br.com.pulsobateria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.pulsobateria.core.BatteryReports
import br.com.pulsobateria.core.ReportReading
import br.com.pulsobateria.data.TelemetrySample
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Relatório de sete dias: só integra intervalos válidos da série técnica local. */
@Composable
fun BatteryReportsCard(telemetry: List<TelemetrySample>) {
 var expanded by remember { mutableStateOf(false) }
 val today=LocalDate.now()
 val report=remember(telemetry,today) {
  BatteryReports.week(telemetry.map { ReportReading(it.sessionId,it.timestamp,it.elapsedMs,
   it.mode,it.percent,it.temperatureC) },today,ZoneId.systemDefault())
 }
 val extra=MaterialTheme.colorScheme.onSurfaceVariant
 Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
    Column(Modifier.weight(1f)) {
     Text("Relatório da bateria",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
     Text("Hoje e últimos 7 dias • leituras locais",style=MaterialTheme.typography.bodySmall,color=extra)
    }
    TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "Ocultar" else "Ver relatório") }
   }
   if(expanded) {
    if(report.observedMs==0L) {
     Text("Ainda não há intervalos contínuos suficientes. Mantenha o monitoramento ativo para formar o histórico.",style=MaterialTheme.typography.bodySmall)
    } else {
     Text("Resumo dos últimos 7 dias",fontWeight=FontWeight.SemiBold)
     Text("Bateria: caiu ${report.dropPercent}% • carregou +${report.gainedPercent}%")
     Text("Tempo efetivamente observado: ${formatObservedHours(report.observedMs)}",style=MaterialTheme.typography.bodySmall)
     Text("Recarga observada: ${formatObservedHours(report.chargeMs)} • Descarga observada: ${formatObservedHours(report.dischargeMs)}",style=MaterialTheme.typography.bodySmall)
     Text("Sessões de recarga com leituras válidas: ${report.chargingSessions}",style=MaterialTheme.typography.bodySmall)
     report.maxTemperatureC?.let { Text("Maior temperatura registrada: ${String.format(Locale.forLanguageTag("pt-BR"),"%.1f",it)} °C",style=MaterialTheme.typography.bodySmall) }
     HorizontalDivider()
     Text("Por dia",fontWeight=FontWeight.SemiBold)
     val maxDrop=report.days.maxOf { it.dropPercent }.coerceAtLeast(1)
     val dateLabel=DateTimeFormatter.ofPattern("dd/MM",Locale.forLanguageTag("pt-BR"))
     report.days.reversed().forEach { day ->
      Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(day.date.format(dateLabel),style=MaterialTheme.typography.bodySmall)
        Text(if(day.observedMs==0L) "Sem dados" else "Caiu ${day.dropPercent}% • carregou +${day.gainedPercent}%",
         style=MaterialTheme.typography.bodySmall)
       }
       Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
        if(day.dropPercent>0) Box(Modifier.fillMaxWidth(day.dropPercent.toFloat()/maxDrop).fillMaxHeight()
         .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
       }
      }
     }
     Text("As barras representam queda observada no dia, não consumo por aplicativo. Intervalos acima de 3 min, mudanças de sessão e passagens pela meia-noite não são atribuídos. Totais podem ser parciais; porcentagens são diferenças na escala da bateria, não mAh.",
      style=MaterialTheme.typography.bodySmall,color=extra)
    }
   }
  }
 }
}
private fun formatObservedHours(ms: Long): String = "${ms/3_600_000}h ${ms%3_600_000/60_000}min"
