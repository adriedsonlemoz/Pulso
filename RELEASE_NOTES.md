# Pulso Bateria 0.1.0-alpha.4

versionCode: 4. Android 8.0+.

**Ajustes de interface:** consumo/corrente, temperatura e ritmo na mesma linha quando há espaço; textos acessíveis em telas estreitas ou com fontes ampliadas. Toque nas medições para ver explicações, inclusive quando o Android não oferece valor confiável. O medidor circular e o cartão de autonomia estão mais compactos, e a temperatura aparece em um único cartão de leitura. O gráfico usa escala ajustada aos percentuais observados (sem inventar dados) e informa o período realmente amostrado.

**Configurações:** seletor horizontal de tema Sistema/AMOLED/Claro; intervalo de 15/30/60 segundos e temperatura °C/°F lado a lado quando couberem, ou empilhados para acessibilidade. Preferências continuam salvas no DataStore.

Sem alterações nos cálculos de autonomia, captura BatteryManager, histórico Room ou serviço de monitoramento contínuo. Mantém retomada controlada de pausas curtas da Alpha 3 e demais funções anteriores.

Após aprovação do workflow, baixe `Pulso-Bateria-v0.1.0-alpha.4-debug.apk` na Release ou pelo link do resumo. Apenas o APK Debug é publicado, sem ZIP intermediário e sem assinatura Release. É um build de desenvolvimento; a instalação sobre builds de outros runners pode exigir reinstalação por assinatura Debug diferente.

A compilação Android completa e os testes físicos de interface devem ser confirmados no workflow e no dispositivo; consulte `docs/VALIDATION.md`.
