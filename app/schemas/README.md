# Schemas Room

Banco v1: apenas `sessions` (as versões anteriores não incluíam export JSON versionado).

Banco v2: mantém `sessions` e acrescenta `session_samples` usando a migração 1→2 em `Store.kt`.
O SQL de referência da migração está em `room-migration-1-2.sql`.

**Pendente:** executar a compilação Android/KSP em ambiente com as dependências necessárias e versionar o schema JSON **gerado pelo Room**, sem inventar um schema JSON exportado. `exportSchema = true` permanece habilitado. A migração precisa ser testada com o APK instalado sobre a Alpha 5.

Room v3: as colunas anuláveis `currentMa`, `temperatureC`, `currentSource` são adicionadas pela migração 2→3. O arquivo JSON oficial `3.json` ainda precisa ser gerado pelo KSP em uma compilação Android e versionado sem modificar manualmente.

## Alpha.12

Banco permanece v3: nenhuma mudança de entidades ou nova migração. Foram adicionados testes de reabertura v3 e atomicidade do DAO. O workflow guarda os JSONs reais produzidos pelo KSP em artefatos de validação. Ainda é necessário obtê-los de um build Android bem-sucedido e versioná-los.

- **Room v4:** migração `MIGRATION_3_4` em Store.kt e SQL de referência `room-migration-3-4.sql`. O JSON oficial gerado pelo KSP para v4 ainda deve ser capturado e versionado após um build Android bem-sucedido.

- **Room v5:** migração `MIGRATION_4_5`, adicionando `chargeSource` e `chargerLabel` anuláveis à tabela `sessions`; SQL de referência em `room-migration-4-5.sql`. JSON oficial v5 deve ser obtido do KSP após build Android, nunca escrito manualmente.

- **Room v6 (alpha.23):** tabela independente `lab_tests`, migração explícita 5→6 em `room-migration-5-6.sql`. Capturar o JSON oficial KSP 6.json após compilar Android em CI; nunca fabricar schema JSON.

- **Room v7 (alpha.24):** migração `MIGRATION_6_7` com três colunas em `lab_tests`: `kind`, `externalSeen` e `lastPlugged`. Testes anteriores tornam-se `SOLAR`. SQL em `room-migration-6-7.sql`. Gerar schema KSP 7.json apenas por build real.
