# Pulso Bateria 0.1.0-alpha.3

versionCode: 3. Android 8.0+.

Telas Recarga e Autonomia escolhidas automaticamente pelo estado real. Porcentagem e temperatura em destaque no indicador. Notificação compacta mostra porcentagem e temperatura; segunda linha mostra tempo até 100% ou autonomia. Dados sem suporte continuam indisponíveis; previsões sem amostras suficientes mostram Calculando.

Sessões podem retomar após pausa de até 90 s no mesmo processo, somente com mesmo estado e porcentagem. Não contamos o tempo parado e reiniciamos curva/taxa recente. Mudança de estado, percentual, relógio, processo ou pausa maior abre novo registro. Histórico anterior é preservado; os registros mostrados nas imagens não serão apagados nem fundidos retroativamente.

Após sucesso do workflow, baixe Pulso-Bateria-v0.1.0-alpha.3-debug.apk pelo link APK direto no resumo ou em Releases. Apenas o APK é publicado pelo workflow, sem compactação e sem upload-artifact/relatórios extras. A pré-release usa GITHUB_TOKEN automático com contents: write; não exige Secret personalizado. APK Debug não é Release com assinatura de distribuição.

Build Android completo, lint e validação no aparelho dependem do Actions/dispositivo; consultar docs/VALIDATION.md para os testes realmente executados.
