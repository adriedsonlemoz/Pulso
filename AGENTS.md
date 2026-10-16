# Regras do projeto Pulso Bateria

- Android Kotlin/Compose, offline, sem anúncios/rastreamento/INTERNET. Não inventar medições ou capacidades do aparelho.
- Antes de editar, ler README, docs/MEASUREMENTS.md e docs/VERSIONING.md.
- Versão atual 0.1.0-alpha.28 / versionCode 28. Próxima 0.1.0-alpha.29 / 29. Incrementar sequencialmente cada entrega e sincronizar todos os documentos, Gradle, workflow e nome do ZIP.
- Preservar applicationId. Keystore privada e Secrets nunca entram no ZIP de código ou repositório. Para Debug atualizável, requer Secrets estáveis; sem Secrets a chave temporária deve receber aviso.
- Manter cálculos no módulo core, testáveis sem Android; validar nulos, sentinelas, sinal, relógio e lacunas.
- Não iniciar foreground service fora de ação permitida; manter tipo specialUse e sua justificativa, sem simular dataSync/health. Não adicionar wake locks, permissões privilegiadas ou mecanismos de evasão.
- Histórico só contém períodos observados. Não preencher lacunas. Migrações Room explícitas; manter schemas versionados.
- Rodar `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` quando possível. Distinguir sucesso real de pendência no relatório.
- Entregar ZIP com os arquivos na raiz, incluindo .github, Wrapper JAR e scripts. Excluir caches/build/local.properties/keystores. Verificar CRC do ZIP e SHA-256 do Wrapper.
- Qualquer revisão futura deve atualizar docs/VALIDATION.md com o que foi efetivamente executado e limitações restantes.

- Na alpha.21, páginas internas compactam explicações, mas avisos de dados insuficientes/parciais continuam visíveis. Não modificar cálculos/Room/backup sem necessidade.

- Alpha.22: notificação compacta/expandida sem duplicar corrente, média zero ambígua oculta em carga/descarga, estado FULL dependente do Android e aviso térmico passivo. Testar 100% e fontes alternativas.

- Alpha.23: Labs com Room v6, backup JSON v4, prazos e lacunas observacionais. Finalização automática pode atrasar sob restrições do Android; testar em aparelho real e não alegar precisão de timer/wake lock inexistente.

- Alpha.24: Labs apresenta menu e Teste de Consumo. Room v7 (6→7) com legado solar preservado, backup v5. Testes de consumo com fonte externa detectada são interferidos; não inventar amostras durante lacunas.

- Alpha.25: Configurações usam menu por categoria e controles compactos, com temas claro/AMOLED harmonizados. Preservar todas as ações (incluindo permissão de alertas, backup e diagnóstico) e manter avisos importantes visíveis. Sem migração Room/backup.

- Alpha.26: Labs permite excluir apenas teste concluído por ID mediante confirmação; cartão mostra Início/Fim juntos e aviso quando não houve intervalo observado. Room v7 / backup v5 inalterados.

- Alpha.27: Labs Room v8 com migração 7→8 e backup JSON v6; temperatura da bateria vem apenas de leituras reais (initial/final/max opcionais). Campos de contexto são declarados manualmente. Ação copiar é textual, excluir mantém confirmação. Não preencher valores legados.

- Alpha.28: Home apresenta hora estimada de descarga e média com 100% calculadas no core usando apenas histórico válido, mantendo previsão de carga anterior. Room v8 / backup v6 sem alteração. Próxima alpha.29 / 29.
