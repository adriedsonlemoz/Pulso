# Validação — Pulso Bateria 0.1.0-alpha.5 (5)

## Verificações executadas

- Origem do problema confirmada na Alpha 4: `BatteryRepository.sample()` aceitava `computeChargeTimeRemaining()` em qualquer valor de 1 a 604.800.000 ms, dando prioridade ao sistema mesmo antes de 5 minutos de observação.
- Alteração: `BatteryMath.validatedSystemChargeTimeMs()` exige uma taxa local válida, verifica limites de tempo, velocidade implícita e consistência com a extrapolação local; a tela e a notificação usam exclusivamente o ETA validado ou o ETA local.
- **51/51 testes do módulo `core` aprovados** com `kotlinc` 1.9.0 (JVM) e executor local que fornece asserções compatíveis com JUnit. São os 43 testes da Alpha 4 mais 8 testes de validação de ETA. **Não é execução real do JUnit via Gradle.** Log: `docs/test-results/alpha.5-local-tests.txt`.
- `python tools/validate_source.py` passou: 14 arquivos obrigatórios, 7 XMLs, permissões, versões e hashes/CRC do Gradle Wrapper.
- PSI de sintaxe Kotlin nos quatro arquivos alterados passou sem erro. Isso não comprova resolução de APIs Android/Compose.
- Tentativa de `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --stacktrace` falhou **antes do build**, por `UnknownHostException: services.gradle.org` ao baixar Gradle 8.11.1. Não foi gerado APK no ambiente. A compilação Android, Android Lint e teste em dispositivo continuam pendentes.

## Verificações ainda necessárias em aparelho

1. Com 58% e carregador conectado, se Android informar 2 min antes de 5 min e 2 p.p. observados, ver “Calculando…” e texto de amostragem. Nunca exibir o valor do Android diretamente nessas condições.
2. Após variação observada suficiente, verificar ETA local com fonte identificada; valores do Android discrepantes devem ser ignorados, sem oscilações absurdas. Acompanhar aproximação até 100% e desaceleração.
3. Testar aparelho sem leitura de `BATTERY_PROPERTY_CURRENT_NOW`: o ETA por porcentagem deve funcionar mesmo com corrente indisponível. Conferir notificação e interface iguais.
4. Regressão: estado descarregando/cheia/pausada, sessões Room sem perda, gráfico, fundo/notificação, três cartões e tela estreita.
5. Confirmar publicação `Pulso-Bateria-v0.1.0-alpha.5-debug.apk` no workflow e assinatura no Android.
