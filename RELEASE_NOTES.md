# Pulso Bateria 0.1.0-alpha.24

**Labs com menu e teste de consumo sem carregador.**

- Página Labs agora exibe **Teste Solar**, **Teste de Consumo** e **Últimos testes**; histórico completo em Ver todos.
- Teste de Consumo registra níveis inicial/final, queda em pontos percentuais, tempo observado em descarga, lacunas e horário; duração de 120 min por padrão, outras durações e modo manual.
- É necessário iniciar com o carregador desconectado. Se o Android detectar alimentação externa em qualquer leitura durante o teste, o resultado fica marcado como **interferido**.
- Teste solar permanece intacto, inclusive alterações do céu e corrente líquida média válida.
- Finalização programada salva relatório e emite notificação quando o Android executa a tarefa e permite notificações; pode haver atrasos no segundo plano.
- Room v7 com migração explícita 6→7; backup JSON v5 preserva compatibilidade v1–v4. Sem câmeras, internet ou novas permissões.
- A porcentagem final é a última leitura disponível, não uma medição contínua. Se a leitura estiver antiga, o relatório avisa. Conexões rápidas entre amostras podem não ser detectadas.

**APK ainda depende de compilação e testes em Android/GitHub Actions.**

APK esperado: `Pulso-Bateria-v0.1.0-alpha.24-debug.apk`.
