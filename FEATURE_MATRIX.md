# FEATURE_MATRIX

Esta matriz registra o escopo aprovado para a primeira versao. `ADIADO` e `FORA_V1` nao autorizam implementacao nesta etapa.

## Atualizacao aprovada em 2026-10-07

### Segunda aprovacao: treino e evolucao

| Funcao | Decisao / estado |
|---|---|
| Flashcards | DISPENSADO pelo usuario; nao implementar |
| Treino reutilizavel dos concluidos + novos casos escolhidos | IMPLEMENTADO / TESTADO_LOCALMENTE / PREPARADO RC24; 12 JVM, cinco Compose construtor e cinco timer compilados, aparelho pendente |
| Revisao espacada | IMPLEMENTADO / TESTADO_LOCALMENTE / PREPARADO RC25; 21 JVM e dois backup passaram, sete Compose compilados; aparelho pendente, Por erros preservado |
| Formula pessoal preferida por caso | IMPLEMENTADO / TESTADO_LOCALMENTE / PREPARADO RC26; 15 JVM e quatro backup passaram, 16 Android compilados; Room9/backup8, original/setup preservados; aparelho pendente |
| Comparacao de casos confundidos | IMPLEMENTADO / TESTADO_LOCALMENTE / PREPARADO RC27 em 2026-10-08; 20 JVM novos, 2.626 pares/pistas conferidos, nove Compose compilados; Room9/backup8 preservados, aparelho pendente |
| Desempenho de execucao por caso | IMPLEMENTADO / TESTADO_LOCALMENTE / PREPARADO RC24; sete JVM e dois Compose compilados, aparelho pendente; separado de reconhecimento |
| Metas diarias curtas | IMPLEMENTADO / TESTADO_LOCALMENTE / PREPARADO RC28 em 2026-10-08; 21 JVM novos, nove Compose e tres Room/DataStore compilados, nao executados; opcionais Mais/Inicio, fora do timer; Room9/backup9, aparelho pendente |

Nao ha autorizacao nesta etapa para instalar nova versao ou publicar.
Manter o espaco amplo do cronometro e os dados importados do csTimer.

As seis melhorias desta segunda aprovacao estao implementadas localmente.
Isso nao encerra a validacao fisica/visual, testes Android e eventuais correcoes.
Fechamento/assinatura do APK final exige estrategia autorizada. GitHub privado
foi escolhido, mas preparar/criar/enviar repositorio permanece adiado pelo usuario
ate concluir as etapas locais; nenhum remoto ou upload nesta etapa.

As decisoes posteriores documentadas prevalecem sobre a matriz inicial abaixo
somente em seus escopos especificos. O usuario aprovou estas tres melhorias:

| Etapa | Funcao | Estado |
|---|---|---|
| 1 | Busca/filtros do catalogo e bibliotecas salvas | INSTALADO no RC23; seis testes isolados e cinco de navegacao real passaram no Samsung, incluindo busca/rolagem ao voltar |
| 2 | Quiz de reconhecimento F2L/OLL/PLL | INSTALADO no RC23; seis testes isolados passaram e telas reais das tres etapas inspecionadas; persistencia ponta a ponta e cubo fisico pendentes |
| 3 | Revisao dos casos dificeis por erros/desempenho | INSTALADO no RC23; cinco testes isolados passaram, fila/estado/retorno cobertos e revisao vazia real inspecionada; historico pessoal nao recebeu respostas de teste |

## Matriz inicial

