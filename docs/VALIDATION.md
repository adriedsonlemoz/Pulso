# Validação — Pulso Bateria 0.1.0-alpha.17 (17)

## Escopo

- Nome/ícone de aplicativos via PackageManager com `<queries>` limitado a `MAIN/LAUNCHER`; fallback para pacotes invisíveis.
- Aba **Uso** com ícone próprio, títulos mais legíveis.
- Variação da bateria apresentada em `%`, mantendo diferença de níveis 0–100; testes `SessionHistoryTextTest` atualizados.
- Cartão **Corrente da bateria** com média útil, origem e detalhes expansíveis.
- Room v3, backup JSON v1, assinatura, receitas do GitHub Actions e cálculos de corrente não modificados.

## Testes e limitações

- `python3 tools/validate_source.py`: **PASS**, 14 arquivos obrigatórios, 7 XMLs, versão, permissões e integridade do Wrapper.
- `python3 tools/test_storage_sql.py`: **8 testes SQLite aprovados**, não executam Room em Android.
- Kotlin/JVM compilado usando `kotlinc` 1.9.0 local, com um executor de asserções compatível com JUnit 4 (não Gradle/JUnit oficial): **148 testes aprovados, 0 falhas**, inclusive novos resultados em `%` de `SessionHistoryTextTest`. O teste primeiro falhou por esperar um espaço antes de `%/h`; a expectativa foi corrigida e a suíte reexecutada.
- `./gradlew :core:test :app:assembleDebug --no-daemon`: **não concluiu**, o Gradle Wrapper falhou no download da distribuição por ausência de acesso de rede. Não foi possível compilar Compose/Android, nem executar lint ou validar APK.
- O manifesto inclui `<queries>` restrito a `MAIN/LAUNCHER` e não inclui `QUERY_ALL_PACKAGES`.


## Pendências em aparelho

1. Build completo `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` no GitHub Actions.
2. Conferir nomes e ícones de jogos e apps conhecidos em Android 11+ com permissão de uso, incluindo pacotes ocultos ou apps sem launcher.
3. Visualizar a nova aba e o gráfico de barras em tela pequena/fonte grande; conferir layout dos cards de média e a expansão técnica.
4. Atualização assinada com os mesmos quatro Secrets; verificar persistência do histórico, permissões e backup.
