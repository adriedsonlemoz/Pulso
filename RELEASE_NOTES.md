# Pulso Bateria 0.1.0-alpha.1

versionCode: 1. Android mínimo: 8.0 (API 26).

Primeira versão Alpha com porcentagem, estado, temperatura, corrente quando disponível, taxa e previsão condicionadas a dados suficientes, histórico local e monitoramento opcional por notificação.

Design em português com AMOLED verdadeiro, tema claro e sistema; abas Início, Sessões e Configurações. Sem rede, anúncios ou conta.

O artefato do Actions é APK Debug, sem necessidade de Secrets. Para instalar sobre builds anteriores, a assinatura deve coincidir. Esta Alpha não inclui assinatura Release.

Limitações: sensores dependem do fabricante; corrente contraditória é omitida; sem previsão imediata antes de amostragem suficiente; serviço pode ser suspenso; não retoma automaticamente após reinício/morte do processo; curvas históricas não são armazenadas; até 500 sessões. Validação e testes pendentes estão detalhados em docs/VALIDATION.md.
