# Validação — Pulso Bateria 0.1.0-alpha.26 / versionCode 26

## Alterações

- Labs: detalhes do histórico com Início/Fim na mesma linha, comandos Recolher/Excluir lado a lado e exclusão confirmada.
- Exclusão por ID restrita aos testes com `status != RUNNING`, protegendo experimentos ativos e os demais históricos.
- Resumos solares recolhidos exibem nível inicial/final; períodos integralmente desconhecidos recebem alerta claro.
- Sem migração nem alteração de cálculos: Room v7, backup JSON v5, leitura e serviço preservados.

## Validações locais

- `python tools/validate_source.py`: **APROVADO** (14 arquivos essenciais, 7 XML, versão e SHA-256/CRC do Gradle Wrapper).
- `python tools/test_navigation_contract.py`: **11/11 APROVADOS** (contratos estáticos de navegação).
- `python tools/test_settings_ui.py`: **21/21 APROVADOS** (Configurações preservadas).
- `python tools/test_storage_sql.py`: **14/14 APROVADOS** (migrações e invariantes SQLite; avisos ResourceWarning em teste preexistente).
- `python tools/test_labs_history.py`: **12/12 contratos estáticos APROVADOS + 5 cenários SQL** (excluir solar/consumo, não excluir ativo/ausente, preservação de registro ativo).
- `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon`: **NÃO INICIOU**, pois Gradle Wrapper tentou baixar `gradle-8.11.1-bin.zip` e houve `UnknownHostException: services.gradle.org`. Sem APK compilado.
- Kotlin compiler PSI: **45 arquivos, 0 erros sintáticos** (não verifica referências Android/Compose).

## Pendências

- Validar Compose/Room via Gradle no CI, incluindo exclusão persistente dos dois tipos de teste, ação de cancelamento e atualização do histórico e exportação de backup após excluir.
- Testar em celular real acessibilidade/fontes grandes, orientação, modo AMOLED e exclusão acidental.
- Confirmar atualização sobre alpha.25 com a mesma assinatura, sem desinstalar antes de exportar backup.
