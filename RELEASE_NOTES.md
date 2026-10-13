# Pulso Bateria 0.1.0-alpha.13 • versionCode 13

- Corrigida a apresentação de corrente positiva durante descarga (ou negativa durante carga): leitura contraditória fica no diagnóstico, não no cartão/notificação como valor confiável. Fallback utiliza apenas fontes com direção compatível.
- Estimativa de corrente: dez capacidades predefinidas (2.500–7.000 mAh), campo manual e formatação `5.000`, sem alterar o valor numérico armazenado.
- Monitoramento contínuo com `START_STICKY`, opção **Iniciar com o sistema** e tentativa após `BOOT_COMPLETED`/atualização. A retomada depende das restrições Android/OEM; não garante operação permanente, e força-parada do usuário impede reinício automático.
- Ação Parar desabilita a opção persistida; tela distingue preferência e serviço efetivamente ativo, com botão Tentar agora.
- Banco Room v3, backup JSON v1, assinatura via mesmos Secrets e histórico preservados.

**Teste Android pendente:** validar compilação/foreground service, boot, atualização, permissões, desativação manual, assinatura e medições reais antes de considerar esta alpha estável.

APK esperado no workflow: `Pulso-Bateria-v0.1.0-alpha.13-debug.apk`.
