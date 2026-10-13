# Pulso Bateria 0.1.0-alpha.22

**Foco: notificação compacta, legível e tecnicamente conservadora.**

- Notificação compacta com nível, temperatura e corrente líquida útil, sem repetição no painel expandido.
- Estado FULL é distinguido de CHARGING a 100%; ETA permanece aproximada e só aparece quando existe cálculo válido.
- Zero de corrente/média não vira garantia de ausência de consumo; média válida pelo contador pode ser apresentada com identificação de origem.
- Limiar de temperatura existente é usado para destaque passivo, sem som ou vibração no canal de monitoramento.
- Ações Abrir e Parar, nesta ordem; nenhuma mudança nas permissões ou serviços.
- Room v5, backup JSON v3, banco, identidade e Secrets preservados.

**Validação Android/Compose pendente:** gerar APK no GitHub Actions, revisar layout compacto/expandido na MIUI/HyperOS e comportamento de 0 mA perto de 100%.

APK esperado: `Pulso-Bateria-v0.1.0-alpha.22-debug.apk`.
