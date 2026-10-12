# Validação — Pulso Bateria 0.1.0-alpha.9 (9)

## Escopo desta revisão

A alpha.9 altera a apresentação da **notificação expandida** do monitoramento contínuo. O objetivo é reduzir a poluição visual, manter no título `porcentagem • temperatura • corrente/gasto` e evitar a duplicação do texto no conteúdo expandido.

## Verificações executadas

- Revisão estática do código de `MonitorService.kt` e dos formatadores em `ui/Format.kt`.
- Sincronização de versão e artefatos: `app/build.gradle.kts`, workflow, README, CHANGELOG, RELEASE_NOTES, `docs/VERSIONING.md`, `tools/validate_source.py` e `AGENTS.md`.
- A integridade estrutural do projeto foi preservada; não houve alteração no banco Room, no backup/restauração nem na assinatura.

## Pendências

1. Compilar a alpha.9 no Android/GitHub Actions. Neste ambiente a compilação completa continua sujeita à indisponibilidade de `services.gradle.org`.
2. Validar visualmente em aparelho a notificação recolhida e expandida, incluindo casos com corrente instantânea, média, estimativa e indisponível.
3. Conferir atualização do APK com a mesma assinatura estável já introduzida na alpha.8.
