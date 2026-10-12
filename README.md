# Pulso Bateria

**0.1.0-alpha.8 • versionCode 8** — Android nativo, Kotlin e Jetpack Compose.
Android 8.0 (API 26) ou superior. Identidade: `br.com.pulsobateria`.

## Funcionalidades

- Porcentagem e estado via ACTION_BATTERY_CHANGED; temperatura da bateria, fonte USB/tomada/sem fio e corrente líquida em mA com indicação de origem (instantânea, média ou estimativa).
- `CURRENT_NOW` e `CURRENT_AVERAGE` com calibração automática conservadora de sinal e unidade; `CHARGE_COUNTER` como fonte de média alternativa após observação suficiente. Zero/sentinelas e leituras ambíguas são sinalizados, não apresentados como medição válida.
- Capacidade útil opcional em Configurações (500 a 30.000 mAh), para estimar mA a partir de %/h quando sensores falham. Diagnóstico de corrente exibe dados brutos locais e decisões do algoritmo. Não requer permissões ou Internet extras.
- Estados carregando, descarregando, cheia, conectado sem carregar e desconhecido.
- Taxa em %/h (pontos percentuais por hora) e estimativas com critérios mínimos. Previsão do Android só é aceita na carga após validação com taxa local observada e limites de coerência; valores suspeitos são descartados.
- Sessões persistentes Room; filtros todas/carga/descarga, agrupamento por data, cartões compactos e expansíveis com estado Em andamento/Pausada/Finalizada; até 500 resumos com início, fim, duração, percentuais, médias e gráfico individual quando existem amostras.
- Gráfico **ao vivo**, com janela de até 30 minutos. Para sessões da Alpha 7 em diante, são salvos pontos reais por alteração de nível ou após 60 s com nível estável, até 480 pontos por sessão (primeiro mais 479 recentes). O gráfico individual usa o instante real da leitura e não conecta lacunas superiores a 3 minutos. O banco migra de Room 1 para 2 preservando os resumos antigos, sem fabricar pontos retroativos.
- Temas Sistema, Escuro AMOLED (#000000) e Claro, selecionáveis horizontalmente. Interface em português, ícone e splash próprios.
- Notificação opcional por foreground service, com ação Parar e tratamento de permissão Android 13+.
- DataStore: tema, intervalo 15/30/60 s (padrão 30), unidade °C/°F, capacidade útil opcional e preferência de monitoramento.
- Sem INTERNET, anúncios, analytics, conta, root ou coleta externa. Backup automático do Android desativado; exportação e restauração manual JSON pelo seletor de arquivos, com checksum SHA-256.

## Estado desta entrega

A Alpha 8 foi preparada a partir da Alpha 7 após os logs do workflow **Pulso Android 8**. Os logs comprovam **BUILD SUCCESSFUL** nas tarefas Gradle (testes, lint e APK Debug), mas a etapa de publicação falhou por HTTP 500 da API GitHub Releases. O workflow agora preserva o APK em Artifacts por 30 dias antes da tentativa de criar a Release e faz até quatro tentativas. Não representa correção na compilação, que já havia passado.

**Mudanças:** backup/restauração JSON local via Configurações, Room v3 com migração explícita 2→3 e amostras contendo corrente/temperatura quando disponíveis, persistência conservadora da calibração confirmada por 7 dias (com opção de recalibrar), instrumentação de teste de migração, assinatura estável via Secrets e publicação robusta do APK. O monitoramento é congelado durante a restauração e não existe restauração automática por instalação.

### Gerar APK pelo GitHub Manager

1. Importe o ZIP do código-fonte na raiz do repositório (incluindo `.github`).
2. Importe **os quatro Secrets** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` pelo módulo Secrets do GitHub Manager. Os arquivos confidenciais com a chave foram fornecidos **separadamente** e nunca devem ser colocados no GitHub como arquivo de código. Guarde uma cópia da keystore privada e a senha fora do GitHub.
3. Execute **Android • Pulso Bateria**, verifique se a etapa de assinatura reconheceu os Secrets e baixe o APK pelo link da Release ou em **Actions → execução → Artifacts** caso a API de Releases esteja indisponível.

Sem os Secrets, o Debug continua compilável, porém a assinatura é temporária e **pode impedir a atualização preservando dados**. Com os mesmos Secrets importados nas próximas execuções, o certificado permanece constante. A chave criada para a Alpha 8 **não é a chave dos APKs antigos**. Portanto, não é possível garantir atualização direta sobre a Alpha 7 já instalada: nunca desinstale o app antigo contando que a Alpha 8 irá recuperar seus dados. Veja `docs/BACKUP_AND_SIGNING.md`.

## Compilar localmente

Java 17, Android SDK 35 e Build Tools 35.0.0. Configure `ANDROID_HOME` ou um `local.properties` com `sdk.dir`.

```sh
chmod +x gradlew
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Saída: `app/build/outputs/apk/debug/app-debug.apk`.
Dependências fixadas: AGP 8.9.2, Gradle 8.11.1, Kotlin/Compose Compiler 2.1.20, Compose BOM 2025.04.01, KSP 2.1.20-1.0.32, Room 2.7.1. O Wrapper oficial inclui verificação SHA-256 da distribuição. A primeira compilação exige internet para ferramentas e dependências; o aplicativo instalado não exige internet. Também compile os testes instrumentados via `:app:assembleDebugAndroidTest`, e execute em aparelho/emulador com `:app:connectedDebugAndroidTest`.

## Estrutura

- `app/src/main/java/br/com/pulsobateria/ui`: Compose, ViewModel e formatação.
- `app/src/main/java/br/com/pulsobateria/monitor`: coletor, coordenação de observadores, notificações e serviço.
- `core/src/main/kotlin/br/com/pulsobateria/core/CurrentTracking.kt`: calibração conservadora e escolha da fonte de corrente, com testes JVM.
- `app/src/main/java/br/com/pulsobateria/data`: entidades, DAO Room e DataStore.
- `core/src/main/kotlin`: cálculos independentes de Android.
- `core/src/test/kotlin`: testes JUnit de conversões, taxas, estimativas e fronteiras de sessão.
- `app/schemas`: SQL versionado das migrações 1→2 e 2→3; o JSON oficial de schema 3 deve ser capturado do compilador Room no CI e versionado após compilação.
- `.github/workflows/android.yml`: validação, assinatura por Secrets, artefato de fallback e publicação de APK direto com tentativas.
- `docs/MEASUREMENTS.md`, `docs/VALIDATION.md`, `docs/VERSIONING.md`: critérios, validação e versionamento.

## Uso e limitações

Abra o app para iniciar a observação. Sem serviço, a coleta para quando a Activity deixa de estar visível. Pausas de até 90 segundos podem retomar a mesma sessão somente se estado e porcentagem não mudaram, no mesmo processo. O tempo parado não entra na duração, e a curva/taxa recente reinicia para não ligar amostras através da pausa. Ative a notificação nas configurações para observar em segundo plano. O serviço é iniciado apenas por ação explícita com o app aberto; não há boot receiver, reinício automático nem wake lock. Se o sistema encerrar o processo ou o aparelho reiniciar, reative a opção. A preferência é preservada, mas a interface distingue preferência anterior de serviço realmente ativo.

O Android e o fabricante podem adiar leituras ou encerrar serviços. Não se promete notificação impossível de dispensar. Negar notificações mantém o modo de observação com o app aberto. `specialUse` é declarado com justificativa específica para a função opcional de telemetria; eventual publicação no Google Play depende da revisão das declarações de serviço pela loja.

Medições variam por aparelho. Corrente ausente ou contraditória é indisponível; a estimativa em mA por capacidade útil só existe quando o usuário informa o valor. A calibração confirmada é temporariamente persistida por até 7 dias, sem backup entre dispositivos. O gráfico não inventa pontos em lacunas. Consulte as limitações no próprio aplicativo e o relatório de validação antes de tratar esta Alpha como pronta para produção.
