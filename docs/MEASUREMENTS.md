# Medições e decisões técnicas — alpha.17

A alpha.13 usa um **filtro de apresentação** sobre as fontes de corrente: quando o estado é carregando, corrente negativa não é considerada confiável; quando descarregando, corrente positiva também não. As leituras originais continuam disponíveis para diagnóstico. Um zero isolado não é preferido a outra fonte compatível não nula; se coexistir com uma leitura contraditória, não é apresentado como confirmação de corrente zero. Não se altera o sinal sem corroboração; um estado transitório pode tornar o valor temporariamente indisponível. A preferência Iniciar com o sistema é opcional; tentativas de serviço após boot/atualização podem ser bloqueadas pelo Android, e lacunas não são preenchidas.


## Novo módulo Uso (alpha.16/17)

`PACKAGE_USAGE_STATS` precisa ser autorizado manualmente em Acesso ao uso. O app consulta eventos de foreground/background e de tela interativa/não interativa por `UsageStatsManager.queryEvents` apenas quando a aba é aberta ou atualizada, sem capturar conteúdo dos aplicativos. O Android pode restringir o histórico de eventos, e uma permissão concedida não garante dados completos.

A alpha.17 acrescenta nomes/ícones reais com consulta limitada de apps iniciáveis no Manifest. Quedas e recargas recebem o sufixo `%` na UI, significando diferença entre percentuais inicial e final (por exemplo, 80% → 60%: "Caiu 20%"). Isso é uma **convenção de apresentação**, não alteração do cálculo; a distinção matemática permanece documentada.

Tempo por aplicativo vem de trechos identificados por eventos de primeiro plano; isso **não** representa corrente, watts nem mAh individual. Gráfico de uso agrega o tempo em blocos de duas horas. A correlação de bateria usa amostras `session_samples`, somente pares consecutivos da **mesma sessão**, no período consultado, com intervalo `1..180 s` e níveis válidos. Assim evitamos interpolar lacunas. Para classificar queda com tela ligada/desligada, o estado do sistema precisa ser conhecido no início e não pode mudar no meio do intervalo; caso contrário, o trecho fica como não classificado. Não é possível atribuir queda de bateria ao aplicativo em primeiro plano como causalidade, nem estimar valores individuais sem modelo adicional validado.

## Cartão de corrente média

A interface **Corrente da bateria** prioriza a média derivada do contador, quando compatível com o estado, e recorre à média do Android apenas quando aquela falta. Valor zero do hardware não é tratado como média útil durante carga/descarga sem outro contexto; continua no diagnóstico. As fontes e suas incertezas podem ser consultadas em **Detalhes da medição**. Nenhum algoritmo elétrico foi alterado.

## Contratos e fontes

ACTION_BATTERY_CHANGED fornece nível/escala, status, conexão e temperatura. Porcentagem: arredondamento de 100×level/scale; temperatura: décimos de °C÷10. Nulos/sentinelas não viram valores fabricados. Limite defensivo de temperatura: −40 a 100 °C.

CURRENT_NOW e CURRENT_AVERAGE declaram **µA**; converter por 1.000 inclusive valores pequenos. Positivo indica entrada líquida na bateria, negativo saída. Isso não é corrente do cabo USB. Zero é preservado como leitura informada, sem garantia de resolução/exatidão. Int.MIN_VALUE é ausência/erro neste targetSdk 35; Int.MAX_VALUE e |bruto|>30.000.000 são rejeitados defensivamente. Os brutos, inclusive sentinelas, continuam no diagnóstico.

FULL/IDLE também podem mostrar corrente informada. Sinal divergente do status não é invertido por votação; o diagnóstico pede investigação. Não se transforma divergência em certeza física.

Ordem de apresentação principal: instantânea disponível → média Android → média do contador → estimativa %/h com capacidade explícita → indisponível. É uma política de origem/atualidade, não uma classificação universal de precisão. Média Android e contador permanecem visíveis separadamente mesmo quando os valores se aproximam.

CURRENT_AVERAGE é média de hardware com intervalo desconhecido. Não é média de 5/20 min. A alpha.12 não corrige sua escala/sinal automaticamente porque não conhece a janela de integração. Uma possível violação OEM dessa propriedade precisa de diagnóstico específico.

## Contador e ajuste OEM da instantânea

CHARGE_COUNTER é carga restante em µAh. A média é `ΔµAh×3.600/Δt_ms` mA. Critérios locais: pelo menos três pontos e 90 s, janela até 600 s, |ΔQ|≥1.000 µAh, intervalos crescentes de até 180 s. Contador deve estar em 0..100.000.000 µAh. Zero não é rejeitado isoladamente. Mudança de direção ou salto que implique >30 A reinicia a janela. Não se obriga o sinal ao status. A janela efetiva é mostrada na tela.