| Area | Funcao observada ou candidata | Recomendacao inicial | DECISAO_APROVADA |
|---|---|---|---|
| Navegacao | Inicio com acesso aos modulos principais | ALTERAR: criar identidade e hierarquia proprias | ALTERAR |
| Aprendizado | Explicacao introdutoria do metodo CFOP | MANTER, com texto original e fontes independentes | MANTER |
| Algoritmos | Catalogo F2L | MANTER, apos validacao tecnica independente | MANTER |
| Algoritmos | Catalogo OLL | MANTER, apos validacao tecnica independente | MANTER |
| Algoritmos | Catalogo PLL | MANTER, apos validacao tecnica independente | MANTER |
| Algoritmos | Alternancia reduzido/completo | MANTER e explicar a progressao didatica | MANTER |
| Algoritmos | Pesquisa e filtros gerais | ADICIONAR ou ampliar | ADIADO |
| Algoritmos | Reordenacao manual dos cards | ADICIONAR; aparece como pedido recorrente de usuario na loja | ADIADO |
| Visualizacao | Cubo 3D animado | MANTER com implementacao/licenca propria | MANTER |
| Visualizacao | Controle de velocidade | MANTER | MANTER |
| Visualizacao | Cores e ponto de vista configuraveis | MANTER | MANTER |
| Progresso | Favoritos | MANTER | MANTER |
| Progresso | Marcacao de concluidos | MANTER | MANTER |
| Progresso | Estatisticas de estudo | ADICIONAR | ADIADO |
| Treino | Cronometro | MANTER | MANTER |
| Treino | Historico de tempos | MANTER | MANTER |
| Treino | Scrambles gerados | ADICIONAR | ADIADO |
| Quiz | Quiz visual de notacao | MANTER | MANTER |
| Quiz | Niveis essencial/intermediario/avancado | MANTER | MANTER |
| Quiz | Modos pratica e 60 segundos | MANTER | MANTER |
| Quiz | Recordes, precisao e sequencia | MANTER | MANTER |
| Personalizados | Construtor visual de algoritmos | MANTER | MANTER E LIBERAR |
| Personalizados | Tags, pesquisa e filtros | MANTER | MANTER |
| Personalizados | Editar, duplicar e excluir | MANTER | MANTER |
| Personalizados | Previa do cubo | MANTER | MANTER |
| Dados | Backup e importacao local | MANTER com formato documentado e versionado | ALTERAR PARA `.3x3backup` |
| Dados | Sincronizacao em nuvem | ADICIONAR somente se houver conta/sincronizacao desejada | REMOVER |
| Dados | Uso totalmente offline | MANTER como requisito principal | MANTER |
| Idiomas | Portugues | MANTER como idioma principal | MANTER COMO UNICO IDIOMA |
| Idiomas | Outros dez idiomas | ALTERAR: escolher somente idiomas necessarios | REMOVER |
| Aparencia | Tema claro/escuro | MANTER | ALTERAR: CLARO, ESCURO E SISTEMA |
| Aparencia | Identidade visual do app original | REMOVER e substituir completamente | REMOVER E RECRIAR |
| Tutorial | Onboarding e ajuda contextual | MANTER, reescrito | MANTER |
| Contato | Enviar sugestao/bug | MANTER com canal proprio | REMOVER |
| Audio | Atalho ou musica para pratica | DEFINIR utilidade e origem do audio | REMOVER |
| Monetizacao | Anuncios de tela cheia | REMOVER por padrao | REMOVER |
| Monetizacao | Video recompensado para visualizar algoritmo | REMOVER por padrao | REMOVER |
| Monetizacao | Compra Premium | DEFINIR modelo de negocio antes de implementar | REMOVER; TUDO GRATUITO |
| Privacidade | Firebase Analytics | REMOVER por padrao | REMOVER |
| Privacidade | Firebase In-App Messaging | REMOVER por padrao | REMOVER |
| Notificacoes | Firebase/OneSignal push | REMOVER e avaliar lembretes locais | ALTERAR: SOMENTE LOCAL, HORARIO E DIAS |
| Conta | Login e perfil | ADICIONAR somente se necessario para sincronizacao | REMOVER |
| Metodos | Roux, ZZ ou outros metodos | CANDIDATO A ADICAO | FORA_V1 |
| Puzzles | 2x2, 4x4 ou outros cubos | CANDIDATO A ADICAO futura | FORA_V1; SOMENTE 3x3 |
| Acessibilidade | Escala de texto, contraste e leitor de tela | ADICIONAR desde a primeira versao | MANTER COMO REQUISITO |
| Distribuicao | Android | MANTER como primeira plataforma | MANTER COMO UNICA PLATAFORMA |
| Distribuicao | iOS/Web | DEFINIR antes da escolha arquitetural final | REMOVER |

## Decisoes globais confirmadas

- Nome de trabalho: `3x3`.
- Uso pessoal.
- Todas as funcoes aprovadas ficam gratuitas e desbloqueadas.
- Interface pode ser modernizada.
- Spotify e qualquer outro recurso de musica ficam fora do escopo.
- Lembretes sao locais, opcionais e configuraveis por horario e dias.
- Backup usa formato proprio e nao precisa importar `.cfop` original.

## Pendencias que nao autorizam implementacao

- Nome definitivo e pacote Android.
- Tecnologia final.
- Identidade visual detalhada.
- Fonte independente dos dados CFOP.
- Analise dinamica caso os splits ou um aparelho sejam disponibilizados.

