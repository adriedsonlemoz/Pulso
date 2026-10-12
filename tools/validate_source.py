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
assert permissions == {'android.permission.POST_NOTIFICATIONS','android.permission.FOREGROUND_SERVICE','android.permission.FOREGROUND_SERVICE_SPECIAL_USE'}
gradle=(root/'app/build.gradle.kts').read_text()
version=re.search(r'versionName = "([^"]+)"',gradle).group(1)
assert version == '0.1.0-alpha.2'
assert re.search(r'versionCode = 2\b',gradle)
for file in ['README.md','CHANGELOG.md','RELEASE_NOTES.md','docs/VERSIONING.md','.github/workflows/android.yml']:
 assert version in (root/file).read_text(), f'Version mismatch: {file}'
jar=root/'gradle/wrapper/gradle-wrapper.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest() == '2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046'
with zipfile.ZipFile(jar) as archive:
 assert archive.testzip() is None
 assert 'org/gradle/wrapper/GradleWrapperMain.class' in archive.namelist()
assert 'distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6' in (root/'gradle/wrapper/gradle-wrapper.properties').read_text()
print(f'PASS: {len(required)} required files, {len(xmls)} XMLs, permissions, version references and official Wrapper SHA-256/CRC.')
