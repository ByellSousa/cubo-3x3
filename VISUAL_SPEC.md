# VISUAL_SPEC

## Status

`DIRECAO_E_CONCEITOS_PRINCIPAIS_APROVADOS_PELO_USUARIO`

A direcao visual, o conceito inicial das telas Inicio, Algoritmos e Cronometro e os conceitos dos fluxos Detalhe do algoritmo, Quiz, Criador e Mais foram aprovados explicitamente pelo usuario em 2026-10-01. Essa aprovacao nao autoriza implementar o aplicativo Android nem aprova automaticamente estados e wireframes complementares ainda nao apresentados.

## Linguagem visual aprovada

- Estilo moderno, limpo e tecnico.
- Formas inspiradas nos blocos do cubo, sem copiar a identidade do aplicativo de referencia.
- Material 3 como base, adaptado para uma identidade propria.
- Temas claro e escuro completos, alem do modo automatico conforme o sistema.
- Cartoes com cantos moderadamente arredondados, icones simples e pouco relevo.
- Cores tradicionais do cubo reservadas principalmente para diagramas e animacoes, evitando excesso de cor na interface.

## Paleta aprovada

| Uso | Tema claro | Tema escuro |
|---|---|---|
| Fundo | `#F7F8FC` | `#0E1015` |
| Superficie | `#FFFFFF` | `#191C24` |
| Primaria | `#4D5DFF` | `#9AA4FF` |
| Texto principal | `#171923` | `#F3F4F7` |
| F2L | `#008FA8` | `#59D7EA` |
| OLL | `#C47A00` | `#FFC857` |
| PLL | `#7447C8` | `#C4A1FF` |

Regras:

- F2L usa azul-ciano, OLL usa amarelo-ambar e PLL usa violeta.
- Cor nunca sera o unico meio de comunicar categoria, estado ou resultado.
- Estados de erro, sucesso e aviso deverao manter contraste e significado proprios.
- Os tokens finais do Material 3 poderao receber pequenos ajustes de contraste durante testes de acessibilidade, preservando a direcao aprovada.

## Tipografia

- Inter como fonte principal proposta e aprovada para a interface.
- Numeros do cronometro usam uma fonte monoespacada legivel.
- Pesos visuais contidos, evitando excesso de negrito.
- Texto deve respeitar ampliacao do Android sem cortar conteudo essencial.
- Arquivos, versoes e licencas das fontes devem ser validados antes de incorporar ativos ao aplicativo.

## Icone e marca

Direcao aprovada:

- cubo 3x3 isometrico original;
- tres faces visiveis;
- movimento visual sutil;
- geometria simples que funcione em tamanhos pequenos;
- nenhuma copia do icone, marca ou composicao do aplicativo de referencia.

A variante conceitual `Faces` foi escolhida e aprovada pelo usuario em 2026-10-01. Ela usa um cubo 3x3 isometrico original com tres faces visiveis. O arquivo grafico final de producao e o logotipo ainda precisam ser criados e validados. O bloco `3x3` usado no conceito inicial permanece apenas uma marca temporaria de interface.

## Componentes e espacamento

- Cartoes principais com raios visuais moderados, aproximadamente entre 16 e 20 dp.
- Botoes e alvos de toque com area adequada para Android.
- Barra inferior com os cinco destinos aprovados e destaque discreto da aba ativa.
- Pouco relevo e sombras suaves apenas quando ajudarem a hierarquia.
- Conteudo organizado por proximidade e importancia, sem paineis decorativos desnecessarios.
- Redesign do cronometro aprovado em 2026-10-06: area do timer dominante, embaralhamento compacto e medias discretas; historico em tela propria, sessao/treino/inspecao/som no painel Opcoes. O cabecalho global nao ocupa a tela do timer; controles e barra inferior do app ficam ocultos durante inspecao/resolucao, retornando apos concluir ou cancelar com confirmacao. Preservar margens seguras do Android e identidade Material 3 propria.

## Movimento e acessibilidade

- Animacoes de interface curtas e suaves.
- Respeitar a preferencia de reducao de movimento do Android.
- Contraste forte em temas claro e escuro.
- Texto ampliavel e alvos de toque acessiveis.
- Icones acompanhados por rotulos quando a acao nao for obvia.
- Informacao nunca representada somente por cor.

## Conceito inicial criado

O primeiro conceito navegavel inclui:

- tela Inicio com continuidade de estudo, progresso CFOP e acessos rapidos;
- tela Algoritmos com F2L/OLL/PLL, conjuntos essencial/completo e lista de casos;
- tela Cronometro com estado inicial, execucao local demonstrativa e tempos recentes;
- troca local entre tema claro, escuro e automatico;
- navegacao demonstrativa entre as tres telas.

O conceito foi validado tecnicamente em larguras de telefone, nos temas claro e escuro, com troca de telas e cronometro demonstrativo. O usuario o aprovou em 2026-10-01. Ele permanece apenas uma referencia de design e nao e codigo do aplicativo Android.

## Refinamento aprovado do cronometro

Em 2026-10-06, o usuario substituiu o conceito inicial do cronometro por uma experiencia voltada a resolucao real:

- os numeros monoespacados ocupam uma grande area central e continuam sendo o foco visual;
- toda a area do tempo funciona como alvo para iniciar e encerrar, evitando um pequeno botao durante a resolucao;
- embaralhamento, estatisticas e historico permanecem visiveis sem copiar o layout das referencias externas;
- a tabela mostra tempo, `mo3`, `ao5` e `ao12`;
- o detalhe de uma tentativa segue os componentes e temas ja estabelecidos no aplicativo.

## Conceitos complementares criados

O segundo conceito navegavel inclui:

- detalhe do algoritmo com cubo isometrico, sequencia e controles de reproducao;
- partida de Quiz com resposta e pontuacao demonstrativas;
- construtor de algoritmos com teclado de movimentos e desfazer;
- tela Mais com configuracoes e acessos secundarios;
- troca entre tema claro, escuro e automatico;
- comparacao das variantes de icone `Faces`, `Giro` e `Monograma`.

Os fluxos principais e a variante `Faces` foram aprovados pelo usuario em 2026-10-01. O conceito permanece referencia de design fora do Git e nao constitui implementacao Android.

## Refinamentos de Quiz e navegacao

Em 2026-10-06, a visualizacao do Quiz foi refinada apos testes sucessivos no aparelho:

- cada pergunta repete um ciclo curto de posicao inicial, giro, pausa e reinicio;
- o cubo conserva o angulo, as cores e a linguagem visual do prototipo;
- cada cubinho e desenhado como bloco solido durante o giro, com faces internas escuras;
- nao usar setas sobrepostas nem adesivos isolados, pois ambos se mostraram confusos no aparelho;
- giros duplos exibem um selo `2x` e recebem tempo de animacao ligeiramente maior;
- F2L, OLL e PLL na Inicio sao alvos tocaveis diretos;
- listas retornam a mesma posicao depois que o usuario abre e fecha um caso.

## Pendencias visuais

- Producao do arquivo grafico final do icone `Faces` e do logotipo.
- Wireframes complementares de apresentacao inicial, favoritos/concluidos, historico, resultado do Quiz e estados globais.
- Refinamento do cubo, diagramas e estados de animacao.
- Validacao final de contraste, texto ampliado e reducao de movimento na implementacao real.