Correção de CURRENT_NOW considera somente quatro hipóteses discretas: µA com sinal original/invertido, mA com sinal original/invertido. Não ajusta um ganho livre para coincidir com outro app.

- Integra os brutos por trapézios: soma de `(raw_anterior+raw_atual)/2×Δt_ms`.
- Compara a média dessa integral ao contador exatamente nos mesmos extremos temporais.
- Exige três blocos consecutivos sem duração sobreposta, cada um com ≥90 s e ≥4 amostras. No intervalo padrão de 30 s, o mínimo é 4,5 minutos; com 60 s, é maior.
- Exige sinal estável, magnitude máxima/mínima≤1,25, |ΔQ|≥1.000 µAh e média do contador≥10 mA em módulo.
- Só há candidato quando exatamente uma hipótese produz razão assinada entre 0,75 e 1,25. Isso é uma **heurística explícita de evidência**, não erro máximo de medição nem confiança de 75/100%.
- Um candidato alternativo ainda não confirmado suspende a instantânea; outras fontes mantêm seu rótulo. Sem evidência alternativa, aplica-se o contrato Android.
- Leituras inválidas, lacunas, relógio regressivo, mudanças de estado e saltos reiniciam a evidência. Um ajuste salvo fica suspenso até novo bloco compatível. Contradição estável o revoga; prazo máximo de sete dias é conferido durante a observação.
- Registro v2 inclui escopo SHA-256 de identidade local não incluída em backup + fingerprint do firmware, instante de confirmação e hipótese. Não renovar prazo só porque o registro foi reutilizado. O formato alpha.11 é rejeitado.

CURRENT_NOW e CHARGE_COUNTER podem vir do mesmo medidor: a comparação é por caminhos de cálculo distintos, não por dois instrumentos independentes. Saltos plausíveis ou erros comuns às duas propriedades ainda podem escapar. A calibração não é metrológica.

## Ritmo, autonomia e horário

Janela de até 30 min, baseada em elapsedRealtime. Ritmo = `(p_final−p_inicial)×3.600.000/Δt_ms` em pontos percentuais/h. Exige ≥3 pontos, ≥5 min, ≥2 p.p., sequência crescente de tempo, intervalos≤3 min, variação sem inversão e módulo≤200 p.p./h.

Descarga: `p/|ritmo|` horas. Carga: `(100−p)/ritmo` horas. Limite de sete dias. Independe da corrente em mA. Não assume capacidade nominal.

computeChargeTimeRemaining retorna aproximação em ms. Somente é aceita durante carga, com taxa local suficiente, tempo positivo≤7 dias, taxa implícita≤200 p.p./h e razão para ETA local entre 0,5 e 4. Esses filtros não comprovam precisão. Se rejeitada, usa-se a extrapolação local.

Não há modelo para aquecimento, controle de carga, fase final, envelhecimento ou mudanças de uso. Uma janela longa pode atrasar a percepção da redução de velocidade. Perto de 100%, extrapolação linear é especialmente limitada; “Calculando” deve permanecer sem dados suficientes. A interface explicita a aproximação.

Notificação: instante civil da leitura + ETA; arredondamento **para cima** ao próximo múltiplo de cinco minutos, evitando horários passados. Fuso local, amanhã/data preservados. Em 100%, informa bateria em 100%. Arredondar apresentação não melhora o estimador.

## Linguagem do histórico e continuidade (alpha.15)

Os resumos Room existentes informam percentuais de início/fim, tempo observado e estado. A tela apresenta delta como **pontos adicionados/consumidos** (não mAh) e taxa média como **pontos/h**, calculada da variação líquida pelo tempo efetivamente observado. `p.p.` deixou de aparecer solto no cartão. Se a direção contradiz o estado, apresenta apenas uma variação neutra, sem chamá-la de consumo/carga.

A UI pode exibir **Possível retomada** para duas sessões imediatamente consecutivas no histórico completo, com mesmo modo e mesmo percentual na fronteira, sessão anterior encerrada por motivo compatível e `0..180 s` entre o fim da anterior e o início da seguinte. Isso é um indício heurístico, não prova de continuidade; não une Room, não atribui amostras inexistentes, não calcula uma média combinada e não supõe que o Android trabalhou sem interrupções. A verificação usa a lista completa, anterior à filtragem visual. A fragmentação observada após morte de processo/serviço pode ser legítima: não há informação suficiente para reunificá-la sem riscos.

## Sessões e integridade

Resumo, ponto e retenção são gravados juntos em transação Room. Máximo de 500 sessões e 480 pontos por sessão (primeiro + 479 recentes), primeira leitura, mudanças de nível ou intervalo de 60 s para nível estável. Não há integral de corrente de sessão persistida. O gráfico reduzido não é matéria-prima suficiente para calculá-la.

