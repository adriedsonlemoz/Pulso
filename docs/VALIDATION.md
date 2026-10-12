# Validação — Pulso Bateria 0.1.0-alpha.11 (11)

## Escopo

Formatação do horário de carga na notificação. A rotina de previsão e os dados persistidos **não mudaram**. O relógio é arredondado a cinco minutos, com indicação de amanhã/data para previsões após meia-noite.

## Testes

- **96 testes Kotlin/JVM locais aprovados**, sem falhas (módulo core e formatação de notificação; executor de asserções compatíveis com JUnit). Foram incluídos casos de previsão local, ausência de ETA, bateria em 100%, meia-noite, data futura e arredondamento.
- `tools/validate_source.py` aprovado: 14 arquivos essenciais, 7 XML, permissões, versão, integridade SHA-256/CRC do Gradle Wrapper.
- Compilação Android e testes com notificação real no aparelho ainda necessários.

## Pendências anteriores mantidas

- Confirmar APK gerado no GitHub Actions e assinatura estável conforme Secrets existentes.
- Executar testes instrumentados Room e backup/restauração em Android.
- Versionar JSON de schema 3 exportado pelo KSP.
