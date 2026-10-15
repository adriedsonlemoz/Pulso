# Changelog

## 0.1.0-alpha.24 (24) — 2026-10-15

### Labs: menu de experimentos e teste de consumo
- A aba Labs abre com menu Teste Solar / Teste de Consumo, prévia dos últimos cinco testes e botão Ver todos para histórico completo.
- Teste de Consumo exige bateria sem fonte conectada para iniciar; acompanha perda de pontos percentuais em 30/60/120/240 min, tempo personalizado ou manual. No prazo, salva relatório e notifica (quando o Android permite).
- Conexão externa observada no teste é registrada como interferência e não gera número enganoso de consumo puro. Lacunas permanecem desconhecidas e leituras finais desatualizadas recebem aviso.
- Teste Solar e leituras anteriores preservados. Room v7, migração 6→7 com tipo do experimento e marcador de fonte externa; backup JSON v5 compatível com v1–v4.
- Sem permissão nova, segundo serviço, gravação, GPS ou internet. Revisar build Android e temporização em dispositivo real.

## 0.1.0-alpha.23 (23) — 2026-10-15

### Labs: testes solares programados
- Nova aba Labs nativa Compose, clima escolhido manualmente, painel e duração (30, 60, 120, 240 min, manual ou personalizada).
- Autoencerramento, notificação e relatório persistido com tempos observados carregando/sem carregar, lacunas e corrente média válida (não a saída do painel).
- Room v6, migração 5→6, tabela lab_tests isolada; backup JSON v4 inclui Labs finalizados e mantém importação v1/v2/v3.
- Sem novos serviços em primeiro plano, wake locks ou permissão de internet. Encerramento pode atrasar sob restrições do Android; lacunas não são fabricadas.


## 0.1.0-alpha.22 (22) — 2026-10-13

### Notificação mais enxuta e estados claros
- Notificação compacta mostra nível, temperatura e uma corrente aproveitável; expandida mostra estado/previsão e fonte ou média independente quando relevante, sem duplicar corrente exibida no título.
- Corrente zero durante carga/descarga não vira afirmação de ausência de consumo; contador/média não nulos podem assumir a apresentação, identificados como média. Média Android 0 mA é omitida da notificação, mas não do diagnóstico.
- Estado `CHARGING` em 100% não é rotulado como bateria cheia; apenas `FULL` recebe esse estado. ETA não é inventado perto de 100%, respeitando os cálculos existentes.
- Temperatura acima do limite configurado recebe aviso passivo na notificação de monitoramento, sem mudar canal, som ou regras de alertas opt-in. Abrir antecede Parar nas ações.
- Room v5, backup JSON v3, medições, coleta, permissão e assinatura preservados. Novos testes de renderização e segurança do texto.

## 0.1.0-alpha.21 (21) — 2026-10-13

### Navegação e hierarquia visual
- Página inicial simplificada: resumo 5/20/60 min e atalho para análise completa; textos extensos deslocados para ícones de ajuda. Gráfico de percentual recente mais compacto.
- Histórico sem comparador extenso acima das sessões: atalhos Comparar e Relatórios levam a telas próprias; edição dos nomes dos carregadores aparece sob opção de expansão.
- Aba Uso dividida em Resumo / Aplicativos / Noite, preservando o acesso autorizado e as consultas locais; card resumido de variação e informação de ciclos cumulativos acima de 100%.
- Avisos de dados insuficientes ou cobertura parcial ficam visíveis. Navegação Voltar e barra inferior preservadas.
- Room v5 / JSON v3 / Secrets / permissões / cálculos intactos. Sem migração.

## 0.1.0-alpha.20 (20) — 2026-10-13

- Alertas opcionais de bateria baixa, alta temperatura, meta de recarga e descarga acelerada. Limiares ajustáveis, confirmação por duas amostras, canal de alerta separado, bloqueio persistido por episódio e pausa mínima de uma hora.
- Relatórios por dia e últimos sete dias no Histórico com % de queda/recarga, duração efetivamente monitorada, sessões de carga e temperatura máxima. Despreza lacunas de mais de três minutos e travessias da meia-noite.
- Room v5, JSON v3, identidade Android, assinatura e histórico anteriores preservados, sem migração nova. Alertas não são importados de backup.
- Documentação, testes JVM e versionamento do CI sincronizados; APK Android ainda requer validação.

