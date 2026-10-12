# Auditoria técnica — Pulso Bateria alpha.11 → alpha.12

**Data:** 12/10/2026. **Entrada analisada:** 0.1.0-alpha.11 / versionCode 11.
**Entrega corrigida:** 0.1.0-alpha.12 / versionCode 12.

SHA-256 do ZIP original: `c239b677cb3781fbe917fbe48263c2d7000e54952bb6194b4a350ea4d6acfcfc`.

## 1. Conclusão e alcance

Foram encontrados defeitos reais de interpretação, calibração e apresentação de corrente. **Não foi encontrado erro dimensional nas duas conversões principais**: µA/1.000 e ΔµAh×3.600/Δt_ms. A diferença para o AccuBattery não autoriza multiplicar leituras por um fator arbitrário.

O código original foi inspecionado antes das alterações: AGENTS integral, README, regras de medição/versionamento, core, repositório, serviço, interface, Room, DataStore, backup, manifesto, testes e workflow. O diagnóstico foi apresentado antes da implementação. Oito regressões portáveis foram executadas contra o código original: **8 falharam na alpha.11 e passam na alpha.12**. Os 96 testes antigos passavam: eles não cobriam os contratos corrigidos e alguns validavam o comportamento inadequado.

Esta auditoria valida algoritmos sob entradas controladas. Não mede o sensor físico do celular, não verifica a implementação do AccuBattery e não certifica precisão metrológica. APK/Compose/Room no Android ainda dependem de build e execução em ambiente Android.

## 2. Diagnóstico com evidências no código original

Caminhos/linhas abaixo referem-se à **alpha.11 do ZIP recebido**, não às linhas alteradas na alpha.12. P1=alto impacto na interpretação/integridade; P2=impacto moderado ou dependente de condição; P3=limitação/documentação/evolução. Não foi confirmada vulnerabilidade crítica ou perda sistemática de histórico ao atualizar com assinatura compatível.

### P1-01 — Sinal inferido do status, sem referência elétrica

`core/.../CurrentTracking.kt`, `CurrentProbe.observe`, linhas 98–106: `expectedSign` é +1 em CHARGING e −1 em DISCHARGING; após quatro leituras em 60 s, `flipped=proposedFlip`. Não se consulta o contador para comprovar a inversão. Um status atrasado/inconsistente ou um comportamento OEM não documentado pode converter saída em entrada. Os caminhos `BatteryMath.currentMa` e `NotificationText.format` também filtravam pelo status.

**Reprodução:** cinco leituras −1.500.000 µA, CHARGING, sem contador. A alpha.11 termina exibindo +1.500 mA. Correção: o sinal informado permanece; divergência com o status é diagnosticada. Exceção OEM exige integral temporal corroborada pelo contador, sem usar o status para decidir o sinal.

### P1-02 — Corrente válida pequena descartada

`CurrentTracking.kt:115–149`: magnitude≤30.000 só é aceita com unidade previamente confirmada ou referência suficiente. Pela API, 1.600 µA representa 1,6 mA; magnitude pequena não torna a unidade declarada ambígua. **Reprodução:** primeira leitura 1.600 retorna indisponível. Correção: aplicar µA como padrão, reservando mudança de escala para evidência temporal explícita.

### P1-03 — Comparação entre intervalos incompatíveis invalida unidade

`CurrentTracking.kt:121–147`: compara o bruto atual à média do contador de até dez minutos; aceita candidato pela razão 0,25..4 e invalida unidade fora de 0,1..10. Uma corrente instantânea pequena após carga forte é fisicamente diferente de sua média histórica.

**Reprodução:** corrente de 1.500 mA com contador compatível, seguida de 10 mA. A alpha.11 abandona a instantânea e seleciona a média antiga. Correção: comparar a integral de CURRENT_NOW ao contador nos mesmos extremos, sem usar uma leitura pontual para recalibrar unidade.

### P1-04 — Persistência da calibração não cumpre o contrato anunciado

`CurrentTracking.kt:168–176`: registro contém somente horário e dois códigos. Não há vínculo ao firmware/instalação; aceita códigos de dois caracteres parcialmente reconhecidos; prazo só é verificado ao carregar. `BatteryRepository.kt:30,174–178` usa `stateIn(..., Settings())` e marca carregado mesmo se a primeira emissão persistida ainda não chegou. O bug de corrida é identificado pela ordem do código; **não foi reproduzido em Android**.

