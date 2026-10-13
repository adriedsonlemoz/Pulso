# Pulso Bateria 0.1.0-alpha.20

- **Alertas inteligentes:** ative em Configurações, defina limites de bateria baixa, temperatura, meta de recarga e descarga acelerada. O sistema exige confirmação por duas leituras e evita repetições. Notificações precisam de autorização do Android.
- **Relatórios:** em Sessões → Relatório da bateria → Ver relatório. Consulte totais observados de hoje e sete dias com queda e recarga em %, tempo de acompanhamento, sessões e maior temperatura observada. Não preenche interrupções ou leituras históricas inexistentes.
- **Compatibilidade:** Room v5, backup JSON v3, calibragem, assinatura, Secrets e identificador `br.com.pulsobateria` mantidos. Alertas desativados por padrão, não exportados pelo backup. Nenhuma permissão de rede adicionada.
- **Limites:** alertas só surgem quando o Pulso coleta leituras; Android pode atrasar o monitoramento. Relatórios parciais não equivalem ao consumo total do período. Percentuais representam diferença dos níveis 0–100%.

**Validação pendente:** compilação Android, instrumentação Room e verificação de notificações e relatórios em aparelho físico. Não foi gerado APK local.

APK esperado: `Pulso-Bateria-v0.1.0-alpha.20-debug.apk`.