## 0.1.0-alpha.19 (19) — 2026-10-13

- Análise de consumo noturno no Uso com separação de tela ligada/apagada, janelas reais e cobertura.
- Comparador de duas recargas na mesma faixa medida, identificação opcional da fonte de carga e migração Room 4→5, backup JSON v3 compatível com v1/v2.

## 0.1.0-alpha.18 (18) — 2026-10-13

- Adicionadas médias de corrente de 5/20/60 minutos, carga acumulada em mAh por fonte, cobertura temporal e rótulo de origem. Apenas pares contínuos, sem mudanças de sessão/estado e sem sinal contraditório; estimativas CAPACITY não viram leitura medida.
- Gráfico técnico na tela inicial: corrente (mA), temperatura (°C) ou bateria (%), com janelas de 1 ou 6 horas. Séries sem ligação entre lacunas >180s ou sessões diferentes.
- Room 3→4 com tabela independente `telemetry_samples`, migração explícita, retenção de 1.440 por sessão e poda global aproximada 30 mil, sem modificar registros anteriores.
- Backup JSON v2 para telemetria, importando também o v1 sem inventar pontos. Versões, testes e documentação sincronizados.

## 0.1.0-alpha.17 (17) — 2026-10-13

- Aba **Apps** renomeada para **Uso** com ícone de gráfico de barras; cabeçalho **Uso de aplicativos**.
- Mostra nome e ícone nativos de cada aplicativo quando acessíveis via PackageManager e `<queries>` restrito a `MAIN/LAUNCHER` (sem `QUERY_ALL_PACKAGES`). Fallback neutro para pacotes ocultos; sem duplicar package name nos cartões.
- Diferença de bateria exibida em `%`: `Caiu 20%`, `Carregou +20%`, comparação tela ligada/desligada e ritmo `%/h`, com esclarecimento de que se trata da variação entre percentuais da bateria.
- Cartão **Corrente da bateria** destaca a média observada útil e sua origem, com detalhes das duas fontes sob expansão; zero reportado durante carga/descarga não é apresentado como média confirmada.
- Preservados Room 3, backup JSON v1, assinatura/Secrets, monitoramento e cálculo das medições.


## 0.1.0-alpha.16 (16) — 2026-10-13

- Nova aba **Apps**: ranking por tempo em primeiro plano, gráfico de atividade em blocos de duas horas e análise da última janela de 24h, a partir de `UsageStatsManager.queryEvents` com autorização especial opcional `PACKAGE_USAGE_STATS`.
- Relação com dados reais do Pulso: queda/recarga observadas no histórico Room e classificação conservadora entre tela interativa/não interativa por eventos Android. Intervalos de amostragem superiores a 3 minutos, mudanças de sessão e eventos de tela ambíguos não são atribuídos a períodos fictícios.
- Coleta sob demanda ao abrir/atualizar a aba, sem permissão de Internet, acessibilidade, root, `QUERY_ALL_PACKAGES` ou serviço adicional em segundo plano.
- Informações de aplicativos são **tempo de uso**, não mA/mAh por pacote. Sem estimativa individual sem evidência; períodos sem dados e limitações de eventos são explicitados.
- Mantidos Room v3, backup JSON v1, histórico antigo, assinatura via Secrets, demais telas e comportamento de monitoramento. Testes puros adicionados para eventos, faixas horárias, transições de tela e lacunas.

## 0.1.0-alpha.15 (15) — 2026-10-12

- Tela Sessões: substituídos valores como `−27 p.p.` por `27 pontos consumidos`, `+30 p.p.` por `30 pontos adicionados` e `0 p.p.` por `Sem variação`. Singular, estados divergentes e amostras insuficientes têm textos específicos.
- Cartões reorganizados para mostrar percentuais inicial/final, duração, variação e ritmo médio por hora em pontos percentuais de forma legível; detalhes técnicos continuam expansíveis, com duração observada explícita.
- Indicação **Possível retomada** quando sessões adjacentes no histórico completo (antes de filtros) têm mesmo estado, percentual de fronteira e lacuna de até três minutos. Não inventa leituras, não soma tempos não observados e não combina registros de Room; exclui motivos incompatíveis como mudança de estado/restauração.
- Criado `SessionHistoryText.kt` com testes Kotlin para pluralização, taxa, lacunas, filtragem conceitual por vizinhança e sinais divergentes. Room v3, backup JSON v1, monitoramento e assinatura preservados.


