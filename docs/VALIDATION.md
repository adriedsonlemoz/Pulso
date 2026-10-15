# Validação — Pulso Bateria 0.1.0-alpha.23 / versionCode 23

## Implementação

- Nova aba Labs Kotlin/Compose Material 3, céu escolhido manualmente, potência do painel e cronômetro (padrão 120 min, alternativas e modo manual).
- Acumulação de períodos entre pares de leituras reais estáveis; status alterado, leitura ausente, intervalo >90 s, inconsistência dos relógios e reinicialização não geram tempo fictício.
- Corrente média em mA ponderada pelo tempo usa somente valores positivos válidos NOW/AVERAGE/COUNTER, não CAPACITY.
- Persistência dos testes em Room v6, migração explícita 5→6, resumo e histórico. Backup JSON v4 incorpora Labs finalizados e aceita v1/v2/v3.
- Reutiliza o monitoramento FGS specialUse existente, sem segundo serviço, permissões novas, wakelocks, rede ou sensores extras. Conclusão notifica o usuário quando o processo está em condições de executar.

## Verificações efetivamente executadas no ambiente

- `python tools/validate_source.py`: **APROVADO** (versão, Manifest, Wrapper oficial SHA-256, identidade, scripts e recursos).
- `python tools/test_navigation_contract.py`: **11/11 APROVADOS** (contratos estáticos das telas anteriores).
- `python tools/test_storage_sql.py`: **13/13 APROVADOS**, incluindo migração SQLite 5→6 e preservação do histórico anterior. O script pré-existente emite ResourceWarning por conexões SQLite em parte dos testes.
- Compilação e execução de um teste Kotlin/JVM real de `LabMath.kt` com `kotlinc`: **7/7 asserções APROVADAS**, incluindo média, lacunas, mudança de estado, estimativa excluída e classificação.
- Revisão sintática com PSI Kotlin do compilador local: **31 fontes Kotlin, zero erros de sintaxe** (não executa resolução de dependências Android).
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon`: **NÃO EXECUTOU O BUILD**; o Gradle Wrapper tentou obter `gradle-8.11.1-bin.zip` de services.gradle.org, mas o ambiente retornou `UnknownHostException` (sem dependências em cache).
- Não houve compilação efetiva Android/Compose/Room KSP, Android Lint nem testes instrumentados no aparelho. A validação SQLite não substitui as migrações instrumentadas.

## Testes pendentes no GitHub Actions/aparelho

1. Executar `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` e `:app:connectedDebugAndroidTest` em emulador/dispositivo; gerar e versionar somente schema Room 6.json real do KSP.
2. Atualizar uma instalação com banco Room v5 e verificar que histórico, telemetria, preferências e assinatura permanecem íntegros.
3. Testar 120 min com tela apagada, serviço autorizado, notificações permitidas, modo economizador Android e encerramento após prazo.
4. Simular interrupções de tela/processo/serviço, desconexão USB, alternância de céu e fonte de corrente OEM contraditória; lacunas devem permanecer não observadas, e notificação pode chegar com atraso.
5. Testar exportação/restauração de backup JSON v4 e importação de v1/v2/v3, incluindo bloqueio durante um teste ativo.
6. Revisar composição Material 3 em telas pequenas e funcionamento da notificação para abrir diretamente Labs.

**Limitação operacional:** o Android pode adiar a execução do timer mesmo com foreground service; esta versão não promete disparo exato ao segundo. Se o processo for encerrado, o prazo será reconciliado na próxima execução sem inventar leituras. O monitoramento contínuo fica habilitado até o usuário desligá-lo nas Configurações, mesmo depois da conclusão de Labs.
