# Formato de backup 3x3

## Identidade

- Extensão: `.3x3backup`.
- Conteúdo: JSON UTF-8.
- Identificador: `com.gabs.cubo3x3.backup`.
- Versão atual do esquema: `9`.
- Limite aceito pelo aplicativo: 5 MB.

O arquivo é criado e escolhido pelo seletor de documentos do Android. O aplicativo não solicita acesso amplo ao armazenamento.

## Estrutura da versão 9

O objeto raiz contém:

- `format`: identificador fixo do formato;
- `schemaVersion`: versão inteira do esquema;
- `appVersion`: versão do aplicativo que exportou o arquivo;
- `exportedAtEpochMillis`: instante da exportação;
- `settings.themeMode`: `system`, `light` ou `dark`;
- `settings.reminderEnabled`: indica se o lembrete local esta ativado;
- `settings.reminderHour` e `settings.reminderMinute`: horario local configurado;
- `settings.reminderDays`: dias da semana em que o lembrete pode disparar;
- `settings.dailyGoals`: configuracao canonica `1|ATIVA|RECONHECIMENTO|EXECUCAO`.
  ATIVA aceita 0 ou 1; alvos inteiros de 0 a 100, sem espacos/zeros extras.
  Zero desativa aquele contador. Metas ativas exigem pelo menos um alvo positivo.
  Exemplo `1|1|5|5`; padrao desativado `1|0|5|5`. Contagens nao sao
  armazenadas: derivam dos historicos restaurados e do dia/fuso do aparelho;
- `settings.cfopTrainingPlan`: configuracao reutilizavel do treino, codificada
  pela versao 1 do codec proprio (ate 2.000 caracteres). Estrutura:
  `1|MODO|CATEGORIA|NUMERO|ETAPAS|CASOS_EXTRAS`. Modos: FREE, CATEGORY,
  CASE, COMPLETED; categoria vazia/numero 0 indicam ausencia. Etapas e IDs
  sao listas separadas por virgula, sem duplicatas. Etapas nao podem ficar
  vazias; IDs devem existir no catalogo e pertencer as etapas selecionadas.
  Campos inativos devem usar os padroes do modo, sem selecoes ocultas.
  Exemplo: `1|COMPLETED||0|F2L,OLL,PLL|F2L-2,OLL-5`;
- `progress`: favoritos e concluídos de F2L, OLL e PLL;
- `timerSessions`: sessões locais do cronômetro, com `id`, `name` e `createdAtEpochMillis`;
- `solveTimes`: histórico do cronômetro, incluindo para cada tentativa `id`, `durationMillis`, `recordedAtEpochMillis`, `scramble`, `comment`, `penalty` (`NONE`, `PLUS_TWO` ou `DNF`), `sessionId` e os campos opcionais vinculados `trainingCategory`/`trainingCaseNumber`;
- `quizRecords`: recordes por nível e modo;
- `cfopAttempts`: respostas de reconhecimento, com identificador textual
  unico `id`, `category`, `caseNumber`, `selectedCaseNumber` e
  `recordedAtEpochMillis`. Acerto e derivado da igualdade dos numeros.
  Limite de 100.000 respostas, dentro do limite global de 5 MB. Numeros,
  categoria, data, identificador e duplicatas sao validados antes da confirmacao.
- `customAlgorithms`: nome, notação, tags, esquema de cores, ponto de vista e datas dos algoritmos personalizados;
- `preferredFormulas`: colecao obrigatoria (pode ser vazia), uma alternativa
  por caso. Campos: `category`, `caseNumber`, `notation` e
  `updatedAtEpochMillis`. Maximo 119 entradas; cada notacao tem ate 2.000
  caracteres/200 movimentos. IDs, datas, duplicatas, tokens e resolucao
  semantica da etapa sao conferidos sobre o caso ORIGINAL. Espacos
  normalizados; caixa, largos/fatias/rotacoes e primas duplas preservados.

Os identificadores locais dos tempos e algoritmos personalizados são preservados. A notação também é preservada semanticamente, inclusive variantes duplas primas como `U2'`.

## Validação e restauração

Antes de permitir a confirmação, o aplicativo valida:

