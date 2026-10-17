# Pulso Bateria

**0.1.0-alpha.36 • versionCode 36** — Android nativo, Kotlin e Jetpack Compose.
Android 8.0/API 26 ou superior. Identidade: `br.com.pulsobateria`.

## Novidades da alpha.36 — Multiaplicativos na API Labs

- Até 20 aplicativos autorizados, cada qual com liga/desliga, assinatura e histórico de último teste próprio; revogação individual/global com confirmação.
- Clientes instalados anteriormente na alpha.35 continuam pareados após atualizar, sem resetar o último ID de teste quando o certificado é o mesmo.
- A API AIDL v1 permanece compatível; chamadas concorrentes serializadas, UID compartilhado rejeitado e testes manuais protegidos.
- Monitoramento não é ligado pela API; apenas um teste Labs pode estar ativo por vez. Não houve alteração no cálculo de corrente, no Room v8, backup JSON v6, nem na ordem das abas.
- A capacidade útil configurável em Configurações > Estimativa de corrente **já** era usada como fallback de `CurrentTracker` quando fontes de corrente oficiais não eram utilizáveis e a taxa percentual tinha dados suficientes. A corrente calculada é identificada como estimativa, não medição do sensor.

## Novidades da alpha.35 — API local dos Labs

- API Android AIDL v1 para um app pareado (como Vigia IA) iniciar testes Solar/Consumo, consultar estado, encerrar seu próprio teste e recuperar relatório.
- Autorize explicitamente o aplicativo em **Config. → Integração com aplicativos**, com verificação de UID/assinatura e revogação. Desativada por padrão, sem INTERNET.
- O Pulso não ativa seu foreground service quando recebe o comando externo; requer monitoramento ativo. Contrato e exemplo do cliente em `docs/INTEGRACAO_VIGIA_IA.md`.
- Sem alteração de Room v8, backup v6 ou cálculos de bateria. **Vigia IA ainda precisa implementar o cliente.**

## Histórico da alpha.34 — Sessões restauradas na navegação

- Barra inferior: **Início, Sessões, Labs e Config.**; a aba **Uso** não aparece mais.
- A aba Sessões volta a exibir diretamente o histórico completo com agrupamento, filtros, gráficos, comparação e relatórios. O botão **Histórico de sessões** da Home agora seleciona essa aba.
- As melhorias visuais dos Labs da alpha.33 foram preservadas integralmente.
- O código da antiga tela Uso permanece no repositório, sem entrada na navegação, para evitar excluir funcionalidades/dados sem solicitação.
- Alteração de UI e documentação: Room v8, backup JSON v6, dados das sessões, permissões, assinatura, medições e serviço mantidos.

## Novidades da alpha.33 — Labs mais compacto e navegação com quatro abas (histórico)

- Barra inferior agora tem **Início, Uso, Labs e Config.**; a aba Sessões foi removida. O histórico segue acessível por **Início → Histórico de sessões**, mantendo filtros, agrupamentos, comparação, relatórios e gráficos.
- Labs: menu direto com Teste Solar, Teste de Consumo, três resultados recentes e acesso ao histórico completo.
- Configuração compacta: opções de céu, painel e duração com menos altura, preservando tempo manual, intervalos personalizados e os avisos sobre dados insuficientes.
- Condições opcionais de tela, câmera/vídeo, Wi-Fi, gravação e observações ficam recolhidas por padrão; os valores informados não são apagados quando recolhidos.
- **Iniciar teste** fica fixo no rodapé nas telas de configuração. Campos numéricos fora dos limites são rejeitados sem iniciar o experimento.
- Room v8, backup JSON v6, monitoramento e cálculos preservados. APK e aparência no Android ainda dependem do build e testes reais.

## Novidades da alpha.32 — Sessões próximas reunidas na visualização

- Sessões consecutivas de carga ou descarga podem ser mostradas em um cartão único, com opção de **Agrupar sessões próximas** habilitada por padrão. Um clique em **Ver originais** revela todos os cartões e gráficos individuais.
- Agrupamento conservador: mesmas direção e porcentagem na fronteira, lacuna de 0 a 3 minutos, motivo de interrupção compatível e mesma fonte/nome de carregador na carga. Filtra sempre sobre o histórico completo para não esconder alterações de estado.
- A duração agrupada soma **somente os tempos observados**. O tempo sem amostras entre os registros é informado separadamente, não entra na taxa calculada e nunca gera leituras artificiais.
- Selo **ATUAL** junto ao estado da sessão somente quando existe leitura recente e registro aberto, coerente com o estado do Android. Sessões pausadas ou antigas não ganham esse selo.
- Room v8, backup JSON v6, previsão histórica, relatórios, gráficos, comparação, monitoramento e Secrets inalterados. A união é **visual**, reversível com a chave e não modifica IDs nem dados salvos.

## Novidades da alpha.31 — Cartões inferiores compactos

