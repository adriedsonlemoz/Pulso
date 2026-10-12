# Pulso Bateria

**0.1.0-alpha.2 • versionCode 2** — Android nativo, Kotlin e Jetpack Compose.
Android 8.0 (API 26) ou superior. Identidade: `br.com.pulsobateria`.

## Funcionalidades

- Porcentagem e estado via ACTION_BATTERY_CHANGED; temperatura da bateria, corrente instantânea quando suportada e fonte USB/tomada/sem fio.
- Estados carregando, descarregando, cheia, conectado sem carregar e desconhecido.
- Taxa em %/h (pontos percentuais por hora) e estimativas com critérios mínimos. Previsão do sistema tem prioridade na carga.
- Sessões persistentes Room; filtros todas/carga/descarga; até 500 registros, com início, última leitura/término, duração, percentuais e médias.
- Gráfico **ao vivo**, dos últimos 30 minutos observados. O histórico conserva resumos; as curvas não são persistidas nesta Alpha.
- Temas Sistema, Escuro AMOLED (#000000) e Claro. Interface em português, ícone e splash próprios.
- Notificação opcional por foreground service, com ação Parar e tratamento de permissão Android 13+.
- DataStore: tema, intervalo 15/30/60 s (padrão 30), unidade °C/°F e preferência de monitoramento.
- Sem INTERNET, anúncios, analytics, conta, root ou coleta externa. Backup do app desativado.

## Estado desta entrega

Correção do setup Android: pacotes definidos explicitamente como `platform-tools`, evitando a tentativa de instalar `tools` que falhou nos builds #1 e #2. Código-fonte completo; os 31 testes dos cálculos e os recursos AAPT2 foram aprovados na Alpha 1 e não foram reexecutados nesta correção de workflow. **A compilação Android completa e o Android Lint não foram concluídos neste ambiente; não há APK local validado.** O workflow executará essas etapas. Consulte `docs/VALIDATION.md`.

## Gerar APK pelo GitHub Manager

1. Importe o conteúdo deste ZIP em um repositório: `settings.gradle.kts`, `gradlew`, `app/` e `.github/` precisam ficar na raiz. Inclua arquivos ocultos.
2. Envie para `main` ou `master`, ou execute manualmente **Actions → Android • Pulso Bateria → Run workflow** na branch importada.
3. Aguarde testes, lint e compilação. Baixe o artefato **Pulso-Bateria-v0.1.0-alpha.2-debug**.
4. Extraia `app-debug.apk` e instale no Android permitindo instalação por essa fonte, quando solicitado.

Nenhum Secret é necessário. O APK Debug é assinado automaticamente com chave de desenvolvimento: **não é uma Release assinada para distribuição**. Runners novos podem gerar chaves Debug diferentes; nesse caso uma atualização sobre o APK anterior pode exigir desinstalação (perdendo os dados locais). Para distribuição e atualizações duráveis, configure uma chave estável em uma versão futura.
Não foram inventados metadados proprietários do GitHub Manager; a integração usa o repositório e GitHub Actions padrão.

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
- `.github/workflows/android.yml`: validação e artefatos.
- `docs/MEASUREMENTS.md`, `docs/VALIDATION.md`, `docs/VERSIONING.md`: critérios, validação e versionamento.

## Uso e limitações

Abra o app para iniciar a observação. Sem serviço, a coleta para quando a Activity deixa de estar visível. Ative a notificação nas configurações para observar em segundo plano. O serviço é iniciado apenas por ação explícita com o app aberto; não há boot receiver, reinício automático nem wake lock. Se o sistema encerrar o processo ou o aparelho reiniciar, reative a opção. A preferência é preservada, mas a interface distingue preferência anterior de serviço realmente ativo.

O Android e o fabricante podem adiar leituras ou encerrar serviços. Não se promete notificação impossível de dispensar. Negar notificações mantém o modo de observação com o app aberto. `specialUse` é declarado com justificativa específica para a função opcional de telemetria; eventual publicação no Google Play depende da revisão das declarações de serviço pela loja.

Medições variam por aparelho. Corrente ausente ou contraditória é indisponível; não há estimativa de capacidade em mAh. O gráfico não inventa pontos em lacunas. Consulte as limitações no próprio aplicativo e o relatório de validação antes de tratar esta Alpha como pronta para produção.
