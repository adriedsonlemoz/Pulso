# Pulso Bateria 0.1.0-alpha.19

- **Consumo noturno:** nova seção na aba Uso com a última noite (22h–7h local), estado da tela, percentuais de queda/recarga, duração observada e cobertura. Acesso ao uso é opcional; intervalos ausentes não são estimados.
- **Comparar recargas:** em Sessões, escolha duas recargas finalizadas. Cálculos comparáveis exigem uma faixa comum real de pelo menos 5% com telemetria contínua; mostra tempos, ritmo em %/h, temperatura média e mAh de contador válido.
- **Identificação de fontes:** salva tipo de alimentação reportada pelo Android e permite nome manual de carregador por sessão. Não identifica marca/modelo ou cabo automaticamente.
- **Banco Room v5:** migração explícita e não destrutiva 4→5 com colunas opcionais `chargeSource` e `chargerLabel`. Histórico antigo preservado sem inventar origem de carregador.
- **Backup JSON v3:** inclui os campos novos de sessão e aceita backups v1/v2, preservando verificação SHA-256.
- Mantém monitoramento offline, segurança, identidade `br.com.pulsobateria`, assinatura estável opcional e toda funcionalidade alpha.18.

**Pendente:** compilar e executar os testes de instrumentação Android/Room em GitHub Actions ou aparelho real. A validação local de SQL não prova a migração real.

APK esperado: `Pulso-Bateria-v0.1.0-alpha.19-debug.apk`.