- A seção inferior da Home agora segue o mesmo padrão compacto da alpha.30, com menos espaços vazios, sem esconder indicadores técnicos.
- **Corrente da bateria:** valor, categoria da média e fonte da medição aparecem agrupados; o ícone de ajuda e a expansão **Detalhes da medição** preservam acesso às médias do contador e do Android.
- **Pulso recente:** gráfico de percentual com menor altura, mantendo escala, leitura inicial/final e recorte dos últimos 30 minutos.
- **Consumo por período:** filtros interativos de 5/20/60 min, média em mA, carga em mAh, cobertura de dados e atalho **Análise completa** reorganizados com layout responsivo para fontes maiores.
- Alteração somente de interface. Todos os cálculos, amostragens, dados e demais telas, Room v8, backup JSON v6 e segurança do monitoramento permanecem inalterados.

## Novidades da alpha.30 — Home compacta

- A Home ganhou um visual **compacto e mais hierárquico**: o estado da bateria, a fonte, o percentual atual e o tempo observado aparecem no topo em um cartão-resumo com medidor menor.
- O cartão de **Autonomia estimada** / **Recarga estimada** sobe na tela e passa a destacar o horário previsto, o tempo restante e a média histórica em dois blocos compactos, reduzindo rolagem sem remover informações.
- Os cartões técnicos, o gráfico recente, a análise, o histórico, Labs, configurações, Room v8 e backup JSON v6 permanecem preservados; a mudança é visual e de organização.
- Sem alterações nos cálculos do `core`, nas permissões, no serviço ou na estrutura do banco. A próxima etapa continua sendo validar a renderização real no aparelho e no GitHub Actions.

## Novidades da alpha.29 — Recarga histórica na Home

- Carregando: horário aproximado de 100%, tempo restante e média **equivalente** de 0 a 100%, calculada só sobre sessões CHARGING reais (recargas parciais contam). Sessões de descarga, FULL/IDLE e leituras inválidas não contam.
- Previsão combina ritmo local (70%) e histórico (30%) quando disponíveis; recargas recentes e mesmo **tipo de fonte** (USB/tomada/sem fio) têm preferência com dados suficientes. Tipo de fonte não identifica carregador, cabo ou painel solar específico.
- Em 80..100%, usa uma taxa separada apenas se houver sessões inteiras nessa faixa; sem dados, mantém extrapolação sinalizada. Caso histórico e taxa não estejam disponíveis, conserva fallback local preexistente do Android.
- Sem mudanças no Room v8, backup v6, sensores, serviço, permissões ou Secrets. Continua uma estimativa e não um horário garantido.

## Novidades da alpha.28 — Autonomia histórica na Home

- A Home mostra **hora prevista para acabar a bateria** (arredondada para cima a cada 5 minutos), **tempo restante** e **média teórica com 100%** a partir das sessões reais de descarga preservadas (até 500).
- Média por tempo observado das sessões elegíveis; ignora cargas, variações incoerentes e durações com evidência insuficiente. As 8 descargas válidas mais recentes nos últimos 7 dias ajudam a adaptar a previsão. Quando há taxa atual confiável, seu peso é 70% e o histórico 30%.
- Faltando dados ou leitura recente, sinaliza indisponibilidade; a recarga originalmente mantinha a previsão local preexistente até 100% (agora complementada na alpha.29). Sem migração: Room v8 e backup JSON v6 inalterados.

## Novidades da alpha.27 — Relatórios Labs, temperatura e cópia

- Resultado expandido segue a referência visual: quatro métricas grandes em grade, duração em cápsula, barra de cobertura dos intervalos observados, temperatura inicial/final/máxima, início e fim lado a lado, ações Recolher / Copiar / Excluir com confirmação.
- Condições opcionais informadas manualmente antes do teste: tela, câmera/vídeo, transmissão Wi-Fi, gravação em arquivo e notas. Não infere automaticamente que outros apps estão ativos.
- Medidas térmicas opcionais obtidas exclusivamente do `BatteryRepository` já existente, sem adicionar sensor/serviço ou supor dados faltantes.
- Room v8, migração não destrutiva 7→8; backup JSON v6 com compatibilidade para backups anteriores.
- A versão é **código-fonte**, sem compilação Android certificada. Ver `docs/VALIDATION.md`.

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

Nas versões anteriores com a aba **Uso**, toque em **Abrir acesso ao uso** e autorize Pulso Bateria nas configurações especiais do Android. Volte ao app e use **Atualizar** se necessário. O acesso não é obrigatório para nenhuma medição de bateria. A API `UsageStatsManager` informa eventos de primeiro plano e mudanças de estado interativo da tela, mas não mede o consumo individual em mAh, nem possui eventos históricos ilimitados. Nomes e ícones de apps iniciáveis são consultados pelo sistema com visibilidade limitada; pacotes ocultos ou sem launcher podem permanecer sem nome disponível. A análise combina eventos (últimas 24h) com amostras do Room já armazenadas, ignora pausas/lacunas superiores a 3 minutos, não conecta sessões distintas nem recupera intervalos sem observação. A classificação por tela pode ficar incompleta se os eventos não estiverem disponíveis. Os resultados são locais e não exigem Internet, root ou permissões privilegiadas.

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
