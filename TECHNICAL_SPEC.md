# TECHNICAL_SPEC

## Status

`DIRECAO_TECNICA_BASE_ANDROID_E_ORDEM_APROVADAS_PELO_USUARIO`

Direcao tecnica aprovada explicitamente pelo usuario em 2026-09-30. Este documento registra o alinhamento arquitetural e a ordem de desenvolvimento, mas nao autoriza criar ou modificar codigo do aplicativo.

## Plataforma e tecnologia

- Aplicativo Android nativo.
- Identificador do aplicativo: `com.gabs.cubo3x3`.
- Android minimo: API 26 (Android 8.0).
- SDK de compilacao e Android alvo: API 36.
- Linguagem Kotlin.
- Interface com Jetpack Compose e base visual Material 3.
- Nao usar Flutter ou Dart neste projeto.
- Estrutura modular por funcionalidades, separando interface, regras de negocio e persistencia.
- Operacao offline como regra.
- Nao declarar permissao de internet apenas para as funcoes do nucleo.

## Organizacao arquitetural

Cada funcionalidade deve manter responsabilidades separadas:

- `ui`: telas, componentes, estados e eventos de interface;
- `domain`: modelos e regras do produto independentes da interface;
- `data`: persistencia local, importacao, exportacao e implementacoes de repositorios.

A separacao deve ser proporcional ao tamanho do aplicativo. Nao criar camadas, modulos ou abstracoes sem uso concreto.

## Dados locais

- Room para progresso, tempos, recordes e algoritmos personalizados.
- DataStore para preferencias simples, como tema e configuracoes do cubo.
- Dados CFOP de leitura devem ter origem e licenca validadas antes de serem incorporados.
- Nenhuma conta, sincronizacao em nuvem, analytics ou telemetria.

## Backup e restauracao

- Arquivo JSON proprio e versionado.
- Extensao provisoria `.3x3backup`.
- Exportacao e importacao pelo seletor nativo de documentos do Android.
- Validacao de esquema e versao antes de restaurar.
- Confirmacao explicita antes de substituir dados locais.
- Nenhuma permissao ampla de armazenamento.

## Interface e navegacao

- Material 3 como base, com identidade visual propria.
- Temas claro, escuro e automatico conforme o sistema.
- Cinco destinos principais: Inicio, Algoritmos, Cronometro, Quiz e Mais.
- Preservar o estado das abas conforme definido em `SCREEN_MAP.md`.
- Direcao visual, paleta, tipografia e estilo de componentes seguem `VISUAL_SPEC.md`.
- Os conceitos principais aprovados e a variante de icone `Faces` seguem `VISUAL_SPEC.md`.

## Cubo animado

O cubo animado e o maior risco tecnico e deve ser validado antes da expansao das demais funcionalidades.

O primeiro trabalho de implementacao, quando autorizado, sera um prototipo que confirme:

- representacao correta do estado do cubo;
- interpretacao da notacao aprovada;
- execucao e pausa de sequencias;
- controle de velocidade;
- rotacao e visualizacao adequadas;
- desempenho em aparelho Android;
- licenca compativel de qualquer biblioteca eventualmente adotada.

O primeiro prototipo usa um modelo geometrico proprio e renderizacao isometrica animada em Jetpack Compose Canvas, sem biblioteca 3D externa e sem copiar o modulo HTML/JavaScript do APK de referencia. A opcao foi validada por compilacao, testes unitarios e execucao em um Samsung SM-A256E com Android 14 (API 34). O usuario aprovou o prototipo em aparelho em 2026-10-01, portanto o renderizador Compose Canvas passa a ser a base adotada para o aplicativo.

## Cronometro competitivo para treino

- O tempo usa `SystemClock.elapsedRealtime` por meio da regra de dominio `Stopwatch`, evitando alteracoes do relogio civil durante uma tentativa.
- A area principal do cronometro e o alvo de toque para iniciar e encerrar; a interface nao depende de um botao pequeno durante a resolucao.
- Cada registro Room guarda duracao bruta, instante, embaralhamento, comentario, penalidade `NONE`, `PLUS_TWO` ou `DNF`, sessao e origem CFOP opcional; o banco atual esta na versao 9, com migracoes explicitas preservando os registros legados.
- `mo3` usa os tres resultados; `ao5` e `ao12` descartam o melhor e o pior. Um DNF pode ser descartado como pior resultado; dois ou mais tornam a media DNF.
- O embaralhamento 3x3 e gerado offline pelo artefato GPL-3.0 `org.worldcubeassociation.tnoodle:scrambler-min2phase:0.20.0`, partindo de estado aleatorio e calculando uma solucao inversa de no maximo 21 movimentos.
- O uso no aplicativo e para treino pessoal. Competicoes oficiais devem usar a versao oficial atual do programa TNoodle disponibilizada pela WCA.
- O backup atual usa esquema 9, inclui sessoes, origem de treino, respostas CFOP, plano reutilizavel, formulas preferidas e metas diarias e continua aceitando os esquemas 1 a 8 com valores padrao seguros e avisos de substituicao; o contrato detalhado esta em `BACKUP_FORMAT.md`.
- A portabilidade publica csTimer transfere tempos e sessoes por conversor proprio, mantendo o backup integral separado; campos, limites e confirmacoes estao em `PORTABILITY_FORMAT.md`.

## Integracoes locais

### Reconhecimento e revisao CFOP

- `cfop_attempts` registra resposta por caso alvo e identificador unico.
  `INSERT IGNORE` protege replay do mesmo id; historico separado de notacao.
