# Validação — Pulso Bateria 0.1.0-alpha.25 / versionCode 25

## Alteração implementada

- Página Configurações substituída por menu por categorias, com oito destinos, status resumido, seletores compactos, detalhes técnicos recolhíveis e retorno por botão ou gesto/sistema.
- Restauração da rolagem ao entrar ou voltar de uma categoria, evitando abrir uma tela no fim da lista.
- Cor principal, containers selecionados, bordas e indicador de navegação coordenados nos temas claro/AMOLED. Todas as opções originais de configuração continuam ligadas aos mesmos métodos ViewModel.
- Nenhuma alteração de `BatteryManager`, medições, Labs, Room v7, backup JSON v5, serviço, permissões ou aplicativo ID.

## Verificações executadas

- `python tools/validate_source.py`: **APROVADO** (14 arquivos essenciais, XML/Manifest, versionamento, Wrapper SHA-256/CRC).
- `python tools/test_navigation_contract.py`: **11/11 APROVADOS** (contratos estáticos de páginas e navegação existentes).
- `python tools/test_storage_sql.py`: **14/14 APROVADOS** (testes SQLite/migrações existentes). O script exibe avisos ResourceWarning de conexões não fechadas em um dos testes, mas sem falhas.
- `python tools/test_settings_ui.py`: **21/21 APROVADOS** (contratos estáticos das novas categorias, persistência das ações, permissões, backup, retorno, rolagem e tokens de cor). Não é um teste visual nem substitui build Android.
- Parser PSI do compilador Kotlin: **45 fontes Kotlin, zero erros de sintaxe**. Não resolve tipos Compose nem outras dependências Android.
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon`: **BUILD NÃO INICIADO**, pois o Gradle Wrapper tentou baixar `gradle-8.11.1-bin.zip` e recebeu `UnknownHostException: services.gradle.org`. Não foi gerado APK local.
- `tools/run_local_jvm_tests.py`: não executado, depende de `--toolchain-dir` com pacotes que não estão disponíveis neste ambiente.

## Validação pendente no GitHub Actions e aparelho

1. Rodar `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` com Android SDK e dependências.
2. Inspecionar tela em temas claro e AMOLED, fontes padrão/ampliadas e largura reduzida, inclusive opções com 5 alternativas.
3. Exercitar ícones/retorno, navegação por categoria, scroll, exportação/restauração, permissões dos alertas, monitoramento e recalibração.
4. Confirmar atualização por cima da alpha.24 usando assinatura estável. Não desinstalar versões anteriores sem backup dos dados.

**Não afirmar build Android, APK ou validação visual em aparelho como concluídos.**
