# Versionamento

Versão atual: **0.1.0-alpha.9**, **versionCode 9**.

Próxima atualização: `0.1.0-alpha.10`, `versionCode 10`. Incrementar ambos sequencialmente a cada entrega. A fonte Android é `app/build.gradle.kts` (versionName/versionCode). A tela usa BuildConfig.

Sincronizar README, CHANGELOG, RELEASE_NOTES, este documento, nome do ZIP e nome do artefato no workflow. Manter applicationId `br.com.pulsobateria`. Registrar alterações reais e resultados de testes; não declarar validações não executadas. Ao alterar o banco, subir schema version, preservar JSON exportado e fornecer migração explícita; não usar migração destrutiva para evitar perda silenciosa.
