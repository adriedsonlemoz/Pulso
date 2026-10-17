# Validação — Pulso Bateria 0.1.0-alpha.35 / versionCode 35

## Escopo alpha.35

- API AIDL local (start/stop/status/report) para um cliente pareado. Verificação de UID real na thread Binder, vínculo do pacote ao certificado SHA-256 e toggle desligado por padrão.
- Tela de pareamento/revogação em Configurações; nenhuma permissão ou serviço foreground novo.
- Requer monitoramento já ativo; não encerra testes manuais. Room v8 e backup JSON v6 preservados.
- A autorização externa não é incluída no backup e é revogada ao restaurar.

## Verificações locais (17/10/2026)

- `python3 tools/test_external_labs_contract.py`: **18/18 PASS** (contrato, identidade, revogação, proteção de testes, UI e Docs). São verificações estáticas.
- Kotlin PSI em seis arquivos modificados: **6/6 sem erros sintáticos** (não resolve dependências Android/AIDL).
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon`: **bloqueado antes dos testes** por `java.net.UnknownHostException: services.gradle.org` ao obter Gradle 8.11.1; não houve APK local.
- `python3 tools/validate_source.py`: **PASS** (versão, Manifest, permissões originais e Wrapper oficial).
- `python3 tools/test_storage_sql.py`: **15/15 PASS** em SQLite; o harness emite ResourceWarning de conexões não fechadas.
- `python3 tools/test_home_compact.py`: **16/16 PASS**.
- `python3 tools/test_navigation_contract.py`, `tools/test_labs_history.py`, `tools/test_session_grouping_ui.py`: **PASS** (histórico e Labs preservados).
- `python3 tools/test_settings_ui.py`: **21/21 PASS**.
- `kotlinc` 1.9.0 compilou os arquivos Kotlin puros do módulo `core`: **PASS** (não equivale a compilação do Android).
- Instrumentação AIDL de dois apps assinados, permissões e UI de pareamento: **pendentes** em aparelho/emulador.

---

# Validação — Pulso Bateria 0.1.0-alpha.35 / versionCode 35

## Mudanças

- Barra inferior usa **Início, Sessões, Labs e Config.**. O histórico `History` aparece como aba 1, preservando agrupamento, filtros, gráficos e acessos a comparação/relatórios.
- Atalho da Home seleciona diretamente Sessões; a rota anterior `detailPage="sessions"` foi retirada.
- A aba Uso e seu ícone deixam de ser exibidos, mantendo módulos de leitura no código sem novos acessos de coleta ou permissões.
- Labs mantém toda a compactação e ação fixa da alpha.33; Room v8, backup JSON v6, motor de medição, serviço, Secrets e dados históricos sem modificação.

## Execução local (17/10/2026)

- `python3 tools/validate_source.py`: **PASS**, 14 arquivos essenciais, 7 XML, versão, permissões e SHA-256/CRC do Gradle Wrapper.
- `python3 tools/test_navigation_contract.py`: **19/19 PASS**, inclusive aba Sessões ativa, ausência da aba Uso, atalho da Home, histórico e páginas internas.
- `python3 tools/test_labs_history.py`: **PASS** (contratos de interface, retenção e cinco operações SQL verificadas).
- `python3 tools/test_home_compact.py`: **16/16 PASS**.
- `python3 tools/test_session_grouping_ui.py`: **10/10 PASS**.
- `python3 tools/test_settings_ui.py`: **21/21 PASS**.
- `python3 tools/test_storage_sql.py`: **15/15 PASS** em SQLite; apresentou `ResourceWarning` de conexões não fechadas no harness, mas nenhum teste falhou.
- `kotlinc` 1.9.0 compilou os 13 fontes Kotlin de `core`: **PASS** (não equivale a testes JUnit do Gradle).
- Kotlin PSI de `PulsoScreen.kt` e `LabsScreen.kt`: **2 fontes, zero erros sintáticos**; não resolve APIs Android/Compose.
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon`: **NÃO EXECUTOU** testes/build; o wrapper tentou baixar Gradle 8.11.1, mas `services.gradle.org` retornou `UnknownHostException`. Não foi gerado APK.
- `python3 tools/run_local_jvm_tests.py` depende do argumento obrigatório `--toolchain-dir` e não foi utilizado: não afirmar aprovação desses testes.


## Pendências

- A compilação Android/Compose no Gradle e testes de UI no aparelho dependem de ambiente com as dependências Android/Gradle; não confundir testes estáticos e Kotlin PSI com execução de APK.

---

# Validação — Pulso Bateria 0.1.0-alpha.33 / versionCode 33

## Mudanças

