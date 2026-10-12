# Pulso Bateria 0.1.0-alpha.6

versionCode: 6. Android 8.0+.

**Sessões redesenhadas:** cartões compactos e expansíveis, filtros Todas/Carga/Descarga, agrupamento por data, identificação de sessão Em andamento/Pausada/Finalizada e detalhes técnicos sem poluir a lista.

**Gráficos por sessão nova:** os pontos de percentual passam a ser preservados no banco local quando a leitura muda ou a cada minuto, com máximo de 480 pontos por sessão. Sessões antigas mantêm os resumos, mas não ganham gráficos artificiais. As curvas não unem intervalos longos sem observação.

**Migração de dados:** Room passa de versão 1 para 2 com migração explícita que mantém as sessões existentes. O app continua offline e sem permissões adicionais. Mantidas as proteções contra estimativas absurdas da Alpha 5.

Quando o workflow for aprovado, baixe `Pulso-Bateria-v0.1.0-alpha.6-debug.apk` na Release ou pelo link no resumo. Apenas o APK Debug é publicado. A compilação Android e o teste físico devem ser confirmados no GitHub Actions e no dispositivo; consulte `docs/VALIDATION.md`.
