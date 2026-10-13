# Validação — Pulso Bateria 0.1.0-alpha.20 (20)

## Alterações

- Alertas configuráveis e opt-in com confirmação em duas leituras e limitação de repetição; sem mudança de permissões e sem novos serviços.
- Relatório local de sete dias com série Room v5, exibindo apenas intervalos observados e separando dias civis.
- Room v5 / backup JSON v3 mantidos sem migração; alertas restaurados como desligados.

## Executado

- Kotlin 1.9 / executor JUnit mínimo: 8 novos casos (SmartAlertsTest e BatteryReportsTest) **aprovados**. Compila cálculos novos e fontes do módulo core (não valida Android/Compose).
- Banco SQLite: verificar em `docs/test-results/alpha.20-local-tests.txt`.
- Estrutura do código, XML, versão, workflow e integridade ZIP: verificar em `docs/test-results/alpha.20-local-tests.txt`.

## Pendente

- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` no GitHub Actions.
- Validar Android/Compose e notificações reais, limites OEM, suspensão, canais e permissão no Android 13+.
- Validar visual do relatório e formação dos registros após vários dias; migrar Room 1→5 e restaurar JSON v1/v2/v3 em emulador/aparelho.