A evidência de sinal/unidade também pode sobreviver a leituras inválidas porque o retorno ocorre antes de limpar candidatos. Correção: formato v2 validado estritamente, escopo local+firmware, leitura efetiva do DataStore antes de concluir carga, expiração durante observação, reset de evidência e revalidação após interrupções. Somente a calibração derivada antiga é invalidada; configurações e histórico não são apagados.

### P2-01 — Média Android perdida na interface

`CurrentResult`, `CurrentTracker.observe`: a média convertida só é usada como fallback e não é preservada separadamente. `NotificationText.kt:85–88` usa exclusivamente `counterMeanMa` e ainda a oculta quando difere menos de 10 mA da instantânea. Assim, “sem média” não comprova ausência de CURRENT_AVERAGE.

Correção: campos próprios para instantânea, média Android e média do contador; fontes separadas na tela/notificação, sem supressão por proximidade numérica. A janela efetiva do contador é mostrada. Não se afirma que o AccuBattery usa CURRENT_AVERAGE: sua fonte não foi determinada.

### P2-02 — Zero, FULL e IDLE rejeitados em bloco

`CurrentTracking.kt:90,111`: zero é sempre considerado não confirmado, e estados diferentes de carga/descarga retornam indisponível. `NotificationText.kt:53` esconde módulos abaixo de 0,5 mA. Zero pode ser leitura real; a sentinela documentada para o target deste app é Int.MIN_VALUE. Correção: preservar zero e pequenas correntes, identificando limitações; FULL/IDLE podem apresentar a corrente informada. Não se certifica que todo zero OEM seja uma medição verdadeira.

### P2-03 — Contador força direção pelo status e dilui saltos

`ChargeCounterTracker.observe`, `CurrentTracking.kt:27–47`: contador diminuindo em CHARGING é reiniciado sempre. O limite de 30 A é aplicado à diferença da janela inteira, permitindo diluir salto curto. **Reprodução:** +100.000 µAh em 1 s implica 360.000 mA, mas o cálculo em 91 s é aceito na alpha.11.

Correção: validar cada incremento, relógio e lacuna; permitir sinal líquido observado; reiniciar janela em mudança de direção. Limitação: um reset pequeno/ajuste do medidor que permaneça plausível pode não ser distinguido de fluxo real.

### P2-04 — Resumo e ponto sem transação conjunta

`BatteryRepository.kt:165,186–188`: `dao.update(session)` e `insertSample`/retenção são operações separadas. Morte do processo ou falha entre elas pode deixar resumo avançado sem ponto correspondente. Não há evidência de que isso causou perda no aparelho do usuário.

Correção: `SessionDao.saveObservation`, com `@Transaction`, agrupa inserção/atualização do resumo, amostra e retenção; o objeto ativo só é avançado após commit. Sem nova tabela, sem migração destrutiva, sem reescrever registros antigos.

### P2-05 — Restauração pode aguardar confirmação indefinidamente

`BatteryRepository.freezeForRestore` cria um Deferred; se `close()` lança exceção, o catch externo só atualiza a mensagem e não conclui o Deferred. Correção: confirmação pertence ao comando, recebe a exceção e a retomada só limpa o estado quando o congelamento ocorreu. ViewModel impede restauração duplicada enquanto ocupado. Falha Android real de armazenamento continua pendente de injeção instrumentada.

### P2-06 — Coerência de backup incompleta e dois armazenamentos

`BackupManager.inspect`, linhas 98–130, valida IDs, limites e checksum, mas não relaciona contagem de temperaturas às amostras nem o horário do ponto ao período da sessão. Correção: validações compartilhadas em `HistoryValidation`; duração monotônica **não** é limitada artificialmente pela diferença de horários civis.

`BackupManager.restore:138–146`: Room é transacional, mas DataStore é atualizado depois. **Limitação mantida:** falha após o commit pode deixar histórico restaurado com preferências anteriores. A mensagem informa conferir ambos; não se promete atomicidade entre sistemas. Uma recuperação com journal/outbox seria evolução adicional.

