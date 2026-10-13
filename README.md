# Pulso Bateria

**0.1.0-alpha.16 • versionCode 16** — Android nativo, Kotlin e Jetpack Compose.
Android 8.0/API 26 ou superior. Identidade: `br.com.pulsobateria`.

## Funcionalidades

- **Nova aba Apps (alpha.16):** acesso especial opcional às estatísticas de uso do Android (`PACKAGE_USAGE_STATS`, habilitado manualmente), ranking por tempo aproximado em primeiro plano e gráfico de atividade por blocos de duas horas nas últimas 24 h. Sem estimar mAh individuais de aplicativos: a queda total da bateria é correlacionada apenas a intervalos com amostras Room preservadas e classificação de tela do sistema. A UI explicita cobertura parcial, ausência de eventos e incertezas. A consulta ocorre apenas quando a aba é aberta ou atualizada, sem monitoramento adicional nem dados enviados pela rede.
- Porcentagem, estado, temperatura, fonte externa e corrente líquida da bateria.
- CURRENT_NOW e CURRENT_AVERAGE em µA, convertidos em mA. Zero e pequenas correntes não são automaticamente descartados. Uma fonte cujo sinal contradiz carga/descarga é preservada no diagnóstico, mas não apresentada como consumo/carga confiável; outras fontes compatíveis podem fornecer fallback.
- Hipóteses de correção OEM da instantânea exigem três blocos estáveis com integral temporal comparada ao contador. Não se calibra a média do hardware usando uma janela desconhecida.
- Média Android e média calculada do CHARGE_COUNTER exibidas separadamente. Não são médias próprias de 5 ou 20 minutos.
- Capacidade útil opcional (500–30.000 mAh), com 10 opções de 2.500 a 7.000 mAh, além de campo livre formatado com milhar (ex.: 5.000). Estimativas por %/h são identificadas, sem capacidade inventada.
- Taxa em pontos percentuais por hora e estimativas aproximadas após pelo menos cinco minutos e dois pontos percentuais. Não há modelo térmico nem previsão garantida da fase final de carga.
- Sessões Room: até 500 resumos, filtros, agrupamento por data, cartões expansíveis e gráfico individual. A alpha.15 substitui `p.p.` por descrições claras de pontos adicionados/consumidos, destaca duração observada e ritmo médio em pontos/h e indica **possível retomada** de sessões adjacentes (mesmo estado/nível, intervalo de até 3 min). Não une os registros nem atribui consumo a lacunas. Até 480 pontos por sessão (primeiro e 479 mais recentes). Pontos são reduzidos: não servem para reconstituir a integral completa da corrente.
- Gráfico ao vivo de até 30 minutos. Pausas curtas podem retomar sessões; tempo sem observação é excluído da duração. O gráfico persistido não possui marcador de pausa curta: veja limitações no relatório.
- Temas Sistema/AMOLED/Claro; °C/°F; intervalos 15/30/60 s, padrão 30. Eventos do Android podem antecipar leituras; suspensão pode atrasá-las.
- Notificação opcional e serviço specialUse iniciado pelo usuário; ao ativar monitoramento contínuo, pode continuar após fechar a interface. O sistema pode recriar o serviço via START_STICKY. Com opção separada "Iniciar com o sistema", tenta retomar após reiniciar o aparelho; tenta também após atualização. Essas tentativas dependem das permissões e restrições do Android/fabricante, sem garantia de serviço ininterrupto.
- Backup manual JSON v1, checksum SHA-256, seletor de arquivos, restauração confirmada pelo usuário. O arquivo não é criptografado. Calibração e ativação automática de serviço não são importadas.
- Sem INTERNET, anúncios, analytics, conta, root ou wake locks. A alpha.13 adiciona somente a permissão normal RECEIVE_BOOT_COMPLETED para a retomada opcional após reiniciar.

## Aplicativos e limites (alpha.16)

