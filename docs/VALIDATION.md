# Validação — Pulso Bateria 0.1.0-alpha.28 / versionCode 28

## Autonomia histórica e horário previsto

- Novo `core/DischargeForecast.kt`: usa sessões `DISCHARGING` válidas com pelo menos 3 amostras, 5 min observados, queda de pelo menos 2 pontos e ritmo compatível. A média histórica exige no total 30 min e 4 pontos de queda; pondera o ritmo pelo **tempo observado**. Limita a até 500 sessões retidas no Room; ignora carga, níveis inválidos, regressões e durações absurdas. Não reconstitui lacunas.
- Janela recente: no máximo 8 descargas válidas encerradas nos últimos 7 dias, exigindo juntas pelo menos 15 min e 4 pontos. Previsão: 70% de peso para taxa local quando disponível, 30% para histórico recente ou geral. Somente histórico é usado quando a taxa local ainda não foi estabelecida.
- A previsão na Home aparece em horário civil aproximado, arredondada **para cima** em blocos de 5 min, com previsão para amanhã/data quando necessário. O tempo restante e a média de bateria cheia são apresentados separadamente.
- Leituras com mais de 3 min não dão ETA. Sem histórico confiável, a taxa local validada ainda pode servir à previsão; se ambos faltam, a UI indica dados insuficientes. Carga mantém o cálculo preexistente para 100%.
- Room v8 e backup JSON v6 preservados. Nenhuma migração, permissão, serviço ou Secret criado.

## Testes executados nesta entrega

- `python tools/validate_source.py`: **PASS**, incluindo versão 28, 14 arquivos essenciais, 7 XML, hash/CRC oficiais do Gradle Wrapper.
- `python tools/test_navigation_contract.py`: **11/11 PASS** (contratos estáticos).
- `python tools/test_settings_ui.py`: **21/21 PASS** (contratos estáticos).
- `python tools/test_storage_sql.py`: **15/15 PASS** (SQLite, preservação das migrações).
- `python tools/test_labs_history.py`: **14 verificações estáticas e 5 casos SQL PASS**.
- Compilação de todos os fontes do módulo `core` com `kotlinc` 1.9.0: **PASS**.
- `DischargeForecastTest.kt`: **6/6 PASS**, compilados com `kotlinc` e executados em JVM usando um substituto mínimo de asserts JUnit somente neste ambiente, por indisponibilidade do Gradle. Esse método verifica os seis testes, mas não substitui a execução real do framework JUnit no CI.
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon`: **NÃO EXECUTOU**, porque a distribuição Gradle 8.11.1 não está em cache e o wrapper não acessou `services.gradle.org` (`UnknownHostException`).

## Pendências

- CI: compilar Android, Kotlin/Compose e KSP, rodar JUnit Gradle, lint e testar no aparelho com tema claro/escuro, relógio cruzando a meia-noite, sessões antigas e recentes e comportamento após pausa/retomada de monitoramento.
- Verificar o JSON de schema Room 8 pelo KSP no GitHub Actions, caso ainda não versionado; a alpha.28 não o altera.
- O ZIP é de código-fonte: **nenhum APK Android foi gerado ou instalado neste ambiente**.

---

# Validação — Pulso Bateria 0.1.0-alpha.28 / versionCode 28

## Alterações verificadas em código-fonte

- Labs reorganiza o relatório expandido seguindo a captura fornecida: duração no cabeçalho, quatro métricas, qualidade/cobertura das observações, temperaturas da bateria e rodapé com **Recolher, Copiar e Excluir**. A exclusão de um resultado concluído continua exigindo confirmação; o DAO nunca exclui o `RUNNING`.
- Uma configuração opcional de tela/câmera/vídeo/Wi-Fi/gravação/observações é informada **manualmente** ao iniciar (não detectada pelo app). Temperaturas inicial, última e máxima são lidas do `BatteryRepository.Reading.temperature`, com valores inválidos descartados; sem amostra válida exibe Indisponível.
- Room v8: migração aditiva 7→8, preservando Labs existentes com temperaturas nulas e condições antigas padrão. Backup local JSON v6 passa a salvar/importar campos novos e continua aceitando backups anteriores. O schema JSON **real** do Room 8 depende de execução do KSP no CI; não foi fabricado manualmente.
- Nenhuma permissão nova, biblioteca externa, gravação, internet, wake lock ou serviço extra.

## Execução local

- `python tools/validate_source.py`: **APROVADO**, 14 arquivos básicos, 7 XML e SHA-256/CRC oficial do wrapper.
- `python tools/test_navigation_contract.py`: **11/11 APROVADOS**.
- `python tools/test_settings_ui.py`: **21/21 APROVADOS**.
- `python tools/test_storage_sql.py`: **15/15 APROVADOS**, incluindo migração 7→8 e preservação dos dados anteriores.
- `python tools/test_labs_history.py`: **14 contratos estáticos APROVADOS e 5 casos SQL**, incluindo histórico e exclusão protegida.
- Compilação direta de **todos os arquivos Kotlin do módulo core** pelo `kotlinc` 1.9: **APROVADA**.
- Sete verificações executáveis de `LabReportMath` para cobertura, limites e taxa de descarga: **APROVADAS**.
- Análise sintática com parser PSI de Kotlin: **47 arquivos .kt, 0 erros**. Não comprova resolução de dependências Android/Compose/KSP nem execução no aparelho.
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon`: **NÃO INICIOU**, porque o wrapper tentou baixar a distribuição Gradle 8.11.1 em `services.gradle.org` e falhou com `UnknownHostException`. APK **não produzido**.

## Testes pendentes no aparelho/CI

- Compilar com Gradle e KSP, executar `RoomMigrationTest` a partir de versões 1..7 e registrar o JSON de schema Room 8 gerado oficialmente.
- Verificar visualmente layout da captura com fontes grandes, temas claro/escuro/AMOLED; copiar relatório pelo clipboard, confirmar exclusão, backup/restauração incluindo campos novos.
- Testar captura térmica (início/última/máxima) e lacunas com tela apagada, sem dados do sensor, interrupção/retomada e sob restrições de economia Android.
- Instalar por cima da alpha.26 apenas se o APK tiver o mesmo certificado; **exportar backup** antes de eventual reinstalação.
