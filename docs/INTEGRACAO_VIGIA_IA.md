# Pulso Bateria ↔ Vigia IA: API local Labs v1 (alpha.35)

## Objetivo e limites

O Pulso Bateria fornece **um serviço Android bound/AIDL, offline**, para um aplicativo instalado no **mesmo Android** solicitar testes Labs e consultar o teste/relatório correspondente. Não é API HTTP, não usa INTERNET nem monitora remotamente outro aparelho. Um teste mede a **bateria inteira** durante o período observado, não somente o Vigia IA.

O serviço externo **não** inicia o foreground service: antes do primeiro comando, o usuário deve abrir o Pulso Bateria e habilitar **Monitoramento contínuo**. Caso contrário, `startTest` retorna `MONITOR_OFF`. O Android pode suspender o monitoramento, e os lapsos permanecem sem dados. O serviço de integração não usa wake lock ou alarmes exatos.

## Como autorizar

1. Instale no mesmo aparelho o Pulso Bateria alpha.35 e o Vigia IA (ou outro cliente Android).
2. No Pulso, Config. → **Integração com aplicativos** → **Selecionar aplicativo instalado**. Escolha o aplicativo correto conferindo nome **e pacote**. O pacote do Vigia IA deve ser obtido do aplicativo instalado, não há um ID presumido neste projeto.
3. Ative **Permitir controle externo**. Essa autorização fica **desligada por padrão** e não é exportada no backup. O Pulso guarda o pacote e o SHA-256 do conjunto de certificados atuais; trocar/reinstalar o cliente com outra assinatura exige novo pareamento.
4. Habilite o monitoramento contínuo do Pulso e valide uma leitura recente. A chamada de consumo requer aparelho **desconectado de carregador/fonte**.

Apenas um cliente fica pareado por vez. O controle é validado em **todas** as transações AIDL usando o **UID fornecido pelo Binder** na thread original e a assinatura real do pacote instalado. Informar outro pacote nos parâmetros **não** concede acesso. A revogação é imediata para novos comandos; um experimento já iniciado pode ser encerrado manualmente na interface Labs.

## Contrato estável v1

**Componente**: `br.com.pulsobateria.integration.LabsBridgeService`  
**Ação do bind**: `br.com.pulsobateria.LABS_API_V1`  
**Pacote remoto**: `br.com.pulsobateria`

Copie `app/src/main/aidl/br/com/pulsobateria/integration/ILabsRemote.aidl` para `android/app/src/main/aidl/br/com/pulsobateria/integration/ILabsRemote.aidl` no módulo Android cliente e habilite `buildFeatures { aidl = true }`, se necessário. No `AndroidManifest.xml` do **Vigia IA**, para Android 11+, declare:

```xml
<queries>
    <package android:name="br.com.pulsobateria"/>
</queries>
```

**Vinculação** pelo Android nativo (por exemplo, em código chamado de um `MethodChannel` Flutter), fora do fluxo de renderização da tela:

```kotlin
val intent = Intent("br.com.pulsobateria.LABS_API_V1")
    .setPackage("br.com.pulsobateria")
val connected = applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
// connection: ServiceConnection com onServiceConnected(name, service) {
//     remote = ILabsRemote.Stub.asInterface(service)
// }
// Antes de chamar, confirme connected e execute as operações AIDL em Dispatchers.IO.
// Faça unbindService(connection) quando terminar. Trate binderDied/RemoteException.
```

O `BIND_AUTO_CREATE` pode ativar **o processo do Pulso**, mas **não** ativa seu monitoramento em primeiro plano. O cliente deve tratar `bindService=false`, desconexões e ausência do aplicativo.

### Métodos

`Bundle startTest(in Bundle request)` com primitivas:

