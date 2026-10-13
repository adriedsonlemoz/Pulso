# Pulso Bateria 0.1.0-alpha.14 • versionCode 14

- Corrigidos dois testes antigos: `netDischargeIsNotFlippedByChargingStatusAlone` (falha observada em `:core:test` no workflow 13) e `netCurrentUsesItsOwnSign` (incompatibilidade identificada ao executar os testes locais do app).
- O teste agora distingue o sinal real recebido (`rawNow`/`nowMa`) do filtro de apresentação (`ma`/`source`): a leitura negativa durante status de carga não é invertida, mas permanece só no diagnóstico até haver confirmação compatível.
- Algoritmos de corrente, banco Room v3, backup JSON v1, tela, inicialização, assinatura e Secrets permanecem idênticos à alpha.13.
- Testes locais: 138 Kotlin/JVM aprovados em executor compatível com JUnit 4; 8 testes SQLite aprovados. O APK Debug e a execução Gradle/JUnit real permanecem pendentes no GitHub Actions.

APK esperado no workflow: `Pulso-Bateria-v0.1.0-alpha.14-debug.apk`.
