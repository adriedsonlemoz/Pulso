# Regras do projeto Pulso Bateria

- Android Kotlin/Compose, offline, sem anúncios/rastreamento/INTERNET. Não inventar medições ou capacidades do aparelho.
- Antes de editar, ler README, docs/MEASUREMENTS.md e docs/VERSIONING.md.
- Versão atual 0.1.0-alpha.35 / versionCode 35. Próxima 0.1.0-alpha.36 / 36. Incrementar sequencialmente cada entrega e sincronizar todos os documentos, Gradle, workflow e nome do ZIP.
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

- Alpha.28: Home apresenta hora estimada de descarga e média com 100% calculadas no core usando apenas histórico válido, mantendo previsão de carga anterior. Room v8 / backup v6 sem alteração. Próxima alpha.30 / 30.

- Alpha.29: Home inclui previsão histórica de recarga independente da descarga; apenas CHARGING, com preferências por fonte detectada e faixa 80..100 observada. Sem banco novo.

- Alpha.30: Home redesenhada no estilo compacto, com resumo superior mais enxuto, medidor reduzido e cartão de previsão promovido para a primeira dobra. Mudança visual somente; Room v8, backup v6 e cálculos do core intactos. Próxima alpha.31 / 31.

- Alpha.31: três cartões inferiores da Home compactados visualmente; preservar filtro 5/20/60, detalhes independentes de corrente, acessibilidade do gráfico e acesso à análise completa. Lógica, Room v8 e backup v6 inalterados. Próxima alpha.32 / 32.

- Alpha.32: agrupar sessões somente na UI, com origem preservada, toggle opt-out, lacunas não somadas e badge ATUAL apenas em leitura recente. Core provê particionamento testável. Não mesclar linhas reais do Room. Próxima alpha.33 / 33.

- Alpha.33: remover apenas a aba Sessões da navegação; histórico consultável a partir da Home, sem apagar registros. Labs com condições adicionais recolhíveis, CTA persistente e entradas numéricas validadas. Room v8 e backup v6 intactos. Próxima alpha.34 / 34.

- Alpha.34: a navegação usa Início/Sessões/Labs/Config.; **Uso foi removida da barra**, não os dados. O histórico ocupa a aba 1 e permanece acessível pela Home. Labs conserva melhorias da alpha.33. Room v8 e backup v6 inalterados. Próxima alpha.35 / 35.

- Alpha.35: AIDL externo apenas para um cliente instalado explicitamente pareado, com UID do Binder coletado antes da troca de dispatcher e certificado SHA-256 conferido a cada chamada. Não ativar FGS remoto, não incluir API HTTP, não incluir autorizados no backup, proteger testes manuais e preservar Room v8/JSON v6. Contrato v1 documentado em docs/INTEGRACAO_VIGIA_IA.md. Próxima alpha.36 / 36.
