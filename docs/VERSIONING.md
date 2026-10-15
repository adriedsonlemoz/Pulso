# Versionamento

Versão atual: **0.1.0-alpha.23**, **versionCode 23**.
Próxima entrega: **0.1.0-alpha.24**, **versionCode 24**.

Fonte Android: `app/build.gradle.kts`; a tela Sobre usa BuildConfig. Sincronizar README, CHANGELOG, RELEASE_NOTES, AGENTS, este documento, validação, workflow e ZIP. Preservar `applicationId = br.com.pulsobateria` e os Secrets/keystore existentes.

Alpha.19 usa **Room v5** (migração 4→5 adiciona campos opcionais de fonte e nome manual a `sessions`) e **backup JSON v3** (continua importando v1/v2). Não existe migração destrutiva. Mudança no formato interno da calibração descarta somente decisões antigas de sinal/unidade, preservando histórico e preferências. Os schemas JSON reais do KSP ainda precisam ser gerados e versionados; não criar JSON fictício.

Alpha.20 mantém **Room v5** e **backup JSON v3**; as preferências opcionais de alertas são locais, desativadas após restauração. Não há migração de banco nem novos Secrets.

Alpha.21 reorganiza telas e textos; **nenhuma migração Room** (permanece v5), **backup JSON v3** e as mesmas quatro Secrets de assinatura.

Alpha.22 altera apenas apresentação da notificação/ações do serviço, sem mudanças no banco Room v5, backup JSON v3 ou Secrets.

Alpha.23 usa **Room v6**, migração não destrutiva 5→6 com `lab_tests`, isolada das sessões; **backup JSON v4** adiciona Labs concluídos e continua aceitando v1/v2/v3. O service specialUse e as quatro Secrets existentes são mantidos. Não criar schemas KSP manualmente.
