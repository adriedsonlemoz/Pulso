# Validação — Pulso Bateria 0.1.0-alpha.13 (13)

## Alterações e verificações locais

- Modificados os filtros de corrente no core e na notificação; nenhuma alteração de schema Room.
- `kotlinc` (Kotlin 1.9 do ambiente) compilou os arquivos puros do core da alpha.13 sem erros.
- Executadas 10 verificações funcionais num programa Kotlin: sinal positivo/negativo, valores não finitos, manutenção de bruto, rejeição de descarga positiva e fallback negativo para fonte média. **10/10 aprovadas.**
- Novos testes JUnit `CurrentDirectionTest.kt` foram adicionados ao projeto para execução no GitHub Actions; esses testes JUnit não foram executados localmente.
- `tools/validate_source.py`: **aprovado** em estrutura, manifest/XML, permissão adicional, versões e Gradle Wrapper.
- `tools/test_storage_sql.py`: mantém testes SQL de migração/consistência sem mudança de schema. Ver resultados registrados após execução.
- ZIP verificado contra CRC e presença de arquivos principais.

## Pendências reais

1. Compilar Android completo, Compose/KSP, lint e instrumentação no GitHub Actions.
2. Testar reinicialização física, APP `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED`, assinatura compatível, permissões de notificação e restrições OEM. `START_STICKY` não é garantia de operação permanente.
3. Verificar máscara `5.000` e a lista suspensa na UI do aparelho, inclusive cursor e fontes ampliadas.
4. Comparar o diagnóstico com sensores reais durante carga/descarga, verificando as leituras brutas e fallback.
5. Confirmar preservação de histórico e backup durante upgrade do APK assinado com os Secrets já configurados.

Sem `INTERNET`, wake locks ou mecanismos para burlar restrições do Android. A preferência após backup importado continua desabilitada por segurança.

## Resultado da tentativa de compilação Android

`./gradlew --offline :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`: **não executou Gradle** porque o Wrapper não conseguiu obter a distribuição (erro `UnknownHostException: services.gradle.org`). Esse bloqueio não demonstra falha nem sucesso da compilação Android.

Parser Kotlin PSI local do compilador instalado: **21 arquivos Kotlin, zero erros sintáticos**. Compilação Kotlin/JVM isolada: core, `Format.kt` e `NotificationText.kt` aprovados. A verificação isolada não inclui as APIs Android/Compose.

Teste direto de notificação: ao descarregar, média Android positiva é omitida; o aviso de polaridade aparece; `formatCapacityMah(5000)` e `formatCapacityMah(4500)` produziram `5.000` e `4.500`.

`tools/test_storage_sql.py`: **8 testes SQLite, 0 falhas**.
