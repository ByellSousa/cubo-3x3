# Reconhecimento e revisao CFOP

## Funcionalidade

Notacao continua separada, com pratica e 60 segundos. Reconhecer CFOP oferece
rodadas aleatorias de dez casos sem repeticao da etapa escolhida, sem tempo
limite. Diagramas sao os mesmos do catalogo: F2L focado, OLL superior amarelo,
PLL superior com setas. Orientacao: amarelo acima, branco abaixo, vermelho a
frente e verde a direita. Numero nao e revelado antes de responder.

Quatro formulas originais, incluindo primas duplas, compõem as alternativas.
Outras formulas que tambem resolvem a etapa desenhada sao excluidas. Acerto
tem criterio por etapa: primeiras camadas para F2L, primeiras camadas e amarelo
orientado para OLL, todas as faces uniformes para PLL. Feedback permite estudar
o caso, retornar a pergunta, continuar ou encerrar conservando respostas.

## Historico e prioridade

Room 8 acrescenta cfop_attempts: id, categoria, caso alvo, caso escolhido,
instante. Id unico por rodada/pergunta torna replay idempotente. Cada resposta
e salva individualmente; abandonar a rodada nao apaga as anteriores.
Dados permanecem offline; nao alteram favorito/conclusao nem recordes de notacao.

Revisao agrupa por caso alvo. Mostra acertos/total, precisao e ultimo instante.
O modo Por erros preserva a prioridade: proporcao de erros nas ultimas dez respostas; desempata por
mais erros nessa janela, ultima resposta mais antiga e numero. Somente casos
com erro nessa janela entram na fila. Sem historico nao inventa dificuldade.
Dez respostas recentes certas retiram um erro antigo da fila, sem apagar dados.
Nao se trata de uma medida de velocidade de resolucao ou recorde do timer.

Por prazo acrescenta revisao espacada e agenda futura recolhida, inclusive
para casos respondidos corretamente sem erro recente. Intervalos, erros
imediatos e regra de repeticao antecipada em CFOP_SPACED_REVIEW.md.

Comparar casos confundidos usa as mesmas dez respostas por alvo de Por erros.
Agrupa trocas A->B/B->A mantendo contagens separadas; consultar diagramas/
pistas/formulas originais nao grava resposta nem altera a revisao. Janela,
orientacao, continuidade e aceite em CFOP_COMPARISON.md. Room9/backup8
atuais preservados; historico antigo sem respostas nao inventa pares.

Treinar revisao congela ate dez ids da etapa escolhida na ordem de prioridade.
Novas respostas nao mudam a rodada. Pode conter de um a dez casos.
Detalhe, volta e recriacao da interface preservam pergunta/resposta e rolagem.
Backup 7 protege historico e plano; 6 preserva respostas, 1-5 usam historico vazio com aviso antes
da substituicao. Confirmar restauracao descarta rodada antiga e invalida suas
gravações pendentes, impedindo replay que repovoe o historico restaurado.

## Validacao RC23 no Samsung em 2026-10-07

RC23/codigo 32 instalado por atualizacao sobre RC20, com autorizacao especifica.
Dezoito regressões isoladas passaram: seis busca, seis reconhecimento, cinco
revisao e uma migracao 7->8 em banco separado. Cinco testes de navegacao com
MainActivity passaram, incluindo busca e posicao da lista ao voltar do caso.
Capturas reais conferidas: pergunta F2L focada, OLL superior amarelo, PLL superior
amarelo/setas e revisao vazia. Quatro alternativas legiveis, sem numero do caso
antes de responder, com orientacao explicitada.

Nao foram gravadas respostas CFOP artificiais no banco pessoal. Fila com erros,
feedback, restauracao de estado e invalidacao apos substituicao foram exercitados
com dados/callbacks isolados. Isso nao equivale a persistencia Room ponta a ponta
de respostas reais ou confirmacao substitutiva de backup no aplicativo completo.
Migracao real preservou todas as tabelas legadas e preferencias; cfop_attempts
permaneceu vazia. Evidencias privadas no ARTIFACT_INDEX.md.

## Aceite restante no aparelho/ambiente isolado

Instalar somente a versao expressamente autorizada, preservando dados atuais.
Nao restaurar snapshots pessoais antigos para testar.

1. Notacao: pratica/60s e animacao continuam funcionando.
2. Cada etapa CFOP: padrao sem numero, quatro formulas legiveis, so uma resposta,
   feedback, proximo e resultado. Conferir com cubo fisico em amarelo acima.
3. Responder errado/certo, fechar e reabrir: historia e estatisticas mantidas.
4. Estudar caso no meio da rodada e voltar; pergunta/feedback nao reiniciam.
5. Revisao vazia, filtro por etapa, prioridade/estatisticas e rodada com 1/10
   casos. Novos resultados nao pulam/reordenam perguntas. Volta preserva rolagem.
6. Rodar regressões Compose, backup Room e migracao 7->8 em ambiente autorizado.
   Esquema 8 tambem deve migrar a partir de versoes anteriores pela cadeia.
   A partir do RC24, teste de backup usa Room em memoria e DataStore exclusivo
   por teste; suite antiga RC23 ainda usava preferencias reais. Ensaios de
   restauracao no aplicativo completo exigem perfil isolado e autorizacao.
7. Backup novo com respostas e legado sem respostas: conferir previa, cancelamento
   sem alteracao e confirmacao substitutiva com descarte da rodada antiga.
8. Tema claro/escuro, fonte ampliada, alternativas longas e navegacao por toque/
   acessibilidade. Diagramas visuais ainda requerem avaliacao com leitor de tela.

Os 23 testes acima nao representam a suite Android completa; a conferencia
visual nao substitui o aceite com cubo fisico, acessibilidade ou backup isolado.
