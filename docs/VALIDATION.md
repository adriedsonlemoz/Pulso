# Validação — Pulso Bateria 0.1.0-alpha.8 (8)

## Fonte dos logs recebidos

ZIP `Pulso-Android-Pulso-Bateria-8-logs.zip`: no job `build/8_Testes, lint e APK Debug.txt`, o Gradle finalizou com `BUILD SUCCESSFUL in 1m 41s`, 55 tarefas executadas, incluindo `:core:test`, `:app:assembleDebug` e `:app:lintDebug`. A etapa **posterior** `gh release create` falhou com HTTP 500 `https://api.github.com/repos/adriedsonlemoz/Pulso/releases`. O erro não é de compilação.

## Testes locais da alpha.8

- `kotlinc` do módulo core e dois testes novos para calibração: **75 testes aprovados, 0 falhas** (executor JUnit compatível, não Gradle).
- PSI de Kotlin: **14 fontes testadas, 0 erros de sintaxe**. Análise sintática não substitui compilação Android/Compose/KSP.
- SQL de migração 1→2 e 2→3 testado separadamente com `sqlite3` e preservação de registro, campos opcionais e exclusão em cascata; não equivale a testar migração Room em aparelho.
- YAML do workflow interpretado, XMLs e Python `tools/validate_source.py` aprovados. Verificar ZIP/CRC e ausência de chaves privadas após empacotar.
- Keystore PKCS12 gerada fora do pacote; os Secrets são arquivos PRIVADOS separados. SHA-256 do certificado público documentado em `docs/BACKUP_AND_SIGNING.md`.

## Pendências

1. **Compilar a alpha.8 no GitHub Actions**. A alpha.8 não foi compilada pelo Gradle neste ambiente: `UnknownHostException` ao buscar `services.gradle.org`. Testes Kotlin sintáticos não comprovam compilação Android.
2. **Executar `:app:connectedDebugAndroidTest`** num dispositivo/emulador. O workflow usa `:app:assembleDebugAndroidTest`, que apenas compila os testes. Confirmar schema Room3/SQL real e exportar `app/schemas/br.com.pulsobateria.data.BatteryDatabase/3.json` gerado por KSP para versionamento; não inventar esse arquivo.
3. Validar exportação, importação, transação e UI com dados reais em aparelho. DataStore é independente do Room; falha após commit pode deixar preferências desatualizadas.
4. Conferir fingerprint de cada APK com `apksigner verify --print-certs` e testar atualização entre builds **ambos assinados com a chave estável**. Sem chave antiga, a alpha.8 não pode atualizar o APK Debug anterior com certificado diferente.
5. Testar permanência de histórico após fechar/reabrir, reiniciar, e restaurar backups extensos. Limite de arquivo 64 MiB. Calibração possui validade de sete dias.
