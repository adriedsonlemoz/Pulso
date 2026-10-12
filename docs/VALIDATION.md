# Validação — Pulso Bateria 0.1.0-alpha.2 (2)

Data: 2026-10-12. Base: Alpha 1 entregue neste chat.

## Problema confirmado

Os dois logs do workflow Android • Pulso Bateria (#1 e #2) registram `packages: tools platform-tools`, aceitam as licenças e falham em `sdkmanager tools` com `Warning: Failed to find package 'tools'` e exit code 1. A compilação do aplicativo não começou.

## Correção

Em `.github/workflows/android.yml`, definir `with: packages: platform-tools` na ação `android-actions/setup-android@v3`. Manter `sdkmanager 'platforms;android-35' 'build-tools;35.0.0'` na etapa seguinte. Versão sincronizada para 0.1.0-alpha.2 / 2.

## Verificações executadas nesta entrega

- YAML analisado: ação Android recebe exatamente platform-tools, sem tools; instalação de SDK 35/Build Tools preservada; tarefas de testes/lint/build preservadas.
- Script `python3 tools/validate_source.py`: arquivos obrigatórios, XMLs, permissões, versão e Wrapper oficial SHA-256/CRC aprovados.
- Comparação com o ZIP anterior: nenhum código Kotlin, recurso ou dependência de execução alterado; mudança de build do app limitada à versão.
- ZIP completo: CRC, estrutura na raiz e inclusão de .github/Wrapper verificados; caches, builds e configuração local excluídos.

## Pendências e evidência anterior

Não foi executado um novo GitHub Actions nem compilado APK nesta entrega. Compilação Android completa, KSP, Android Lint e testes no aparelho continuam pendentes. A correção remove o bloqueio identificado, mas não prova que as etapas seguintes passarão.

Os 31 testes dos cálculos passaram na Alpha 1, com Kotlin 2.1.20 e JUnit 4.13.2. Esse resultado anterior foi preservado em `docs/test-results/core-junit.txt`; não é uma nova execução. O relatório anterior e o roteiro de testes físicos estão em `docs/VALIDATION-alpha.1.md`.

Inicie um novo workflow após enviar o código atualizado. Não basta reexecutar o job do commit antigo.
