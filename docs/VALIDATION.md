# Validação — Pulso Bateria 0.1.0-alpha.7 (7)

## Executado neste ambiente

- **73/73 testes Kotlin/JVM aprovados**, compilados com `kotlinc` usando executor de asserções compatíveis com JUnit (55 testes antigos + 18 novos). Cobrem polaridade correta/invertida, propriedades ausentes, corrente média, unidade µA/mA, contador em µAh, lacunas/reversões, valor zero, sinais e fallback por capacidade opcional. **Não equivale ao JUnit via Gradle.**
- **12 arquivos Kotlin analisados por PSI sem erros sintáticos**; análise PSI não valida dependências Android, tipos Compose ou KSP.
- `python tools/validate_source.py`: estrutura, XML, permissões, versão e Wrapper SHA-256/CRC (executar antes de fechar entrega).
- ZIP final verificado com CRC e referências da versão (executar antes de fechar entrega).
- Banco Room mantido na versão 2; sem mudança de schema nem novas permissões. A migração 1→2 continua a mesma da Alpha 6.

## Pendências e riscos

1. Compilação real `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` por Gradle, validação de Compose/Android/KSP e geração de APK ainda pendentes caso o Gradle não esteja disponível neste ambiente.
2. Testar em hardware leituras reais `CURRENT_NOW`, `CURRENT_AVERAGE`, `CHARGE_COUNTER` e verificar se a unidade e o sinal inferidos batem com um medidor externo, quando houver. Comparar contra AccuBattery apenas como referência secundária, não como medição laboratorial.
3. Testar calibração com carga e descarga por vários minutos: as propriedades podem ser proprietárias, intermitentes ou incorretas. A polaridade inferida por estado pode falhar perto de 100% e se reinicia ao recriar o processo.
4. Confirmar que informar capacidade útil opcional não transforma taxa %/h em leitura supostamente medida; revisar valores em telas estreitas, fontes ampliadas e notificação.
5. Instalar atualização sobre Alpha 6 e verificar preservação de Room v2, histórico e sessões novas.

## Resultado final desta preparação

- `python tools/validate_source.py`: **PASS** (14 arquivos obrigatórios, 7 XMLs, permissões, metadados e SHA-256/CRC do Gradle Wrapper).
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --stacktrace`: tentativa encerrada antes do build em `UnknownHostException: services.gradle.org` ao baixar Gradle 8.11.1. **Não houve compilação Android ou APK local.**