### P2-07 — Horário previsto pode ficar no passado; relógio regressivo

`NotificationText.kt:30`: arredonda ao múltiplo mais próximo. Às 14h21, ETA de 30 s podia aparecer como 14h20. Corrigido para arredondamento para cima. `MonitorService` usava horário da renderização, podendo deslocar ETA ao atualizar preferências sem nova medição; agora usa o horário da leitura.

`BatteryMath.startsNewSession` só detectava diferença civil/monotônica acima de 60 s. Pequena regressão civil podia gerar horários incoerentes e impedir inserção de ponto. Agora regressão civil também separa sessões e impede retomada de pausa.

### P3 — Limitações confirmadas, não confundidas com erros de conversão

- Temperatura média usa soma/contagem; não é média temporal. Rótulo tornou-se explícito.
- Não existem médias próprias de corrente em 5/20 min, mAh integrados nem acumulador de corrente da sessão completa.
- Pontos salvos são reduzidos e limitados a 480; bruto, tempo monotônico, hipótese de calibração e seu histórico não são persistidos em cada ponto.
- Retomada em até 90 s exclui tempo parado da duração, mas Room v3 não guarda marcador de segmento. O gráfico salvo pode ligar visualmente uma pausa curta. O gráfico não é base válida para integrar mAh.
- A soma das variações percentuais não vistas numa pausa é desconhecida, mesmo se o percentual no retorno for igual.
- DataStore captura IOException e emite defaults; não há diagnóstico específico de corrupção/erro persistente. Isso não prova exclusão física das configurações, mas pode apresentar valores padrão durante falha.
- JSON exportado de schema Room não veio no ZIP; SQL versionado existe. Nenhum JSON de schema foi inventado.

## 3. O que explica — e o que não explica — a comparação com AccuBattery

| Observação fornecida | Conclusão sustentável |
|---|---|
| +437 versus +822 mA | Razão ≈1,881. Não corresponde ao fator 1.000 de µA/mA. Pode envolver instantes/fontes/filtros distintos; os números sozinhos não escolhem a causa. |
| −161 versus −346 mA | Razão dos módulos ≈2,149, diferente da comparação na carga. Não há evidência de ganho fixo. |
| Média −192 versus −583 mA | Razão ≈3,036. A alpha.11 usa média do contador na notificação; janela do outro app não foi identificada. Não são grandezas comprovadamente equivalentes. |
| Pulso sem média na carga | A perda da média Android convertida é um defeito confirmado capaz de explicar indisponibilidade na apresentação, se o hardware efetivamente a fornece. |
| −8,0 versus −7,0 %/h; 10h06 versus 11h30 | No Pulso, autonomia depende de %/h, não do mA instantâneo. A razão das taxas é ≈1,143 e a das autonomias ≈1,139; são compatíveis com janelas/arredondamentos diferentes, sem provar algoritmo idêntico. |

**Hipóteses a testar:** horários de leitura diferentes; alternância entre apps muda carga da CPU/tela; amostragem 15/30/60 s mais broadcasts; diferentes janelas/algoritmos; resolução e atualização do fuel gauge; pausas/suspensão; seleção de outra fonte; violações OEM de unidade/sinal.

**Não confirmado:** sensor invertido ou em mA neste celular; erro de calibração naquelas capturas; superioridade de qualquer app; coeficiente fixo de correção. 38,9 °C não demonstra por si só limitação térmica: faltam curva, estado do controlador e especificação do aparelho. Não foi inferido hardware dual-cell, capacidade nominal, tensão ou potência.

A documentação Android define µA para as correntes e janela de média dependente do hardware [1]. O AOSP registra que propriedades, resolução e frequência variam por medidor/configuração [2]. Isso justifica medições comparáveis no tempo; não justifica inventar correções.

## 4. Médias de 5/20 minutos, sessão e mAh: projeto recomendado

São tecnicamente adequadas **se os intervalos observados forem explícitos**. Não implementadas como funcionalidade nova na alpha.12, para não atribuir falsa integral ao histórico existente.

Para duas leituras válidas consecutivas, `Iᵢ` em mA, `tᵢ` monotônico em ms:

