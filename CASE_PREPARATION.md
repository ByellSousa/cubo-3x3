# Preparacao de casos e editor offline

## Contrato aprovado em 2026-10-07

Cada caso CFOP oferece duas origens: cubo resolvido ou configuracao manual das
seis faces. O destino e o estado completo de AlgorithmCatalog.initialState,
nao apenas uma mascara visual. A formula de resolucao original permanece
inalterada, incluindo apostrofos em movimentos duplos.

## Cartao Sequencia - RC20

O detalhe regular de cada caso mostra Resolucao (caso -> resolvido) e
Preparacao (resolvido -> caso) no mesmo cartao. A segunda formula usa
AlgorithmCatalog.setupNotation, isto e, a inversa com os ajustes de
orientacao descritos abaixo, nao uma inversa literal com centros girados.
O texto informa amarelo acima, branco abaixo, vermelho a frente e verde a
direita como ponto inicial. Copiar preparacao nao inicia animacao nem altera
o contador/destaque da resolucao.

Preparar este caso continua abrindo a reproducao propria. O bloco novo nao
aparece nesse player nem no resultado calculado a partir da pintura.
Regressao local RC20: 142 JVM passaram, incluindo a conferencia exata dos
119 preparos nos 54 adesivos e preservacao/ordem reversa de todos os giros 2'.
Tres testes de interface F2L/OLL/PLL adicionais foram compilados/conferidos
no DEX, mas NAO EXECUTADOS; a suite de preparacao agora tem 19 testes.
RC20 preparado, nao instalado nem validado visualmente no celular.

Atualizacao fisica autorizada em 2026-10-07: RC20/codigo 29 instalado no
Samsung/API 34; 19/19 testes desse fluxo passaram em 44,549 s.
Cartao F2L 1 inspecionado em captura real, com copia confirmada e animacao 1/3.
Banco/preferencias pessoais preservados contra snapshot atual anterior;
apenas metadados Android diferentes. Aceitacao com cubo fisico e fontes
ampliadas permanece pendente. Evidencias em ARTIFACT_INDEX.md.

## Orientacao e preparacao

Amarelo U, verde R, vermelho F, branco D, azul L e laranja B. As faces sao
informadas olhando cada uma de frente; o editor mostra os quatro centros
vizinhos para orientar as linhas. Centros fixos; pintura inicial resolvida.

O alvo resolvido antes de inverter a formula considera os centros resultantes
da propria formula. A preparacao primeiro orienta o cubo resolvido por busca
nas 24 rotacoes rigidas e depois executa a formula inversa. Isso evita que
formulas com rotacoes, fatias e movimentos largos gerem diagramas com centros
girados. Aplica-se a F2L, OLL e PLL, inclusive ao setup do treino direcionado.

## Transformacao direta

CubeFacelets codifica e decodifica os 54 adesivos em URFDLB pelo modelo proprio.
CaseSolver usa CubeConfigurationValidation para validar contagens, centros,
inventario de pecas e quiralidade dos cantos pelo modelo proprio. Depois,
Tools.verify da dependencia existente confere orientacao de arestas/cantos e
paridade. O parser externo isolado nao basta para provar legalidade fisica.

Cada adesivo e identificado pela combinacao de cores da peca e pela sua cor.
A composicao atual * inverso(alvo) e expressa como um cubo relativo legal.
SearchWCA resolve esse relativo; a mesma sequencia transforma o atual no alvo.
Antes de mostrar o resultado, o motor proprio aplica todos os giros e exige
igualdade dos 54 adesivos por posicao e normal. Limite de busca: 21 movimentos
de face; nao se promete otimalidade.

## Responsividade e privacidade

Busca fora da thread de interface, com fila serial cancelavel. A busca Java
ativa tem limite interno de 3000 ms; inicializacao das tabelas nao esta inclusa
nesse limite. Cancelar descarta o resultado e libera a UI imediatamente, mas
nao interrompe forcosamente Java no meio de uma busca. Nova requisicao aguarda
a anterior com cancelamento cooperativo e nao publica resultados obsoletos.

O worker verifica cancelamento entre validacao/composicao, inicializacao das
tabelas, busca e verificacao final. Cancelar durante Search.init impede a busca
seguinte ao retornar da inicializacao. Cancelar na fila nao entra no motor;
erro ou cancelamento libera a fila ao terminar a chamada bloqueante em curso.

Pintura e resultado usam estado salvavel do fluxo do caso; a pintura e
preservada entre editor/resultado/preparacao. Nao sao persistidos no Room,
exportados no backup nem transmitidos. Reiniciar a pintura exige confirmacao.
Sair do fluxo encerra o calculo. Esquemas Room 7 e backup 5 nao mudam.

