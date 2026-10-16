# Pulso Bateria 0.1.0-alpha.32

- Sessões próximas do mesmo estado (carga/descarga), com fronteira de percentual igual e intervalo de até 3 minutos após interrupção, aparecem reunidas em um único cartão na aba Sessões.
- Interruptor **Agrupar sessões próximas** permite alternar entre visualização agrupada e registros independentes; **Ver originais** revela cada trecho e seus detalhes técnicos/gráficos.
- Hora inicial/final e tempo observado são preservados; lacunas não são contabilizadas. Nenhum dado do Room foi fundido ou removido.
- Selo **ATUAL** no cartão da sessão que está realmente recebendo leituras recentes, com origem Android compatível.
- Registros de carregamento com mudança de fonte ou identificação manual não são reunidos; também não agrupa estados FULL/IDLE nem atravessa sessões de outro estado.
- Histórico, análises e Labs, backup JSON v6, Room v8, versão 31 da Home compacta, monitoramento e assinatura anteriores preservados.

APK esperado no GitHub Actions: `Pulso-Bateria-v0.1.0-alpha.32-debug.apk`. Este ZIP contém apenas código-fonte, não APK compilado.
