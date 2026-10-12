# Pulso Bateria 0.1.0-alpha.2

versionCode: 2. Android mínimo: 8.0 (API 26).

Correção do workflow após análise dos logs Pulso-Android-Pulso-Bateria-1-logs.zip e Pulso-Android-Pulso-Bateria-2-logs.zip. Ambos falharam em Android SDK ao executar `sdkmanager tools`, antes de Gradle/testes/compilação.

A ação setup-android agora recebe `packages: platform-tools`, evitando o pacote indisponível `tools`. SDK 35 e Build Tools 35.0.0 continuam instalados explicitamente na próxima etapa.

Importe o ZIP completo e envie os arquivos atualizados, incluindo `.github/workflows/android.yml`, ao repositório. Execute uma NOVA execução do workflow na versão atualizada; repetir o job antigo utiliza a configuração antiga.

Artefato esperado após sucesso: Pulso-Bateria-v0.1.0-alpha.2-debug. Não exige Secrets; APK Debug não é Release assinada para distribuição.

Validação desta correção: YAML, configuração de pacotes, referências de versão, XMLs, Wrapper oficial e integridade do ZIP. Build completo, lint e execução em aparelho permanecem pendentes. Os 31 testes JUnit anteriores estão registrados, mas não foram reexecutados nesta mudança de workflow. Funcionalidades e limitações de medição permanecem descritas no README.
