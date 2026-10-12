# Changelog

## 0.1.0-alpha.2 (2) — 2026-10-12

- Corrige falha `Failed to find package tools` nos builds #1 e #2: `android-actions/setup-android@v3` agora recebe explicitamente `packages: platform-tools`.
- Mantém instalação de SDK 35 e Build Tools 35.0.0 na etapa seguinte.
- Sincroniza versão, documentação e nome do artefato Debug.
- Workflow YAML, versões, Wrapper e ZIP verificados; execução remota e APK ainda pendentes.

## 0.1.0-alpha.1 (1) — 2026-10-12

Primeira Alpha: monitoramento offline real via Android, interface Compose/Material 3, três temas, indicador circular, sessões Room, preferências DataStore, gráfico recente, notificação opcional e workflow de APK Debug. Cálculos isolados em módulo Kotlin/JVM com testes. Corrente não suportada ou inconsistente nunca é simulada.