## Verificacao

Suite inicial: codec comparado com Tools.fromScramble em 80 sequencias;
preparacao exata dos 119 casos; 238 fontes aleatorias com semente fixa,
duas por destino, conferidas integralmente; casos ja alcancados; rejeicao de
contagens, centros, aresta invertida, canto torcido e paridade impossivel.

Primeira amostra JVM: primeiro calculo 182 ms, mediana dos seguintes 2 ms,
maximo dos seguintes 23 ms. Nao e benchmark do telefone nem garantia de prazo.
Fluxo visual, ergonomia e latencia fria/quente no Samsung ainda precisam ser
validados em nova instalacao autorizada.

Candidato final RC15: 111 testes JVM passaram; lintDebug sem erros, com oito
avisos anteriores. Ultima amostra durante build concorrente: primeiro calculo
387 ms, mediana dos demais 3 ms, maximo 94 ms. Os termos primeiro/demais nao
substituem um benchmark controlado de inicializacao fria/quente.
Cinco testes Compose de fluxo, pintura, reinicio, resultado e cancelamento
foram compilados e confirmados no APK instrumentado; ainda nao executados.
O usuario determinou preparar somente o APK, sem instalacao nesta etapa.

## Checkpoint do fluxo RC16

O reprodutor salva identificador da sequencia/estado inicial e numero de giros
concluidos. Reconstrucao reaplica exatamente esses giros: movimento interrompido
nao conta, inclusive ao recuar. Recriacao retoma pausada e segundo plano
interrompe reproducao. Formula, setup e resultado preservam passos separados;
editor conserva face, paleta, pintura e rolagem ao retornar dentro do caso.
Resultado novo nao reutiliza checkpoint de calculo anterior. Sao estados
salvaveis da interface, nao novos registros Room nem campos do backup.

RC16: 114 JVM passaram, incluindo reconstrucao em todos os passos dos 119
setups e giros duplos primados. Lint zero erros/oito avisos anteriores.
CasePreparationScreenTest agora tem nove testes Compose compilados, quatro
deles novos para restauracao, giros interrompidos e navegacao. NAO EXECUTADOS:
AVD sem acelerador; tentativa readonly sem aceleracao nao manteve emulador
ativo. APK apenas preparado. Validacao visual e velocidade no celular pendentes.

## Conferencia de pintura RC17

Contagens com falta/excesso sao atualizadas a cada pintura. Configuracao invalida
desabilita o calculo; o solver repete a validacao independentemente da interface.
Pecas incompativeis, repetidas ou espelhadas recebem ! e semantica de revisao.
Atalhos selecionam as faces e trazem a grade para a tela. Em duplicatas, todas as copias sao
marcadas; isso nao identifica qual adesivo foi pintado incorretamente.
Flip, twist e paridade nao indicam um adesivo especifico. Sem correcao automatica.

Testes revelaram estado com cantos impossiveis aceito por Tools.verify 0.20.0
(troca dos adesivos 0/9 no resolvido). Inventario proprio rejeita combinacoes
inexistentes e duplicadas antes do parser. Cantos espelhados sao detectados pelo
determinante das tres normais na mesma ordem de cores; uma rotacao legitima
preserva esse determinante. Orientacao e paridade globais seguem no motor.

Regressao final RC17: 122 JVM, oito testes novos de validacao, incluindo 500
estados legais aleatorios, 119 casos e resolvido, flips nas 12 arestas,
twists nos oito cantos nos dois sentidos e 24 espelhamentos. Lint zero erros/
oito avisos anteriores. Onze testes Compose do fluxo compilados, nao executados.
Debug preparado sem instalacao; nenhum novo teste de AVD ou operacao no celular.

## Cancelamento e concorrencia RC18

Worker serial extraido para regressao determinista, mantendo Dispatchers.Default
e Mutex. A entrada assincrona continua validando fisicamente origem e destino,
e o resultado continua exigindo igualdade integral dos 54 adesivos.

Sete testes JVM novos cobrem cancelamento na fila, chamada bloqueante que ignora
cancelamento, descarte do retorno, ausencia de sobreposicao, retry apos erro,
thread distinta da chamadora, fronteiras antes/depois da inicializacao e cubo
impossivel na API assincrona. Seis pedidos reais simultaneos usam fontes/destinos
diferentes e foram conferidos pelo modelo proprio. Total: 129 JVM passaram;
lint zero erros/oito avisos anteriores. Nao e benchmark de latencia do telefone.

