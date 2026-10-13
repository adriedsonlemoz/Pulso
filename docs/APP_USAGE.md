# Consumo por aplicativo: limitações e testes (alpha.16)

## Fonte, autorização e privacidade

O módulo utiliza `UsageStatsManager.queryEvents` (`MOVE_TO_FOREGROUND/BACKGROUND`, `SCREEN_INTERACTIVE/NON_INTERACTIVE`) após o usuário conceder Acesso a uso nas Configurações do Android. Sem a permissão especial, a tela oferece uma explicação e o restante do Pulso funciona normalmente. Não há Internet, root, AccessibilityService nem `QUERY_ALL_PACKAGES`. O ranking é calculado sob demanda e não armazena uma cópia do histórico dos aplicativos.

## Interpretação

- Barras e ranking: tempo estimado de primeiro plano, não consumo da bateria por aplicativo.
- Histórico Room: quedas e recargas em pontos percentuais observadas dentro da janela das últimas 24 h. Somente pares do mesmo `sessionId`, monotonicidade de timestamp civil e intervalos de até 3 minutos. Amostras reduzidas do Room e lacunas diminuem a cobertura.
- Tela ligada/desligada: classificação pelo último evento conhecido; não inferir se faltar o evento inicial ou se a tela mudar entre as duas amostras. Trata-se do estado **interativo** do dispositivo, não da luminosidade exata do display.
- O sistema pode remover eventos de uso antigos, bloquear dados com o aparelho trancado e ocultar nomes de alguns pacotes. O módulo não consegue reconstruir horários perdidos nem calcular mAh de outros aplicativos.

## Validação sugerida em aparelho

1. Abrir aba Apps sem autorização: observar mensagem, sem crash.
2. Conceder autorização via Acesso a uso: retornar e atualizar, verificar ranking com 2+ aplicativos utilizados.
3. Comparar as 12 faixas horárias, com uso em primeiro plano e alternância de tela.
4. Verificar que uma sessão interrompida ou lacuna >3min não gera queda atribuída ao intervalo ausente.
5. Revogar permissão, atualizar e conferir que o acesso não permanece habilitado.
6. Testar em Android 8.0 e versões mais recentes, aparelho bloqueado e idioma português.
