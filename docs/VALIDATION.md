# Validação — Pulso Bateria 0.1.0-alpha.21 (21)

## Alterações

- Reorganização exclusiva de interface: resumo compacto 5/20/60 min e tela técnica separada; abas Uso: Resumo / Aplicativos / Noite; atalhos em Sessões para Comparar e Relatórios em telas próprias; ajuda contextual.
- O cálculo de corrente, o contador de carga, o histórico e as proteções contra lacunas permanecem os mesmos.
- Room **v5** e backup **JSON v3** preservados, sem migração. Mesmas permissões, Secrets e assinatura.

## Executado localmente

- **12 testes SQLite aprovados**, inclusive migrações 1→5/4→5, integridade de chave estrangeira e retenção (`tools/test_storage_sql.py`).
- **27 fontes Kotlin (core + app)** analisadas por parser PSI do compilador Kotlin: **0 erros de sintaxe**. Isso não é compilação Compose/Android nem verificação de tipos.
- **11 verificações estáticas de navegação** passaram (`tools/test_navigation_contract.py`): rotas, atalhos, abas internas, botão de ajuda, nomes de carregador ocultos até expansão e avisos de dados parciais.
- `tools/validate_source.py`: manifest, XML, pacote, versão, workflow, wrapper Gradle e schemas de migração, aprovado.
- ZIP do código verificado por integridade CRC e conferência de conteúdos, sem chaves de assinatura.

## Bloqueios / pendências

- Gradle `./gradlew --offline :core:test :app:assembleDebug --no-daemon` não pôde iniciar as tarefas: `UnknownHostException: services.gradle.org` ao tentar obter `gradle-8.11.1-bin.zip`.
- Tentativa de executar a suíte Kotlin/JVM completa com compilador local atingiu limite de execução. **Não** considerá-la aprovada; testes estáticos não substituem a suíte.
- Testar compilação Kotlin/Compose/Android, KSP, navegação Voltar, abertura de ajuda, disposição em telas pequenas e acessibilidade no GitHub Actions e aparelho real. Não há APK validado no ambiente local.
