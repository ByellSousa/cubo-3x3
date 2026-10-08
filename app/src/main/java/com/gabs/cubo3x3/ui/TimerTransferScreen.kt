package com.gabs.cubo3x3.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.data.transfer.TimerTransferPreview

data class TimerTransferUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val importPreview: TimerTransferPreview? = null,
    val exportPreview: TimerTransferPreview? = null,
)

data class TimerTransferActions(
    val chooseImport: () -> Unit = {},
    val prepareExport: () -> Unit = {},
    val confirmImport: () -> Unit = {},
    val confirmExport: () -> Unit = {},
    val cancelPreview: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerTransferScreen(
    state: TimerTransferUiState,
    actions: TimerTransferActions,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tempos e sessões") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Transfira seu histórico 3x3 por arquivo JSON compatível com o csTimer.",
                    style = MaterialTheme.typography.bodyLarge)
            }
            item {
                TransferActionCard(
                    "Importar do csTimer",
                    "Veja as sessões e os avisos antes de confirmar. Os tempos serão adicionados em sessões novas.",
                    !state.busy,
                    actions.chooseImport,
                )
            }
            item {
                TransferActionCard(
                    "Exportar para csTimer",
                    "Exporte todas as sessões com tempos, penalidades, embaralhamentos, comentários e datas em um arquivo .txt com dados JSON.",
                    !state.busy,
                    actions.prepareExport,
                )
            }
            if (state.busy) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            state.message?.let { message -> item { Text(message) } }
            item {
                Text("Seu backup completo", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Continue usando .3x3backup para guardar também os favoritos, o Quiz, os algoritmos personalizados e as preferências. " +
                        "O JSON do csTimer transfere apenas tempos e sessões.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    "Aceita a exportação local sem compressão do csTimer, nas sessões 3x3 (333 e 333o). " +
                        "Outros puzzles são identificados na prévia e ficam de fora. Importar o mesmo arquivo novamente cria outra cópia em novas sessões.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    val preview = state.importPreview ?: state.exportPreview
    if (preview != null) {
        val importing = state.importPreview != null
        AlertDialog(
            onDismissRequest = { if (!state.busy) actions.cancelPreview() },
            title = { Text(if (importing) "Prévia da importação" else "Prévia da exportação") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${preview.sessions.size} sessões · ${preview.solveCount} tempos")
                    preview.sessions.forEach { session ->
                        Text("${session.name}: ${session.solves.size} tempos")
                    }
                    if (preview.skippedSessionCount > 0 || preview.skippedSolveCount > 0) {
                        Text("Ficam de fora: ${preview.skippedSessionCount} sessões e ${preview.skippedSolveCount} tempos de outros tipos.")
                    }
                    preview.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    if (importing) {
                        Text("Os dados atuais serão mantidos. Sessões com nomes iguais receberão um sufixo.")
                    } else {
                        Text("Na próxima tela, escolha onde salvar o arquivo.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = if (importing) actions.confirmImport else actions.confirmExport,
                ) { Text(if (importing) "Importar em novas sessões" else "Escolher arquivo") }
            },
            dismissButton = {
                TextButton(enabled = !state.busy, onClick = actions.cancelPreview) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun TransferActionCard(title: String, body: String, enabled: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
