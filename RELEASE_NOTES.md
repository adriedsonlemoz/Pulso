# Pulso Bateria 0.1.0-alpha.7

versionCode: 7. Android 8.0+.

**Corrente elétrica melhorada:** leitura de corrente instantânea e média pelo Android, detecção conservadora de inversão de polaridade e unidade inconsistente por amostras, e cálculo de corrente média a partir da variação real do contador de carga, quando disponível.

**Estimativa opcional:** informe a capacidade útil estimada (mAh) em Configurações para obter uma aproximação a partir da taxa observada (%/h). O app não presume capacidade do aparelho e identifica a origem como estimativa, média ou leitura Android. Corrente positiva indica entrada líquida na bateria e negativa, saída.

**Diagnóstico:** valores brutos CURRENT_NOW, CURRENT_AVERAGE e CHARGE_COUNTER, além das decisões da calibração, em Configurações. Calibração automática recomeça quando o processo é reiniciado.

As sessões, gráficos, Room 2 e privacidade offline são preservados. APK e testes físicos pendentes de validação via workflow GitHub Actions.

Após sucesso do workflow, baixe `Pulso-Bateria-v0.1.0-alpha.7-debug.apk` na pré-release. Não são necessários Secrets para Debug.