```text
Δtᵢ = tᵢ₊₁ − tᵢ
Aᵢ = (Iᵢ + Iᵢ₊₁)/2 × Δtᵢ              [mA·ms]
Q_observada = ΣAᵢ / 3.600.000             [mAh]
I_média_temporal = ΣAᵢ / ΣΔtᵢ            [mA]
Q_contador = (C_final − C_inicial)/1.000   [mAh], C em µAh
I_contador = ΔC × 3.600 / Δt_ms           [mA]
```

Trapézios supõem interpolação linear **dentro de intervalos admitidos**. Não revelam picos entre coletas. Não integrar intervalos com leitura inválida, mudança de fonte/calibração/estado, pausa, regressão ou lacuna excessiva. A ausência de dado não é zero. Não integrar CURRENT_AVERAGE como se fossem amostras instantâneas com janela conhecida; nem misturar estimativa por capacidade ao total medido.

**Exemplo calculado:** 1.000 mA em t=0; 2.000 mA em 30 s; 1.000 mA em 120 s. Área: 1.500×30 + 1.500×90 = 180.000 mA·s; média temporal=1.500 mA; carga=50 mAh. Média aritmética dos três valores=1.333,3 mA, resultado diferente.

| Indicador | Implementação adequada | Limitação a mostrar |
|---|---|---|
| Média 5 min | Recortar segmentos ao intervalo [agora−5 min, agora], preservando ponto anterior para interpolação da borda | Menos de 5 min de cobertura deve ser “parcial”, com duração coberta |
| Média 20 min | Mesmo método, janela maior | Reage mais lentamente a mudanças |
| Média da sessão | Soma integral/soma de intervalos válidos, acumuladores persistidos atomicamente | Só é “completa” se cobertura integral; sessão pode conter lacunas |
| mAh adicionados/consumidos | Integrar parcelas positiva e negativa separadas; dividir no cruzamento de zero | mAh líquidos não são consumo do sistema inteiro nem energia USB |
| Comparação com contador | Comparar Q integrada e Δcontador no mesmo segmento, informando ambos e diferença | Ambos podem ter erros comuns e contador pode recalibrar |
| Confiabilidade | Fonte, janela, cobertura, amostras válidas, maior intervalo, correções, divergências | Cobertura de 100% não significa precisão de 100% |

Persistir, em futura migração aditiva, acumuladores assinados/positivo/negativo, duração coberta, segmento e versão do algoritmo; guardar diagnóstico bruto limitado com horário civil+monotônico, fonte e decisão. Preservar sessões antigas como **sem esse cálculo**, sem preenchimento retroativo. Comparações relativas perto de carga zero devem ser suprimidas ou acompanhadas do erro absoluto, sem dividir por quase zero.

Casos de aceitação propostos para a funcionalidade futura: recorte exato em 5/20 min; dados a cada 15/30/60 s; intervalos irregulares; lacunas; reset de contador; mudança de fonte; fluxo que cruza zero; mesma integral com subdivisão equivalente; reinício após commit; cobertura parcial; retenção do gráfico sem alterar acumulador completo.

## 5. Autonomia e carregamento

`BatteryMath.rate` usa extremos de janela de até 30 min, sem regressão estatística, após ≥5 min, ≥3 pontos e ≥2 p.p. de variação consistente. O percentual tem resolução grosseira. A duração mínima reduz previsões prematuras, mas não é certificado de estabilidade.

Descarga: `T_h = p / |r|`. Carga: `T_h = (100−p) / r`, com `r` em p.p./h. Para exemplo hipotético de 80%, 8 p.p./h dá 10 h; 7 p.p./h dá aproximadamente 11h26. Não é estimativa do aparelho do usuário.

A previsão do Android é aproximada [1]. O app exige consistência com a tendência local (0,5×..4×) e limites de duração/taxa, mas aceita uma faixa ampla. Se rejeitada, recorre à extrapolação local. Esse fallback pode ser otimista perto de 100% e atrasado após aquecimento/redução de carga. Corrente instantânea não participa do ETA: corrigir mA não elimina essas limitações.

