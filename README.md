# Pulso Bateria

**0.1.0-alpha.5 • versionCode 5** — Android nativo, Kotlin e Jetpack Compose.
Android 8.0 (API 26) ou superior. Identidade: `br.com.pulsobateria`.

## Funcionalidades

- Porcentagem e estado via ACTION_BATTERY_CHANGED; temperatura da bateria, corrente instantânea quando suportada e fonte USB/tomada/sem fio.
- Estados carregando, descarregando, cheia, conectado sem carregar e desconhecido.
- Taxa em %/h (pontos percentuais por hora) e estimativas com critérios mínimos. Previsão do Android só é aceita na carga após validação com taxa local observada e limites de coerência; valores suspeitos são descartados.
- Sessões persistentes Room; filtros todas/carga/descarga; até 500 registros, com início, última leitura/término, duração, percentuais e médias.
- Gráfico **ao vivo**, com janela de até 30 minutos de amostras observadas, escala vertical adaptável sem inventar variação. O histórico conserva resumos; as curvas não são persistidas nesta Alpha.
- Temas Sistema, Escuro AMOLED (#000000) e Claro, selecionáveis horizontalmente. Interface em português, ícone e splash próprios.
- Notificação opcional por foreground service, com ação Parar e tratamento de permissão Android 13+.
- DataStore: tema, intervalo 15/30/60 s (padrão 30), unidade °C/°F e preferência de monitoramento.
- Sem INTERNET, anúncios, analytics, conta, root ou coleta externa. Backup do app desativado.

## Estado desta entrega

A Alpha 5 corrige previsões absurdas de tempo de carga: o Android chegou a informar 2 min para ir de 58% a 100% antes de 1 minuto observado. Agora o app exige histórico local suficiente (≥5 min, ≥3 pontos e ≥2 p.p.), filtra previsões incoerentes e recorre à extrapolação da taxa observada quando necessário. Antes disso, exibe “Calculando…” em vez de uma estimativa sem evidências. Mantém os layouts compactos e o gráfico da Alpha 4. **O APK Android, Compose e Lint ainda precisam de validação no GitHub Actions e em aparelho.** Consulte `docs/VALIDATION.md` para os testes realmente executados.

## Gerar APK pelo GitHub Manager

1. Importe o ZIP completo na raiz do repositório, incluindo `.github` e o Wrapper oficial.
2. Envie para main/master ou execute manualmente Android • Pulso Bateria.
3. Após sucesso, abra o link **APK direto** no resumo da execução ou **Releases** no repositório e baixe `Pulso-Bateria-v0.1.0-alpha.5-debug.apk`.

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
- `app/src/main/java/br/com/pulsobateria/data`: entidades, DAO Room e DataStore.
- `core/src/main/kotlin`: cálculos independentes de Android.
- `core/src/test/kotlin`: testes JUnit de conversões, taxas, estimativas e fronteiras de sessão.
- `app/schemas`: schema Room exportado pelo compilador.
- `.github/workflows/android.yml`: validação e publicação de APK direto.
- `docs/MEASUREMENTS.md`, `docs/VALIDATION.md`, `docs/VERSIONING.md`: critérios, validação e versionamento.

## Uso e limitações

Abra o app para iniciar a observação. Sem serviço, a coleta para quando a Activity deixa de estar visível. Pausas de até 90 segundos podem retomar a mesma sessão somente se estado e porcentagem não mudaram, no mesmo processo. O tempo parado não entra na duração, e a curva/taxa recente reinicia para não ligar amostras através da pausa. Ative a notificação nas configurações para observar em segundo plano. O serviço é iniciado apenas por ação explícita com o app aberto; não há boot receiver, reinício automático nem wake lock. Se o sistema encerrar o processo ou o aparelho reiniciar, reative a opção. A preferência é preservada, mas a interface distingue preferência anterior de serviço realmente ativo.

O Android e o fabricante podem adiar leituras ou encerrar serviços. Não se promete notificação impossível de dispensar. Negar notificações mantém o modo de observação com o app aberto. `specialUse` é declarado com justificativa específica para a função opcional de telemetria; eventual publicação no Google Play depende da revisão das declarações de serviço pela loja.

Medições variam por aparelho. Corrente ausente ou contraditória é indisponível; não há estimativa de capacidade em mAh. O gráfico não inventa pontos em lacunas. Consulte as limitações no próprio aplicativo e o relatório de validação antes de tratar esta Alpha como pronta para produção.
