# Pulso Bateria

**0.1.0-alpha.7 • versionCode 7** — Android nativo, Kotlin e Jetpack Compose.
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
- Sem INTERNET, anúncios, analytics, conta, root ou coleta externa. Backup do app desativado.

## Estado desta entrega

A Alpha 7 adiciona leituras complementares de corrente, calibração automática condicionada a várias amostras, contador de carga como média independente e estimativa opcional baseada em capacidade informada. O diagnóstico expõe os dados originais; valores sem fundamento permanecem indisponíveis. O histórico e gráficos da Alpha 6, a migração Room 1→2 e as correções anteriores foram preservados. **O APK Android, Compose e Lint ainda precisam de validação no GitHub Actions e em aparelho.** Consulte `docs/VALIDATION.md` para os testes realmente executados.

## Gerar APK pelo GitHub Manager

1. Importe o ZIP completo na raiz do repositório, incluindo `.github` e o Wrapper oficial.
2. Envie para main/master ou execute manualmente Android • Pulso Bateria.
3. Após sucesso, abra o link **APK direto** no resumo da execução ou **Releases** no repositório e baixe `Pulso-Bateria-v0.1.0-alpha.7-debug.apk`.

O workflow publica SOMENTE o APK como asset de uma pré-release por execução. Não usa upload-artifact, não compacta o APK e não publica relatórios. O GitHub pode mostrar seus próprios links automáticos de código-fonte nas Releases; esses links não são arquivos produzidos pelo workflow. Logs e relatórios locais permanecem disponíveis apenas nas etapas/build, sem upload adicional.

A permissão `contents: write` do GITHUB_TOKEN automático permite criar a Release. Não exige Secrets personalizados nem assinatura Release. Publicação ocorre somente em push ou execução manual; não há publicação por pull request. Se a organização bloquear escrita pelo token, será necessário permitir essa escrita na configuração de Actions do repositório.

APK Debug é assinado com chave de desenvolvimento. Runners diferentes podem gerar chaves distintas e impedir atualização sobre uma instalação anterior; uma chave estável para distribuição será tratada em versão futura. Não foram inventados metadados proprietários do GitHub Manager.

## Compilar localmente

Java 17, Android SDK 35 e Build Tools 35.0.0. Configure `ANDROID_HOME` ou um `local.properties` com `sdk.dir`.

```sh
chmod +x gradlew
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Saída: `app/build/outputs/apk/debug/app-debug.apk`.
Dependências fixadas: AGP 8.9.2, Gradle 8.11.1, Kotlin/Compose Compiler 2.1.20, Compose BOM 2025.04.01, KSP 2.1.20-1.0.32, Room 2.7.1. O Wrapper oficial inclui verificação SHA-256 da distribuição. A primeira compilação exige internet para ferramentas e dependências; o aplicativo instalado não exige internet.

## Estrutura

- `app/src/main/java/br/com/pulsobateria/ui`: Compose, ViewModel e formatação.
- `app/src/main/java/br/com/pulsobateria/monitor`: coletor, coordenação de observadores, notificações e serviço.
- `core/src/main/kotlin/br/com/pulsobateria/core/CurrentTracking.kt`: calibração conservadora e escolha da fonte de corrente, com testes JVM.
- `app/src/main/java/br/com/pulsobateria/data`: entidades, DAO Room e DataStore.
- `core/src/main/kotlin`: cálculos independentes de Android.
- `core/src/test/kotlin`: testes JUnit de conversões, taxas, estimativas e fronteiras de sessão.
- `app/schemas`: SQL versionado da migração 1→2; o JSON oficial de schema deve ser gerado pelo compilador Room no CI.
- `.github/workflows/android.yml`: validação e publicação de APK direto.
- `docs/MEASUREMENTS.md`, `docs/VALIDATION.md`, `docs/VERSIONING.md`: critérios, validação e versionamento.

## Uso e limitações

Abra o app para iniciar a observação. Sem serviço, a coleta para quando a Activity deixa de estar visível. Pausas de até 90 segundos podem retomar a mesma sessão somente se estado e porcentagem não mudaram, no mesmo processo. O tempo parado não entra na duração, e a curva/taxa recente reinicia para não ligar amostras através da pausa. Ative a notificação nas configurações para observar em segundo plano. O serviço é iniciado apenas por ação explícita com o app aberto; não há boot receiver, reinício automático nem wake lock. Se o sistema encerrar o processo ou o aparelho reiniciar, reative a opção. A preferência é preservada, mas a interface distingue preferência anterior de serviço realmente ativo.

O Android e o fabricante podem adiar leituras ou encerrar serviços. Não se promete notificação impossível de dispensar. Negar notificações mantém o modo de observação com o app aberto. `specialUse` é declarado com justificativa específica para a função opcional de telemetria; eventual publicação no Google Play depende da revisão das declarações de serviço pela loja.

Medições variam por aparelho. Corrente ausente ou contraditória é indisponível; não há estimativa de capacidade em mAh. O gráfico não inventa pontos em lacunas. Consulte as limitações no próprio aplicativo e o relatório de validação antes de tratar esta Alpha como pronta para produção.
