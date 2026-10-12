# Validação — Pulso Bateria 0.1.0-alpha.12 (12)

## Resultado executado

- Kotlin compiler embeddable **2.1.20**, JVM target 17, Java 17, **JUnit real 4.13.2**, Hamcrest 1.3: **132 testes aprovados** no core e na formatação pura de notificações. Log: `test-results/alpha.12-junit.txt`. Não foram usados stubs de asserções para executar estes testes.
- O core original da alpha.11 passou nos seus 96 testes antigos (`alpha.11-baseline-junit.txt`). Oito casos portáveis novos falham contra ele (`alpha.11-audit-regressions.txt`) e passam contra a alpha.12. As falhas desse log são evidência da auditoria, não falhas do código entregue.
- `python3 tools/test_storage_sql.py`: **8 testes SQLite aprovados**, executando SQL real das migrações/queries. Não valida o processador Room nem a implementação gerada do DAO.
- Parser Kotlin PSI: **19 arquivos Kotlin, zero erros sintáticos**. Não valida tipos Android, Compose ou Room/KSP. Log `test-results/alpha.12-syntax.txt`.
- `python3 tools/validate_source.py`: aprovado; 14 arquivos essenciais, 7 XML, permissões originais, versão sincronizada e SHA-256/CRC do Wrapper.
- CRC do ZIP final e exclusão de caches/build/local.properties/keystores conferidos no empacotamento. SHA-256 do Wrapper: `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`.

## Limites e pendências

O comando Gradle requerido foi tentado, mas falhou no download de Gradle 8.11.1 por indisponibilidade de rede. **Não houve build do APK, lint Android, compilação Compose/KSP nem execução instrumentada.** O script local JVM compila os arquivos puros relevantes, não o app inteiro.

Foram preparados quatro testes instrumentados Room: migrações 1→3 e 2→3, preservação ao abrir v3 e rollback conjunto de resumo/ponto. Executar `:app:connectedDebugAndroidTest` em emulador/aparelho de teste. Os schemas JSON reais precisam ser gerados pelo KSP e versionados; o workflow passa a guardá-los como artefato quando produzidos.

Pendentes: round-trip do backup completo no Android, importação de backup legado, falhas de armazenamento/pressão de memória, matar processo/reiniciar, atualização sobre alpha.11 com certificado compatível, aparência/notificação e séries sincronizadas com AccuBattery. Room v3 e backup v1 foram mantidos.

Leituras de hardware não foram simuladas como medições reais. As tolerâncias de calibração são heurísticas explícitas e testadas, não limites de erro físico. Médias 5/20 min e mAh de sessão foram avaliados/propostos; não estão implementados.

Relatório completo: `AUDIT-alpha.11-to-alpha.12.md`.
