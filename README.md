# Pulso Bateria

**0.1.0-alpha.26 • versionCode 26** — Android nativo, Kotlin e Jetpack Compose.
Android 8.0/API 26 ou superior. Identidade: `br.com.pulsobateria`.

## Novidades da alpha.26 — Histórico do Labs mais compacto

- Nos detalhes de cada resultado, horários de início e fim ficam lado a lado em uma única linha com data e hora abreviadas.
- O comando **Excluir** aparece junto de **Recolher**, apenas com o cartão expandido. A exclusão exige confirmação e remove somente o teste finalizado selecionado do Room; o teste em andamento não pode ser apagado.
- Cartões solares recolhidos destacam o nível inicial/final da bateria. Experimentos sem intervalo observado deixam o aviso de medição incompleta explícito, sem sugerir 0 min medidos com confiança.
- Sem migração: Room v7, backup JSON v5, monitoramento e sensores preservados. APK requer validação no CI e aparelho.

## Novidades da alpha.25 — Configurações organizadas e tema consistente

- **Configurações em menu:** categorias Preferências, Controle e segurança, Bateria e diagnóstico, Dados e aplicativo. Cada item apresenta o estado atual; ao tocar, exibe apenas os controles daquela categoria, com retorno à lista.
- **Seletores compactos:** temas, intervalo, unidades e limites de alerta usam opções independentes em cartões menores e com contraste de seleção, evitando grandes faixas segmentadas.
- **Menos rolagem:** informação técnica sobre monitoramento e calibração fica recolhida e diagnóstico bruto só aparece por ação explícita. Capacidade da bateria e backup continuam disponíveis em páginas próprias.
- **Paleta coerente:** tons de azul/verde nos componentes Material 3, sem indicadores lilases desconectados do tema. AMOLED permanece com fundo preto.
- **Preservação:** nenhuma mudança no BatteryManager, Lab, Room v7, backup JSON v5, serviço de monitoramento, permissões ou Secrets. Continuam necessários os testes de interface em aparelho e compilação Android.

## Novidades da alpha.24 — Labs com dois experimentos

- **Menu Labs:** cartões Teste Solar e Teste de Consumo, últimos cinco experimentos e página Ver todos.
- **Teste de Consumo:** exige desconectar fontes de alimentação; mede queda de pontos percentuais da bateria por duração selecionada (120 min por padrão) e salva automaticamente quando o prazo termina.
- **Interferência:** se o Android detectar fonte externa durante o experimento de descarga, o relatório avisa que o resultado não é um consumo puro. Lacunas e leitura final antiga ficam explícitas.
- **Teste Solar preservado:** condições do céu, média de carga e relatórios permanecem disponíveis.
- **Compatibilidade:** Room v7 com migração 6→7 e backup JSON v5 (importa v1 a v4). Sem serviços/permissões extras.

## Novidades da alpha.22 — notificação de monitoramento

- **Compacta:** percentual, temperatura e corrente aproveitável aparecem uma vez; linha de estado mostra previsão de 100% ou autonomia quando disponíveis. Sem repetição de corrente no painel expandido.
- **Expandida:** estado, origem da medição quando necessário e médias independentes somente quando divergirem de forma relevante. Média zero do Android durante recarga/descarga não aparece como média útil; valores brutos permanecem em Configurações > Diagnóstico.
- **Aos 100%:** apenas o estado `FULL` informado pelo Android recebe o rótulo **Bateria cheia**. `CHARGING` em 100% continua **Carregando • 100%**, sem previsão. Zero de corrente não prova ausência de consumo.
- **Temperatura:** acima do limite já configurável, aparece **Temperatura elevada** na própria notificação do serviço, sem disparar som ou substituir as regras opt-in de alertas inteligentes.
- **Ações e economia:** botão Abrir antes de Parar, canal silencioso e `onlyAlertOnce`; coleta, intervalos e frequência de amostragem inalterados.
- **Compatibilidade:** sem migração, mantém Room v5, backup JSON v3, assinatura/Secrets, histórico e identidade Android. Pendente compilação pelo GitHub Actions.

## Novidades da alpha.21 — navegação mais limpa