| Campo | Tipo | Uso |
|---|---|---|
| `kind` | `String` | `DISCHARGE` (padrão) ou `SOLAR` |
| `minutes` | `Int` | 1 a 1440 (padrão 60); prazo aproximado, sem alarme exato |
| `screen` | `String` | `NAO_INFORMADA` (padrão), `ACESA`, `APAGADA` |
| `camera` | `Boolean` | Contexto manual, não detectado pelo Pulso |
| `wifi` | `Boolean` | Contexto manual, não detectado pelo Pulso |
| `recording` | `Boolean` | Contexto manual, não detectado pelo Pulso |
| `notes` | `String` | Até 100 caracteres, controle rejeitado; Pulso acrescenta o pacote cliente |
| `sky` | `String` | Somente Solar: `forte`, `parcial`, `nublado` ou `chuva`; obrigatório |
| `panelWatts` | `Int` | Somente Solar: 1 a 200 W, opcional, potência informada |

Exemplo de consumo com câmera transmitindo durante 120 minutos:

```kotlin
val request = Bundle().apply {
    putString("kind", "DISCHARGE")
    putInt("minutes", 120)
    putString("screen", "APAGADA")
    putBoolean("camera", true)
    putBoolean("wifi", true)
    putString("notes", "Vigia IA: câmera via Wi-Fi")
}
// No dispatcher de I/O, depois de conectar e autenticar:
val response: Bundle = remote.startTest(request)
if (response.getBoolean("ok")) {
    val id = response.getLong("testId") // guardar ID para parar/consultar relatório
}
```

`Bundle stopTest(long testId)` encerra **somente** o último teste criado pelo cliente pareado, se ainda estiver ativo. Não encerra testes manuais da aba Labs.

`Bundle getStatus()` indica `running` e, quando existir um teste deste cliente, campos de resumo. Se houver experimento manual em andamento, retorna `OTHER_TEST_ACTIVE` sem ler dados privados dele.

`Bundle getReport(long testId)` busca no banco Room o último teste iniciado por esse cliente. Durante o teste retorna `status=RUNNING`; após conclusão, `status=COMPLETED` (prazo automático) ou `STOPPED` (encerrado manualmente/API). O relatório fica disponível até a retenção normal do histórico ou exclusão pelo usuário.

**Respostas**: `ok:Boolean`, `code:String`, `message:String`, `protocolVersion:Int=1` e, quando aplicável: `testId`, `kind`, `status`, `startedAt`, `deadlineAt` (`-1` se ausente), `endedAt` (`-1` se ausente), `durationMs`, `initialPercent`/`finalPercent` (`-1` se ausente), `chargingObservedMs`, `notChargingObservedMs`, `unobservedMs`, `sky`, `panelWatts` (quando informado), `screenCondition`, `cameraReported`, `wifiReported`, `recordingReported`, `validCurrentMs`, `meanCurrentMa` (**apenas** se houve corrente válida) e `externalPowerDetected`. Tempos de inatividade sem leituras **não são estimados** como consumo. `durationMs` é o tempo civil decorrido, não o total observado.

**Códigos de erro**: `UNAUTHORIZED`, `BAD_REQUEST`, `BUSY`, `MONITOR_OFF`, `NOT_READY`, `NOT_RUNNING`, `NOT_OWNER`, `NOT_FOUND`, `OTHER_TEST_ACTIVE`, `ERROR`.

O cliente deve persistir o ID retornado e consultar `getReport(id)` após terminar. Somente o último ID externo por pareamento fica consultável via API, embora os anteriores continuem no histórico de Labs. Evite poll excessivo; consulte em pontos relevantes (início, fim, reabertura de tela).

## Integração futura do Vigia IA

O pacote fonte do Vigia IA **não** foi alterado nesta entrega. Quando ele estiver disponível, implemente um wrapper Android (`MethodChannel`) e exponha ações na UI do módulo de navegação. Valide o AIDL no aparelho e faça testes com serviço ativo/desativado, troca de certificado, desautorização, reinício do processo, descarregamento conectado e cronômetro vencido.
