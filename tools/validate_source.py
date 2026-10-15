#!/usr/bin/env python3
"""Checks that do not require Android dependencies. Run at repository root."""
from pathlib import Path
import hashlib
import re
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parents[1]
required = ['settings.gradle.kts','build.gradle.kts','app/build.gradle.kts','gradlew','gradlew.bat',
 'gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties',
 '.github/workflows/android.yml','app/src/main/AndroidManifest.xml','README.md','CHANGELOG.md',
 'RELEASE_NOTES.md','AGENTS.md','docs/VERSIONING.md']
for name in required:
 assert (root/name).is_file(), f'Missing: {name}'
xmls = list((root/'app/src/main').rglob('*.xml'))
for path in xmls:
 ET.parse(path)
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
ns='{http://schemas.android.com/apk/res/android}'
permissions={x.attrib[ns+'name'] for x in manifest.findall('uses-permission')}
assert permissions == {'android.permission.POST_NOTIFICATIONS','android.permission.FOREGROUND_SERVICE','android.permission.FOREGROUND_SERVICE_SPECIAL_USE','android.permission.RECEIVE_BOOT_COMPLETED','android.permission.PACKAGE_USAGE_STATS'}
assert manifest.find("application/receiver[@"+ns+"name='.monitor.MonitorBootReceiver']") is not None
assert (root/'app/src/main/java/br/com/pulsobateria/ui/AppUsageScreen.kt').is_file()
assert (root/'app/src/main/java/br/com/pulsobateria/usage/UsageInsightsReader.kt').is_file()

gradle=(root/'app/build.gradle.kts').read_text()
version=re.search(r'versionName = "([^"]+)"',gradle).group(1)
assert version == '0.1.0-alpha.23'
assert re.search(r'versionCode = 23\b',gradle)
for file in ['README.md','CHANGELOG.md','RELEASE_NOTES.md','docs/VERSIONING.md','.github/workflows/android.yml']:
 assert version in (root/file).read_text(), f'Version mismatch: {file}'
jar=root/'gradle/wrapper/gradle-wrapper.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest() == '2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046'
with zipfile.ZipFile(jar) as archive:
 assert archive.testzip() is None
 assert 'org/gradle/wrapper/GradleWrapperMain.class' in archive.namelist()
assert 'distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6' in (root/'gradle/wrapper/gradle-wrapper.properties').read_text()
store=(root/'app/src/main/java/br/com/pulsobateria/data/Store.kt').read_text()
assert 'version = 6' in store and 'MIGRATION_1_2' in store and 'MIGRATION_2_3' in store and 'MIGRATION_3_4' in store and 'MIGRATION_4_5' in store and 'MIGRATION_5_6' in store
assert '.addMigrations(BatteryDatabase.MIGRATION_1_2, BatteryDatabase.MIGRATION_2_3, BatteryDatabase.MIGRATION_3_4, BatteryDatabase.MIGRATION_4_5, BatteryDatabase.MIGRATION_5_6)' in (root/'app/src/main/java/br/com/pulsobateria/PulsoApp.kt').read_text()
workflow=(root/'.github/workflows/android.yml').read_text()
assert f'Pulso-Bateria-v{version}-debug.apk' in workflow
assert 'actions/upload-artifact@v4' in workflow and 'KEYSTORE_BASE64' in workflow
assert (root/'app/src/main/java/br/com/pulsobateria/data/BackupManager.kt').exists()
assert (root/'app/schemas/room-migration-2-3.sql').exists()
assert (root/'app/schemas/room-migration-3-4.sql').exists()
assert (root/'app/schemas/room-migration-4-5.sql').exists()
assert (root/'app/schemas/room-migration-5-6.sql').exists()
assert (root/'app/src/main/java/br/com/pulsobateria/ui/LabsScreen.kt').exists()
assert (root/'app/src/main/java/br/com/pulsobateria/ui/TelemetryPanel.kt').is_file()
assert (root/'app/src/main/java/br/com/pulsobateria/ui/SectionHelp.kt').is_file()
ui=(root/'app/src/main/java/br/com/pulsobateria/ui/PulsoScreen.kt').read_text()
usage=(root/'app/src/main/java/br/com/pulsobateria/ui/AppUsageScreen.kt').read_text()
assert 'onCompare=' in ui and 'onReports=' in ui and 'detailPage=="analysis"' in ui
assert 'TelemetrySummaryCard(' in ui and 'TelemetryPanel(telemetry,' in ui
assert 'ChargeCompareCard(sessions,vm)' in ui and 'BatteryReportsCard(weeklyTelemetry,alwaysExpanded=true)' in ui
assert 'listOf("Resumo","Aplicativos","Noite")' in usage
assert 'SectionHelpButton(' in usage and 'SectionHelpButton(' in (root/'app/src/main/java/br/com/pulsobateria/ui/TelemetryPanel.kt').read_text()
assert (root/'core/src/main/kotlin/br/com/pulsobateria/core/TelemetryAnalysis.kt').is_file()
assert 'const val VERSION = 4' in (root/'app/src/main/java/br/com/pulsobateria/data/BackupManager.kt').read_text()
assert all(not list(root.rglob(ext)) for ext in ('*.p12','*.jks','*.keystore'))
print(f'PASS: {len(required)} required files, {len(xmls)} XMLs, permissions, version references and official Wrapper SHA-256/CRC.')