- **Início:** resumo compacto dos últimos 5/20/60 minutos com corrente, mAh e indicador de dados parciais. O botão **Ver análise completa** abre página com séries detalhadas de corrente, temperatura e bateria, sem eliminar dados técnicos; o gráfico recente foi reduzido.
- **Sessões:** os cartões e filtros ficam no início; os atalhos **Comparar** e **Relatórios** abrem páginas separadas, com botão Voltar e navegação inferior preservada. Campos para nomear carregadores aparecem somente ao expandir a identificação das recargas selecionadas.
- **Uso:** subseções **Resumo / Aplicativos / Noite**. Cada uma mostra apenas seus dados, sem longa rolagem por cartões de outros assuntos. Totais acumulados de vários ciclos podem superar 100% e são rotulados como tal.
- **Ajuda contextual:** explicações de sensores, cobertura, mAh, limites, comparação e eventos ficam em ícones de informação acessíveis. Alertas de indisponibilidade e cobertura parcial continuam visíveis sem abrir a ajuda.
- **Sem alterações nas medições ou persistência:** Room v5, backup JSON v3, assinatura, permissões, coleta e histórico preservados. Não são atribuídos mAh a aplicativos. A versão depende de compilação e teste visual no GitHub Actions/aparelho.

## Evolução alpha.17

- A aba **Apps** passou a **Uso** (ícone de barras) e a tela recebeu o título **Uso de aplicativos**. Os cartões tentam mostrar o nome e o ícone reais via `PackageManager`; quando o Android restringe a identificação, exibem um símbolo neutro e um único identificador técnico. Sem cadastro manual de nomes nem Internet.
- Visibilidade de pacotes restrita à consulta de aplicativos iniciáveis (`MAIN` + `LAUNCHER`) no `<queries>` do Manifest. **Não** utiliza `QUERY_ALL_PACKAGES`, nem exige nova permissão perigosa. Nomes e ícones de apps não iniciáveis/ocultos podem continuar indisponíveis.
- Variação de bateria apresentada em `%` na interface: **Caiu 20%**, **Carregou +20%**, **Tela ligada: caiu 4%** e ritmo em `%/h`. É a diferença de leituras na escala 0–100%, e não perda percentual relativa, mAh ou consumo atribuível a um aplicativo.
- O cartão da página inicial **Corrente da bateria** apresenta a média aproveitável, o período observado quando disponível e a origem da medição, deixando os valores das fontes separados em **Detalhes da medição**. Zero fornecido pelo Android durante carga/descarga não vira automaticamente uma média útil.
- Alteração de interface/consulta de informações dos aplicativos. **Room v3, backup JSON v1, assinatura, coleta e cálculo das médias permanecem inalterados.**

## Novidades da alpha.18

- **Médias de 5, 20 e 60 minutos:** corrente líquida, mAh integrados, fonte e cobertura. A cobertura parcial não é extrapolada à janela toda. Prioriza a variação de CHARGE_COUNTER, sem misturar fontes dentro da mesma soma.
- **Gráfico técnico** de corrente/temperatura/percentual para 1 e 6 horas, com lacunas sem linha. A série Room v4 é independente dos resumos de sessão e tem limites de retenção.
- **Backup v2** inclui essa série e importa backups v1. Não reconstrói amostras antigas. A migração 3→4 precisa ser validada em aparelho; manter assinatura estável e exportar backup antes da atualização.

## Novidades da alpha.19

- **Consumo noturno (aba Uso):** analisa a última noite local (22h–7h, ou até o horário atual se estiver em andamento), comparando quedas com tela apagada/ligada por eventos do Android. Exibe cobertura e deixa intervalos sem leitura fora dos resultados. Exige autorização opcional ao acesso de uso; os dados continuam locais.
- **Comparador de recargas (aba Sessões):** escolhe duas sessões encerradas, compara somente a mesma faixa percentual com telemetria contínua nas duas, duração, velocidade em %/h, temperatura média das amostras e mAh pelo contador de carga quando válido. Sem faixa adequada, mostra indisponibilidade em vez de inventar medidas.
- **Fonte e nome manual do carregador:** salva o tipo de fonte declarado pelo Android (Tomada, USB, Sem fio ou Fonte externa), sem inventar marca, cabo ou potência. O usuário pode identificar uma sessão com rótulo de até 40 caracteres. Alteração detectável do tipo de fonte encerra a sessão anterior.
- **Persistência:** Room v5 com migração explícita 4→5 (dois campos opcionais em `sessions`) e backup JSON v3 (importa v1/v2). Identidade e assinatura preservadas. Dados anteriores não recebem fonte ou rótulo retroativos.

