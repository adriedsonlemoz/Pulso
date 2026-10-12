# Medições e decisões técnicas

## Dados oficiais

`ACTION_BATTERY_CHANGED`: nível/escala, status, conexão e temperatura; `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW`: µA; `computeChargeTimeRemaining()` em API 28+: ms.

- Porcentagem = arredondamento de `100 × level / scale`. Nível/escala inválidos não viram zero.
- Temperatura = décimos de °C / 10. Ausência é null. Faixa defensiva -40 a 100 °C; fora disso é indisponível, não alerta de segurança.
- Corrente = µA / 1000, exibida arredondada em mA. Int.MIN_VALUE/Int.MAX_VALUE e magnitude acima de 30 A são rejeitados. Zero não é considerado ausência automaticamente.
- Sinal segue o contrato oficial: positivo é entrada líquida na bateria; negativo é saída. Em carga com leitura negativa, ou descarga com leitura positiva, mostramos indisponível e indicamos inconsistência. Não deduzimos o sinal apenas pela presença do cabo e não calibramos automaticamente sensores de fabricantes.
- Cheia e conectado sem carregar são estados separados. A sessão muda quando o status muda; isso evita misturar carga, manutenção em 100% e descarga.

## Taxa e previsão

Janela móvel de até 30 minutos, tempos monotônicos `elapsedRealtime`. Taxa = `(percentual final − inicial) × 3.600.000 / duração em ms`. A unidade %/h indica pontos percentuais por hora, não percentual relativo.

Exigências: ≥3 pontos, ≥5 min, ≥2 p.p., timestamps crescentes, intervalos ≤3 min, direção consistente e |taxa| ≤200 p.p./h. Sem critérios: Calculando. Descarga: `percentual / |taxa|` horas. Carga: `(100 − percentual) / taxa` horas. Estimativas acima de sete dias são rejeitadas. A carga não é linear; a previsão é aproximada e pode mudar. Previsão do Android (`computeChargeTimeRemaining`, API 28+) não é exibida isoladamente: só é considerada quando existe taxa local válida, é positiva e ≤7 dias, implica taxa média de no máximo 200 p.p./h e fica entre 0,5× e 4× da estimativa linear local. Esses limites são **heurísticas conservadoras**, não uma garantia de precisão, e admitem desaceleração perto de 100%. Sem amostras locais suficientes, exibe “Calculando…” mesmo quando o Android retorna um número positivo; se o número do Android é rejeitado e há taxa local válida, usa a extrapolação local. A previsão aceita do Android é identificada. A ausência de sensor de corrente instantânea não impede medir a variação percentual; previsões continuam aproximadas e podem oscilar.

A média de uma sessão é a variação líquida observada dividida pela duração; não é a média das correntes. Exige ≥3 amostras, ≥5 min e ≥2 p.p. Temperatura média é média aritmética das amostras válidas (≥3), não uma média temporal ponderada. Eventos extras podem influenciar a ponderação.

## Apresentação das leituras

Na Alpha 4, o indicador de corrente mostra `—` quando o Android não fornece corrente confiável, e a taxa mostra `—` enquanto não satisfaz os critérios ou quando o estado não comporta taxa. O motivo é mostrado ao tocar no cartão. A unidade %/h permanece em pontos percentuais por hora.

A curva recente usa exclusivamente amostras reais e ajusta a escala vertical para o mínimo e máximo observados com margem de 2 pontos percentuais (limitada a 0–100%). Escala visual não muda as leituras nem produz estimativas: uma curva de 55% constante continua horizontal. O eixo horizontal representa o intervalo real das primeiras às últimas amostras da janela de até 30 minutos, não 30 minutos artificiais.

## Integridade e consumo

Um único coletor atende Activity e serviço; comandos são serializados em coroutine. Banco usa métodos suspend do Room. Observação para quando não há proprietário. Rotação mantém a sessão. Pausas explícitas até 90 s podem retomar o mesmo registro apenas com estado/percentual iguais no mesmo processo; pausas não entram na duração e reiniciam pontos da taxa. Sem retomada, o registro fecha na última amostra. A duração usa relógio monotônico; alteração do relógio civil >60 s, regressão monotônica, status diferente ou lacuna >3 min encerra a sessão na **última leitura válida**, nunca no fim presumido da lacuna. Reabertura após morte do processo fecha registros incompletos como interrompidos. Não se reconstitui o que não foi medido.

Amostras da curva recente ficam em RAM (até 30 minutos). Desde a Alpha 6, a tabela Room `session_samples` guarda pontos de cada sessão: a primeira leitura, mudanças de porcentagem e leituras com percentual estável separadas por pelo menos 60 segundos. Há limite de 480 pontos por sessão (primeiro + até 479 mais recentes). O gráfico de sessão usa horários reais, sem conectar intervalos superiores a 3 minutos; em sessões longas que ultrapassam o limite, trechos antigos intermediários deixam de aparecer, mas o primeiro ponto permanece. A migração explícita do banco 1 para 2 não destrói resumos anteriores, e não tenta reconstruir curvas de sessões da versão 1. Cada exclusão dos 500 resumos mais antigos remove automaticamente seus pontos associados pela chave estrangeira. Sessões resumidas são persistidas a cada coleta, com limite de 500 registros. O app não mantém CPU acordada, não usa alarmes exatos e não força isenção de otimização. Eventos de bateria podem antecipar leituras, com limite de uma coleta por segundo para o mesmo estado. Mudanças de estado são processadas imediatamente. O intervalo configurado controla o polling, não a entrega de broadcasts do Android.

## Referências oficiais

- https://developer.android.com/reference/android/os/BatteryManager
- https://developer.android.com/develop/background-work/services/fgs/service-types#special-use
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/ui/views/notifications/notification-permission
- https://developer.android.com/build/releases/agp-8-9-0-release-notes
- https://developer.android.com/jetpack/androidx/releases/room
- https://kotlinlang.org/docs/compose-compiler-migration-guide.html
