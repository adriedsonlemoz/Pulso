# Validação — Pulso Bateria 0.1.0-alpha.4 (4)

## O que foi executado nesta entrega

- Código-fonte da Alpha 3 mantido; alterações nos composables de painel e configurações e extração da escala visual do gráfico para o módulo `core`.
- **43/43 testes do módulo `core` passaram** por compilação Kotlin/JVM 1.9.0 (JVM 17) com um executor local e implementação mínima compatível das asserções JUnit. Incluem os 40 testes preexistentes e três novos testes de escala do gráfico (nível constante, extremos 0/100, níveis inválidos). **Não equivalem à execução do JUnit real pelo Gradle**, que segue pendente. Log: `docs/test-results/alpha.4-local-tests.txt`.
- Análise PSI de sintaxe Kotlin aprovada em `PulsoScreen.kt`, `BatteryMath.kt` e `BatteryMathTest.kt` (sem erros de parsing). Isto não verifica resolução de APIs Compose ou execução no Android.
- `python tools/validate_source.py` aprovado: 14 arquivos obrigatórios, 7 XMLs, permissões, versionName/versionCode e SHA-256/CRC do Wrapper Gradle.
- Tentativa de `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --stacktrace` **não passou da etapa de download do Wrapper**, por `UnknownHostException: services.gradle.org`. Não existe APK criado ou compilação Android validada localmente.
- Verificação de integridade do ZIP de distribuição, versões e exclusão de caches/keystores realizada após empacotamento.

## Verificações pendentes no GitHub Actions e em dispositivo

1. Rodar Gradle, testes unitários reais, Android Lint e `:app:assembleDebug` no GitHub Actions.
2. Inspecionar no aparelho a tela inicial com largura usual, fontes normal/ampliada e modo de janela estreita: três cartões lado a lado quando houver espaço, sem truncamento de porcentagem, unidade ou título. Toque em cada cartão deve abrir a explicação correta.
3. Conferir seletor Sistema/AMOLED/Claro e controles 15/30/60 s e °C/°F (lado a lado ou empilhados), persistência após reabrir o app e legibilidade de estados selecionados.
4. Conferir curva em 0%, 100%, nível constante (eixo local em vez de 0–100%) e variação de 1–2 p.p.; texto do intervalo observado deve corresponder às amostras reais. Lacunas devem continuar separadas conforme as regras da coleta.
5. Confirmar histórico Room, monitoramento opcional/serviço, troca de estado de carga e notificação inalterados. Testar tema AMOLED em tela real.
6. Conferir o link `Pulso-Bateria-v0.1.0-alpha.4-debug.apk` em Releases e a possibilidade de instalação/atualização de builds assinados.

Nenhum schema Room foi alterado, nenhuma permissão adicionada e não foram realizados testes físicos nesta entrega.