## 0.1.0-alpha.14 (14) — 2026-10-12

- Corrigido o teste de regressão `netDischargeIsNotFlippedByChargingStatusAlone`, único teste que falhou entre 114 no workflow 13, e antecipada a correção do teste de notificação `netCurrentUsesItsOwnSign`, também baseado no comportamento anterior à alpha.13. A expectativa antiga de exibir corrente negativa durante status `CHARGING` conflita com o filtro de apresentação introduzido na alpha.13.
- O teste agora verifica que os valores brutos/normalizados permanecem negativos para diagnóstico, enquanto `ma` não é disponibilizado como corrente confiável, `source=NONE` e nenhuma calibração de sinal é criada apenas pelo status de conexão.
- Verificação local: 138 testes Kotlin/JVM em executor compatível com JUnit e 8 testes SQLite aprovados; Gradle/Android pendente por indisponibilidade de `services.gradle.org`. Nenhuma alteração no algoritmo de corrente, na persistência Room, no backup, nas permissões, na assinatura ou nos Secrets.

## 0.1.0-alpha.13 (13) — 2026-10-12

- Correntes com sinal contrário ao estado de carga/descarga são preservadas no diagnóstico, mas não mostradas como medições confiáveis. Fonte alternativa com direção compatível pode ser usada; sem fonte válida, interface indica sinal em análise, sem inverter artificialmente o valor.
- Notificação e médias exibidas filtram valores contraditórios e identificam a verificação de sinal.
- Configurações de capacidade ganham dez valores predefinidos (2.500–7.000 mAh), opção de digitação e formatação local de milhares, mantendo o armazenamento numérico e o intervalo 500–30.000 mAh.
- Serviço de monitoramento retorna START_STICKY para solicitar recriação após encerramento pelo Android. Nova preferência opt-in Iniciar com o sistema e receptor de BOOT_COMPLETED/MY_PACKAGE_REPLACED; retorno automático condicionado às permissões e políticas Android. O botão Parar desativa a preferência, e há opção Tentar agora quando solicitado mas inativo.
- Room v3, backup JSON v1, histórico, assinatura e Secrets mantidos. Adicionada apenas a permissão normal RECEIVE_BOOT_COMPLETED. Sem Internet ou wake locks.
- Testes específicos de direção/fallback executados localmente; build Android completo e reboot real pendentes.

## 0.1.0-alpha.12 (12)

- Auditoria profunda registrada em `docs/AUDIT-alpha.11-to-alpha.12.md`.
- Corrige descarte de pequenas correntes e zero, inversão por status e comparação indevida entre leitura pontual e média histórica.
- Ajuste OEM da instantânea com integração trapezoidal, três blocos sem sobreposição e evidência do contador; média do hardware permanece na escala oficial.
- Calibração v2 vinculada à instalação/firmware, carregamento real do DataStore, expiração e revalidação.
- Expõe média Android separada, janela do contador, sentinelas e horário no diagnóstico.
- Transação Room para resumo/amostra/retenção; restauração propaga falhas de congelamento; backup valida limites de sessão e contagens de temperatura.
- Interrompe sessões ao regredir o relógio civil; preserva duração monotônica.
- Arredonda horário previsto para cima e usa instante da medição.
- 132 testes Kotlin/JUnit e 8 SQLite aprovados. Oito novos testes falham contra a alpha.11 e passam na alpha.12. Build Android/instrumentação pendentes.
- Sem mudança de Room v3, formato de backup v1, assinatura ou permissões.

## 0.1.0-alpha.11 (11) — 2026-10-12

- Previsão de carga da notificação agora exibe **horário estimado até 100%** em vez de duração, por exemplo `100% previsto: ~14h45`.
- O horário é calculado a partir da previsão de duração **já validada** pela rotina existente e do relógio local. Arredondamento de cinco minutos evita falsa precisão; exibe `amanhã` ou data quando o horário passa para o dia seguinte.
- Se não houver previsão de carga, mantém `Carregando`. A autonomia em descarga continua indicando duração; nenhum cálculo da bateria, da corrente, do banco ou do backup foi alterado.
- Adicionados testes para relógio, mudança de dia, incerteza, ausência de ETA e preservação do formato de descarga.

