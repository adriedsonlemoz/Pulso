# Validação — Pulso Bateria 0.1.0-alpha.6 (6)

## Verificações executadas neste ambiente

- **55/55 testes do módulo `core` aprovados**, compilados com `kotlinc` e executados com um executor JVM e classes de asserção compatíveis com JUnit. Incluem 51 testes já existentes e quatro novos para seleção de amostras, rejeição de relógios inválidos e quebra de linhas em lacunas acima de três minutos. **Não equivale à execução de JUnit via Gradle.**
- **Teste SQLite em memória aprovado**: simulada tabela v1 com registro real, executado o SQL de criação v2 documentado, verificada preservação da sessão antiga, FK de cascata, limite de 480 pontos (primeiro + 479 mais recentes) e remoção dos pontos ao apagar o registro da sessão. Teste do SQL com sqlite3, **não é teste de migração Room gerada/validada no Android**.
- **PSI de sintaxe Kotlin sem erros** nos oito fontes do app e dois arquivos Kotlin do módulo `core`. Isso não verifica tipos, Compose, Android nem KSP.
- `python tools/validate_source.py`: arquivos obrigatórios, 7 XML, permissões, versão e Wrapper SHA-256/CRC aprovados.
- Workflow, nomes de arquivo, README, CHANGELOG, notas de versão, AGENTS e regras de versionamento sincronizados com Alpha 6 / versionCode 6.
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --stacktrace`: tentado, interrompido **antes do build** por `UnknownHostException: services.gradle.org` ao buscar Gradle 8.11.1. Não foi gerado APK neste ambiente.

## Riscos e verificações pendentes

1. Executar build completo no GitHub Actions, incluindo Room/KSP (geração do schema v2), Kotlin/Compose, Lint e APK Debug.
2. Instalar o APK Alpha 6 **sobre a Alpha 5 com sessões existentes** e confirmar a migração Room 1→2 sem apagar o banco ou as preferências. Validar schema gerado pelo Room e versionar o JSON oficial.
3. Observar uma sessão nova com porcentagem estável por pelo menos dois minutos, abri-la, conferir pontos reais e gráfico individual. Alterações de percentual devem criar pontos adicionais, respeitando o limite e lacunas.
4. Na aba Sessões, testar filtros, agrupamento por dias, expandir/recolher, estados ativo/pausado/finalizado e sessões antigas sem curva. Verificar layout com letras grandes, AMOLED e telas estreitas.
5. Encerrar uma sessão e reabrir o app para conferir que os pontos persistiram; remover resumos mais antigos ao superar 500, verificando cascata. Testar carga, descarga e retomada de pausa de até 90 s.
6. Confirmar que a previsão absurda de 2 min em 58% não voltou e que notificação, consumo e monitoramento não tiveram regressão.
7. Confirmar publicação `Pulso-Bateria-v0.1.0-alpha.6-debug.apk` na Release e assinatura no Android.

Nenhuma medição real foi inventada. Gráficos de versões anteriores permanecem indisponíveis por falta de pontos armazenados. Não houve verificação física em Android nesta entrega.
