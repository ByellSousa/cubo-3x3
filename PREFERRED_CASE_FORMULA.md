# Formula preferida por caso

## Uso e limites

No detalhe: Minha formula > editar, conferir e salvar. Uma alternativa por
caso, sem substituir catalogo/original. Original/Preferida alternam somente
reproducao; apagar exige confirmacao e devolve a original como padrao.
Entrada aceita ate 2.000 caracteres/200 movimentos, nao vazia; caixa, largos,
fatias, rotacoes e primas duplas preservados. Espacos normalizados pelo
parser proprio, sem simplificar/reordenar movimentos.

Validacao sobre AlgorithmCatalog.initialState do caso ORIGINAL, nunca sobre
um estado reconstruido da formula pessoal. Mesmo criterio do reconhecimento:
F2L primeiras duas camadas, OLL primeiras camadas + amarelo orientado,
PLL todas as faces resolvidas. Interpretar centro/cores apos rotacoes.
Formula que nao resolve a etapa alvo ou que estrague camadas anteriores e recusada.
Nao prometer formula minima; valida equivalencia da etapa, nao rapidez.

## Reproducao e os dois sentidos

- Sempre partir do caso canonico; contadores/checkpoints identificam notacao
  ativa e estado inicial, troca para original/preferida reinicia pausada.
- Catalogo, diagramas, busca, quiz e setup do treino continuam originais.
  Quiz/revisao nao mudam suas alternativas pela preferencia.
- Preparacao desde resolvido continua sendo a formula original auditada,
  com ajustes de centros e conferencia dos 54 adesivos.
- Mostrar/copy inversa pessoal como Voltar ao caso a partir do resultado
  desta formula. F2L/OLL podem deixar a ultima camada diferente; essa inversa
  NAO e anunciada como setup desde resolvido. Conferir retorno exato em testes.
- Edicao/gravacao/verificacao fora da interface quando houver calculo.
  Cancelamento/erro nao altera preferencia; descartar rascunho exige confirmacao.
  Nao modificar favorito/concluido/historicos de reconhecimento ou execucao.

## Persistencia e backup

Room 9: preferred_case_formulas, chave categoria + numero, notacao e data.
Migracao 8->9 somente acrescenta tabela vazia; migracoes anteriores mantidas.
Backup 8 inclui colecao obrigatoria (max119), com IDs/catalogo, notacao
e resultado semantico validados antes de permitir confirmacao.
Le 1-7 com preferencias vazias e aviso explicito; restaura substitutivamente
em uma transacao junto com tabelas atuais. Nao mesclar preferencias legadas.

Restauracao confirmada invalida editor/reprodutor e gravacoes pendentes sob
o mesmo Mutex/geracao do historico CFOP. Callback antigo nao pode repovoar
formula que o backup substituiu. Dados pessoais atuais nunca usados como fixture.

## Aceite

JVM: 119 originais aceitas, alternativas validas por etapa, tokens invalidos,
vazio/limites, primas duplas, rotacoes, camadas anteriores, inversa exata e
backup 1-8, duplicatas/campos/semantica. Room/Compose com dados isolados,
migracao 8->9 e cadeia, persistencia/remove/backup e recriacao/troca de formula.
Compilar Android nao implica executar no celular ou validar usabilidade.

## Evidencia local RC26

2026-10-07: 15 JVM do motor e quatro de backup passaram; suite completa
240/37 suites, sem falhas/erros/skips. lintDebug zero erros/seis avisos.
APKs app/debug e AndroidTest gerados, assinatura v2/versao/classes conferidas.
Nove Compose, tres Room, dois backup e dois migracao novos COMPILADOS,
NAO EXECUTADOS. Gradle final passou apos ajustar reset no texto identico.

tools/verify_rc26_schema.py: SQL exato da migracao e exports 8/9 contra
SQLite em memoria sintetico. Valores/esquemas antigos preservados, nova
tabela vazia/estrutura correta, integridade e quatro controles negativos.
Esta prova NAO e execucao de migracao Room no Android; celular nao acessado.

Aceite fisico futuro exige nova autorizacao, snapshot privado ATUAL,
conferencia de dados importados e teste de edicao/copia/temas/texto ampliado.
Nunca usar snapshots anteriores a importacao pessoal como estado de teste.
