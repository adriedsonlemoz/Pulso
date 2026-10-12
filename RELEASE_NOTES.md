# Pulso Bateria 0.1.0-alpha.11

- Notificação de carregamento: substitui `Até 100%: 25 min` por horário local estimado, como `100% previsto: ~14h45`.
- Exibe `amanhã` ou a data se o horário ultrapassar o dia atual. Arredondamento para cinco minutos indica aproximação, sem alterar o cálculo original da previsão.
- Em descarga, mantém a estimativa de autonomia por duração. Corrente e ritmo continuam identificados conforme a fonte.
- Banco Room v3, histórico, backup/restauração e assinatura são preservados, sem migrações.

APK: `Pulso-Bateria-v0.1.0-alpha.11-debug.apk`. Necessita compilação e avaliação da notificação no aparelho.