Treze testes Compose do fluxo compilados, incluindo falha seguida de retry e
restauracao durante calculo sem reinicio automatico da busca. Os dois metodos
novos foram conferidos no DEX; NAO EXECUTADOS. RC18 debug apenas preparado,
sem instalacao, operacao no celular, nova tentativa de AVD ou mudanca Windows.

## Correcao de pintura RC19

Desfazer/refazer mantem ate 100 alteracoes (101 estados completos). Uma nova
pintura depois de desfazer substitui somente o caminho futuro; tocar a cor
ja existente nao muda historico nem redo. Reiniciar segue exigindo confirmacao,
mas a troca das seis faces e um unico passo que pode ser desfeito.
Quando uma unica face muda, undo/redo selecionam essa face; paleta permanece.
Ambos os controles ficam bloqueados durante calculo.

O historico e imutavel e usa checkpoint versionado, limitado e com cursor.
Todos os snapshots devem ter 54 adesivos das seis cores e centros canonicos.
Contagens/pecas fisicamente invalidas sao permitidas como rascunho, nunca como
entrada direta sem a validacao normal do solver. Checkpoint malformado e
rejeitado como um todo. Historico e pintura permanecem locais ao estado da
interface do caso, inclusive retorno do resultado/restauracao, sem Room/backup.

Doze testes novos JVM passaram: 48 adesivos editaveis em todas as cores,
centros protegidos, no-op, ramificacoes, reset reversivel, limite de memoria,
checkpoint e 1000 operacoes conferidas contra timeline independente.
Tres configuracoes restauradas depois de undo chegaram por calculo real a
destinos F2L/OLL/PLL, conferidos pelo modelo proprio nos 54 adesivos; pintura
e historico nao foram modificados pelo calculo. Total final: 141 JVM passaram,
lint zero erros/oito avisos anteriores e APKs debug/testes gerados offline.
Dezesseis testes Compose do fluxo compilados; tres novos metodos confirmados
no DEX, ainda NAO EXECUTADOS. RC19 apenas preparado; interface e latencia
no celular ainda pendentes. Sem nova tentativa de AVD ou alteracao Windows.

## Validacao fisica RC19 em 2026-10-07

RC19/codigo 28 instalado com autorizacao no Samsung SM-A256E, Android 14/API 34,
atualizando RC14 sem desinstalar/limpar. O APK do app nao mudou neste ciclo.
Primeira rodada: 14/16 Compose. Falhas no proprio teste: contagem fora da
viewport e falta de frames entre as fases da restauracao com relogio pausado.
Correcao adiciona rolagem explicita e frames; DisposableEffect comprova o
descarte da tela antiga. Indice antes e depois de interromper giro para frente/
tras continua sendo o numero de giros inteiros concluidos. Rodada dirigida 2/2
e suite final 16/16 passaram (54,839 s). Os tres testes de undo/redo RC19 passaram.
Esses testes exercitam estado salvo da composicao, nao morte real do processo.

CaseSolverDeviceTest executado separadamente em novo processo, sem UI ou dados
pessoais. Fontes artificiais legais geradas com seed 20261007; API assincrona
normal e conferencia independente no CubeState de todos os 54 adesivos.

| Caso alvo | Worker ms | Ponta a ponta ms | Giros |
|---|---:|---:|---:|
| F2L 1 (primeiro pedido) | 774 | 785 | 20 |
| OLL 2 | 102 | 104 | 21 |
| PLL 18 | 26 | 27 | 21 |
| F2L 33 | 110 | 111 | 21 |
| OLL 57 | 37 | 38 | 20 |
| PLL 21 | 22 | 23 | 21 |

Primeiro pedido inclui inicializacao; demais tiveram mediana ponta a ponta
38 ms. Nao e garantia para todo cubo nem benchmark de interacao/pintura manual.
No app real, F2L 1 desde pintura resolvida mostrou 580 ms, animou passo 1/3
e chegou a 3/3. Capturas reais de setup/editor/contagens/resultado inspecionadas.
Validacao fisica manual do cubo pelo usuario permanece pendente.

Banco Room 7 conferido integralmente contra snapshot atual anterior a instalacao:
5 tempos, 6 algoritmos personalizados, 1 recorde Quiz, 1 sessao e 0 marcacoes,
mesmos valores em todas as tabelas/colunas e integridade OK. DataStore pessoal
identico em bytes. Somente arquivos internos profileInstalled e ActivityThread.IDS
mudaram; igualdade integral false, dados pessoais preservados true.
Logs, copias privadas e manifesto em analysis/rc19-device-20261007-112552/,
ignorados pelo Git e indexados em ARTIFACT_INDEX.md. Sem nova versao/publicacao.
