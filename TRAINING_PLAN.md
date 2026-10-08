# Treino dos concluidos e desempenho por caso

## Escopo aprovado

Treino reutilizavel dos casos que o usuario marcou como concluidos, com
novos casos escolhidos para aprender. Flashcards foram dispensados.
Interface propria, offline, em Material 3. Nao adicionar paineis permanentes
ao espaco de toque do cronometro.

## Configuracao

Cronometro > Opcoes > Modo do cronometro > Meus concluidos + novos.
Selecionar etapas F2L/OLL/PLL e casos novos; aplicar inicia uma rodada e
salva a configuracao. Reutilizar a mesma selecao em outro dia sem remonta-la.
Por enquanto existe um plano salvo, nao uma biblioteca de varios planos.

Outros modos preservados: Treino livre WCA, categoria aleatoria completa e
caso especifico. A regra sem repeticao e exclusiva da rodada dos concluidos.
Sem concluido nem novo selecionado, nao iniciar treino/vazar para sorteio
livre ou todos os casos. Permitir selecionar novos mesmo sem concluidos.

Selecao = uniao dos concluidos reais nas etapas selecionadas e IDs extras
validos dessas etapas. Favorito sozinho nao significa concluido. Nao
duplicar caso extra que depois seja marcado como concluido. Invalidar
configuracoes fora do catalogo (41 F2L, 57 OLL, 21 PLL).

## Rodada

- Congelar membros e ordem embaralhada no inicio; novas marcacoes entram
  somente na proxima rodada, inclusive quando clicar Repetir treino.
- Gerar cada setup com AlgorithmCatalog.setupNotation: inverso auditado mais
  orientacao dos centros. Partir do cubo resolvido na orientacao didatica:
  amarelo acima, branco abaixo, vermelho a frente e verde a direita.
- Ao finalizar um tempo positivo, registrar tentativa com setup/categoria/
  numero e contar aquele caso uma vez na cobertura. DNF conta como tentativa,
  nao como caso dominado. Repetir um caso pelo historico salva outro tempo,
  mas nao incrementa duas vezes a cobertura da rodada.
- Prever/pular/navegar/cancelar nao conta. Casos pulados permanecem pendentes.
  Escolher o proximo entre pendentes; ao cobrir todos, interromper a rodada e
  oferecer Repetir treino. Nenhuma marcacao de favorito/concluido e modificada.
- Guardar fila, cobertura, setup e posicao em estado salvavel da interface,
  mantendo-os ao trocar aba ou recriar atividade. Nao regenerar ao voltar.
  Toques durante geracao nao iniciam tentativa e finalizacao le estado atual,
  nao uma closure RUNNING antiga.

## Persistencia e backup

Plano reutilizavel no DataStore app_preferences/cfop_training_plan, com
CfopTrainingPlanCodec versao 1. Backup proprio inclui configuracao desde o
esquema 7 (atual 8); le esquemas 1 a 6 usando Treino livre, com aviso
antes da substituicao.
Codec estrito, limitado, rejeita membros/duplicatas/campos incoerentes.
Estado transitorio da rodada nao e exportado. Restauracao confirmada invalida
estado e writes pendentes e reconstrui usando progresso/plano restaurados.
Plano/agregados nao exigiram alteracao de Room 8 na etapa RC24; Room atual
9 acrescentou apenas formulas preferidas. Portabilidade csTimer inalterada.

## Desempenho de execucao

Cronometro > Analise > Desempenho CFOP > Ver desempenho por caso.
Painel recolhido por padrao, filtros de etapa e grafico individual expansivel.
Somente tempos direcionados da sessao e periodo escolhidos: tentativas,
DNFs, ultimo resultado, melhor, media valida e evolucao cronologica.
Media aritmetica inclui +2 e exclui DNF, sem confundir com ao5/ao12 ou
precisao do reconhecimento. Todos DNF: sem valor numerico/grafico inventado.
Agregados derivados, nao persistidos; dados brutos permanecem inalterados.

## Validacao e limites

JVM: selecao/conjuntos/filtros, vazio, caso unico, 20 rodadas de 119 casos,
setup contra os 54 adesivos de todos os casos, formas originais, deduplicacao,
previews, congelamento, codecs/backup 1 a 7 e analise/penalidades/cronologia.
Android isolado: construtor e recriacao, rodadas/toques/retorno e analise;
backup usa Room em memoria e DataStore privado exclusivo por teste.
Compilar testes nao equivale a executar nem a confirmar visualmente no celular.
Validacao fisica/usabilidade dependem de ciclo novo autorizado, preservando
historico atual importado. Nao restaurar snapshots RC23 antigos.

Revisao espacada acrescentada na etapa RC25 em Quiz > Revisao > Por prazo,
usando respostas de reconhecimento, nao tempos deste treino. Contrato em
CFOP_SPACED_REVIEW.md. Formula preferida validada acrescentada no RC26,
sem mudar o setup deste treino, em PREFERRED_CASE_FORMULA.md. Comparacao
dos casos confundidos e metas diarias permanecem aprovadas e pendentes.
