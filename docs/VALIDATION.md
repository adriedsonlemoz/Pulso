# Validação — Pulso Bateria 0.1.0-alpha.24 / versionCode 24

## Implementação

- A aba Labs abre em menu: Teste Solar / Teste de Consumo, últimos cinco experimentos e acesso ao histórico completo. Os controles de clima ficam somente na página Solar.
- Teste de Consumo exige leitura recente de bateria desconectada, mede queda em pontos percentuais (não mAh), permite duração programada com padrão de 120 min e finaliza automaticamente ao prazo quando o Android executa o processo.
- Leitura de `EXTRA_PLUGGED` permite sinalizar interferência por fonte externa observada, inclusive nos estados de carga completa/ocioso. Pares com mudanças, lacunas ou relógios incoerentes não viram tempo medido. Conexões entre amostras podem escapar da detecção.
- Teste Solar, relatório e notificações preservados. Room v7 com migração 6→7 e valores padrão para testes legados; backup JSON v5 com importação v1–v4.
- Reutiliza FGS especial existente, sem nova permissão/serviço, wakelock, internet, câmera ou sensor externo.

## Verificações executadas neste ambiente

- `python tools/validate_source.py`: **APROVADO** (arquivos, versão, Manifest, SHA-256 e CRC do Gradle Wrapper).
- `python tools/test_navigation_contract.py`: **11/11 APROVADOS** (contratos anteriores das telas).
- `python tools/test_storage_sql.py`: **14/14 APROVADOS**, incluindo migração 6→7, default SOLAR e persistência de testes DISCHARGE. São verificações SQLite locais, não migrações Android instrumentadas. Script legado pode produzir `ResourceWarning` de SQLite.
- `kotlinc` + execução Java de LabMath real: **13/13 verificações APROVADAS**, incluindo tempos desconectados, fonte externa, lacunas e queda em pontos percentuais.
- Parser PSI do compilador Kotlin: **44 arquivos Kotlin do projeto, zero erros de sintaxe**, sem resolver dependências Compose/Room.
- `./gradlew :core:test :app:assembleDebug --offline --no-daemon`: **NÃO EXECUTOU BUILD**: o wrapper tentou baixar gradle-8.11.1 e falhou com `UnknownHostException` em services.gradle.org. Não houve compilação Android, lint, APK nem testes instrumentados.

## Pendências em CI/aparelho

1. Rodar `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` e `:app:connectedDebugAndroidTest` com Android SDK e rede.
2. Obter e versionar somente `7.json` gerado pelo compilador KSP real; não escrever JSON fictício.
3. Instalar sobre a versão alpha.23 com banco Room v6; verificar migração e retenção do teste Solar anterior.
4. Testar duas horas com tela apagada, serviço autorizado, notificação permitida, Android restringindo background, processo interrompido e relógio ajustado.
5. Testar fonte externa presente antes do início (bloqueio), conectada durante o teste (aviso), conectada entre leituras (limitação) e porcentagem final defasada (aviso).
6. Verificar backup v5 exportado/restaurado, importação v1–v4 e compatibilidade dos relatórios existentes.

**Limitação:** prazo de finalização pode ser observado tardiamente pelo Android, e a notificação pode atrasar. Tempo sem dados permanece desconhecido. O monitoramento contínuo continua ativo até o usuário desligá-lo nas Configurações.