## Novidades da alpha.20

- **Alertas inteligentes (Configurações):** quatro regras opt-in de bateria baixa, aquecimento, meta de carga e descarga rápida, com limiares ajustáveis. Exigem duas leituras consecutivas e dispõem de deduplicação persistida por episódio e intervalo mínimo de 1 hora. Notificações usam canal separado de importância padrão e respeitam permissões Android. Não interrompem o carregamento, não iniciam monitoramento oculto e só funcionam durante a observação ativa.
- **Relatório diário e dos últimos sete dias (Sessões):** resume a queda e carga em %, tempo de observação, sessões de recarga e maior temperatura, com barras diárias. Calcula sobre `telemetry_samples` persistidos, sem completar lacunas acima de três minutos ou cruzamentos de meia-noite; não atribui gasto a aplicativos. Histórico anterior à alpha.18 pode não ter telemetria.
- **Persistência:** sem migração; mantém Room v5 e JSON v3. Configurações de alertas ficam em DataStore, não são incluídas no backup e são desativadas após restauração por segurança. Não exige rede, root, wakelock ou nova permissão.

## Funcionalidades

- **Aba Uso (desde alpha.16):** acesso especial opcional às estatísticas de uso do Android (`PACKAGE_USAGE_STATS`, habilitado manualmente), ranking por tempo aproximado em primeiro plano e gráfico de atividade por blocos de duas horas nas últimas 24 h. Sem estimar mAh individuais de aplicativos: a queda total da bateria é correlacionada apenas a intervalos com amostras Room preservadas e classificação de tela do sistema. A UI explicita cobertura parcial, ausência de eventos e incertezas. A consulta ocorre apenas quando a aba é aberta ou atualizada, sem monitoramento adicional nem dados enviados pela rede.
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
- Backup manual JSON v4 (importa v1/v2/v3), checksum SHA-256, seletor de arquivos, restauração confirmada pelo usuário. O arquivo não é criptografado. Calibração e ativação automática de serviço não são importadas.
- Sem INTERNET, anúncios, analytics, conta, root ou wake locks. A alpha.13 adiciona somente a permissão normal RECEIVE_BOOT_COMPLETED para a retomada opcional após reiniciar.

## Aplicativos e limites (alpha.16)

Na aba **Uso**, toque em **Abrir acesso ao uso** e autorize Pulso Bateria nas configurações especiais do Android. Volte ao app e use **Atualizar** se necessário. O acesso não é obrigatório para nenhuma medição de bateria. A API `UsageStatsManager` informa eventos de primeiro plano e mudanças de estado interativo da tela, mas não mede o consumo individual em mAh, nem possui eventos históricos ilimitados. Nomes e ícones de apps iniciáveis são consultados pelo sistema com visibilidade limitada; pacotes ocultos ou sem launcher podem permanecer sem nome disponível. A análise combina eventos (últimas 24h) com amostras do Room já armazenadas, ignora pausas/lacunas superiores a 3 minutos, não conecta sessões distintas nem recupera intervalos sem observação. A classificação por tela pode ficar incompleta se os eventos não estiverem disponíveis. Os resultados são locais e não exigem Internet, root ou permissões privilegiadas.

## Histórico mais legível na alpha.15

Na alpha.17 os cartões mostram `Caiu 27%`, `Carregou +30%` ou `Sem variação`, e apresentam duração e ritmo médio em `%/h`. Os percentuais representam mudança de nível na escala da bateria, não mAh. Em sessões vizinhas do mesmo modo e percentual de fronteira, com diferença temporal de até três minutos e sem mudança de estado conhecida, aparece **Possível retomada**. Trata-se de um **indício** calculado a partir dos resumos, não de um vínculo confirmado. Os registros e as curvas permanecem separados, pois reiniciar o serviço/processo ou perder leituras interrompe a observação real.

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
