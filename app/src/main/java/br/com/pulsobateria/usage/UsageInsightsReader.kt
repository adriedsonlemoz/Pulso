package br.com.pulsobateria.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import br.com.pulsobateria.core.*
import br.com.pulsobateria.data.SessionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Consulta somente dados locais após consentimento em Acesso a uso, sem monitorar outros apps em fundo. */
data class AppUsageEntry(val packageName: String, val displayName: String, val durationMs: Long)
data class UsageInsights(
 val from: Long, val until: Long, val apps: List<AppUsageEntry>,
 val hourlyMs: List<Long>, val foregroundMs: Long,
 val battery: ScreenBatteryStats,
 val sampleCount: Int,
 val hasUsageEvents: Boolean
)

class UsageInsightsReader(private val context: Context, private val dao: SessionDao) {
 fun hasPermission(): Boolean {
  val ops=context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
  return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),context.packageName)==AppOpsManager.MODE_ALLOWED
 }

 suspend fun query(): UsageInsights = withContext(Dispatchers.IO) {
  if (!hasPermission()) throw SecurityException("Autorize o acesso ao uso dos aplicativos.")
  val until=System.currentTimeMillis()
  val start=until-24L*3_600_000L
  // Eventos anteriores ajudam a conhecer o estado inicial sem preencher lacunas por hipótese.
  val warmup=start-6L*3_600_000L
  val usageManager=context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
  val cursor=usageManager.queryEvents(warmup,until)
   ?: throw IllegalStateException("O Android não disponibilizou eventos de uso. Desbloqueie o aparelho e tente novamente.")
  val events=ArrayList<UsageEventPoint>()
  val raw=UsageEvents.Event()
  while(cursor.hasNextEvent()) {
   cursor.getNextEvent(raw)
   val kind=when(raw.eventType) {
    UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventPoint.Kind.FOREGROUND
    UsageEvents.Event.MOVE_TO_BACKGROUND -> UsageEventPoint.Kind.BACKGROUND
    UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventPoint.Kind.SCREEN_INTERACTIVE
    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventPoint.Kind.SCREEN_NON_INTERACTIVE
    else -> null
   } ?: continue
   events.add(UsageEventPoint(raw.timeStamp,raw.packageName,kind))
  }
  val foreground=UsageAnalysisMath.foreground(events,start,until)
  // Nunca extrapolar a bateria para um período não observado ou para outra sessão.
  val points=dao.batterySamplesWindow(start,until).map {
   ObservedBatteryPoint(it.sessionId,it.timestamp,it.percent)
  }
  val battery=UsageAnalysisMath.battery(points,events,start,until)
  val pm=context.packageManager
  // Em Android 11+, a consulta limitada no Manifest permite resolver apps iniciáveis.
  // Nunca recorrer a QUERY_ALL_PACKAGES ou a nomes inventados em um catálogo fixo.
  @Suppress("DEPRECATION")
  val launchers=pm.queryIntentActivities(
   Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0
  ).associateBy { it.activityInfo.packageName }
  val apps=foreground.applications.map { stat ->
   val direct=try {
    @Suppress("DEPRECATION")
    val info=pm.getApplicationInfo(stat.packageName,0)
    pm.getApplicationLabel(info).toString().takeIf { it.isNotBlank() }
   } catch (_: Exception) { null }
   val launcherLabel=try { launchers[stat.packageName]?.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } }
    catch (_: Exception) { null }
   AppUsageEntry(stat.packageName,direct ?: launcherLabel ?: stat.packageName,stat.durationMs)
  }
  UsageInsights(start,until,apps,foreground.hourlyForegroundMs,foreground.observedForegroundMs,battery,points.size,events.isNotEmpty())
 }
}
