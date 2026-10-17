# Versionamento

Versão atual: **0.1.0-alpha.36**, **versionCode 36**.
Próxima entrega: **0.1.0-alpha.37**, **versionCode 37**.

Alpha.36: múltiplos clientes autorizados por pacote+certificado, até 20, migração da autorização alpha.35 em DataStore, revogação individual, AIDL v1 inalterado; Room v8 e backup v6 preservados. Próxima alpha.37 / 37.

Alpha.35: API AIDL local desativada por padrão, pareamento com assinatura fixada e UID verificado. Settings DataStore adiciona campos opcionais, sem migração Room v8 e sem alterar JSON v6. A autorização não é incluída em backups e é apagada na restauração. O cliente Vigia IA fica para outra entrega. Próxima alpha.36 / 36.

Alpha.34: somente interface. Reposiciona a aba Sessões no lugar de Uso e preserva toda a melhoria de Labs da alpha.33. Sem alteração no banco Room v8, backup JSON v6, assinatura e permissões. Próxima alpha.35 / 35.

Alpha.33: somente interface. Remove a aba Sessões da navegação inferior, mantendo a tela e os dados no acesso via Home. Compacta o Labs, com opções adicionais recolhíveis e ação Iniciar persistente. Room v8, backup JSON v6 e Secrets inalterados. Próxima alpha.34 / 34.

Fonte Android: `app/build.gradle.kts`; a tela Sobre usa BuildConfig. Sincronizar README, CHANGELOG, RELEASE_NOTES, AGENTS, este documento, validação, workflow e ZIP. Preservar `applicationId = br.com.pulsobateria` e os Secrets/keystore existentes.

Alpha.19 usa **Room v5** (migração 4→5 adiciona campos opcionais de fonte e nome manual a `sessions`) e **backup JSON v3** (continua importando v1/v2). Não existe migração destrutiva. Mudança no formato interno da calibração descarta somente decisões antigas de sinal/unidade, preservando histórico e preferências. Os schemas JSON reais do KSP ainda precisam ser gerados e versionados; não criar JSON fictício.

Alpha.20 mantém **Room v5** e **backup JSON v3**; as preferências opcionais de alertas são locais, desativadas após restauração. Não há migração de banco nem novos Secrets.

Alpha.21 reorganiza telas e textos; **nenhuma migração Room** (permanece v5), **backup JSON v3** e as mesmas quatro Secrets de assinatura.

Alpha.22 altera apenas apresentação da notificação/ações do serviço, sem mudanças no banco Room v5, backup JSON v3 ou Secrets.

Alpha.23 usa **Room v6**, migração não destrutiva 5→6 com `lab_tests`, isolada das sessões; **backup JSON v4** adiciona Labs concluídos e continua aceitando v1/v2/v3. O service specialUse e as quatro Secrets existentes são mantidos. Não criar schemas KSP manualmente.

Alpha.24 usa **Room v7**, migração não destrutiva 6→7 que acrescenta tipo e indicador de fonte externa a `lab_tests`, com defaults que preservam os testes solares antigos. **Backup JSON v5** inclui o novo experimento e importa v1 a v4. Keystore e Secrets inalterados; 7.json só pode ser gerado pelo KSP, não manualmente.

Alpha.25 reorganiza apenas as Configurações e as cores do Material 3; permanece **Room v7** e **backup JSON v5**. Nenhuma migração, serviço ou permissão nova.

Alpha.26 altera somente a UI do Labs e adiciona exclusão individual segura no DAO, sem migração (Room v7) e sem mudar JSON v5 ou Secrets.

Alpha.27: **Room v8** com migração aditiva 7→8 (temperaturas opcionais e contexto manual Labs), **backup JSON v6** preserva importação v1..v5. Nenhuma permissão ou Secret adicional.

Alpha.28 adiciona apenas projeção sobre os resumos de sessões do Room v8: não altera tabelas ou backup JSON v6, nem os Secrets. A próxima sequência é alpha.30 / 30.

Alpha.29 acrescenta projeção de recarga no `core` com filtro por estado, tipo de fonte e fase final observada; Room v8, backup JSON v6 e Secrets mantidos. Próxima alpha.30 / 30.


Alpha.30 reorganiza somente a interface da Home em estilo compacto: não altera Room v8, backup JSON v6, cálculos do core, permissões ou Secrets. Próxima sequência: alpha.31 / 31.

Alpha.31 reorganiza os cartões inferiores da Home, exclusivamente em Compose. Sem alterações no Room v8, backup JSON v6, módulo core, serviço, fontes de leitura ou Secrets. Próxima alpha.32 / 32.

Alpha.32 implementa agrupamento visual na aba Sessões, sem junção permanente de dados. Room v8 e backup JSON v6, core de autonomia/recarga, Secrets e serviços mantidos. Próxima alpha.33 / 33.
