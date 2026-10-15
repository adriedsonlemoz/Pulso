# Pulso Bateria 0.1.0-alpha.23

**Nova aba Labs 🔬 para testes solares offline.**

- Selecione a condição do céu (sol forte, sol entre nuvens, nublado, chuva), potência do painel e duração. A opção padrão é **120 min**.
- Observe cronômetro, tempo carregando e sem carregar; intervalos sem dados aparecem separadamente. Corrente média válida durante carga, em mA, representa a corrente líquida da bateria.
- Encerramento automático, notificação de conclusão e histórico consultável. É possível finalizar antes e registrar mudanças do céu.
- Reutiliza o serviço de monitoramento existente; o usuário deve autorizar notificações e iniciá-lo a partir do aplicativo aberto para coleta com tela apagada.
- Room v6 com migração 5→6, backup JSON v4 (leitura retrocompatível v1–v3). Sem redes, câmeras, gravação ou permissões adicionais.
- Limite: Android pode atrasar a finalização em segundo plano; tempo sem observação não é atribuído à carga. O serviço de monitoramento permanece ativado após o teste, podendo ser desativado nas Configurações.

**APK Android/Compose e testes em aparelho ainda requerem validação.**

APK esperado: `Pulso-Bateria-v0.1.0-alpha.23-debug.apk`.
