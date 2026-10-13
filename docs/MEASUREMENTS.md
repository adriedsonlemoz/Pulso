# Medições e decisões técnicas — alpha.13

A alpha.13 usa um **filtro de apresentação** sobre as fontes de corrente: quando o estado é carregando, corrente negativa não é considerada confiável; quando descarregando, corrente positiva também não. As leituras originais continuam disponíveis para diagnóstico. Um zero isolado não é preferido a outra fonte compatível não nula; se coexistir com uma leitura contraditória, não é apresentado como confirmação de corrente zero. Não se altera o sinal sem corroboração; um estado transitório pode tornar o valor temporariamente indisponível. A preferência Iniciar com o sistema é opcional; tentativas de serviço após boot/atualização podem ser bloqueadas pelo Android, e lacunas não são preenchidas.


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

## Sessões e integridade

Resumo, ponto e retenção são gravados juntos em transação Room. Máximo de 500 sessões e 480 pontos por sessão (primeiro + 479 recentes), primeira leitura, mudanças de nível ou intervalo de 60 s para nível estável. Não há integral de corrente de sessão persistida. O gráfico reduzido não é matéria-prima suficiente para calculá-la.

Duração usa relógio monotônico. Mudança de estado, lacuna>3 min, regressão monotônica, regressão civil ou desvio civil/monotônico>60 s inicia outra sessão. Pausas explícitas de até 90 s podem retomar sessão com estado/percentual iguais; sua duração é excluída. Taxa recente e comparação de corrente reiniciam. A tabela atual não guarda segmentos de pausa; uma linha do gráfico persistido pode atravessar pausa curta. Não usar essa linha para integrar corrente.

Temperatura média de sessão é aritmética por amostras, agora rotulada como tal; eventos extras influenciam o peso. Ritmo médio de sessão é variação percentual líquida/duração observada, não média de corrente. Oscilações de percentual durante uma pausa não podem ser inferidas.

Sem serviço, observar somente com Activity visível. Polling 15/30/60 s e broadcasts, com mínimo de 1 s por estado. Um coletor serializado atende Activity/serviço. Não há wake lock. Registros incompletos são fechados na reabertura, sem fabricar dados de processo morto/reinício.

## Evoluções propostas, ainda não implementadas

Médias temporais próprias de 5/20 min, integral mAh e média de sessão requerem pontos por coleta ou acumuladores transacionais, identificação de segmentos, cobertura e política de retenção. Ver projeto matemático, casos de teste e diferenças entre observado/estimado em `AUDIT-alpha.11-to-alpha.12.md`.

## Referências oficiais

- https://developer.android.com/reference/android/os/BatteryManager
- https://source.android.com/docs/core/power/device
- https://developer.android.com/training/data-storage/room/migrating-db-versions
- https://developer.android.com/topic/libraries/architecture/datastore
