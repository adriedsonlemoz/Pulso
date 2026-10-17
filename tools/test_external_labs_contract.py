#!/usr/bin/env python3
"""White-box source/security contract; doesn't replace build or Android instrumentation."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
svc=(root/'app/src/main/java/br/com/pulsobateria/integration/LabsBridgeService.kt').read_text()
trust=(root/'app/src/main/java/br/com/pulsobateria/integration/TrustedPartner.kt').read_text()
store=(root/'app/src/main/java/br/com/pulsobateria/data/Store.kt').read_text()
ui=(root/'app/src/main/java/br/com/pulsobateria/ui/SettingsScreen.kt').read_text()
aidl=(root/'app/src/main/aidl/br/com/pulsobateria/integration/ILabsRemote.aidl').read_text()
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
ns='{http://schemas.android.com/apk/res/android}'
service=next(x for x in manifest.iter('service') if x.attrib.get(ns+'name') == '.integration.LabsBridgeService')
checks={
 'exported only dedicated bridge':service.attrib[ns+'exported']=='true' and 'foregroundServiceType' not in service.attrib,
 'dedicated action':any(x.attrib.get(ns+'name')=='br.com.pulsobateria.LABS_API_V1' for x in service.iter('action')),
 'four public methods': all(x in aidl for x in ('startTest(in Bundle request)','stopTest(long testId)','getStatus()','getReport(long testId)')),
 'no network/wakelock perms': all('INTERNET' not in x.attrib.get(ns+'name','') and 'WAKE_LOCK' not in x.attrib.get(ns+'name','') for x in manifest.iter('uses-permission')),
 'UID captured before dispatcher':svc.index('val callerUid = Binder.getCallingUid()') < svc.index('runBlocking(Dispatchers.IO)'),
 'caller tied to UID': 'getPackagesForUid(uid)?.contains(name)' in svc and 'caller(callerUid)' in svc,
 'cert checked on every command': 'TrustedPartner.signerFingerprint(this, name)' in svc and 'current == pinned' in svc,
 'caller chooses no package': 'request.getString("caller")' not in svc,
 'monitor required': 'app.repository.serviceActive.value' in svc and 'MONITOR_OFF' in svc,
 'manual test cannot be stopped': 's.externalLabsLastId != testId' in svc and 'current?.id != testId' in svc,
 'manual state hidden': 'OTHER_TEST_ACTIVE' in svc,
 'report scoped': 'externalLabsLastId != testId' in svc.split('override fun getReport',1)[1],
 'pair default off': 'externalLabsEnabled: Boolean = false' in store and 'it[extEnabled] = false' in store,
 'restore revokes pairing': 'it.remove(extPackage); it.remove(extCert); it.remove(extLastId)' in store,
 'signer reads installed package': 'PackageManager.GET_SIGNING_CERTIFICATES' in trust and 'apkContentsSigners' in trust,
 'user selects app from launcher': 'queryIntentActivities' in ui and 'Intent.CATEGORY_LAUNCHER' in ui,
 'UI enables and revokes': 'vm::externalLabsEnabled' in ui and 'vm.clearExternalLabs()' in ui,
 'client doc exists':(root/'docs/INTEGRACAO_VIGIA_IA.md').is_file(),
}
for label,ok in checks.items():
 print(('PASS' if ok else 'FAIL'),label)
assert all(checks.values())
print(f'{len(checks)} external labs static contracts PASS; Android build/instrumentation still required.')
