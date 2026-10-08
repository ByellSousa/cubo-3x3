# Roadmap do cronômetro e treino competitivo

## Princípio de implementação

O csTimer e outros cronômetros abertos podem ser consultados para entender comportamentos,
fluxos e formatos públicos. Este projeto não copia código, ativos, identidade visual ou layout
de terceiros. Toda interface, regra de domínio e implementação permanece própria, coerente com
a identidade Material 3 já aprovada para o aplicativo.

## Etapas aprovadas

### Extensao aprovada em 2026-10-07

- Construtor em Cronometro > Opcoes > Modo do cronometro > Meus concluidos +
  novos. Reutiliza as marcacoes explicitas, filtra etapas e permite escolher
  novos casos. A rodada congela IDs e passa por todos sem repetir praticados;
  contar somente tentativas finalizadas, inclusive DNF, nao previews/pulos.
- Configuracao guardada no DataStore/backup 7; estados de rodada preservados
  ao navegar/recriar, sem iniciar um novo setup silenciosamente ao retornar.
- Analise recolhida por caso, com filtros F2L/OLL/PLL e evolucao individual,
  respeitando sessao/periodo. +2 conta, DNF nao entra na media/grafico, e
  acertos do Quiz nao sao velocidade de execucao.
- Contrato e testes em TRAINING_PLAN.md. Etapas originais abaixo sao historicas.

Status: RC24 IMPLEMENTADO/TESTADO_LOCALMENTE/PREPARADO. 198 JVM passaram,
lint zero erros/seis avisos, APKs gerados/assinados v2. 13 testes Android
novos preparados e compilados, nao executados. NAO INSTALADO/NAO PUBLICADO.

### 1. Leitura do scramble

- Mostrar o estado resultante em uma planificação 2D `U / L F R B / D`.
- Aplicar o scramble com o mesmo modelo matemático usado pelo cubo animado.
- Permitir voltar e avançar pelos scrambles gerados na sessão atual.

Status: implementado no RC8 e corrigido/validado no RC9 com a orientacao WCA
branco acima e verde a frente. As vistas didaticas CFOP continuam usando amarelo acima.

### 2. Inspeção WCA

- Tornar a inspeção de 15 segundos opcional.
- Sinalizar os marcos de 8 e 12 segundos de forma visual e, quando habilitado, sonora.
- Aplicar `+2` e `DNF` conforme os limites oficiais vigentes, com testes deterministas.
- Manter treino sem inspeção disponível.

Status: implementado e validado no RC9. A opcao fica desativada por padrao; os alertas
sonoros sao independentes e tambem opcionais.

### 3. Sessões de treino

- Criar, renomear, selecionar e excluir sessões locais.
- Associar cada solve à sessão escolhida sem perder o histórico existente.
- Incluir sessões no backup próprio com migração explícita de banco e de esquema.

Status: implementado e validado no RC10. Os tempos legados migram para `Principal`,
a selecao persiste localmente e excluir uma sessao move seus tempos para a principal.
Room usa o esquema 6 e o backup proprio usa o esquema 4 com leitura dos esquemas 1 a 3.

### 4. Recordes e análise

- Destacar recordes pessoais de single, mo3, ao5, ao12 e ao100.
- Mostrar evolução, distribuição e consistência em gráficos próprios.
- Filtrar cálculos por sessão e período sem esconder os dados brutos.

Status: implementado e validado localmente no RC11. Os calculos usam a sessao
selecionada e filtros independentes de tudo/7/30/90 dias; o historico bruto continua
visivel sem filtro. O RC13 foi instalado no Samsung; o teste instrumentado de analise passou
e os recordes/grafico de evolucao foram inspecionados. Aceitacao manual do usuario pendente.

### 5. Treino CFOP direcionado

- Gerar treinos ligados aos casos F2L, OLL e PLL do catálogo auditado.
- Permitir escolher categorias ou casos e acompanhar o desempenho local.
- Preservar o scramble completo usado em cada tentativa.

Status: implementado e validado localmente no RC12. O usuario pode manter treino livre WCA,
sortear todos os casos de F2L/OLL/PLL ou selecionar um caso especifico. O setup completo e
preservado em cada tentativa junto com categoria e numero do caso; a analise mostra tentativas,
casos distintos, melhor e media por categoria. Room usa o esquema 7 e o backup proprio usa o
esquema 5 com leitura dos esquemas 1 a 4. O RC13 foi instalado e o teste de configuracao de
categoria F2L passou no Samsung; validacao manual de uso das tres categorias fica pendente.

### 6. Portabilidade

- Manter o backup `.3x3backup` como formato integral e fonte de verdade do aplicativo.
- Oferecer importação e exportação compatíveis com formatos públicos úteis do csTimer por meio
  de conversores próprios, com validação, prévia e confirmação antes de gravar dados.
- Nunca substituir ou mesclar dados silenciosamente.

Status: implementado no RC13, com conversor proprio para JSON da exportacao local do csTimer
(sessoes 333 e 333o), previa e confirmacao. Importacao cria novas sessoes em transacao sem
substituir dados atuais; exportacao usa snapshot revisado. PORTABILITY_FORMAT.md documenta
campos preservados, limites, avisos e fontes consultadas. Os testes de transferencia e previa
passaram no Samsung; exportacao real e leitura/cancelamento pelo seletor Android foram
verificados com cinco tempos preservados. O ensaio dentro do csTimer continua pendente.

## Fora do ciclo atual

- Cubos Bluetooth.
- Stackmat e outros hardwares externos.
- Múltiplos puzzles além do 3x3.
- Solvers alem do preparo CFOP aprovado em 2026-10-07; a excecao para o editor
  manual e documentada em CASE_PREPARATION.md.

Esses itens exigem nova priorização antes de entrarem no desenvolvimento.
