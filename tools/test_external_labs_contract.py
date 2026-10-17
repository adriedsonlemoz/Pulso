#!/usr/bin/env python3
"""Contratos estáticos da API Labs: devem ser complementados por Android build e dois apps reais."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
svc=(root/'app/src/main/java/br/com/pulsobateria/integration/LabsBridgeService.kt').read_text()
trust=(root/'app/src/main/java/br/com/pulsobateria/integration/TrustedPartner.kt').read_text()
store=(root/'app/src/main/java/br/com/pulsobateria/data/Store.kt').read_text()
policy=(root/'core/src/main/kotlin/br/com/pulsobateria/core/ExternalLabsPolicy.kt').read_text()
ui=(root/'app/src/main/java/br/com/pulsobateria/ui/SettingsScreen.kt').read_text()
aidl=(root/'app/src/main/aidl/br/com/pulsobateria/integration/ILabsRemote.aidl').read_text()
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
ns='{http://schemas.android.com/apk/res/android}'
service=next(x for x in manifest.iter('service') if x.attrib.get(ns+'name') == '.integration.LabsBridgeService')
checks={
 'exported dedicated bound service': service.attrib[ns+'exported']=='true' and ns+'foregroundServiceType' not in service.attrib,
 'dedicated action':any(x.attrib.get(ns+'name')=='br.com.pulsobateria.LABS_API_V1' for x in service.iter('action')),
 'stable v1 four public methods':all(x in aidl for x in ('startTest(in Bundle request)','stopTest(long testId)','getStatus()','getReport(long testId)')),
 'no network/wakelock permissions':all('INTERNET' not in x.attrib.get(ns+'name','') and 'WAKE_LOCK' not in x.attrib.get(ns+'name','') for x in manifest.iter('uses-permission')),
 'UID before dispatcher':svc.index('val callerUid = Binder.getCallingUid()')<svc.index('runBlocking(Dispatchers.IO)'),
 'Binder UID packages authenticated': 'getPackagesForUid(uid)' in svc and 'caller(callerUid)' in svc,
 'shared UID rejected': 'packages.size != 1' in svc,
 'certificate verified per call':'TrustedPartner.signerFingerprint(this, packages.single())' in svc and 'it.signer == signer' in policy,
 'caller cannot self declare name':'request.getString("caller")' not in svc,
 'service remains foreground independent':'app.repository.serviceActive.value' in svc and 'MONITOR_OFF' in svc,
 'mutex prevents simultaneous starts':'operationLock.withLock' in svc,
 'manual test protected':'current?.id != testId' in svc and 'ExternalLabsPolicy.owns(caller, testId)' in svc,
 'other active test hidden':'OTHER_TEST_ACTIVE' in svc,
 'reports scoped to owner':'ExternalLabsPolicy.owns(caller, testId)' in svc.split('override fun getReport',1)[1],
 'partner ID saved by package':'rememberExternalLab(caller.packageName, test.id)' in svc,
 'global default off':'externalLabsEnabled: Boolean = false' in store,
 'migration reads prior partner':'oldClient(prefs)' in store and 'prefs[extLastId]' in store,
 'new partners persisted together':'external_labs_clients_v2' in store and 'saveClients(prefs' in store,
 'per-app toggle':'enableExternalPartner' in store and 'ExternalLabsPolicy.enabled' in store,
 'per-app revoke':'removeExternalPartner' in store and 'filterNot { it.packageName == packageName }' in store,
 'cert change drops old ownership':'prior.signer == signer' in policy,
 'restore revokes all':'it[extEnabled] = false; saveClients(it, emptyList())' in store,
 'signer reads installed package':'PackageManager.GET_SIGNING_CERTIFICATES' in trust and 'apkContentsSigners' in trust,
 'UI picks installed apps':'queryIntentActivities' in ui and 'Intent.CATEGORY_LAUNCHER' in ui,
 'UI per-app revoke with confirmation':'pendingRemove' in ui and 'removeExternalPartner' in ui,
 'client doc exists':(root/'docs/INTEGRACAO_VIGIA_IA.md').is_file(),
}
for label,ok in checks.items(): print(('PASS' if ok else 'FAIL'), label)
assert all(checks.values())
print(f'{len(checks)} contratos estáticos PASS; build e integração Android ainda pendentes.')