- Barra inferior com quatro abas. Sessões continua como página interna da Home via **Histórico de sessões**, com dados e ferramentas anteriores acessíveis.
- Laboratório com seletor de céu compacto, opções de painel/duração menores, contexto opcional recolhível, botão de início fixo no rodapé e menu com três experimentos recentes mais histórico completo.
- UI exclusivamente: não altera Room v8, backup JSON v6, chave de assinatura, sensores, cálculos ou serviço. Nenhum arquivo de chave/Secret foi incluído.

## Execução local (17/10/2026)

- `python3 tools/validate_source.py`: **PASS**, estrutura, versão, permissões, XML e hash do wrapper.
- `python3 tools/test_navigation_contract.py`: **14/14 PASS** (contratos estáticos da navegação, inclusive quatro abas e acesso ao histórico).
- `python3 tools/test_labs_history.py`: **18 verificações estáticas + 5 casos SQL PASS** (incluindo recolhimento, CTA e persistência do histórico).
- `python3 tools/test_home_compact.py`: **16/16 PASS** (contratos estáticos).
- `python3 tools/test_session_grouping_ui.py`: **10/10 PASS** (contratos estáticos).
- `python3 tools/test_settings_ui.py`: **21/21 PASS** (contratos estáticos).
- `python3 tools/test_storage_sql.py`: **15/15 PASS** (execução SQLite das migrações e regras de retenção).
- `kotlinc` compilou os fontes do módulo `core`: **PASS**; não houve mudança no módulo.
- Kotlin PSI analisou `PulsoScreen.kt` e `LabsScreen.kt`: **0 erros sintáticos**; análise PSI não verifica resolução de API Compose.
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon`: **NÃO FOI POSSÍVEL**; o wrapper tenta baixar Gradle 8.11.1, indisponível por `UnknownHostException: services.gradle.org`. Nenhum APK foi gerado localmente.

## Pendências

- Compilar Android/Compose/KSP e executar testes Gradle e lint no GitHub Actions. Verificar a aparência e o rodapé persistente com fonte grande, tema AMOLED e teclado aberto em tela real.
- Testar início/finalização de ambos experimentos com tela apagada e política de bateria restritiva; confirmar histórico da versão instalada e assinatura estável na atualização. Fazer backup antes de desinstalar se certificados divergirem.

---

# Validação — Pulso Bateria 0.1.0-alpha.32 / versionCode 32

## Implementação — alpha.32 (sessões próximas e marcador Atual)

- `SessionHistoryText.groupConsecutive` é função pura no módulo `core`, computando somente índices do histórico completo em ordem cronológica; usa as condições conservadoras descritas em `docs/MEASUREMENTS.md`.
- `History` agrupa a exibição por padrão com interruptor para desagrupar. O resumo combinado soma durações observadas e preserva os cartões, amostras, gráficos e motivos individuais em `Ver originais`. A fonte de dados Room, regras de retenção, relatórios e previsões não foram alteradas.
- `ATUAL` requer leitura recente, sessão aberta/não pausada e correspondência da leitura com a sessão; não é selo decorativo da última linha.
- Room v8, backup JSON v6, permissões e Secrets inalterados.

## Testes locais — alpha.32

- Núcleo Kotlin compilado com `kotlinc` 1.9.0; `SessionHistoryTextTest` e `SessionHistoryGroupingTest`: **10/10 PASS** (3 existentes + 7 novos). Testes reais executados na JVM com substitutos locais de assert JUnit, **não** com o runner JUnit/Gradle.
- `tools/validate_source.py` **PASS**, `tools/test_storage_sql.py` **15/15 PASS**, `tools/test_navigation_contract.py` **11/11 PASS**, `tools/test_settings_ui.py` **21/21 PASS**, `tools/test_labs_history.py` **PASS**, `tools/test_home_compact.py` **16/16 PASS**, `tools/test_session_grouping_ui.py` **10/10 PASS** (contratos estáticos). Parser Kotlin PSI: **21 fontes Android, 0 erros sintáticos**.
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon` **não começou a compilação**: o wrapper tentou baixar Gradle 8.11.1 de services.gradle.org e terminou com `UnknownHostException`. Compilação Android/Compose/KSP, execução instrumentada e validação visual em dispositivo real seguem pendentes.

---

## Implementação — alpha.31 (Home inferior compacta)

- `CompactCurrentCard` substitui somente o bloco de layout de corrente: a escolha conservadora do contador e da média Android é idêntica; mantém explicação técnica e detalhes expansíveis.
- `Chart` ficou menor, sem mudanças em escala, pontos, curvas ou cálculos da bateria.
- `TelemetrySummaryCard` reorganiza os mesmos filtros, média mA, mAh e cobertura, mantendo o link para `TelemetryPanel`; layout responde a largura e fonte ampliada.
- Versão e workflow foram sincronizados, sem mudar Room v8, backup JSON v6, core, serviços, permissões ou armazenamento.

