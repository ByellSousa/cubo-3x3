# Comparacao dos casos CFOP confundidos

## Origem e significado

Quiz > Revisao > Comparar casos confundidos. Somente respostas erradas
do reconhecimento, nao quiz de notacao ou tempos do cronometro. Usar a
mesma janela da revisao por erros: ultimas dez respostas de CADA alvo,
incluindo acertos antes de filtrar erros. Dez acertos recentes retiram
erro antigo da comparacao sem apagar historico.

Deduplicar id antes da janela; ordenar por instante/id. Par por etapa com
numero menor primeiro; A->B e B->A agrupadas uma unica vez, mas contagens
direcionais exibidas separadamente. Ordenar frequencia decrescente, ultima
troca decrescente e numeros crescentes. Sem respostas/erros nao inventar
pares, sem criar indicador de dominio ou alegar semelhanca automatica.

## Estudo

- Mostrar diagramas originais: F2L focado, OLL superior amarelo, PLL setas.
  Amarelo acima/branco abaixo, vermelho frente/verde direita. Nada de alinhar
  os casos com giros para fabricar semelhanca; comparacao na orientacao
  original usada no reconhecimento.
- Pistas sao derivadas do modelo: posicao/orientacao do canto branco/
  vermelho/verde e aresta vermelho/verde em F2L; amarelos no topo/laterais
  em OLL; destino de cantos/arestas em PLL. Distinguir posicao de adesivo,
  peca e destino. Coordenadas topologicas L1C1...L3C3 com legenda explicita.
- Formulas originais recolhidas ate pedir; alternativa pessoal continua no
  detalhe, nunca substitui criterio/diagramas da comparacao. Ampliacao
  empilha diagramas; texto ampliado/tela estreita tambem usa leitura vertical.
- Abrir/voltar/comparar/revelar formulas nao registra resposta, nao conta
  tentativa cronometrada, nao muda favorito/concluido ou agenda.
- Filtro, par selecionado, rolagem e revelacao/ampliacao salvaveis. Consulta
  ao detalhe volta a mesma comparacao. Atualizar historico nao troca o par
  aberto: se sai da janela, manter casos e informar ausencia de trocas recentes.
  Restauracao confirmada de backup descarta estado sob geracao Quiz existente.

## Dados e validacao

Calcular agregados fora da UI. Nenhuma persistencia redundante: Room9,
backup8 e portabilidade csTimer inalterados. Historico restaurado reconstroi
comparacao; legado sem respostas nao inventa pares. Dados pessoais nunca
usados como fixture ou sobrescritos por snapshots antigos.

JVM: vazio/acertos/deduplicacao/direcoes/categorias/janelas/ordem/cronologia,
validacao de IDs e reversibilidade/limites da chave, pistas contra diagramas
do modelo e backup reconstituido. Compose: lista/filtro/retorno/recriacao,
formulas/ampliacao, atualizacao de historico e invalidacao por backup.
Compilar Android nao equivale a executar/validar visualmente no celular.

## Evidencia local RC27 - 2026-10-08

Gradle offline final: testDebugUnitTest, lintDebug, assembleDebug e
assembleDebugAndroidTest passaram. 260 JVM/39 suites, zero falhas/erros/skips.
20 novos: 14 pares/chaves/janela/ordem, quatro pistas cobrindo 119 casos e
2.626 pares, dois backup (atual/legados). Nenhum estado/colecao alterado.
Uma expectativa inicial de campos inexistentes no backup antigo foi
corrigida conforme contrato; codec/migracoes nao precisaram mudar.

Nove Compose preparados/compilados: vazio, direcoes/read-only, filtros,
rolagem/recriacao, retorno do estudo/formulas/ampliacao, retirada de erro/
novo par, texto ampliado, retorno a revisao e geracao de backup. Classes e
metodos confirmados no APK AndroidTest, mas NAO EXECUTADOS.
Lint zero erros/seis avisos anteriores. APKs codigo36/rc27 assinados v2,
versao/manifesto/ausencia de internet verificados; hashes em ARTIFACT_INDEX.
PREPARADO/TESTADO_LOCALMENTE, NAO INSTALADO/NAO PUBLICADO.

Roteiro de aceite futuro (pendente de autorizacao): executar testes isolados, conferir diagramas/
legenda/texto ampliado/tema claro e escuro no Android, abrir/voltar/recriar,
confirmar que consultas nao gravam dados, e comparar com cubo fisico.
Nao preencher historico pessoal com fixtures nem restaurar snapshot antigo.
