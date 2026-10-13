# Regras do projeto Pulso Bateria

- Android Kotlin/Compose, offline, sem anúncios/rastreamento/INTERNET. Não inventar medições ou capacidades do aparelho.
- Antes de editar, ler README, docs/MEASUREMENTS.md e docs/VERSIONING.md.
- Versão atual 0.1.0-alpha.16 / versionCode 16. Próxima 0.1.0-alpha.17 / 17. Incrementar sequencialmente cada entrega e sincronizar todos os documentos, Gradle, workflow e nome do ZIP.
- Preservar applicationId. Keystore privada e Secrets nunca entram no ZIP de código ou repositório. Para Debug atualizável, requer Secrets estáveis; sem Secrets a chave temporária deve receber aviso.
- Manter cálculos no módulo core, testáveis sem Android; validar nulos, sentinelas, sinal, relógio e lacunas.
- Não iniciar foreground service fora de ação permitida; manter tipo specialUse e sua justificativa, sem simular dataSync/health. Não adicionar wake locks, permissões privilegiadas ou mecanismos de evasão.
- Histórico só contém períodos observados. Não preencher lacunas. Migrações Room explícitas; manter schemas versionados.
- Rodar `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` quando possível. Distinguir sucesso real de pendência no relatório.
- Entregar ZIP com os arquivos na raiz, incluindo .github, Wrapper JAR e scripts. Excluir caches/build/local.properties/keystores. Verificar CRC do ZIP e SHA-256 do Wrapper.
- Qualquer revisão futura deve atualizar docs/VALIDATION.md com o que foi efetivamente executado e limitações restantes.
