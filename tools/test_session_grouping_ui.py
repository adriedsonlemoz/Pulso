#!/usr/bin/env python3
"""Static regression checks for session presentation. Not Android UI compilation."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
ui=(root/'app/src/main/java/br/com/pulsobateria/ui/PulsoScreen.kt').read_text()
core=(root/'core/src/main/kotlin/br/com/pulsobateria/core/SessionHistoryText.kt').read_text()
checks={
    'history sees fresh reading': 'History(sessions,settings,reading,vm' in ui,
    'grouping before filter': ui.index('SessionHistoryText.groupConsecutive')<ui.index('val filteredGroups'),
    'opt-out toggle': 'Switch(checked=groupNearby,onCheckedChange={groupNearby=it})' in ui,
    'original sessions accessible': 'members.forEach { session ->' in ui and 'SessionHistoryCard(session,settings,vm,possibleResumptions[session.id]' in ui,
    'sum only observed session duration': 'members.sumOf { it.durationMs.coerceAtLeast(0L) }' in ui,
    'gap shown separately': 'sem leitura (não contado)' in ui,
    'current requires fresh Android reading': 'screenNow-reading.updated !in 0L..180_000L' in ui and '!it.reason.startsWith("Pausada")' in ui,
    'current indicator available for originals and groups': 'CurrentSessionBadge(accent)' in ui and 'isCurrent=session.id==currentSessionId' in ui,
    'reasons restricted': 'previous.reason in setOf("Observação interrompida", "Intervalo sem observação", "Monitoramento pausado")' in core,
    'both source and name protected': 'previous.chargeSource == current.chargeSource' in core and 'previous.chargerLabel == current.chargerLabel' in core,
}
for label,passed in checks.items(): print(('PASS' if passed else 'FAIL')+': '+label)
assert all(checks.values()), 'Session grouping presentation regression'
print(f'{len(checks)}/{len(checks)} session grouping static checks passed')