## 0.1.0-alpha.10 (10) — 2026-10-12

- Notificação com rótulos `Carregando` (corrente positiva) e `Gastando` (corrente negativa), sem confundir fluxo líquido da bateria com corrente do carregador USB.
- O título mantém porcentagem e temperatura; adiciona mA somente se houver valor válido. Leituras de média e estimativa são identificadas no texto expandido.
- A média do contador aparece separadamente apenas quando é válida, independente e perceptivelmente diferente da leitura instantânea. `Média: Indisponível`, `Corrente: Indisponível` e ritmo não calculado deixam de ocupar linhas.
- Em 100%, substitui previsões residuais de chegada a 100% pelo status `Bateria em 100%`.
- Lógica textual isolada em `NotificationText.kt` para testes locais; Room v3, assinatura, backup/restauração e monitoramento mantidos.

## 0.1.0-alpha.9 (9) — 2026-10-12

- Notificação expandida reorganizada para ficar mais limpa e semelhante ao estilo desejado. O título agora mantém **porcentagem, temperatura e corrente/gasto** em uma única linha.
- Corpo da notificação simplificado, sem repetir o título: mostra apenas previsão/autonomia, corrente/média/estimativa e ritmo em `%/h`.
- Corrente formatada com melhor legibilidade, usando separador de milhar e sinais `+` e `−`, por exemplo `+1.018 mA` e `−450 mA`.
- Mantidos a ação `Parar`, o foreground service, o banco Room, o backup/restauração e a persistência da alpha.8, sem mudanças estruturais nesses componentes.

## 0.1.0-alpha.8 (8) — 2026-10-12

- Corrigido o fluxo de distribuição após o workflow 8: testes, lint e APK Debug passaram; o erro foi HTTP 500 em `gh release create`. O APK agora é salvo como Artifact antes de publicar a Release, e a publicação é tentada novamente até quatro vezes.
- Assinatura estável opcional para variantes Debug/Release por quatro Secrets GitHub Manager; sem Secrets completos, alerta explícito para assinatura temporária.
- Backup JSON pelo Storage Access Framework, SHA-256 de integridade, prévia e confirmação antes de substituir histórico; transação Room na restauração.
- Room 2→3 preserva sessões e pontos e acrescenta corrente, temperatura e origem nas próximas amostras. Histórico mostra último valor disponível em detalhes.
- Calibração confirmada armazenada localmente com validade de sete dias, rejeição de leitura incompatível e comando Recalibrar.
- Adicionados testes instrumentados de migração 1→3 e 2→3 (necessário executar em dispositivo/emulador); testes locais core 75/75 aprovados.
- Código não inclui nem registra chaves privadas; lembre-se de que nova assinatura não atualiza APKs antigos sem a chave original.

## 0.1.0-alpha.7 (7) — 2026-10-12

- Corrente mais útil: consulta `BATTERY_PROPERTY_CURRENT_NOW` e `CURRENT_AVERAGE` (µA declarados), usa fallback por diferença temporal no `CHARGE_COUNTER` (µAh) e, opcionalmente, estimativa por %/h e capacidade útil fornecida pelo usuário.
- Calibração automática e separada dos dois sensores: inversão de polaridade só após quatro amostras consistentes em pelo menos 60 s; escala mA incorreta do fabricante só é aceita após comparação com contador independente em pelo menos três amostras e 60 s. A calibração fica em memória durante a execução.
- Leitura zero, unidades ambíguas, sensores inválidos, lacunas, direções contraditórias e dados ausentes não geram corrente falsa; sinal positivo sempre representa entrada líquida na bateria e negativo, saída.
- Cartão informa mA instantâneos, médios ou estimados; notificações identificam a natureza da média/estimativa. Configurações incluem capacidade opcional e diagnóstico expansível dos valores brutos e da origem.
- Banco Room, sessões e permissões mantidos. Testes locais adicionais para calibração, prioridades de fonte e rejeição de estimativas não fundamentadas.

## 0.1.0-alpha.6 (6) — 2026-10-12

