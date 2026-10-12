# Schemas Room

Banco v1: apenas `sessions` (as versões anteriores não incluíam export JSON versionado).

Banco v2: mantém `sessions` e acrescenta `session_samples` usando a migração 1→2 em `Store.kt`.
O SQL de referência da migração está em `room-migration-1-2.sql`.

**Pendente:** executar a compilação Android/KSP em ambiente com as dependências necessárias e versionar o schema JSON **gerado pelo Room**, sem inventar um schema JSON exportado. `exportSchema = true` permanece habilitado. A migração precisa ser testada com o APK instalado sobre a Alpha 5.

Room v3: as colunas anuláveis `currentMa`, `temperatureC`, `currentSource` são adicionadas pela migração 2→3. O arquivo JSON oficial `3.json` ainda precisa ser gerado pelo KSP em uma compilação Android e versionado sem modificar manualmente.
