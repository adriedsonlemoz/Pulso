# Pulso Bateria 0.1.0-alpha.12 • versionCode 12

Auditoria técnica da alpha.11 e correções de medição e persistência.

- Correntes pequenas e zero seguem o contrato Android em µA; o status não força o sinal da leitura.
- Ajustes de sinal/unidade da instantânea exigem três blocos estáveis, com integração temporal e comparação ao contador nos mesmos intervalos.
- Calibração vinculada à instalação e ao firmware; expiração em sete dias e revalidação após interrupções. Calibrações antigas são descartadas, sem apagar preferências ou histórico.
- Média Android e média do contador apresentadas separadamente, com a janela real do contador. O hardware não informa a janela de CURRENT_AVERAGE.
- Gravação de resumo/ponto/retenção na mesma transação; falha ao pausar para restauração retorna erro ao chamador.
- Validação de coerência dos backups e proteção contra regressão do relógio civil.
- Horário previsto arredondado para cima em cinco minutos, baseado no instante da leitura. Não melhora a precisão do ETA.

Validação local: **132 testes Kotlin/JUnit + 8 SQLite aprovados**. Oito regressões reproduzidas na alpha.11. Build Android, lint, Room instrumentado, backup no aparelho e instalação sobre alpha.11 continuam pendentes. Nenhum APK foi gerado nesta entrega.

Room v3, backup v1, identidade e configuração de assinatura preservados. Sem permissões novas, Internet, chaves ou Secrets no código. Não há fator para igualar leituras ao AccuBattery.

Médias próprias de 5/20 minutos, mAh e média da sessão completa são propostas no relatório, não funcionalidades implementadas nesta versão. O histórico atual não contém dados suficientes para reconstruí-las corretamente.
