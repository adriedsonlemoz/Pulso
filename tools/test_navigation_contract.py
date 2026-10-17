#!/usr/bin/env python3
"""Static UX regression checks; not a substitute for Android Compose compilation."""
from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/pulsobateria/ui'
root=(p/'PulsoScreen.kt').read_text()
usage=(p/'AppUsageScreen.kt').read_text()
telemetry=(p/'TelemetryPanel.kt').read_text()
charge=(p/'ChargeCompareCard.kt').read_text()
help=(p/'SectionHelp.kt').read_text()
checks={
 'Exactly four tabs including Sessions': 'listOf("Início","Sessões","Labs","Config.")' in root and 'listOf("Início","Uso"' not in root,
 'No Use tab or usage screen in navigation': '1 -> AppUsageScreen(' not in root and 'UsageTabIcon()' not in root and '1 -> History(sessions,settings,reading,vm,Modifier.padding(padding)' in root,
 'Sessions is a real tab': '1 -> "Sessões"' in root and '1 -> Icon(Icons.Default.DateRange,label)' in root,
 'Home shortcut switches to Sessions tab': 'onSessions={tab=1}' in root and 'onSessions={detailPage="sessions"}' not in root,
 'History features intact': 'onCompare={detailPage="compare"}' in root and 'onReports={detailPage="reports"}' in root,
 'Sessions not an internal detail route': 'detailPage=="sessions"' not in root,
 'Sessions accessible from home': 'onSessions={tab=1}' in root and 'Text("Histórico de sessões")' in root,
 'Labs opens index two': 'if(openLabs) 2 else 0' in root and 'if(openLabs) tab=2' in root,
 'Subpages and back handler': 'BackHandler(detailPage.isNotEmpty() || tab != 0)' in root and 'if(detailPage.isNotEmpty()) detailPage="" else tab=0' in root,
 'Detail actions available': all('detailPage=="'+v+'"' in root for v in ('analysis','compare','reports')),
 'Home compact + detailed graph': 'TelemetrySummaryCard(telemetry,System.currentTimeMillis(),onDetails)' in root and 'TelemetryPanel(telemetry,System.currentTimeMillis())' in root,
 'History buttons open separate pages': 'onCompare={detailPage="compare"}' in root and 'onReports={detailPage="reports"}' in root,
 'Comparison no longer embedded in history feed': 'item(key="charge-comparison")' not in root,
 'Legacy Use module retained but not navigable': 'listOf("Resumo","Aplicativos","Noite")' in usage and 'when(section)' in usage and 'AppUsageScreen(' not in root,
 'Help buttons really open dialogs': 'IconButton(onClick={open=true})' in help and 'AlertDialog(' in help,
 'Charger names behind expanders': 'if(showFirst) ChargerLabelEditor(first,vm)' in charge and 'if(showSecond) ChargerLabelEditor(second,vm)' in charge,
 'Partial data flags remain visible': 'dados parciais' in usage and '"Parciais"' in telemetry,
 'Core calculations unchanged': 'TelemetryAnalysis.summarize' in telemetry,
 'Percentage labels': '"Caiu ${stats.observedDrop}%"' in usage and 'Caiu ${s.battery.onDrop}%' in usage,
}
for label,ok in checks.items():
 print(('OK' if ok else 'FAIL') + ' — ' + label)
assert all(checks.values()),'Missing or regressed UI navigation contract'
