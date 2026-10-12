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

Exigências: ≥3 pontos, ≥5 min, ≥2 p.p., timestamps crescentes, intervalos ≤3 min, direção consistente e |taxa| ≤200 p.p./h. Sem critérios: Calculando. Descarga: `percentual / |taxa|` horas. Carga: `(100 − percentual) / taxa` horas. Estimativas acima de sete dias são rejeitadas. A carga não é linear; a previsão é aproximada e pode mudar. Previsão válida do sistema tem prioridade e é identificada.

A média de uma sessão é a variação líquida observada dividida pela duração; não é a média das correntes. Exige ≥3 amostras, ≥5 min e ≥2 p.p. Temperatura média é média aritmética das amostras válidas (≥3), não uma média temporal ponderada. Eventos extras podem influenciar a ponderação.

## Integridade e consumo

Um único coletor atende Activity e serviço; comandos são serializados em coroutine. Banco usa métodos suspend do Room. Observação para quando não há proprietário. Rotação mantém a sessão. A duração usa relógio monotônico; alteração do relógio civil >60 s, regressão monotônica, status diferente ou lacuna >3 min encerra a sessão na **última leitura válida**, nunca no fim presumido da lacuna. Reabertura após morte do processo fecha registros incompletos como interrompidos. Não se reconstitui o que não foi medido.

Amostras da curva ficam apenas em RAM. Sessões resumidas são persistidas a cada coleta, com limite de 500 registros. O app não mantém CPU acordada, não usa alarmes exatos e não força isenção de otimização. Eventos de bateria podem antecipar leituras, com limite de uma coleta por segundo para o mesmo estado. Mudanças de estado são processadas imediatamente. O intervalo configurado controla o polling, não a entrega de broadcasts do Android.

## Referências oficiais

- https://developer.android.com/reference/android/os/BatteryManager
- https://developer.android.com/develop/background-work/services/fgs/service-types#special-use
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/ui/views/notifications/notification-permission
- https://developer.android.com/build/releases/agp-8-9-0-release-notes
- https://developer.android.com/jetpack/androidx/releases/room
- https://kotlinlang.org/docs/compose-compiler-migration-guide.html
