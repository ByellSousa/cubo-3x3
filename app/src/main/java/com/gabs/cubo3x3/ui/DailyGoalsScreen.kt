package com.gabs.cubo3x3.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.gabs.cubo3x3.data.timer.SolveTime
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.domain.training.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberDailyGoalProgress(
    attempts: List<CfopAttempt>, times: List<SolveTime>,
    clock: () -> Long = System::currentTimeMillis, zone: () -> ZoneId = ZoneId::systemDefault,
): DailyGoalProgress? {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    val latestClock by rememberUpdatedState(clock)
    val latestZone by rememberUpdatedState(zone)
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { tick++; delay(60_000) }
        }
    }
    val progress by produceState<DailyGoalProgress?>(null, attempts, times, tick) {
        val now = latestClock().coerceAtLeast(0L)
        val currentZone = latestZone()
        value = withContext(Dispatchers.Default) { DailyGoals.progress(attempts, times, now, currentZone) }
    }
    return progress
}

@Composable
internal fun DailyGoalProgressCard(
    settings: DailyGoalSettings, progress: DailyGoalProgress?, modifier: Modifier = Modifier,
    onConfigure: (() -> Unit)? = null,
) {
    Card(modifier.fillMaxWidth().testTag("daily-goals-progress")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Metas diárias", style = MaterialTheme.typography.titleMedium)
            if (!settings.enabled) Text("Metas desativadas. Ative somente se quiser acompanhar sua prática.")
            else if (progress == null) Text("Carregando progresso…")
            else {
                Text("Hoje · " + progress.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    Modifier.testTag("daily-goals-date"))
                if (settings.recognitionTarget > 0) GoalCounter("Respostas CFOP",
                    progress.recognitionCount, settings.recognitionTarget, "recognition")
                if (settings.executionTarget > 0) GoalCounter("Tentativas dirigidas CFOP",
                    progress.executionCount, settings.executionTarget, "execution")
                if (progress.reached(settings)) Text("Metas de hoje atingidas!",
                    Modifier.testTag("daily-goals-reached"), fontWeight = FontWeight.SemiBold)
            }
            onConfigure?.let { action ->
                TextButton(onClick = action, modifier = Modifier.testTag("daily-goals-configure")) { Text("Configurar metas") }
            }
        }
    }
}

@Composable
private fun GoalCounter(label: String, count: Int, target: Int, tag: String) {
    Text("$label: $count / $target", Modifier.testTag("daily-goals-$tag"))
    LinearProgressIndicator(progress = { (count.toFloat() / target).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun DailyGoalsScreen(
    settings: DailyGoalSettings, attempts: List<CfopAttempt>, times: List<SolveTime>,
    onSave: suspend (DailyGoalSettings) -> Unit, onBack: () -> Unit,
    clock: () -> Long = System::currentTimeMillis, zone: () -> ZoneId = ZoneId::systemDefault,
) {
    val incoming = DailyGoalSettingsCodec.encode(settings)
    var enabled by rememberSaveable(incoming) { mutableStateOf(settings.enabled) }
    var recognition by rememberSaveable(incoming) { mutableStateOf(settings.recognitionTarget.toString()) }
    var execution by rememberSaveable(incoming) { mutableStateOf(settings.executionTarget.toString()) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val draft = runCatching { DailyGoalSettings(enabled, recognition.toInt(), execution.toInt()) }.getOrNull()
    val progress = rememberDailyGoalProgress(attempts, times, clock, zone)
    val list = rememberLazyListState()
    BackHandler { if (!saving) onBack() }
    Scaffold { innerPadding ->
    LazyColumn(Modifier.fillMaxSize().padding(innerPadding).imePadding().testTag("daily-goals-screen"), state = list,
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack, enabled = !saving, modifier = Modifier.testTag("daily-goals-back")) { Text("Voltar") }
            Text("Sua prática diária", style = MaterialTheme.typography.titleLarge)
            Text("Opcional, local e sem ocupar o cronômetro. Dia e fuso horário do aparelho.")
        }
        item { DailyGoalProgressCard(settings, progress) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ativar metas", Modifier.weight(1f))
                        Switch(checked = enabled, onCheckedChange = { enabled = it; status = null },
                            enabled = !saving, modifier = Modifier.testTag("daily-goals-enabled"))
                    }
                    OutlinedTextField(value = recognition,
                        onValueChange = { recognition = it.filter(Char::isDigit).take(4); status = null },
                        label = { Text("Respostas CFOP por dia") }, enabled = !saving, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("daily-goals-recognition-input"))
                    OutlinedTextField(value = execution,
                        onValueChange = { execution = it.filter(Char::isDigit).take(4); status = null },
                        label = { Text("Tentativas dirigidas por dia") }, enabled = !saving, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("daily-goals-execution-input"))
                    Text("De 0 a 100; zero desativa aquela meta. Sugestão inicial: 5 de cada.")
                    if (draft == null) Text("Use valores de 0 a 100 e, se ativar, pelo menos uma meta maior que zero.",
                        Modifier.testTag("daily-goals-invalid"), color = MaterialTheme.colorScheme.error)
                    Button(enabled = !saving && draft != null, modifier = Modifier.testTag("daily-goals-save"),
                        onClick = {
                            val value = draft ?: return@Button
                            saving = true
                            scope.launch {
                                try { onSave(value); status = "Metas salvas." }
                                catch (error: CancellationException) { throw error }
                                catch (_: Exception) { status = "Não foi possível salvar. Reabra esta tela e tente novamente." }
                                finally { saving = false }
                            }
                        }) { Text(if (saving) "Salvando…" else "Salvar metas") }
                    status?.let { Text(it, Modifier.testTag("daily-goals-status")) }
                    if (draft != settings) Text("Alterações só valem após salvar. Voltar não grava o rascunho.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("O que conta", style = MaterialTheme.typography.titleMedium)
            Text("Reconhecimento: respostas certas ou erradas em Reconhecer CFOP e Revisão. " +
                "Execução: tentativas de treino CFOP salvas no cronômetro, inclusive +2 e DNF. " +
                "São metas de prática, não de domínio ou resoluções corretas.")
            Text("Todas as sessões e etapas. Consulta, pular, quiz de notação e tempos livres/importados sem " +
                "origem CFOP não contam. Excluir ou restaurar o histórico recalcula o progresso.")
        }
    }
    }
}
