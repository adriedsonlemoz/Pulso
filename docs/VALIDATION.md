# Validação — Pulso Bateria 0.1.0-alpha.14 (14)

## Falha recebida do workflow 13

ZIP: `Pulso-Android-Pulso-Bateria-13-logs.zip`, arquivo `build/10_Testes, lint e APK Debug.txt`.
O Gradle chegou à tarefa `:core:test`, executou **114 testes, um falhou**:
`AuditRegressionTest > netDischargeIsNotFlippedByChargingStatusAlone`, `AuditRegressionTest.kt:14`.
O build foi interrompido em `:core:test`; não existem evidências nesse workflow de sucesso nas tarefas de lint e APK.

## Correção aplicada

O teste antigo esperava corrente negativa em `CurrentResult.ma` mesmo com `Mode.CHARGING`.
A alpha.13 introduziu filtro de apresentação que retém o sinal no diagnóstico mas não exibe
essa medição como carregamento confiável. O teste agora valida `rawNow`, `nowMa`,
`ma == null`, `source == NONE`, explicação de sinal e ausência de calibração indevida.
O teste `NotificationTextTest.netCurrentUsesItsOwnSign` também foi atualizado para exigir que um valor +400 mA durante descarga não apareça no título nem no corpo; a taxa incoerente também deve ser omitida. Sem alteração no algoritmo de sensores nem no schema Room.

## Verificações executadas nesta entrega

- **138 testes Kotlin/JVM aprovados, 0 falhas**: compilação com `kotlinc` local 1.9.0 e um executor de asserções/`@Test` compatível com JUnit 4 criado apenas no ambiente de validação (não incluído no ZIP). Executados testes de `core` e `NotificationText`/`Format`, inclusive as duas regressões corrigidas. Isso **não equivale à execução do Gradle/JUnit real nem compila o app Android/Compose**.
- **8 testes SQLite aprovados**, incluindo migrações históricas, rollback e retenção.
- `python3 tools/validate_source.py`: aprovado (14 arquivos estruturais, sete XMLs, permissões, versão e integridade do Wrapper).
- Tentativa `./gradlew --offline :core:test :app:assembleDebug` falhou **antes de iniciar o Gradle**, com `UnknownHostException: services.gradle.org`, pois a distribuição Gradle 8.11.1 não está disponível neste ambiente.
- Testes reproduzidos e documentados em `docs/test-results/alpha.14-local-tests.txt`.

## Pendências

- Compilar, executar `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` no GitHub Actions.
- Testar a atualização do APK usando os mesmos quatro Secrets de assinatura da alpha.8 ou posterior.
- Confirmar a apresentação de correntes contraditórias em aparelho e a preservação do histórico.