Duração usa relógio monotônico. Mudança de estado, lacuna>3 min, regressão monotônica, regressão civil ou desvio civil/monotônico>60 s inicia outra sessão. Pausas explícitas de até 90 s podem retomar sessão com estado/percentual iguais; sua duração é excluída. Taxa recente e comparação de corrente reiniciam. A tabela atual não guarda segmentos de pausa; uma linha do gráfico persistido pode atravessar pausa curta. Não usar essa linha para integrar corrente.

Temperatura média de sessão é aritmética por amostras, agora rotulada como tal; eventos extras influenciam o peso. Ritmo médio de sessão é variação percentual líquida/duração observada, não média de corrente. Oscilações de percentual durante uma pausa não podem ser inferidas.

Sem serviço, observar somente com Activity visível. Polling 15/30/60 s e broadcasts, com mínimo de 1 s por estado. Um coletor serializado atende Activity/serviço. Não há wake lock. Registros incompletos são fechados na reabertura, sem fabricar dados de processo morto/reinício.

## Histórico das propostas de telemetria

Antes da alpha.18, médias temporais próprias de 5/20 min e integrais mAh ainda não estavam implementadas. Desde a alpha.18, as novas janelas usam série técnica e cobertura de amostras válidas, conforme explicado abaixo. Ver o projeto matemático e as limitações em `AUDIT-alpha.11-to-alpha.12.md`.

## Referências oficiais

- https://developer.android.com/reference/android/os/BatteryManager
- https://source.android.com/docs/core/power/device
- https://developer.android.com/training/data-storage/room/migrating-db-versions
- https://developer.android.com/topic/libraries/architecture/datastore

## Janelas de telemetria (alpha.18)

A coleta agora salva amostras adicionais em `telemetry_samples` (Room v4), com os relógios do sistema e `elapsedRealtime`, fonte e contador bruto válido. Leituras de eventos recebidas em intervalos <15 s não são persistidas nessa tabela. A poda mantém as 1.440 amostras mais recentes por sessão, com poda global de aproximadamente 30 mil pontos realizada ao iniciar uma nova sessão. A consulta da tela limita-se aos 1.500 pontos recentes; janelas longas e sessões muito antigas podem ter cobertura parcial.

Médias de 5, 20 e 60 minutos só integram **pares completos** registrados na mesma sessão e no mesmo estado, com deltas de tempo civil e monotônico consistentes (≤5 s de diferença), intervalo de 1–180s e sem salto de sessão. Prioridade ao CHARGE_COUNTER em µAh, cuja diferença deve ter o sinal compatível com o estado e média ≤30 A; se não houver cobertura suficiente, considera a integral trapezoidal de CURRENT_NOW/AVERAGE normalizados. Uma única fonte é escolhida para toda a janela, **sem somar fontes diferentes**. CAPACITY (percentual × capacidade configurada) não é integrado como medição.

O mAh é apenas a integral dos intervalos válidos cobertos, não uma extrapolação para o período completo. A média mA divide a carga integrada pelo **tempo válido**. Cobertura elétrica = tempo válido / duração solicitada. Cobertura de monitoramento = intervalos temporais de mesma sessão / duração. Cobertura não corresponde a precisão metrológica.

Gráfico permite selecionar corrente, temperatura e nível (cada um com escala própria) e 1/6h. Não liga intervalos maiores que 180s nem sessões diferentes. Percentuais e temperaturas vêm do sensor; correntes CAPACITY não aparecem como medidas no gráfico de corrente. Gráficos técnicos antes da alpha.18 não existem e não são inventados.

## Consumo noturno e comparação de carregamentos (alpha.19)

A última noite civil local vai das 22h às 7h, ou termina no instante atual quando ainda em andamento. Usa eventos `SCREEN_INTERACTIVE`/`SCREEN_NON_INTERACTIVE` do Android, com autorização explícita de acesso ao uso e amostras da bateria realmente armazenadas. Pares devem pertencer à mesma sessão, separados por ≤3 min. Se o estado da tela mudou entre duas amostras ou é desconhecido, a queda não é atribuída à tela ligada/apagada. O percentual indicado é diferença de nível na escala 0–100. A cobertura é tempo observado/duração da janela, **não confiabilidade metrológica**. Ritmo de descarga noturno só aparece com pelo menos 20 min observados de tela apagada e queda ≥2%.

