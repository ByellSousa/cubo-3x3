# SCREEN_MAP

## Status

`MAPA_DE_TELAS_APROVADO_PELO_USUARIO`

Mapa de telas e navegacao aprovado explicitamente pelo usuario em 2026-09-30. A direcao visual e os conceitos principais foram aprovados posteriormente em `VISUAL_SPEC.md`; ativos finais e wireframes de estados secundarios continuam pendentes. Este documento nao autoriza implementacao.

## Estrutura aprovada

```text
Aplicativo 3x3
|-- Inicio
|   |-- Continuar estudos
|   |-- Favoritos
|   |-- Concluidos
|   `-- Atalhos CFOP
|-- Algoritmos
|   |-- F2L
|   |-- OLL
|   |-- PLL
|   `-- Detalhe do algoritmo + cubo 3D
|-- Cronometro
|   `-- Historico de tempos
|-- Quiz
|   |-- Configuracao
|   |-- Partida
|   `-- Resultado
`-- Mais
    |-- Algoritmos personalizados
    |   |-- Lista
    |   `-- Construtor/Editor
    |-- Sobre o metodo CFOP
    `-- Configuracoes
        |-- Aparencia
        |-- Cubo
        |-- Lembretes
        |-- Backup e restauracao
        `-- Dados e sobre
```

## Navegacao principal

Atualizacao RC28 (2026-10-08): Mais > Ferramentas > Metas diarias abre
configuracao opcional com salvamento explicito. Inicio mostra resumo compacto
somente se ativadas, com atalho para configurar. Voltar retorna ao destino
anterior sem gravar rascunho; recriacao preserva rascunho/rolagem, restauracao
de backup invalida esses rascunhos. Nenhum controle novo no Cronometro.
Contagens/fontes e dia local documentados em DAILY_GOALS.md.

Barra inferior aprovada com cinco destinos:

1. `Inicio`
2. `Algoritmos`
3. `Cronometro`
4. `Quiz`
5. `Mais`

Regras:

- Preservar o destino selecionado ao alternar entre abas.
- Botao Voltar respeita a pilha da aba antes de sair do aplicativo.
- Telas de detalhe e edicao nao aparecem como itens da barra inferior.
- Acoes destrutivas sempre exigem confirmacao.
- Tema e escala de texto devem funcionar em todas as telas.

## 1. Inicializacao e apresentacao

### Tela de inicializacao

Finalidade:

- carregar preferencias e dados locais;
- aplicar o tema salvo antes da primeira tela util;
- encaminhar para apresentacao inicial ou Inicio.

Nao deve:

- acessar rede;
- exibir anuncio;
- bloquear a abertura com cadastro ou permissao.

### Apresentacao inicial

Exibida somente no primeiro uso ou quando solicitada nas configuracoes.

Conteudo:

- objetivo do aplicativo;
- resumo de Cross, F2L, OLL e PLL;
- explicacao de favoritos, concluidos, cronometro e quiz;
- aviso de que os dados ficam no aparelho;
- nenhuma solicitacao de notificacao nesta etapa.

Acao principal: `Comecar`.

## 2. Inicio

Finalidade: oferecer acesso rapido ao estudo e ao progresso.

Blocos propostos:

- continuar de onde parou;
- F2L, OLL e PLL;
- favoritos;
- concluidos;
- cronometro;
- quiz de notacao;
- algoritmos personalizados;
- sobre o metodo CFOP;

Estados:

- primeiro uso, sem progresso;
- progresso existente;

## 3. Algoritmos

### Selecao de categoria

Cartoes proprios para:

- F2L;
- OLL;
- PLL.

Cada cartao informa quantidade de casos e progresso local, quando disponivel.

### Lista de algoritmos

Elementos:

- titulo da categoria;
- alternancia entre conjunto reduzido e completo, quando aplicavel;
- lista de casos;
- indicador de favorito;
- indicador de concluido;
- representacao visual propria do caso.

Estados:

- lista completa;
- filtro sem resultados, caso filtros sejam aprovados posteriormente;
- dados locais indisponiveis ou inconsistentes, com mensagem recuperavel.

### Detalhe do algoritmo

Elementos:

- identificacao do caso;
- sequencia de movimentos;
- cubo 3D;
- reproduzir, pausar, reiniciar e controlar velocidade;
- marcar/desmarcar favorito;
- marcar/desmarcar concluido;
- configuracoes visuais do cubo quando aplicavel.

Nao deve existir bloqueio por anuncio, pontos ou compra.

## 4. Favoritos e concluidos

Uma tela com duas abas internas:

- `Favoritos`;
- `Concluidos`.

