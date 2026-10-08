# PRODUCT_SPEC

## Status

`ESPECIFICACAO_APROVADA_PELO_USUARIO`

Especificacao aprovada explicitamente pelo usuario em 2026-09-30. Nenhuma implementacao foi iniciada. Tecnologia e ordem de desenvolvimento foram aprovadas em `TECHNICAL_SPEC.md`, e a direcao visual foi aprovada em `VISUAL_SPEC.md`; o inicio da implementacao ainda exige autorizacao posterior.

## Visao do produto

`3x3` e o nome de trabalho de um aplicativo Android pessoal para aprender, consultar e praticar o metodo CFOP do cubo 3x3. Ele preservara as funcoes uteis do aplicativo de referencia, mas sera uma implementacao nova, gratuita, offline, sem anuncios, sem bloqueios e com interface modernizada.

## Publico e distribuicao

- Usuario principal: proprietario do projeto, para uso pessoal.
- Plataforma: somente Android.
- Idioma: somente portugues do Brasil.
- Distribuicao externa e publicacao em loja nao fazem parte desta etapa.

## Principios obrigatorios

1. Todas as funcoes implementadas ficam disponiveis sem pagamento.
2. O nucleo do aplicativo funciona offline.
3. Nenhum anuncio, rastreamento ou telemetria.
4. Nenhuma conta, login ou sincronizacao em nuvem.
5. Interface e ativos proprios, sem copiar a identidade do aplicativo de referencia.
6. Codigo novo, modular e limitado ao necessario.
7. Dados do usuario permanecem locais, salvo exportacao manual de backup.
8. Tema claro, escuro e automatico conforme o sistema.

## Escopo funcional da primeira versao

### Aprendizado CFOP

- Explicacao original em portugues sobre Cross, F2L, OLL e PLL.
- Catalogos de algoritmos F2L, OLL e PLL.
- Alternancia entre conjuntos reduzido e completo quando aplicavel.
- Dados tecnicos validados por fontes independentes antes da implementacao.

### Visualizacao de algoritmos

- Cubo 3D animado com implementacao ou biblioteca de licenca compativel.
- Execucao visual da sequencia de movimentos.
- Controle de velocidade.
- Configuracao de cores, ponto de vista, linhas e estado inicial quando aplicavel.

### Progresso

- Adicionar e remover favoritos.
- Marcar e desmarcar algoritmos concluidos.
- Listas separadas de favoritos e concluidos.
- Limpeza manual desses dados mediante confirmacao.

### Cronometro

- Exibir o tempo em uma grande area central tocavel, mantendo os numeros como foco visual da tela.
- Um toque nessa area inicia a tentativa e qualquer toque na mesma area encerra e salva o tempo, sem exigir um pequeno alvo durante a resolucao.
- Gerar offline um novo embaralhamento 3x3 por estado aleatorio para cada tentativa, com opcao de gerar outro antes de iniciar.
- Exibir tempo atual, melhor tempo, `mo3`, `ao5` e `ao12`.
- Historico local em tabela com tempo e medias correspondentes a cada tentativa.
- Ao tocar em um tempo, abrir seus detalhes com embaralhamento, data, comentario, penalidade `OK`, `+2` ou `DNF` e acao de copiar.
- Exclusao individual e limpeza total mediante confirmacao.
- O gerador e as estatisticas sao destinados a treino pessoal; o escopo nao inclui sincronizacao, competicoes online ou ranking.

### Quiz de notacao

- Niveis essencial, intermediario e avancado.
- Modo pratica sem limite de tempo.
- Modo cronometrado de 60 segundos.
- Pontuacao, sequencia, acertos, precisao e recordes locais.
- Resultado ao final e opcao de jogar novamente.

### Algoritmos personalizados

- Construtor visual totalmente gratuito e desbloqueado.
- Movimentos basicos, rotacoes, movimentos largos e movimentos de fatia.
- Nome e tags.
- Pesquisa e filtro por tags.
- Criar, editar, duplicar e excluir.
- Previa no cubo.
- Configuracao de cores e ponto de vista.

### Backup e restauracao

- Exportacao manual de todos os dados locais.
- Importacao manual com confirmacao antes de substituir dados existentes.
- Extensao provisoria: `.3x3backup`.
- Formato proprio, versionado e documentado.
- Sem obrigacao de importar o formato `.cfop` do aplicativo original.
- Usar o seletor de documentos do Android, sem permissao ampla de armazenamento.

O backup devera representar pelo menos:

- versao do esquema;
- versao do aplicativo;
- data da exportacao;
- configuracoes;
- favoritos;
- concluidos;
- tempos;
- recordes do quiz;
- algoritmos personalizados;
- agenda de lembretes locais.

### Aparencia

- Tema claro.
- Tema escuro.
- Tema automatico conforme o sistema.
- Interface modernizada, responsiva e com identidade propria.
- Preferencia de tema persistida localmente.

### Lembretes locais

- Funcao opcional e desativada por padrao.
- Usuario escolhe horario e dias da semana.
- Permissao de notificacao solicitada somente quando necessaria e em contexto.
- Sem Firebase, OneSignal ou qualquer servidor de push.
- Preferir agendamento local que nao exija permissao de alarme exato; avaliar tecnicamente durante a implementacao.

### Configuracoes e manutencao de dados

- Tema.
- Velocidade e opcoes do cubo.
- Lembretes locais.
- Exportar e importar backup.
- Limpar tempos.
- Limpar favoritos e concluidos.
- Limpar recordes do quiz.
- Redefinir todos os dados, sempre com confirmacao.
- Exibir versao do aplicativo.

## Recursos removidos

- Anuncios de qualquer tipo.
- Videos recompensados e pontos para desbloqueio.
- Premium, paywall, compra unica, assinatura e restauracao de compras.
- Google Play Billing.
- AdMob e identificadores publicitarios.
- Firebase Analytics, Firebase Messaging e Firebase In-App Messaging.
- OneSignal e notificacoes remotas.
- Coleta de telemetria.
- Conta, login e sincronizacao em nuvem.
- Contato com o desenvolvedor original.
- Dez idiomas alem do portugues.
- IDs, links, credenciais, pacote, assinatura e marca do aplicativo original.

## Fora do escopo inicial

- iOS e Web.
- Cubos 2x2, 4x4 ou outros puzzles.
- Metodos Roux, ZZ ou outros alem de CFOP.
- Ranking, competicao ou recursos sociais.
- Compatibilidade com backup `.cfop` original.
- Publicacao em loja.

## Permissoes pretendidas

- Notificacoes, somente quando o usuario ativar lembretes e quando exigido pela versao do Android.
- Nenhuma permissao ampla de armazenamento.
- Nenhuma permissao de identificador publicitario.
- Nenhuma permissao de faturamento.

## Criterios gerais de aceite

- Todas as telas funcionam em tema claro e escuro.
- O tema automatico acompanha o sistema.
- O nucleo funciona com o aparelho sem internet.
- Nenhuma tela ou funcao solicita pagamento ou exibe anuncio.
- Nenhum SDK de analytics, publicidade, faturamento ou push remoto esta presente no build.
- Backup exportado pode restaurar os dados em instalacao limpa da mesma linha de versao.
- Lembretes permanecem locais e podem ser desativados.
- O build final solicita somente permissoes justificadas pelas funcoes aprovadas.

## Decisoes ainda pendentes

- Nome definitivo e identificador do pacote Android.
- Ativo final do icone e do logotipo e wireframes das demais telas.
- Fonte independente e licenca dos dados/diagramas de algoritmos.
- Versoes minima e alvo do Android e detalhes do renderizador do cubo.
