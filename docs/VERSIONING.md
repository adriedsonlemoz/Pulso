# Versionamento

Versão atual: **0.1.0-alpha.19**, **versionCode 19**.
Próxima entrega: **0.1.0-alpha.20**, **versionCode 20**.

Fonte Android: `app/build.gradle.kts`; a tela Sobre usa BuildConfig. Sincronizar README, CHANGELOG, RELEASE_NOTES, AGENTS, este documento, validação, workflow e ZIP. Preservar `applicationId = br.com.pulsobateria` e os Secrets/keystore existentes.

Alpha.19 usa **Room v5** (migração 4→5 adiciona campos opcionais de fonte e nome manual a `sessions`) e **backup JSON v3** (continua importando v1/v2). Não existe migração destrutiva. Mudança no formato interno da calibração descarta somente decisões antigas de sinal/unidade, preservando histórico e preferências. Os schemas JSON reais do KSP ainda precisam ser gerados e versionados; não criar JSON fictício.