- Histórico reorganizado: cabeçalho compacto com ajuda, filtros horizontais, sessões agrupadas por data e cartões responsivos expansíveis.
- Estados Em andamento, Pausada ou Finalizada; resumo de percentuais, delta, tempo observado, horário e média. Detalhes mostram datas completas, temperatura média, número de amostras e motivo do encerramento.
- Persistência real de amostras de sessão em Room 2: nova tabela `session_samples` com chave estrangeira e exclusão em cascata; migração 1→2 preserva as sessões existentes. Sessões antigas continuam sem curva pois não havia pontos salvos.
- Novos pontos: primeira leitura, mudanças de porcentagem ou a cada 60 s com porcentagem estável; limite de 480 pontos por sessão, preservando o primeiro e os 479 mais recentes. Gráfico ajusta escala e não liga lacunas acima de 3 min.
- Testes do núcleo para a política de armazenamento e desenho sem conectar lacunas; permissões, identificador e medição original preservados.


## 0.1.0-alpha.5 (5) — 2026-10-12

- Corrige previsão até 100% que aceitava qualquer valor positivo retornado pelo Android, incluindo 2 min com bateria em 58%.
- `BatteryMath.validatedSystemChargeTimeMs`: aceita previsão do fabricante só com taxa observada válida e após conferir plausibilidade da velocidade implícita e compatibilidade com o histórico recente (0,5–4× do ETA local).
- Exibe “Calculando…” e os requisitos de amostragem enquanto não há dados suficientes; quando o Android devolve estimativa inconsistente, utiliza a extrapolação dos percentuais realmente observados, se disponível.
- Exibe a origem da estimativa validada; a notificação usa o mesmo valor filtrado pelo repositório.
- 8 novos testes Kotlin para valores implausíveis, sem histórico, válidos, discrepantes, perto de 100% e fallback local.
- Sem mudanças no banco Room, permissões, periodicidade de coleta ou telas de configuração.

## 0.1.0-alpha.4 (4) — 2026-10-12

- Tela inicial mais compacta: corrente/consumo, temperatura e taxa de carga/descarga na mesma linha em larguras compatíveis; layout vertical quando a tela é estreita ou a fonte é ampliada.
- Cartões de medição podem ser tocados para consultar sinal inconsistente, ausência de sensor, fonte do dado e requisitos de estimativa; sem inventar valores.
- Indicador circular reduzido e temperatura concentrada nos cartões; cartão de autonomia/previsão com espaçamento menor.
- Gráfico recente com escala percentual adaptada aos dados reais, limites de 0 a 100%, marcadores de escala e tempo observado; não interpola medições ausentes.
- Aparência em seletor horizontal Sistema/AMOLED/Claro. Intervalo (15/30/60 s) e temperatura (°C/°F) organizados lado a lado quando há espaço.
- Sem alterações no armazenamento Room, medições do BatteryManager ou política de monitoramento contínuo.


## 0.1.0-alpha.3 (3) — 2026-10-12

- Telas automáticas Recarga e Autonomia com apresentação própria e componentes compartilhados.
- Porcentagem e temperatura em destaque; notificação compacta com ambas e estimativa na segunda linha.
- Retomada da mesma sessão após pausa de até 90 s, somente sem mudança de estado/percentual; duração exclui pausa e estimativas recentes reiniciam.
- Histórico identifica sessão e pausa; registros antigos preservados.
- Workflow publica apenas APK direto em Releases, sem artifacts ZIP nem relatórios extras.


## 0.1.0-alpha.2 (2) — 2026-10-12

- Corrige falha `Failed to find package tools` nos builds #1 e #2: `android-actions/setup-android@v3` agora recebe explicitamente `packages: platform-tools`.
- Mantém instalação de SDK 35 e Build Tools 35.0.0 na etapa seguinte.
- Sincroniza versão, documentação e nome do artefato Debug.
- Workflow YAML, versões, Wrapper e ZIP verificados; execução remota e APK ainda pendentes.

## 0.1.0-alpha.1 (1) — 2026-10-12

Primeira Alpha: monitoramento offline real via Android, interface Compose/Material 3, três temas, indicador circular, sessões Room, preferências DataStore, gráfico recente, notificação opcional e workflow de APK Debug. Cálculos isolados em módulo Kotlin/JVM com testes. Corrente não suportada ou inconsistente nunca é simulada.
