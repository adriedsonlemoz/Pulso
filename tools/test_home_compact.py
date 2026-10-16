#!/usr/bin/env python3
"""Static UI regression contracts. Not a substitute for Compose compilation/UI tests."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
home=(root/'app/src/main/java/br/com/pulsobateria/ui/PulsoScreen.kt').read_text()
tele=(root/'app/src/main/java/br/com/pulsobateria/ui/TelemetryPanel.kt').read_text()
checks={
 'compact current is used':'CompactCurrentCard(' in home and 'private fun CompactCurrentCard(' in home,
 'current sensors preserved':'counter=r.currentInfo.counterMeanMa?.takeIf' in home and 'androidAverage=r.currentInfo.androidAverageMa?.takeIf' in home,
 'current 0 mA guard preserved':'(it != 0.0 || !measuring)' in home,
 'both current sources detail':'SessionDetailRow("Média do contador"' in home and 'SessionDetailRow("Média informada pelo Android"' in home,
 'current details toggle':'TextButton(onClick={showDetails=!showDetails}' in home,
 'help available':'SectionHelpButton("Corrente da bateria"' in home,
 'graph still uses points':'BatteryMath.chartRange(points)' in home and 'drawPath(path,accent' in home,
 'graph accessibility':'contentDescription="Variação observada' in home,
 'graph summary retained':'${points.first().percent}% • $timeLabel' in home and '${points.last().percent}% • agora' in home,
 'telemetry windows preserved':'listOf(5,20,60).forEach' in tele,
 'telemetry math retained':'TelemetryAnalysis.summarize(readings,now,minutes)' in tele,
 'telemetry accumulated mAh retained':'result.deltaMah?.let(::signedMah)' in tele,
 'telemetry data coverage retained':'result.coveragePercent<80' in tele,
 'telemetry details navigation retained':'TextButton(onClick=onDetails)' in tele and 'TelemetrySummaryCard(telemetry,System.currentTimeMillis(),onDetails)' in home,
 'responsive fonts':'LocalDensity.current.fontScale>1.2f' in tele,
 'detailed telemetry preserved':'fun TelemetryPanel(' in tele and 'Gráfico de medições' in tele,
}
for name, passed in checks.items():
 print(('PASS' if passed else 'FAIL')+': '+name)
assert all(checks.values()), 'Compact home UI contract failed'
print(f'{len(checks)}/{len(checks)} UI contracts passed')
