# Validação — Pulso Bateria 0.1.0-alpha.3 (3)

- Código revisado para retomada de pausas curtas, exclusão do período sem coleta, encerramento após 90 s e preservação de sessões antigas. Banco Room mantém schema 1 e dados existentes.
- **40 testes Kotlin/JUnit aprovados**, zero falhas, Kotlin Compiler 2.1.20 / Java 17 / JUnit 4.13.2. Incluem retomada com mesmo estado/percentual, rejeição de mudanças/lacunas/relógio e exclusão da pausa da duração. Resultado em docs/test-results/alpha.3-junit.txt.
- YAML e script shell da publicação verificados; workflow usa Release asset APK direto e não contém upload-artifact. Nenhuma publicação remota foi realizada neste ambiente.
- Script estrutural: versões, XMLs, permissões, arquivos obrigatórios e Wrapper SHA-256/CRC verificados. ZIP conferido com CRC.
- Compilação Android completa, Android Lint e execução no aparelho NÃO foram realizadas nesta entrega. Resultados anteriores de AAPT2/JUnit permanecem identificados nos relatórios históricos; não equivalem a validação do novo layout em dispositivo.

Validar em aparelho: saída/retorno em <90 s com mesmo percentual conserva ID, exclui pausa da duração e reinicia taxa; mudança percentual/estado ou >90 s abre nova sessão; ativar serviço e sair não deve pausar; morte do processo não pode fundir sessões. Conferir notificação recolhida e expandida, temperatura °C/°F, estados cheia/desconhecido e tela pequena. Após build, conferir download APK direto via resumo/Releases.