O comparador exige duas sessões `CHARGING` encerradas, com pontos da telemetria Room v4 e o mesmo par de níveis (uma faixa ≥5%) registrado em ambas. Para evitar interpolação e médias inventadas, cada trecho precisa de pontos consecutivos com intervalos civil/monotônico ≤3 min e coerência ≤5 s, sem redução de percentual durante a faixa. Compara %/h e tempo **somente nessa mesma faixa observada**, nunca duas sessões completas com faixas diferentes. Temperatura é média simples das amostras no trecho; mAh são `ΔCHARGE_COUNTER/1000` somente quando contador válido e média implícita entre 10 e 30.000 mA. Sem dados, informar indisponível. O Android fornece o **tipo de conexão** (Tomada, USB, Sem fio) e não a identidade do carregador. Rótulos são manuais; mudar a fonte USB para Tomada durante carga pode iniciar nova sessão.

## Alertas e relatórios na alpha.20

Alertas são derivados da leitura do Android e exigem duas amostras sequenciais após opção explícita; não funcionam com o monitoramento suspenso. Descarga acelerada é tendência %/h baseada no histórico, não amperagem. O gerenciamento dos episódios persiste localmente, exige pelo menos 1 h entre alertas do mesmo tipo e rearma ao desaparecer a condição. As configurações não são restauradas pelo backup.

O relatório de sete dias lê até 30 mil amostras técnicas Room da semana corrente, limitadas pela retenção global. Soma apenas diferenças de nível em pares válidos da mesma sessão e do mesmo modo, com distância temporal 1–180 s, tempo monotônico coerente e dentro do mesmo dia civil. Intervalos que atravessam meia-noite são ignorados. Uma variação de 80% para 60% aparece como “Caiu 20%”. Soma de quedas entre sessões não equivale a percentual relativo da carga original nem a mAh. Temperatura máxima provém de amostras pareadas válidas. Relatório pode ficar incompleto pelo limite de retenção ou pelo sistema suspender a coleta.

## Apresentação dos dados na alpha.21

As alterações de navegação não mudam sensores, sinais, cálculo da média em mA, mAh, ritmo em %/h ou janela noturna. O resumo de 5/20/60 min pode mostrar **Dados parciais** se a cobertura elétrica for inferior a 80%; isso não é um escore de precisão. O total acumulado de variações por múltiplas sessões pode superar 100%. Explicações técnicas ficam atrás do botão de ajuda; falta de amostras e cobertura parcial permanecem explícitas.

## Notificação compacta e medição zero (alpha.22)

O monitor em primeiro plano exibe nível e temperatura válidos, seguidos de uma **corrente aproveitável e não zero**. Zero lido durante carga/descarga continua disponível no diagnóstico, mas não afirma ausência de consumo. Se a leitura principal for zero/inexistente, uma média não zero e de direção compatível pelo contador pode aparecer no título, identificada no corpo como média. Esta escolha afeta apenas a apresentação, **não** recalcula corrente.

Médias independentes só entram no corpo expandido quando a diferença relativa/absoluta é suficientemente grande (maior que 5% e 20 mA, segundo o limiar implementado). Não se apresenta média Android 0 mA como prova de corrente nula. Fonte instantânea e média não representam a mesma janela; as fontes brutas continuam consultáveis no aplicativo.

A classificação de 100% depende do estado: CHARGING + 100% permanece "Carregando • 100%", FULL resulta em "Bateria cheia". Previsão de conclusão não aparece quando nível alcança 100%. A temperatura igual ou superior ao limite configurável exibe aviso passivo na notificação, independente do recurso opcional de alertas inteligentes. A notificação de monitoramento permanece silenciosa e não altera a taxa de leituras.

## Testes Labs (alpha.23)

A condição do céu é manual, não uma medição meteorológica. Duração é relógio civil entre começo e prazo/fim; não pressupõe amostras em cada segundo. Dois estados Android iguais em leituras adjacentes com intervalo monotônico válido de até 90 segundos e diferença entre relógios de até 10 segundos são contabilizados como carregando ou sem carregar. Mudança de estado, relógio reiniciado e lacunas não são atribuídos a carga; aparecem em `Sem leitura confirmada`. A distinção 'sem carregar' inclui DISCHARGING, IDLE e FULL, não se confunde com wattagem entregue pelo painel. Corrente média em mA usa pares positivos compatíveis das fontes NOW/AVERAGE/COUNTER, ponderados por duração; CAPACITY não é tratada como medição. Isso é uma média das correntes apresentadas, que podem ter janelas distintas, não uma medição calibrada da entrada USB.

O Lab usa o serviço existente para leituras com tela apagada, mantendo Room atualizado nas amostras; o timer no processo tenta terminar ao prazo sem alarme exato ou wakelock. Se o Android suspender/matar o processo, poderá terminar depois ao reabrir, preservando a lacuna e sem assumir dados ausentes. O serviço pode continuar ativo após o teste e pode ser desativado nas Configurações.