- Alternativas verificadas pelo motor por etapa, nao somente por igualdade da
  formula: F2L primeiras camadas, OLL orientacao amarela/F2L, PLL resolvido.
- Revisao e derivada do historico, sem tabela redundante de prioridade e sem
  apagar erros antigos. Por erros preserva a prioridade original; Por prazo
  reconstroi agenda 1/3/7/14/30 dias, erros imediatos e sem avancar/postergar
  com acertos antecipados. Regras: `CFOP_RECOGNITION.md` e
  `CFOP_SPACED_REVIEW.md`. Sem migracao Room ou formato de backup novo.
- Lista atualiza relogio a cada minuto somente em RESUMED e ao retornar ao
  primeiro plano. Iniciar treino revalida hora atual, congela os IDs devidos
  e nao atualiza a rodada por passagem de tempo/respostas novas.
- Estado de tabs e consultas ao detalhe usa `SaveableStateHolder` no app.
  Revisao congela os ids da rodada; novos resultados nao mudam suas perguntas.
- Restauracao confirmada invalida estado Quiz e gravações CFOP pendentes da
  geracao anterior sob Mutex; nenhum replay antigo repovoa o historico importado.

### Comparacao de casos confundidos

- Agregacao derivada de cfop_attempts em Dispatchers.Default, sem tabela ou
  migracao: mesma janela de dez respostas por alvo de Por erros, acertos
  incluidos antes de filtrar erros, ids deduplicados. Pares canonicos por
  etapa mantem contagens A->B/B->A, prioridade por total/ultima troca/numeros.
- Tela independente de callbacks de gravacao: consultar/revelar/estudar nao
  registra tentativa, tempo ou progresso. Usa AlgorithmCaseDiagram e pistas
  puras do CubeState canonico, nunca alinhamento artificial ou preferida.
- Filtro/par/rolagem/formulas/ampliacao salvaveis; voltar do detalhe preserva
  a comparacao sob SaveableStateHolder existente. Historico novo nao troca
  o par aberto; backup confirmado invalida todo estado Quiz por geracao.
- Comparacao nao exigiu migracao Room/backup; importacao csTimer preservada.
  Contrato e aceite em CFOP_COMPARISON.md.

### Metas diarias opcionais

- Configuracao DataStore estrita, desativada por padrao, alvos independentes
  0..100 (sugestao 5/5). Mais > Metas diarias e resumo no Inicio se ativadas;
  nenhuma adicao ao cronometro. Salvar explicito; rascunho salvavel.
- Progresso derivado em Dispatchers.Default: respostas CFOP certas/erradas
  e tempos dirigidos com categoria/caso valido, inclusive +2/DNF como tentativa.
  IDs deduplicados; todas as sessoes/etapas; livre/csTimer sem origem excluidos.
- Nova consulta somente leitura observeDirected filtra origem CFOP no SQL,
  sem ler globalmente os tempos livres/importados para alimentar metas.
  Room permanece9; sem tabela, migracao ou contador diario persistido.
- Dia civil no ZoneId atual, nao janela24h; respeitar DST, excluir datas
  futuras. Recalcular por historico novo e em RESUMED/minuto, sem reescrever
  instantes. Restauracao confirmada invalida rascunhos/saves por geracao/Mutex.
- Backup9 guarda configuracao e aceita1-8 com metas desativadas/aviso.
  Contrato, limites e aceite em DAILY_GOALS.md. Sem notificacao ou streak.

### Formula preferida por caso

- Tabela Room 9 dedicada, chave categoria + numero; migracao 8->9 apenas
  cria tabela vazia, mantendo as seis anteriores. Schema exportado versionado.
- Validacao Default sobre estado ORIGINAL por etapa, limites 2.000/200,
  preservacao da notacao e original sempre disponivel no detalhe.
- Alternancia reinicia pausada; setup/editor/diagramas/quiz permanecem canonicos.
  Inversa pessoal retorna do resultado da alternativa, nao necessariamente
  de totalmente resolvido. Regras: PREFERRED_CASE_FORMULA.md.
- Backup 8 valida alternativas semanticamente antes de confirmar; legado
  1-7 restaura escolhas vazias com aviso. Callback de gravacao captura geracao
  imutavel; restore invalida estado do detalhe sob Mutex CFOP compartilhado.

### Lembretes

- Agendamento local e opcional.
- Desativado por padrao.
- Sem Firebase, OneSignal ou push remoto.
- Solicitar permissao de notificacao apenas no contexto de ativacao.
- Usar `AlarmManager.setAndAllowWhileIdle` com alarme inexato, sem solicitar permissao de alarme exato.
- Reagendar apos reinicio, atualizacao do aplicativo, mudanca manual de hora ou mudanca de fuso.

## Ordem de desenvolvimento aprovada

1. Prototipo do cubo e modelo dos algoritmos.
2. Estrutura do aplicativo, temas e navegacao.
3. F2L, OLL, PLL, favoritos e concluidos.
4. Cronometro e historico.
5. Quiz.
6. Algoritmos personalizados.
7. Backup e restauracao.
8. Lembretes locais.
9. Testes, otimizacao e geracao do APK.

Cada etapa deve ser testada e receber checkpoint antes de a seguinte ser considerada concluida. `IMPLEMENTADO`, `TESTADO`, `GERADO` e `INSTALADO` permanecem estados separados.

## Decisoes tecnicas pendentes

- Estrutura exata de modulos Gradle.
- Fonte e licenca dos dados e diagramas CFOP.
- Ativo grafico final do icone e logotipo e wireframes complementares de estados secundarios.
- Estrategia de assinatura do APK final, sem versionar chaves privadas.