## Testes locais — alpha.31

- `tools/validate_source.py` e verificações de contratos estáticos — PASS.
- `tools/test_home_compact.py` verifica interações e dados preservados — PASS.
- `tools/test_storage_sql.py` — 15/15 PASS.
- `tools/test_navigation_contract.py`, `tools/test_settings_ui.py`, `tools/test_labs_history.py` — PASS.
- `kotlinc` 1.9.0 compila o módulo `core` — PASS.
- Análise sintática PSI com o compilador Kotlin: **51 arquivos .kt, 0 erros**. Não verifica resolução das bibliotecas Android/Compose.
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon`: **não iniciou o build**, pois o Wrapper tentou buscar `services.gradle.org` e retornou `UnknownHostException`. Build Android, lint e testes em aparelho continuam pendentes.

---

## Implementação — alpha.30 (Home compacta)

- A tela Início (`PulsoScreen.kt`) foi reorganizada para o estilo compacto: cabeçalho em cartão com resumo do estado, chips de contexto e medidor circular menor, ocupando menos altura que o medidor semicircular anterior.
- O cartão de previsão foi redesenhado para destacar o horário previsto e mostrar, lado a lado, o tempo restante e a média histórica (descarga ou recarga), preservando exatamente os mesmos dados e regras calculadas no `core`.
- Nenhuma alteração em Room v8, backup JSON v6, cálculos de autonomia/recarga, permissões, serviço, monitoramento, histórico, Labs ou configurações.

## Testes locais executados — alpha.30

- `python3 tools/validate_source.py`: **PASS**.
- `python3 tools/test_storage_sql.py`: **15/15 PASS**.
- `python3 tools/test_navigation_contract.py`: **11/11 PASS**.
- `python3 tools/test_settings_ui.py`: **21/21 PASS**.
- `python3 tools/test_labs_history.py`: **PASS** em verificações estáticas/SQL.
- `kotlinc` 1.9.0 compilando todos os arquivos `.kt` do módulo `core`: **PASS**.
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon`: **não iniciou o build** por `UnknownHostException` ao tentar resolver a distribuição do Gradle em `services.gradle.org`.

## Pendências — alpha.30

- Validar a renderização da Home compacta em aparelho real com fontes grandes, tema claro/AMOLED, 100%, pouca bateria, carregando, descarregando e estado desconhecido.
- Confirmar compilação Android/Compose/KSP e inspeção visual no GitHub Actions antes de afirmar APK pronto.

---

## Implementação

- Adicionado `core/ChargeForecast.kt`: sessões de carregamento observadas e válidas, tempo médio equivalente a carga completa, seleção opcional de histórico do tipo de fonte, média recente e fase 80–100% somente com observação real. Nenhum intervalo de lacuna foi simulado.
- Home durante `CHARGING` mostra previsão aproximada do relógio de 100%, duração restante e média equivalente 0–100%; mantém cálculo anterior do Android como fallback local quando apropriado. O cartão de descarga de alpha.28 permanece separado e inalterado.
- Estimativa não equivale a medição exata de uma recarga completa. Categoria USB/tomada/sem fio não identifica potência ou carregador; painel solar ligado por USB pode apresentar comportamento distinto. Não prever 100% em estado `FULL`/`IDLE`/`UNKNOWN`.
- Sem mudança no banco Room v8, backup JSON v6, permissões, serviço ou Secrets.

## Testes locais executados

- `kotlinc` 1.9.0 compilando todos os arquivos `.kt` do módulo `core`: **PASS**.
- `ChargeForecastTest.kt` **7/7 PASS**; `DischargeForecastTest.kt` **6/6 PASS**. Executados na JVM com stubs mínimos de `org.junit.Test` e `org.junit.Assert` para permitir rodar as asserções, **não** com JUnit 4/Gradle real.
- `python tools/validate_source.py` **PASS**; `python tools/test_storage_sql.py` **15/15 PASS**; `python tools/test_navigation_contract.py` **11/11 PASS**; `python tools/test_settings_ui.py` **21/21 PASS**; `python tools/test_labs_history.py` **PASS** em verificações e SQL.
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon` **não iniciou o build**: Gradle Wrapper tentou alcançar `services.gradle.org` e retornou `UnknownHostException` porque a distribuição 8.11.1 não está no cache local.

## Pendências

- Compilação Kotlin/Compose/Room/KSP pelo Gradle no GitHub Actions e teste visual/funcional no aparelho (fontes grandes, 100%, variação de tipo de fonte, USB/solar intermitente, sessões antigas e fusos/virada de dia).
- Inspecionar APK gerado e validar atualização assinada sobre a instalação existente; não desinstalar o app sem backup das sessões.

---

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
