# Portabilidade de tempos e sessoes

## Escopo

A etapa RC13 oferece um conversor proprio para o JSON da exportacao local do csTimer.
O arquivo original normalmente tem extensao .txt, mas seu conteudo e JSON. O app tambem
aceita .json pelo seletor de documentos do Android. CSV, arquivos comprimidos de nuvem,
codigos de configuracao e outros puzzles ficam fora deste primeiro conversor.

A exportacao do 3x3 usa .txt e MIME text/plain, mantendo conteudo JSON. Isso permite
selecionar o arquivo no importador local do csTimer, que filtra text/* na revisao consultada.

O backup .3x3backup permanece o formato integral do aplicativo. Transferencias csTimer
nao incluem tema, lembretes, progresso, Quiz, personalizados nem vinculo F2L/OLL/PLL.

## Contrato publico consultado

Consulta em 2026-10-06, repositorio oficial cs0x7f/cstimer, revisao
2547d82e32a347dd1b1c6943e8d03389d03eb64c. Nenhum codigo ou ativo foi incorporado.

- [Exportacao local](https://github.com/cs0x7f/cstimer/blob/2547d82e32a347dd1b1c6943e8d03389d03eb64c/src/js/export.js): JSON com sessoes e properties, gravado localmente sem compressao.
- [Armazenamento de sessoes](https://github.com/cs0x7f/cstimer/blob/2547d82e32a347dd1b1c6943e8d03389d03eb64c/src/js/lib/storage.js): listas session1, session2 etc.
- [Tempos e metadados](https://github.com/cs0x7f/cstimer/blob/2547d82e32a347dd1b1c6943e8d03389d03eb64c/src/js/stats/stats.js): duracao, penalidade, embaralhamento, comentario, data de inicio e extensao opcional; nomes e tipo em sessionData.

## Mapeamento

| Campo publico | Tratamento no 3x3 |
| --- | --- |
| sessionN e session1/session2/... | Cada lista suportada vira uma nova sessao; IDs locais nao sao reutilizados |
| properties.sessionData | Pode ser objeto ou string JSON; properties e listas de tempos tambem aceitam a forma string legada |
| name | Nome textual ou numerico; entidades HTML basicas sao decodificadas uma vez; limite de 50 caracteres, com aviso se truncado |
| opt.scrType ou scr legado | 333 e 333o sao aceitos; ausente segue o padrao 333 do csTimer |
| Primeiro item do registro | Lista de penalidade e duracao bruta em milissegundos |
| Penalidade 0 / 2000 / -1 | OK / +2 / DNF; a duracao bruta nao recebe o +2 na importacao |
| Embaralhamento e comentario | Preservados; a notacao deve ser aceita pelo parser proprio, incluindo U2' |
| Data de inicio, segundos Unix | Convertida para data de termino em milissegundos somando a duracao bruta; ausencia/zero permanece sem data |
| Parciais e extensoes | Aviso na previa; somente o total e transferido; puzzle diferente identificado na extensao e excluido da contagem importada |

Na exportacao, o app escreve sessionN, session, sessionData e as listas de tempos em
ordem cronologica. Nomes sao escapados para exibicao textual no csTimer. A data de
inicio e calculada a partir do termino menos a duracao bruta; sua precisao e de segundos,
com perda inferior a um segundo no ciclo exportacao/importacao. A duracao mantem seus
milissegundos. Os tipos exportados sao 333.

## Confirmacao e integridade

- Importar apenas le e valida antes de mostrar a previa; nenhum dado e escrito nessa fase.
- A previa apresenta sessoes, quantidades e avisos de campos ou puzzles excluidos.
- Confirmar adiciona novas sessoes em uma unica transacao Room; nenhuma sessao existente e substituida ou mesclada.
- Nomes repetidos, sem distinguir maiusculas, recebem (2), (3) etc.
- Importar novamente o mesmo arquivo cria novas copias; nao ha deduplicacao silenciosa.
- Falha durante insercao faz rollback de todas as sessoes e tempos daquela operacao.
- Exportar prepara um snapshot; o documento salvo corresponde a previa mesmo se os dados mudarem depois.
- Limites: UTF-8 valido, JSON estrito, 5 MiB, 1.000 sessoes e 100.000 tempos. A importacao tambem verifica o total resultante, para preservar a possibilidade de backup integral.
- Campos JSON duplicados, numeros fracionarios onde se espera inteiro, overflow, data negativa, penalidade desconhecida e notacao invalida sao rejeitados.

## Validacao

Testes unitarios exercitam os formatos publico/legado, conversao de datas, todas as
penalidades, nomes, comentarios Unicode, limites, arquivos invalidos e puzzles mistos.
Testes Android cobrem leitura sem escrita, colisoes de nomes, preservacao de dados,
snapshot de exportacao, rollback de insercao e confirmacao na interface.
Regressoes JVM tambem verificam o ciclo entre o JSON publico e o backup integral,
datas ausentes, sessoes vazias e medias com +2/DNF sem aplicar a penalidade duas vezes.
Execucao no celular e ensaio de transferencia pelo seletor Android ficam registrados
separadamente no TASK_HANDOFF.md; compilacao nao implica validacao fisica.

Em 2026-10-07 o usuario confirmou que realizou a importacao no aplicativo e
ela funcionou. O sentido csTimer -> 3x3 fica validado manualmente por esse
relato. Nao foram fornecidos arquivo/totais para conferencia independente.
A exportacao inversa, ciclo completo e comparacao campo a campo continuam
pendentes; nao repetir a importacao aceita, pois isso cria copias adicionais.