- identificador e versão do formato;
- presença e tipo dos campos obrigatórios;
- limites de tamanho e quantidade;
- categorias e números válidos dos casos CFOP;
- ausência de chaves e identificadores duplicados;
- valores e combinações dos recordes do Quiz;
- duracao, data, tamanho do embaralhamento e comentario e penalidade valida de cada tempo;
- identificadores e nomes únicos das sessões e vínculo de cada tempo com uma sessão existente;
- presença conjunta e validade da categoria/caso CFOP quando a tentativa veio do treino direcionado;
- nomes, tags, esquemas, pontos de vista, datas e notações personalizadas;
- formulas preferidas sobre estado canonico: F2L primeiras camadas, OLL
  primeiras camadas/amarelo e PLL todas as faces, relativas aos centros;
- cada movimento pelo parser próprio do cubo.

Arquivos `.cfop`, JSONs genéricos, documentos corrompidos e versões incompatíveis são recusados sem alterar os dados locais.

Depois da validação e da confirmação explícita, as sete tabelas Room são substituídas em uma única transação. Tema e agenda de lembretes são atualizados no DataStore. A restauração é substitutiva, não uma mesclagem.

O plano de treino e a configuracao das metas tambem sao substituidos no DataStore, sem alterar outras
preferencias de inspeção/sessao. A fila e cobertura da rodada em andamento
nao fazem parte do backup: restauracao confirmada descarta esse estado visual,
invalida gravacoes antigas do plano e reconstrui o treino dos dados restaurados.
Editor/reprodutor do detalhe e gravacoes de formulas preferidas tambem sao
invalidados na confirmacao, sob Mutex/geracao, sem repovoar escolhas antigas.
Rascunhos e salvamentos antigos de metas tambem sao invalidados, mesmo se
o backup trouxer a mesma configuracao. Tema/lembretes/plano/metas sao
atualizados no DataStore apos a transacao Room; nao existe uma transacao
unica envolvendo simultaneamente Room e DataStore.

Os esquemas 1 a 5 nao possuem respostas CFOP: sao aceitos e restauram esse
historico como vazio. A confirmacao avisa quando o arquivo nao contem respostas,
inclusive que o historico CFOP existente sera substituido. Novas exportacoes
usam o esquema 9.

A agenda de revisao espacada e derivada das respostas CFOP restauradas;
nenhuma data/nivel redundante e persistido no backup. Esquemas 6 a 9
reconstituem os mesmos prazos; 1-5 nao inventam agenda sem respostas.

## Compatibilidade com as versões 1 a 8

O leitor aceita backups dos esquemas 1 a 8. Como a versao 1 ainda nao armazenava lembretes, sua importacao usa a configuracao padrao: lembrete desativado, 19:00 e todos os dias selecionados. Como os esquemas 1 e 2 ainda nao armazenavam embaralhamento, comentario e penalidade nos tempos, esses campos sao restaurados como texto vazio e `NONE`. Como os esquemas 1 a 3 ainda nao armazenavam sessoes, a importacao cria a sessao `Principal` e associa a ela todos os tempos legados. Como os esquemas 1 a 4 ainda nao armazenavam a origem do treino CFOP, esses campos opcionais sao restaurados como nulos.

Os esquemas 1 a 6 nao guardam plano reutilizavel: restauram Treino livre WCA.
A confirmacao mostra que esse padrao vai substituir o plano atual. O esquema
6 preserva respostas CFOP. Ausencia/plano invalido nos esquemas 7-9 e erro, nao
fallback silencioso. Backups novos nao devem ser importados por versoes do
app que desconhecem o esquema 9.

Esquemas 1 a 7 nao guardam formulas preferidas: a previa nao muda dados,
mas a restauracao confirmada limpa as escolhas atuais com aviso explicito.
Esquemas 8/9 sem colecao de formulas ou com alternativa invalida sao recusados;
nao se descarta silenciosamente uma formula errada. O catalogo original
nunca e substituido por essas preferencias. Contrato: PREFERRED_CASE_FORMULA.md.

Esquemas 1 a 8 nao possuem metas: a previa nao muda configuracao atual,
mas a confirmacao restaura metas desativadas, com aviso explicito. Esquema 9
sem settings.dailyGoals ou com codec/limites invalidos e recusado antes
da confirmacao; nao transformar arquivo invalido em configuracao padrao.
Contrato de contagem, dia/fuso e fontes: DAILY_GOALS.md.

## Compatibilidade futura

Uma alteração incompatível na estrutura exige um novo `schemaVersion` e uma migração explícita no leitor. Acrescentar suporte a uma versão futura não autoriza aceitar silenciosamente campos ou valores inválidos.