Cada item abre o detalhe do algoritmo.

Estados vazios devem explicar como adicionar itens, sem tratar ausencia de dados como erro.

## 5. Cronometro

Elementos:

- embaralhamento 3x3 atual e acao `Novo` antes da tentativa;
- grande area principal com numeros monoespacados;
- toque em qualquer ponto da area para iniciar e, durante a resolucao, encerrar e salvar;
- tempo atual, melhor tempo, `mo3`, `ao5` e `ao12`;
- historico visivel na propria tela.

### Historico de tempos

- tabela cronologica com numero, tempo, `mo3`, `ao5` e `ao12`;
- toque no tempo para abrir embaralhamento, data, comentario e penalidade `OK`, `+2` ou `DNF`;
- copiar os detalhes de uma tentativa;
- exclusao individual com confirmacao;
- limpar todos com confirmacao;
- estado vazio.

## 6. Quiz

Atualizacao aprovada/implementada em 2026-10-07: abas Notacao, Reconhecer CFOP
e Revisao. Notacao conserva os fluxos abaixo. Reconhecimento mostra padrao,
quatro formulas, feedback, estudo do caso e rodada de dez sem limite de tempo.
Revisao possui filtro F2L/OLL/PLL, estatisticas, prioridade explicada, estado
vazio e rodada de ate dez casos. Ao estudar/voltar, preserva estado e rolagem.
Roteiro de aceite e criterios em `CFOP_RECOGNITION.md`.

### Configuracao

- nivel: essencial, intermediario ou avancado;
- modo: pratica ou 60 segundos;
- melhor pontuacao local;
- botao `Comecar`.

### Partida

- animacao ou representacao do movimento;
- pergunta;
- alternativas;
- pontuacao, sequencia e tempo quando aplicavel;
- retorno imediato de acerto/erro;
- confirmacao antes de abandonar uma partida.

### Resultado

- pontos;
- acertos;
- precisao;
- melhor sequencia;
- indicacao de novo recorde;
- jogar novamente ou voltar.

## 7. Algoritmos personalizados

### Lista

- pesquisa por nome;
- filtro por tags;
- criar novo;
- editar;
- duplicar;
- excluir com confirmacao;
- estado vazio orientando a primeira criacao.

### Construtor e editor

- nome obrigatorio;
- tags;
- grupos de movimentos;
- sequencia construida;
- desfazer/remover movimentos;
- previa no cubo;
- configuracao de cores e ponto de vista;
- salvar ou atualizar;
- aviso antes de descartar alteracoes.

Todas as funcoes ficam disponiveis sem Premium.

## 8. Sobre o metodo CFOP

Conteudo original em portugues:

- introducao;
- Cross;
- F2L;
- OLL;
- PLL;
- comparacao entre aprendizado reduzido e completo;
- dicas de estudo.

O texto e as ilustracoes deverao ter fonte e licenca registradas.

## 9. Configuracoes

### Aparencia

- seguir sistema;
- claro;
- escuro.

### Cubo

- velocidade de animacao;
- esquema de cores;
- ponto de vista e outras opcoes aprovadas para o visualizador.

### Lembretes locais

- chave ativar/desativar;
- horario;
- dias da semana;
- estado da permissao de notificacao;
- explicacao de que o processamento e local.

A permissao deve ser solicitada somente ao ativar a funcao.

### Backup e restauracao

- exportar `.3x3backup`;
- importar `.3x3backup`;
- resumo dos dados incluidos;
- confirmacao antes de substituir os dados atuais;
- mensagens de sucesso, arquivo invalido e versao incompativel.

### Dados

- limpar tempos;
- limpar favoritos e concluidos;
- limpar recordes do quiz;
- redefinir todos os dados;
- confirmacoes especificas para cada acao.

### Sobre

- nome e versao do aplicativo;
- explicacao de uso pessoal e funcionamento offline;
- licencas de bibliotecas e fontes de dados usadas no novo aplicativo.

## Estados globais obrigatorios

Cada fluxo aplicavel deve prever:

- carregamento local breve;
- estado vazio;
- confirmacao de acao destrutiva;
- sucesso;
- erro recuperavel;
- dados de backup invalidos ou incompativeis;
- permissao de notificacao negada;
- tema claro e escuro;
- texto ampliado sem corte de conteudo essencial.

## Pendencias visuais

- arquivo grafico final do icone `Faces` e logotipo;
- estilo do cubo e diagramas;
- densidade e formato dos cartoes;
- refinamento visual da barra inferior aprovada;
- wireframes complementares de apresentacao inicial, favoritos/concluidos, historico, resultado do Quiz e estados globais.
