# Changelog

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
