# Pulso Bateria

**0.1.0-alpha.12 • versionCode 12** — Android nativo, Kotlin e Jetpack Compose.
Android 8.0/API 26 ou superior. Identidade: `br.com.pulsobateria`.

## Funcionalidades

- Porcentagem, estado, temperatura, fonte externa e corrente líquida da bateria.
- CURRENT_NOW e CURRENT_AVERAGE em µA, convertidos em mA. Zero e pequenas correntes não são automaticamente descartados. Um sinal diferente do status é informado no diagnóstico.
- Hipóteses de correção OEM da instantânea exigem três blocos estáveis com integral temporal comparada ao contador. Não se calibra a média do hardware usando uma janela desconhecida.
- Média Android e média calculada do CHARGE_COUNTER exibidas separadamente. Não são médias próprias de 5 ou 20 minutos.
- Capacidade útil opcional (500–30.000 mAh) permite estimativa identificada por %/h quando não há corrente disponível; nenhuma capacidade é inventada.
- Taxa em pontos percentuais por hora e estimativas aproximadas após pelo menos cinco minutos e dois pontos percentuais. Não há modelo térmico nem previsão garantida da fase final de carga.
- Sessões Room: até 500 resumos, filtros, agrupamento por data, cartões expansíveis e gráfico individual. Até 480 pontos por sessão (primeiro e 479 mais recentes). Pontos são reduzidos: não servem para reconstituir a integral completa da corrente.
- Gráfico ao vivo de até 30 minutos. Pausas curtas podem retomar sessões; tempo sem observação é excluído da duração. O gráfico persistido não possui marcador de pausa curta: veja limitações no relatório.
- Temas Sistema/AMOLED/Claro; °C/°F; intervalos 15/30/60 s, padrão 30. Eventos do Android podem antecipar leituras; suspensão pode atrasá-las.
- Notificação opcional, serviço specialUse iniciado pelo usuário, ação Parar e horário aproximado de carga arredondado para cima em cinco minutos.
- Backup manual JSON v1, checksum SHA-256, seletor de arquivos, restauração confirmada pelo usuário. O arquivo não é criptografado. Calibração e ativação automática de serviço não são importadas.
- Sem INTERNET, anúncios, analytics, conta, root, wake locks ou permissões novas.

## Auditoria e validação

Leia `docs/AUDIT-alpha.11-to-alpha.12.md`: diagnóstico com evidências da alpha.11, causas possíveis das diferenças com AccuBattery, correções, propostas de médias temporais/mAh e protocolo de teste no aparelho.

**132 testes Kotlin/JUnit e 8 SQLite passaram localmente.** Oito testes de regressão reproduzem falhas no código original e passam no corrigido. Compilação Android, lint, testes instrumentados e teste de atualização ainda estão pendentes. Esta entrega contém código-fonte, não APK validado.

## Gerar APK pelo GitHub Manager

1. Importe o ZIP na raiz do repositório, incluindo `.github` e Wrapper.
2. Preserve os quatro Secrets existentes: KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS e KEY_PASSWORD. Não coloque os valores no repositório.
3. Execute **Android • Pulso Bateria** e confira testes, lint e assinatura. Baixe o APK da Release ou de Actions → Artifacts.

A assinatura não foi alterada. Sem Secrets estáveis, o Debug usa chave temporária e pode não atualizar a instalação anterior. Não desinstale para contornar conflito de assinatura sem exportar o histórico. A chave introduzida na alpha.8 não garante compatibilidade com APKs anteriores. Veja `docs/BACKUP_AND_SIGNING.md`.

## Compilar e testar

Java 17, SDK 35, Build Tools 35.0.0, ANDROID_HOME ou local.properties com sdk.dir:

```sh
chmod +x gradlew
python3 tools/validate_source.py
python3 tools/test_storage_sql.py
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

A última tarefa exige aparelho/emulador. Saída do app: `app/build/outputs/apk/debug/app-debug.apk`.
AGP 8.9.2, Gradle 8.11.1, Kotlin/Compose Compiler 2.1.20, Compose BOM 2025.04.01, KSP 2.1.20-1.0.32, Room 2.7.1. Dependências não foram atualizadas nesta revisão. A primeira compilação exige rede; o app instalado funciona offline.

`tools/run_local_jvm_tests.py --toolchain-dir DIRETORIO` executa testes reais com um compilador/JUnit locais, sem Gradle. Não valida o app Android. A lista exata de JARs está no script; nenhum desses JARs de teste é incluído no ZIP. Evidências em `docs/test-results`.

## Persistência e limites

Room permanece v3, com migrações explícitas 1→2→3. Não há migração destrutiva. Resumo e ponto são gravados em transação. DataStore preserva configurações. Atualização com identidade e assinatura compatíveis deve manter os dados; validação no aparelho ainda é necessária.

Morte do processo fecha sessões abertas como interrompidas na reabertura. Não há boot receiver nem reinício automático do serviço. Reative o monitoramento após reinício. Não se reconstrói o período em que o app não observou a bateria.

Calibração antiga é invalidada porque usava critérios inadequados; somente esse dado derivado muda. Novo ajuste é vinculado a ID privado em noBackupFilesDir e fingerprint do firmware, expira em sete dias e precisa de confirmação atual após pausas. O ID não é enviado para fora do aparelho.

O checksum do backup detecta alteração acidental, não autentica quem criou o arquivo. Room e DataStore são armazenamentos distintos: uma restauração não é atomicamente única entre os dois. O Android pode limitar coleta em segundo plano. Essas condições estão detalhadas na auditoria.
