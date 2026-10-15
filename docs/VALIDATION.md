# Validação — Pulso Bateria 0.1.0-alpha.27 / versionCode 27

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
