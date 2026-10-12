# Validação — Pulso Bateria 0.1.0-alpha.1 (1)

Data: 2026-10-12.

## Testes efetivamente executados

- **31 testes JUnit 4.13.2 aprovados**, zero falhas. Código real de `core` compilado para JVM com Kotlin Compiler 2.1.20 (a mesma versão do projeto), Java 17. Uma execução preliminar com Kotlin 2.0.20 também passou. Esta execução JUnit independente não substitui o build Android.
- Casos: escala de porcentagem; percentuais inválidos; décimos de grau e ausência; µA → mA; sinal de descarga; sentinelas e extremos; sinal contraditório; corrente zero; taxa de carga e descarga; observação curta; percentual constante; direção reversa; recalibração; timestamp repetido; lacunas; previsão de carga e descarga; entradas inválidas; cheia/inativa; transição; suspensão; alteração de relógio civil; regressão monotônica; continuidade; média de sessão; média de temperatura e mínimo de amostras.
- **AAPT2 35.0.0: compilação dos recursos aprovada**, gerando arquivo compilado de recursos. Não equivale a linkagem Android completa ou geração de APK.
- **Validação estrutural aprovada** via `python3 tools/validate_source.py`: 14 arquivos obrigatórios, 7 XMLs bem formados, permissões limitadas, referências de versão e Wrapper JAR com CRC e SHA-256 oficial.
- Distribuição Gradle 8.11.1 baixada e SHA-256 confirmado. Gradle executou e interpretou settings/root build. A resolução direta das dependências Android falhou por acesso de rede do processo Java aos repositórios, antes da compilação do aplicativo.

- **Workflow YAML validado**: execução manual, tarefas de testes/lint/build e publicação do APK condicionada ao sucesso.
- **ZIP final conferido**: CRC de todos os arquivos, estrutura na raiz, inclusão de .github e Wrapper, exclusão de caches, builds e local.properties.

## Pendências

- **Build Android completo, KSP/Room, testes via Gradle e Android Lint: PENDENTES. Nenhum APK foi gerado ou validado nesta entrega.** A tentativa com dependências oficiais baixadas localmente avançou até tarefas do módulo core, mas não concluiu o aplicativo antes da interrupção. Na retomada, JDK/SDK e dependências temporárias não estavam mais disponíveis. Os fontes e o resultado JUnit permaneceram preservados. Não confundir a execução JUnit independente com estas etapas.
- Instalação, execução e aparência em aparelho/emulador não testadas nesta entrega.
- Precisão dos sensores, Doze, políticas Xiaomi/MIUI/HyperOS, funcionamento prolongado do serviço e consumo real precisam de dispositivo físico.

## Validação no GitHub Actions

O workflow executa `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`, publica o APK apenas com sucesso e preserva relatórios mesmo em falhas. Não usa assinatura Release nem Secrets.

## Roteiro manual no aparelho

1. Instalar; conferir nome, ícone, splash, navegação e botão Voltar.
2. Comparar percentual/temperatura com dados do Android. Conectar/desconectar; conferir carregando, sem carregar e cheia quando disponíveis.
3. Verificar USB/tomada/sem fio somente com fontes realmente disponíveis no aparelho.
4. Observar ≥5 min e ≥2 p.p.; conferir taxa e previsão. Recalibração, valores ausentes e sinal contraditório não podem gerar valores inventados.
5. Ativar notificação no Android 13+ aceitando e recusando permissão; bloquear canal nas configurações do sistema; testar desativar e ação Parar.
6. Trocar temas, unidade e intervalo; reabrir; testar texto ampliado, tela estreita, rotação e navegação gestual.
7. Sair sem serviço: sessão encerra na última leitura. Com serviço: acompanhar notificação e retorno ao histórico sem dupla contagem.
8. Forçar parada/reiniciar: registro anterior termina como interrompido; retomar requer ativação. Intervalos sem observação não entram nas médias.
9. Verificar suspensão por mais de 3 min, filtros e retenção. Não atribuir a autonomia do aparelho ao app sem medir consumo comparativo.
