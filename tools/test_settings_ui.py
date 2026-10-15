#!/usr/bin/env python3
"""Static interface-contract tests; not a replacement for Android compilation/UI tests."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=(root/'app/src/main/java/br/com/pulsobateria/ui/SettingsScreen.kt').read_text()
main=(root/'app/src/main/java/br/com/pulsobateria/ui/PulsoScreen.kt').read_text()
checks={
 'settings entrypoint exposed': 'internal fun SettingsPage(' in source and 'else -> SettingsPage(' in main,
 'home menu is initial state': 'mutableStateOf("home")' in source,
 'back navigation': 'BackHandler(enabled=section!="home")' in source and 'section="home"' in source,
 'reset scroll on category change': 'LaunchedEffect(section) { scrollState.scrollToItem(0) }' in source,
 'all eight categories': all(f'"{v}" ->' in source for v in ('appearance','monitor','reading','alerts','capacity','diagnostic','backup','about')),
 'status summary': 'Ativo em segundo plano' in source and 'Capacidade útil:' in source,
 'responsive scroll': 'LazyColumn(' in source and 'Arrangement.spacedBy(14.dp)' in source,
 'single unified picker': 'private fun <T> SettingsPills(' in source and 'SingleChoiceSegmentedButtonRow' not in source,
 'theme choices': all(x in source for x in ('"system" to "Sistema"','"dark" to "AMOLED"','"light" to "Claro"')),
 'read interval choices': '15 to "15 s",30 to "30 s",60 to "60 s"' in source,
 'fahrenheit toggle': 'true to "Fahrenheit (°F)"' in source,
 'monitoring control': 'vm::startOnBoot' in source and 'onMonitor)' in source,
 'alerts permission': 'POST_NOTIFICATIONS' in source and 'NotificationManagerCompat' in source,
 'four alert thresholds': all(s in source for s in ('vm::alertLow','vm::alertTemp','vm::alertTarget','vm::alertDrain')),
 'capacity persists': 'vm.capacityMah(value)' in source and 'vm.capacityMah(null)' in source,
 'diagnostic all raw sensors': all(n in source for n in ('CURRENT_NOW','CURRENT_AVERAGE','CHARGE_COUNTER','resetCalibration')),
 'backup actions and dialog': all(s in source for s in ('onExport','onImport','vm.pendingBackup','vm.restoreBackup()','vm.cancelBackupRestore()')),
 'limitations action': 'onLimitations' in source,
 'primary palette explicitly themed': 'primaryContainer=Color(0xFFD9EDFC)' in main and 'secondaryContainer=Color(0xFFD3F4E9)' in main,
 'dark palette explicitly themed': 'primaryContainer=Color(0xFF163C57)' in main and 'secondaryContainer=Color(0xFF124B43)' in main,
 'Labs kept in tabs': 'LabsScreen(vm' in main,
}
for label,okay in checks.items():
 print(('PASS' if okay else 'FAIL')+': '+label)
assert all(checks.values()),'Setting UI contract failed'
print(f'{len(checks)}/{len(checks)} static contracts OK; compilation and device visual inspection still required')
