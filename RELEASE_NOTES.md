# Pulso Bateria 0.1.0-alpha.5

versionCode: 5. Android 8.0+.

**Correção da estimativa de carga:** a versão anterior podia mostrar “2 min até 100%” mesmo com a bateria em 58%, porque aceitava a previsão positiva do Android sem verificação. A Alpha 5 só aceita o tempo fornecido pelo Android quando o próprio aplicativo já mediu variação suficiente, compara o tempo informado com a taxa observada e rejeita valores incompatíveis. Se necessário, usa a projeção local; sem dados válidos exibe “Calculando…”.

Os filtros são heurísticas para evitar previsões claramente incoerentes, não garantias de precisão. A carga pode desacelerar perto de 100%, e o Android/fabricante não garante uma estimativa estável. A notificação utiliza a mesma previsão validada.

O layout compacto e o gráfico da Alpha 4 permanecem. Não houve alterações nas sessões Room, permissões ou monitoramento. Consulte `docs/VALIDATION.md` para o alcance dos testes.

Após aprovação do workflow, baixe `Pulso-Bateria-v0.1.0-alpha.5-debug.apk` na Release ou pelo link do resumo. Só o APK Debug será publicado; assinatura Debug de runners diferentes pode impedir atualização direta sobre APK anterior.

A compilação Android e os testes físicos continuam dependentes da execução no GitHub Actions e em aparelho.