**Correções aplicadas:** apresentação reconhece extrapolação e ausência de modelo térmico; horário arredonda para cima a partir da leitura, com fuso/data. **Não implementado:** modelo de taper, ETA baseado em janela curta com histerese, faixa de incerteza ou validação térmica. Exigem dados reais e comparação prospectiva entre previsão e horário efetivo; não se inventa curva genérica do aparelho.

## 6. Persistência, segurança e compatibilidade

| Evento | Comportamento do código | Limite de evidência |
|---|---|---|
| Fechar tela/app | Room/DataStore preservam commits; coleta depende de Activity/serviço | Pendente exercício de ciclo de vida no Android |
| Morte de processo | Dados confirmados permanecem; sessão aberta fecha como interrompida ao reabrir | Última amostra não confirmada pode faltar; RAM é perdida |
| Reiniciar celular | Arquivos persistem; serviço START_NOT_STICKY não inicia sozinho | Sem coleta no período desligado; reativação manual |
| Atualizar APK compatível | Mesmo applicationId e assinatura permitem fluxo normal de atualização [5] | Nenhum APK/certificado do aparelho foi comparado aqui |
| Migração 1→2→3 | Adiciona tabela e colunas anuláveis, preservando conteúdo | SQL testado; validação Room Android pendente |
| Alpha.11→12 | Schema 3 e backup v1 mantidos; calibração antiga é invalidada | Instalação real e backup legado no aparelho pendentes |
| Restauração manual | Inspeção/checksum antes; banco substituído em transação mediante confirmação | Não é mesclagem; DataStore tem commit separado |
| Retenção | 500 resumos e 480 pontos/sessão; exclusão de sessão remove seus pontos | Histórico é deliberadamente limitado, não arquivo ilimitado |

Manifesto permanece sem INTERNET, somente notificações e permissões do foreground service specialUse. Não há serviço exportado, boot receiver, wake lock, nova assinatura ou credencial incluída. O workflow segue consumindo os Secrets existentes; ausência deles mantém o aviso de assinatura temporária.

Backup v1 continua lendo registros legados com campos opcionais nulos; a nova validação rejeita incoerências. SHA-256 verifica consistência, não autenticidade; JSON é legível e não criptografado. Limite de 64 MiB não elimina pressão de memória: payload, envelope e listas são materializados. Teste de backup no limite e falha de armazenamento segue pendente.

`allowBackup=false` não deve ser descrito como bloqueio universal de transferência entre dispositivos em todos os fabricantes [6]. A calibração nova usa identidade em noBackupFilesDir; não importa decisão de outro dispositivo por backup manual. Room usa migrações explícitas, sem fallback destrutivo [3]. DataStore oferece transação própria [4], sem estender a mesma transação ao Room.

## 7. Correções aplicadas versus melhorias opcionais

**Aplicadas:** conversão oficial inclusive pequenos valores/zero; sinal preservado e divergência indicada; ajuste OEM da instantânea com integral temporal/contador; calibração persistida validada por escopo/prazo/evidência atual; média Android separada; controle de saltos do contador; gravação conjunta; propagação de falha na restauração; validação de integridade de backup; regressão civil; horário previsto; documentação/versionamento e evidências no workflow.

**Opcionais, não implementadas:** médias próprias 5/20 min; corrente/mAh da sessão com cobertura persistida; logs brutos exportáveis por coleta; marcador de segmentos no gráfico salvo; temperatura temporal; restauração com journal entre Room/DataStore; streaming de backup; controle de fila de comandos sob eventos extremos; modelo de desaceleração térmica/final de carga; instrumentação automatizada com emulador no CI. São descritas separadamente para não atribuir capacidades inexistentes à alpha.12.

## 8. Testes executados e pendentes

