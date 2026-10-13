# Versionamento

Versão atual: **0.1.0-alpha.14**, **versionCode 14**.
Próxima entrega: **0.1.0-alpha.15**, **versionCode 15**.

Fonte Android: `app/build.gradle.kts`; a tela Sobre usa BuildConfig. Sincronizar README, CHANGELOG, RELEASE_NOTES, AGENTS, este documento, validação, workflow e ZIP. Preservar `applicationId = br.com.pulsobateria` e os Secrets/keystore existentes.

Alpha.14 mantém **Room v3** e **backup JSON v1**. Não existe migração destrutiva. Mudança no formato interno da calibração descarta somente decisões antigas de sinal/unidade, preservando histórico e preferências. Os schemas JSON reais do KSP ainda precisam ser gerados e versionados; não criar JSON fictício.