Na aba **Apps**, toque em **Abrir acesso ao uso** e autorize Pulso Bateria nas configurações especiais do Android. Volte ao app e use **Atualizar** se necessário. O acesso não é obrigatório para nenhuma medição de bateria. A API `UsageStatsManager` informa eventos de primeiro plano e mudanças de estado interativo da tela, mas não mede o consumo individual em mAh, nem possui eventos históricos ilimitados. Nome de aplicativo pode aparecer como pacote por limitações de visibilidade do Android. A análise combina eventos (últimas 24h) com amostras do Room já armazenadas, ignora pausas/lacunas superiores a 3 minutos, não conecta sessões distintas nem recupera intervalos sem observação. A classificação por tela pode ficar incompleta se os eventos não estiverem disponíveis. Os resultados são locais e não exigem Internet, root ou permissões privilegiadas.

## Histórico mais legível na alpha.15

Os cartões de sessões agora mostram `27 pontos consumidos`, `30 pontos adicionados` ou `Sem variação`, e apresentam duração e ritmo médio com rótulos completos. Em sessões vizinhas do mesmo modo e percentual de fronteira, com diferença temporal de até três minutos e sem mudança de estado conhecida, aparece **Possível retomada**. Trata-se de um **indício** calculado a partir dos resumos, não de um vínculo confirmado. Os registros e as curvas permanecem separados, pois reiniciar o serviço/processo ou perder leituras interrompe a observação real.

A alteração da alpha.15 foi de interface e classificação visual. A alpha.16 inclui consultas de leitura ao mesmo Room v3, sem alterar o schema, a estrutura de backup, a coleta periódica ou a retenção.

## Correção do workflow da alpha.13

O workflow 13 compilou e iniciou 114 testes no módulo core, mas um teste de regressão legado
(`AuditRegressionTest.netDischargeIsNotFlippedByChargingStatusAlone`) ainda esperava que uma corrente negativa
fosse exibida como medição principal durante o estado `CHARGING`. Desde a alpha.13, o contrato deliberado
é manter essa corrente negativa **no diagnóstico**, e não apresentá-la como carregamento confiável.
A alpha.14 atualiza a asserção para conferir o valor bruto, o valor normalizado, a ausência de
inversão indevida e a supressão segura da apresentação. Também atualiza um teste legado da notificação
que ainda esperava exibir +400 mA durante uma descarga. Não muda a lógica de sensores nem o Room.

## Auditoria e validação

Leia `docs/AUDIT-alpha.11-to-alpha.12.md`: diagnóstico com evidências da alpha.11, causas possíveis das diferenças com AccuBattery, correções, propostas de médias temporais/mAh e protocolo de teste no aparelho.

A alpha.15 teve 141 testes Kotlin/JVM e 8 testes SQLite aprovados localmente. A alpha.16 acrescenta testes para agregação de eventos, intervalos de duas horas, transições entre tela ligada/desligada e proteção contra lacunas de amostragem. Consulte `docs/VALIDATION.md` para os resultados efetivamente executados e pendências Android/Compose.

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

Morte do processo fecha sessões abertas como interrompidas na reabertura, sem inventar observações. Com o monitoramento contínuo habilitado, o Android pode recriar o serviço START_STICKY. A opção separada Iniciar com o sistema permite uma tentativa no BOOT_COMPLETED; após atualização é feita outra tentativa se o monitoramento estava ativo. Se o Android impedir, o app pode tentar quando for aberto. Parar desativa a preferência. Não é garantido funcionamento ininterrupto; nem reboot, nem força-parada do usuário permitem coletar dados durante o período inativo.

Calibração antiga é invalidada porque usava critérios inadequados; somente esse dado derivado muda. Novo ajuste é vinculado a ID privado em noBackupFilesDir e fingerprint do firmware, expira em sete dias e precisa de confirmação atual após pausas. O ID não é enviado para fora do aparelho.

O checksum do backup detecta alteração acidental, não autentica quem criou o arquivo. Room e DataStore são armazenamentos distintos: uma restauração não é atomicamente única entre os dois. O Android pode limitar coleta em segundo plano. Essas condições estão detalhadas na auditoria.
