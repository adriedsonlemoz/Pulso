# Validação — Pulso Bateria 0.1.0-alpha.19 (19)

## Escopo

Duas melhorias: análise noturna com eventos de tela e leitura histórica local, e comparador de duas recargas com faixa percentual realmente comum, nome de fonte opcional e métrica mAh quando disponível. Room v5, migração 4→5, backup JSON v3 preservam versões anteriores. Nenhuma permissão de internet ou wake lock nova.

## Verificações locais

- `python3 tools/test_storage_sql.py`: 12 testes SQLite, incluindo 4→5 e cadeia 1→5; não substituem Room/KSP em dispositivo.
- `python3 tools/validate_source.py`: checagem de versionamento, permissões, Wrapper e arquivos.
- **9 testes novos de Kotlin/JVM aprovados**, compilando `NightAndChargingAnalysis.kt`, `TelemetryAnalysis.kt` e `UsageAnalysisMath.kt` com Kotlin 1.9 e um executor mínimo compatível com as asserções JUnit. Cobrem limites noturnos, lacunas, faixa comum, contador regressivo e ausência de dados. Não equivale à suíte Gradle com Kotlin 2.1.
- A suíte Kotlin/JVM completa excedeu o tempo de execução local e não foi declarada aprovada. A compilação Gradle foi tentada, mas bloqueada por `UnknownHostException: services.gradle.org` ao baixar Gradle 8.11.1, antes de compilar fontes Android.
- Verificação CRC e listagem do ZIP de fontes, sem keystore nem Secrets.

## Pendências

- Executar `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` no GitHub Actions.
- Testar em dispositivo real migração 1→5/2→5/3→5/4→5 e schema JSON gerado pelo KSP.
- Verificar a consulta noturna no fuso local e diferentes estados de tela, a identificação de fontes, a persistência do nome manual e a comparação de recargas com amostras reais.
- Validar exportação v3/restauração v1/v2/v3 e manter backup da instalação atual antes de atualizar.
