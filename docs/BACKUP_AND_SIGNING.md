# Persistência, backup e assinatura — alpha.12

## Importante para instalações anteriores

Não é possível substituir diretamente um APK assinado com a chave de desenvolvimento aleatória de um runner antigo por um APK com a nova chave estável. O Android verifica a assinatura na atualização. **Não desinstale a versão antiga para conseguir instalar a nova sem antes conseguir preservar os dados.** A alpha.7 não possui exportação; instalar a alpha.8 com outra chave não permite extrair os dados da alpha.7. O aplicativo não tem permissão de root nem um canal oculto de leitura de dados de outro app. Planeje qualquer migração respeitando isso.

## Signing no GitHub Manager

Importe os quatro Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) pelo menu Repositório → Secrets → Importar. Use os arquivos confidenciais entregues SEPARADAMENTE, nunca copie os valores para este repositório. A mesma chave `.p12` deve ser mantida offline em ao menos dois locais seguros, junto à senha. Os Secrets são usados no CI para assinar a variante Debug de forma estável a partir da alpha.8. A variante Release também está configurada, mas o workflow publica Debug para manter o fluxo já adotado. Sem os quatro Secrets, o workflow ainda gera o Debug temporário, e informa que não há continuidade garantida da assinatura. Secrets parciais provocam erro explícito.

Certificado público de referência (SHA-256): `4B:8A:A8:0E:7F:E1:57:11:29:7B:21:A3:29:26:2E:2C:B5:DD:8D:96:97:80:EE:CF:2C:AD:32:32:63:0F:FB:EA`. Compare a impressão digital com `apksigner verify --print-certs` no APK recebido. Atenção: perder a chave privada impede atualizações diretas futuras.

## Backup manual

Configurações → Backup e restauração → Exportar. Salva JSON UTF-8, sem servidor e sem internet, escolhido pelo Storage Access Framework. Inclui resumos de até 500 sessões, pontos de até 480 por sessão, corrente/temperatura quando observadas e preferências. `sha256` confere integridade acidental do arquivo, não autenticidade de sua origem. O backup **não é criptografado**; proteja o arquivo. O manifesto desativa backup automático em nuvem; transferências entre dispositivos podem variar com o fabricante. Não se depende disso para preservar histórico.

Para importar, escolha arquivo pelo seletor Android. O aplicativo verifica limites, integridade, relações entre pontos e sessões e mostra uma confirmação. Ao confirmar, as sessões atuais são **substituídas**, não mescladas. A substituição do banco Room ocorre por transação; as preferências DataStore são gravadas depois, sem transação cruzada. Se as preferências falharem após o commit do banco, pode haver atualização parcial das preferências. Exporte uma cópia da instalação atual antes de importar. Sessões em aberto no backup são encerradas para evitar estado ativo falso.

Desative o monitoramento contínuo antes de confirmar a restauração (o aplicativo bloqueia a confirmação caso ele esteja ativo). O monitoramento em primeiro plano é congelado durante a restauração e recomeça com nova sessão se a tela permanece aberta. A configuração de monitoramento contínuo não é reativada automaticamente. Verifique o histórico e o status após restaurar. O backup não evita perda de dados de versões já instaladas que ainda não dispõem de exportação.

## GitHub Release e artefatos

O workflow compila e faz upload do APK como **Artifact** antes de tentar publicar Release. Se o GitHub devolver HTTP 500 temporário (ocorreu no build 8), ele tenta de novo e o APK segue disponível em Actions → execução → Artifacts (retenção 30 dias). A publicação da Release pode falhar por permissões ou indisponibilidade do GitHub sem apagar o APK produzido.

## Migração Room

DB Room v1 → v2 adiciona `session_samples`, v2 → v3 acrescenta `currentMa`, `temperatureC`, `currentSource` opcionais, não migração destrutiva. Testes instrumentados `RoomMigrationTest` cobrem bases v1/v2 abrindo em v3, reabertura de v3 e rollback conjunto de resumo/ponto. Na alpha.12 foram preparados, mas não executados no Android. `assembleDebugAndroidTest` apenas compila o APK de testes; execução real exige emulador/aparelho e `connectedDebugAndroidTest`.

## Possível resgate de uma instalação Debug antiga (avançado, não validado)

Se você **já possui registros** numa alpha.7 instalada e a nova assinatura impede a atualização, não desinstale o aplicativo. Em um computador com ADB e depuração USB autorizada, pode ser possível resgatar os arquivos privados porque o APK Debug geralmente é `debuggable`. Pare o app e teste:

```sh
adb shell am force-stop br.com.pulsobateria
adb shell run-as br.com.pulsobateria id
adb exec-out run-as br.com.pulsobateria tar -cf - databases files/datastore > pulso-alpha7-dados-privados.tar
```

Somente considere o resgate concluído depois de conferir que o TAR contém `databases/pulso.db` e as preferências. Pode incluir arquivos WAL/SHM. **Esse TAR não é diretamente importável pelo botão JSON da alpha.8**; será necessária recuperação manual supervisionada posteriormente, com validação do banco e acesso ADB ao aplicativo de destino. Não execute `uninstall` sem cópia verificada. Não publique esse TAR ou compartilhe os dados privados.

## Auditoria alpha.12

Schema Room v3 e backup JSON v1 preservados. A gravação de resumo, ponto e retenção passa a usar uma transação única. O leitor de backup rejeita contagens de temperatura incoerentes e pontos fora do período da sessão. Decisões antigas de calibração são descartadas; a nova identidade local de calibração fica em noBackupFilesDir, ligada ao firmware. Não apaga históricos/configurações. Ver `AUDIT-alpha.11-to-alpha.12.md` e `VALIDATION.md`.

## Telemetria e backup (alpha.18)

O exportador passa a produzir JSON **v2** com o campo `telemetry`, preservando o envelope SHA-256. A importação continua aceitando backups v1, nos quais a série técnica fica vazia. Novos backups devem ser testados no aparelho antes de descartar os antigos. O formato não é criptografado e o SHA-256 detecta integridade, não autenticidade. A tabela Room v4 só é preenchida após a atualização e não retroage ao histórico salvo.

## Fonte de carga e backup (alpha.19)

Room v5 adiciona `sessions.chargeSource` e `sessions.chargerLabel` opcionais por migração 4→5. O app registra apenas o tipo relatado pelo Android para novas recargas; rótulos de até 40 caracteres são digitados manualmente pelo usuário. O exportador passa ao envelope JSON **v3**, com esses campos em cada sessão e o array de telemetria da v2. O importador aceita backups v1/v2 sem esses campos, deixando-os nulos. Não restaurar monitoramento contínuo automaticamente. Recomendamos exportar backup antes da atualização e testar restauração em dispositivo de teste.

## Alpha.20

Os alertas inteligentes são opções locais desligadas por padrão. Por segurança, **não são exportados** no JSON v3 e não são reativados ao restaurar um arquivo; as configurações básicas e o histórico permanecem no backup como anteriormente. O Room segue em v5, sem migração nesta versão.

## Alpha.21

Somente interface: Room v5 e backup JSON v3 sem mudanças. Os quatro Secrets de assinatura existentes permanecem os mesmos. Antes de atualizar, faça backup e valide o APK no GitHub Actions.