| Verificação | Resultado real |
|---|---|
| Suíte original da alpha.11, compilada Kotlin 2.1.20/JVM 17/JUnit 4.13.2 | 96 testes aprovados |
| Oito casos de auditoria contra o core original | 8 falhas esperadas, reproduzindo defeitos; log incluído |
| Suíte final alpha.12, mesmo compilador/JUnit real | **132 testes aprovados**, zero falhas |
| SQL extraído das migrações/queries reais, SQLite Python | **8 testes aprovados**: migração 1/2→3, preservação, retenção, FK/cascade e rollback |
| Parser Kotlin PSI | 19 arquivos Kotlin, zero erros de sintaxe; sem validação de tipos Android |
| validate_source.py | Aprovado: versão, arquivos essenciais, XML, permissões, Wrapper SHA/CRC |
| Gradle :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug | Bloqueado antes de compilar: download do Gradle sem acesso à rede |
| Room instrumentado | 4 casos preparados (1→3, 2→3, reabertura v3, rollback resumo/ponto), **não executados** |
| APK, aparência Compose, serviço, backup completo no Android, atualização assinada | **Pendentes** |

Os 132 casos cobrem corrente positiva/negativa, sinal divergente/invertido, µA/mA, pequenas leituras/zero/sentinelas, calibração salva e sua revogação/expiração, interrupções, intervalos irregulares, mudança de estado, contador resetado/contraditório/salto, falta de capacidade, integral alinhada ao contador, relógios, sessões, consistência de backup e notificações. Médias de 5/20 minutos e mAh persistidos não estão implementados; seus casos de aceitação foram propostos, não declarados executados.

Reproduzir em ambiente com dependências: comandos do README. O teste SQLite executa SQL real, mas **não substitui** KSP, validação de schema e comportamento do DAO Room. Os JARs locais usados como ferramentas não são incluídos no código-fonte.

## 9. Protocolo no aparelho para identificar a causa real

1. Gerar APK com os mesmos Secrets. Comparar certificado/applicationId/versão ao instalado; exportar backup antes do teste de atualização. Não desinstalar para forçar instalação.
2. Anotar modelo exato e versão/build Android; selecionar intervalo de 15 ou 30 s e registrar se serviço estava ativo, tela ligada e apps abertos.
3. Em carga e descarga, registrar séries com horário da leitura, CURRENT_NOW bruto, CURRENT_AVERAGE bruto, CHARGE_COUNTER bruto, status, percentual, temperatura, fonte selecionada e decisão de calibração. O diagnóstico da alpha.12 mostra o instante e os valores; exportação automática de todos os brutos ainda não existe.
4. Comparar pelo menos 20 min de períodos alinhados, preservando condições de uso. Alternar telas altera a carga; capturas isoladas em momentos diferentes não validam desvio fixo.
5. Comparar mAh por contador com a integral temporal dos dados de corrente da mesma janela. Separar pausas, transições, zeros, sentinelas e mudanças de correção. Não usar corrente USB como verdade direta da corrente da bateria.
6. Repetir próximo de 100% e registrar temperatura/queda de ritmo, sem aquecer artificialmente o aparelho. Confrontar cada ETA com o horário real em que chega a 100%.
7. Validar preservação após fechar, matar processo, reiniciar e atualizar; conferir quantidade/IDs/períodos das sessões, pontos e preferências. Confirmar ausência de amostras inventadas nos intervalos sem observação.
8. Em emulador/perfil de teste, executar migrações, backup/restauração v1, arquivo adulterado, campos incoerentes, falha de escrita e backup próximo do limite; manter o histórico real do usuário protegido.

**Critério de conclusão:** build e testes Android aprovados, identidade de assinatura verificada e séries de medições reproduzíveis que expliquem a origem/intervalo dos números. Até lá, o software foi corrigido e testado sob entradas controladas, mas o sensor específico continua não validado.

## 10. Fontes oficiais consultadas

[1] [Android BatteryManager: correntes, contador, sentinelas e previsão](https://developer.android.com/reference/android/os/BatteryManager).

[2] [AOSP: medição de energia, propriedades/resolução/frequência do fuel gauge](https://source.android.com/docs/core/power/device). Exemplos de chips nessa página não foram atribuídos ao aparelho do usuário.

[3] [Android: migrações Room e risco de recriação destrutiva](https://developer.android.com/training/data-storage/room/migrating-db-versions).

[4] [Android DataStore: transações de leitura/modificação/gravação](https://developer.android.com/topic/libraries/architecture/datastore).

[5] [Android: assinatura de aplicativos](https://developer.android.com/studio/publish/app-signing).

[6] [Android: Auto Backup e diferenças de transferência entre dispositivos](https://developer.android.com/identity/data/autobackup).
