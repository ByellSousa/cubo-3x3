# Metas diarias locais

## Contrato RC28

Mais > Metas diarias; resumo compacto em Inicio somente se ativadas.
Nao acrescentar controles ao timer. Desativadas por padrao, sugestao 5/5.
Salvar explicitamente; cada alvo de 0 a 100, zero desativa aquele contador.
Metas ativas exigem pelo menos um alvo positivo. Desativar preserva numeros/
historicos. Configuracao no DataStore por codec estrito 1|0-ou-1|alvo|alvo.

Contadores separados, todas as etapas/sessoes, deduplicados por ID:
- Reconhecimento: respostas CfopAttempt salvas, certas ou erradas.
- Execucao: SolveTime salvo com origem CFOP categoria/caso valida; +2/DNF
  contam tentativa, nao solucao correta. Nao exigir casos diferentes:
  repetir e uma pratica legitima, mas replay do mesmo registro nao conta.
- Navegar/consultar/prever/pular, quiz de notacao, tempos livres e tempos
  csTimer sem origem CFOP nao contam. Nao mudar favorito/concluido/prazo.

Dia = data no fuso atual do aparelho, nao janela de 24h; respeitar horario
de verao/dias 23/25h. Contar somente registros com instante <= agora.
Mudar fuso/relogio ou excluir/restaurar historico recalcula; nao reescrever
datas antigas nem guardar contador redundante. Sem streak/premios/notificacoes.
Mostrar contagem real mesmo acima da meta; barra limitada visualmente a 100%.
Atualizar em primeiro plano/resume e a cada minuto, alem de historico/config.

## Dados e protecao

Room9 sem migracao. Backup9 acrescenta settings.dailyGoals (codec acima).
Leitor aceita1-8, restaurando metas desativadas com aviso antes de confirmar.
Todos os historicos/preferidas/plano permanecem nos formatos apropriados.
Importacao csTimer e preferencia de sessao/inspecao nao mudam.
Restauracao confirmada invalida rascunho e saves antigos por geracao/Mutex.
Preparacao APK nao autoriza instalacao; nunca usar dados pessoais como fixture.

## Validacao local RC28 - 2026-10-08

281 JVM/40 suites passaram, zero falhas/erros/skips. 21 testes novos:
16 DailyGoalsTest (inclui 20.401 configuracoes canonicas e dias DST 23/25h),
quatro BackupCodecTest (esquemas1-8, atual/invalidos/zero e colecoes mantidas),
um SolveTimeRepositoryTest (consulta dirigida/sessoes/exclusao, DAO fake).
lintDebug zero erros/seis avisos anteriores. Room9 exportado, TimerScreen
e conversor csTimer sem alteracoes em relacao a baseline c73a77f.

Nove Compose novos e tres Room/DataStore privados compilados e metodos
conferidos no DEX, NAO EXECUTADOS. Cobrem salvar/desativar/validar,
rascunho/recriacao/voltar sem salvar, fontes/replay/exclusao, resume/dia,
erro de save, invalidacao por geracao e exportar/inspecionar/restaurar.
A compilacao inicial exigiu implementar observeDirected no DAO fake;
ajustado e build completo reexecutado com sucesso.

## Aceite pendente no aparelho

Somente com nova autorizacao, mantendo os dados reais:
- Abrir por Mais, ativar alvos, salvar e conferir resumo no Inicio;
  desativar esconde resumo, preserva numeros/historico.
- Voltar retorna ao destino/rolagem anterior. Recriar e usar tema claro,
  escuro, teclado e texto ampliado sem perder campos nem cobrir controles.
- Responder certo/errado em CFOP; salvar tentativa dirigida/+2/DNF;
  livre/csTimer sem origem/notacao/consulta/pular nao aumentam contagem.
- Confira todas as sessoes; excluir registro atualiza contadores.
- Retomar apos meia-noite recalcula dia/fuso sem respostas artificiais.
- Backup atual restaura configuracao e reconstroi contagens; legado8
  avisa e desativa metas somente apos confirmacao, sem replay de rascunho.
- Cronometro conserva area ampla/foco. Nenhuma rotina deve inserir fixture
  pessoalmente nem restaurar automaticamente snapshots RC23 antigos.

APK RC28 preparado, nao instalado; hashes em ARTIFACT_INDEX.md.
Release otimizado sem assinatura gerado e auditado localmente: lintRelease
zero erros/seis avisos, alinhamento4/16KB, manifesto sem debuggable/INTERNET,
nove modelos/60 nomes de campos do backup preservados no DEX. 1.892.959 bytes.
Nao instalavel/publicavel antes de estrategia/assinatura autorizadas.
Teste JVM/compilacao/inspecao estatica nao equivalem a validacao visual
nem a execucao de restauracao/Room/Compose no Android.
