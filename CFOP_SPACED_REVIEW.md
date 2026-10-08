# Revisao espacada CFOP

## Escopo e dados

Quiz > Revisao > Por prazo, mantendo Por erros com a prioridade original.
Nao sao flashcards: a rodada usa o reconhecimento existente, diagramas
proprios e quatro alternativas verificadas. Sem revelar numero antes da
resposta. Nao marcar favorito/concluido automaticamente.

Somente respostas CfopAttempt de reconhecimento; o timer tem sua analise
de execucao separada. Nao usar tempos importados como acertos. Casos sem
respostas nao recebem dificuldade, nivel ou prazo ficticios.

## Agenda deterministica

- Deduplicar por identificador e ordenar cada caso por data, depois id.
- Primeiro acerto agenda em 1 dia. Novo erro agenda imediatamente e reinicia
  progressao. Acerto apos erro volta ao intervalo de 1 dia.
- Acertos em/apos o prazo avancam intervalos: 1, 3, 7, 14, 30 dias, mantendo
  30 no limite. Cada nova revisao vencida ancora prazo na data da resposta.
- Acertos antes do prazo nao avancam nem adiam a agenda, inclusive repeticoes
  no mesmo dia. Erros antes do prazo reiniciam normalmente.
- Dia = 24 horas em milissegundos UTC; vencimento inclusivo. Exibir datas no
  fuso local. Mudanca de fuso nao altera duracao; mudanca do relogio recalcula
  o que esta vencido, sem reescrever respostas.
- Fila devidos: ultimo resultado errado primeiro; depois proporcao de erros
  nas ultimas dez respostas, quantidade de erros, vencimento antigo e numero.
  Empates estaveis. Proximas revisoes: prazo, depois numero.
- Por erros continua considerando erros recentes mesmo antes do prazo.
  Treino voluntario antecipado nesse modo nao infla a progressao por prazo.

Regra simples propria, nao promessa de memoria, dominio ou intervalo ideal.
Sem notificacoes novas ou configuracoes adicionais nesta etapa.

## Interface e continuidade

Exibir devidos, quantidade futura e proximo prazo; agenda futura recolhida.
Rodada congela ate dez IDs da etapa/modo escolhidos. Novas respostas, prazo
que vence, filtro ou relogio nao trocam perguntas da rodada em andamento.
Filtro, agenda aberta e rolagem sao salvaveis ao consultar detalhe/recriar.
Atualizar relogio ao retornar ao app e periodicamente na lista; inicio da
rodada confere prazo atual, nao depende de lista envelhecida em segundo plano.

Sem persistencia redundante, migracao Room ou mudanca de backup: agenda
reconstituida do historico CFOP existente (atualmente Room 9, backup 8).
Restauracao substitutiva continua invalidando respostas/rodadas antigas.

Comparar casos confundidos fica acessivel na revisao sem iniciar rodada.
Usa janela recente de Por erros, nao vencimentos de Por prazo. Consultar
diagramas/formulas ou abrir um caso nao muda intervalo/proxima revisao;
regras em CFOP_COMPARISON.md. Nao e uma resposta de reconhecimento.

## Aceite

JVM: vazio, categoria, deduplicacao, cronologia, prazos inclusivos, acertos
antecipados, progressao/limite, erros/reaprendizagem, UTC, overflow, prioridades
e reproducao por historico exportado. Revisao por erros permanece coberta.
Compose isolado: agenda, filtro, restauracao/retorno, rodada congelada e
recalculo do relogio. Compilacao nao equivale a execucao/validacao visual.
Instalacao futura exige nova autorizacao e preservacao dos dados atuais.

RC25 preparado em 2026-10-07: 221 JVM passaram (21 novos agenda + dois backup),
lint 0 erros/6 avisos, APKs gerados e assinaturas/versao/classe DEX conferidas.
Sete Compose novos compilados, nao executados. Sem acesso ao celular.
Room 8/backup 7 inalterados; validacao visual/operacional futura pendente.
