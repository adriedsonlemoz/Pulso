package br.com.pulsobateria.core

/** Resumo visual de medidas reais, sem interpretar lacunas como tempo medido. */
object LabReportMath {
 fun coveragePercent(totalMs: Long, chargingMs: Long, notChargingMs: Long): Int {
  if(totalMs<=0) return 0
  val observed=(chargingMs.coerceAtLeast(0)+notChargingMs.coerceAtLeast(0)).coerceAtMost(totalMs)
  return ((100.0*observed/totalMs)+0.5).toInt().coerceIn(0,100)
 }
 fun dischargeRate(dropPoints: Int?, totalMs: Long): Double? =
  if(dropPoints!=null && dropPoints>=0 && totalMs>0)
   (dropPoints*3_600_000.0/totalMs).takeIf { it.isFinite() && it<=200.0 } else null
}
