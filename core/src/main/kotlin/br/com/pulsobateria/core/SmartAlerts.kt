package br.com.pulsobateria.core

/** Alertas são regras sobre medições disponíveis, sem inferir medidas ausentes. */
data class AlertLimits(
 val lowPercent: Int=20, val temperatureC: Int=43,
 val chargeTargetPercent: Int=80, val drainPercentHour: Int=20
) {
 init {
  require(lowPercent in 5..40 && temperatureC in 35..55 && chargeTargetPercent in 60..100 && drainPercentHour in 10..40)
 }
}
data class AlertSignal(val id: String,val title: String,val description: String)
object SmartAlerts {
 fun evaluate(percent: Int?, mode: Mode, temperatureC: Double?, ratePercentHour: Double?,
              observedDurationMs: Long, limits: AlertLimits): List<AlertSignal> {
  val result=mutableListOf<AlertSignal>()
  if(mode==Mode.DISCHARGING && percent!=null && percent in 0..limits.lowPercent)
   result+=AlertSignal("low","Bateria baixa","Bateria em $percent%. Limite configurado: ${limits.lowPercent}%.")
  if(temperatureC!=null && temperatureC.isFinite() && temperatureC in limits.temperatureC.toDouble()..90.0)
   result+=AlertSignal("heat","Bateria aquecida","Temperatura observada: %.1f °C (limite %d °C).".format(java.util.Locale.forLanguageTag("pt-BR"),temperatureC,limits.temperatureC))
  if(mode==Mode.CHARGING && percent!=null && percent in limits.chargeTargetPercent..100)
   result+=AlertSignal("target","Meta de carga atingida","Bateria em $percent%. Meta: ${limits.chargeTargetPercent}%.")
  if(mode==Mode.DISCHARGING && observedDurationMs>=300_000 && ratePercentHour!=null &&
    ratePercentHour.isFinite() && ratePercentHour<=-limits.drainPercentHour)
   result+=AlertSignal("drain","Descarga acelerada","Ritmo estimado: %.1f%%/h (alerta a partir de %d%%/h).".format(java.util.Locale.forLanguageTag("pt-BR"),-ratePercentHour,limits.drainPercentHour))
  return result
 }
}
