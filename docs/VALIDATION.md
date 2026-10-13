# Validação — Pulso Bateria 0.1.0-alpha.16 (16)

## Escopo

Nova aba Apps com acesso especial opcional às estatísticas de utilização, gráfico de atividade 24h e comparação conservadora entre queda de bateria e tela ligada/desligada. Adicionada somente uma consulta read-only à `SessionDao`; banco Room v3, migrações, backup e assinatura não foram alterados.

## Validação local executada

- `python3 tools/validate_source.py`: **PASS**, 14 arquivos obrigatórios, 7 XMLs, permissões, referências de versão e integridade SHA-256/CRC do Gradle Wrapper.
- `python3 tools/test_storage_sql.py`: **8 testes aprovados** (incluindo migração Room 1→3/2→3, FK, retenção e rollback); verifica SQL diretamente, não runtime do Room no Android.
- Os testes Kotlin em `core/src/test/.../UsageAnalysisMathTest.kt` foram **adicionados, mas ainda não executados**: cenários de contexto inicial, tela ligada/desligada, faixas horárias, ausência de eventos, lacunas, mudanças de sessão e recarga.
- O Gradle e o compilador Kotlin local não concluíram a inicialização neste ambiente. Logo, **não foi possível compilar Kotlin/Compose/Room ou validar o APK aqui**. O CI com Android SDK precisa executar `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. Não considerar a versão aprovada em dispositivo até esses resultados.


## Pendências em hardware real

- Permissão Acesso a uso, controle de ativação/revogação e labels de aplicativos sob as regras de visibilidade de pacote.
- Conferir eventos de foreground/background e screen interactive/noninteractive em cada versão Android; eventos históricos podem ser incompletos.
- Validar renderização dos gráficos e proporção das barras com fonte ampliada.
- Verificar assinatura estável durante a atualização, preservação do histórico e backup de segurança antes de trocar o APK.
