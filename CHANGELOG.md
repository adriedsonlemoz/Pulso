# Changelog

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
