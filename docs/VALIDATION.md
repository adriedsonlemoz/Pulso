# Validação — Pulso Bateria 0.1.0-alpha.22 (22)

## Escopo

- Notificação compacta/expandida com corrente única no cabeçalho, origem explicitada em média alternativa e 0 mA ambíguo suprimido durante carga/descarga.
- Estado de 100% não implica FULL; aviso de temperatura no limite configurado, atualização silenciosa, botões Abrir/Parar.
- Não modificados cálculos da bateria, Room v5, backup JSON v3, identidade, permissões, banco ou Secrets.

## Verificações executadas

- **25/25 testes de notificação Kotlin/JVM aprovados** com compilação pelo `kotlinc` local 1.9 e executor de asserções compatível com JUnit. Incluem ETA, estados, zero, corrente contraditória, temperatura e deduplicação. Isso **não substitui** a compilação Gradle/Android.
- **12 testes SQLite aprovados**, incluindo migrações, integridade referencial e retenção (`tools/test_storage_sql.py`). O teste existente produz avisos Python `ResourceWarning` de conexões temporárias, sem falha nas asserções.
- `tools/validate_source.py` aprovado: versões, identidade, permissões, Wrapper oficial, manifesto e estrutura.
- `tools/test_navigation_contract.py`: 11 verificações estáticas aprovadas; telas anteriores preservadas.
- Revisão estática do `MonitorService`: limite de temperatura vem de `settings.hotBatteryAlert`; mesmo canal LOW/silencioso com `setOnlyAlertOnce`, ações Abrir/Parar; tratamento do serviço e START_STICKY preservados.

## Limitações e próximos testes

- A tentativa de compilar/rodar a **suíte Kotlin/JVM completa** ultrapassou o tempo disponível do compilador local; **não** foi aprovada ou contabilizada.
- Sem distribuição Gradle disponível localmente, **Android/Compose e build completo ainda não validados**, inclusive comportamento OEM da notificação. Validar `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` no GitHub Actions.
- Testar em aparelho real: notificação compacta/expandida em 97%, CHARGING 100%, FULL 100%, corrente zero com/sem média do contador, canal silenciado, aviso de temperatura e ação Parar sem reinício indesejado.
- Verificar formatação e corte de títulos em diferentes versões MIUI/Android; o sistema operacional controla parte do layout.
- ZIP: conferir CRC, nomes, versões e ausência de keystore/Secrets antes da entrega.
