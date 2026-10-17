# Pulso Bateria ↔ Vigia IA: API local Labs v1 (alpha.36)

## Objetivo e limites

O Pulso Bateria fornece **um serviço Android bound/AIDL, offline**, para um aplicativo instalado no **mesmo Android** solicitar testes Labs e consultar o teste/relatório correspondente. Não é API HTTP, não usa INTERNET nem monitora remotamente outro aparelho. Um teste mede a **bateria inteira** durante o período observado, não somente o Vigia IA.

O serviço externo **não** inicia o foreground service: antes do primeiro comando, o usuário deve abrir o Pulso Bateria e habilitar **Monitoramento contínuo**. Caso contrário, `startTest` retorna `MONITOR_OFF`. O Android pode suspender o monitoramento, e os lapsos permanecem sem dados. O serviço de integração não usa wake lock ou alarmes exatos.

## Como autorizar

1. Instale no mesmo aparelho o Pulso Bateria alpha.36 e o Vigia IA (ou outro cliente Android).
2. No Pulso, Config. → **Integração com aplicativos** → **Adicionar aplicativo**. Escolha o aplicativo correto conferindo nome **e pacote**. O pacote do Vigia IA deve ser obtido do aplicativo instalado, não há um ID presumido neste projeto.
3. Ative **Habilitar API local** e verifique **Permitir comandos** para cada parceiro. Essa autorização fica **desligada por padrão** e não é exportada no backup. O Pulso guarda o pacote e o SHA-256 do conjunto de certificados atuais; trocar/reinstalar o cliente com outra assinatura exige novo pareamento.
4. Habilite o monitoramento contínuo do Pulso e valide uma leitura recente. A chamada de consumo requer aparelho **desconectado de carregador/fonte**.

Até **20 clientes** podem ficar pareados simultaneamente, cada um com sua própria autorização, assinatura fixada e ID do último teste. O controle é validado em **todas** as transações AIDL usando o **UID fornecido pelo Binder** na thread original e a assinatura real do pacote instalado. UIDs compartilhados entre múltiplos pacotes são recusados, pois não permitem atribuir a chamada a um pacote específico. Informar outro pacote nos parâmetros **não** concede acesso. A revogação individual/global se aplica a novos comandos, independentemente dos demais clientes; um experimento já iniciado pode ser encerrado manualmente na interface Labs.

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

`Bundle stopTest(long testId)` encerra **somente** o último teste criado por aquele cliente autorizado, se ainda estiver ativo. Não encerra testes manuais da aba Labs.

`Bundle getStatus()` indica `running` e, quando existir um teste deste cliente, campos de resumo. Se houver experimento manual em andamento, retorna `OTHER_TEST_ACTIVE` sem ler dados privados dele.

`Bundle getReport(long testId)` busca no banco Room o último teste iniciado por esse cliente, com ID salvo separadamente dos outros aplicativos. Durante o teste retorna `status=RUNNING`; após conclusão, `status=COMPLETED` (prazo automático) ou `STOPPED` (encerrado manualmente/API). O relatório fica disponível até a retenção normal do histórico ou exclusão pelo usuário.

**Respostas**: `ok:Boolean`, `code:String`, `message:String`, `protocolVersion:Int=1` e, quando aplicável: `testId`, `kind`, `status`, `startedAt`, `deadlineAt` (`-1` se ausente), `endedAt` (`-1` se ausente), `durationMs`, `initialPercent`/`finalPercent` (`-1` se ausente), `chargingObservedMs`, `notChargingObservedMs`, `unobservedMs`, `sky`, `panelWatts` (quando informado), `screenCondition`, `cameraReported`, `wifiReported`, `recordingReported`, `validCurrentMs`, `meanCurrentMa` (**apenas** se houve corrente válida) e `externalPowerDetected`. Tempos de inatividade sem leituras **não são estimados** como consumo. `durationMs` é o tempo civil decorrido, não o total observado.

**Códigos de erro**: `UNAUTHORIZED`, `BAD_REQUEST`, `BUSY`, `MONITOR_OFF`, `NOT_READY`, `NOT_RUNNING`, `NOT_OWNER`, `NOT_FOUND`, `OTHER_TEST_ACTIVE`, `ERROR`.

O cliente deve persistir o ID retornado e consultar `getReport(id)` após terminar. Cada cliente pode consultar apenas seu próprio último ID de teste via API; os anteriores continuam no histórico local de Labs. Se outro cliente tiver criado o teste ativo, `getStatus` responderá `OTHER_TEST_ACTIVE`. Não existem dois testes Labs simultâneos. Evite poll excessivo; consulte em pontos relevantes (início, fim, reabertura de tela).

## Integração futura do Vigia IA

O pacote fonte do Vigia IA **não** foi alterado nesta entrega. Quando ele estiver disponível, implemente um wrapper Android (`MethodChannel`) e exponha ações na UI do módulo de navegação. Valide o AIDL no aparelho e faça testes com serviço ativo/desativado, troca de certificado, desautorização, reinício do processo, descarregamento conectado e cronômetro vencido.

## Evolução alpha.36: múltiplos clientes sem alterar o contrato

- Mantém a mesma ação de bind, pacote, interface AIDL e `protocolVersion=1`. Clientes alpha.35 continuam usando as mesmas quatro operações.
- Se o usuário tinha um cliente autorizado na alpha.35, ele aparece automaticamente na lista da alpha.36, com seu estado e ID antigo; novos cadastros são armazenados em DataStore num registro por aplicativo.
- A seleção de um segundo aplicativo não substitui o primeiro. Cada um pode ser pausado ou revogado separadamente nas Configurações. Existe também uma chave geral e **Revogar todos** com confirmação.
- Se o cliente for atualizado com outra assinatura, perdeu-se a autorização válida até que seja revogado e cadastrado novamente; reautorizar o mesmo certificado preserva seu último ID, mas uma assinatura diferente não herda IDs antigos.
- **Limitação:** há apenas um teste Labs por vez. Outro cliente recebe `BUSY` ao tentar iniciar durante um teste ativo. Não existe medição do consumo isolado de cada cliente. Sem monitoramento já ativo, `MONITOR_OFF`.
- O serviço mantém um mutex para serializar comandos concorrentes; autorização de cada chamada é conferida sob a trava. UIDs compartilhados são rejeitados.
- Permissões e lista de clientes não são incluídas em exportação/backup; restauração revoga tudo. Room v8/backup JSON v6 inalterados.
