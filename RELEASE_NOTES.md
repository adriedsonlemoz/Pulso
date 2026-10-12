# Pulso Bateria 0.1.0-alpha.8

- Corrigida a distribuição quando a API GitHub Releases responde HTTP 500: o APK fica salvo em Artifacts e a Release é tentada novamente.
- Assinatura estável de Debug/Release por Secrets GitHub Manager (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Sem Secrets, Debug temporário sem garantia de atualização.
- Backup local JSON de sessões, pontos e preferências, com SHA-256, prévia e confirmação para restauração por substituição.
- Persistência Room v3: migração 2→3 não destrutiva; salva corrente, temperatura e origem quando disponíveis.
- Calibração confirmada mantida por até 7 dias e controle Recalibrar.

**Importante:** a nova assinatura não é compatível automaticamente com APKs antigos assinados em runners temporários. Não desinstale a versão antiga se ainda há registros sem backup.

APK: `Pulso-Bateria-v0.1.0-alpha.8-debug.apk`. A execução exige testes no aparelho/emulador para confirmar fluxo de restauração e migrações Room.
