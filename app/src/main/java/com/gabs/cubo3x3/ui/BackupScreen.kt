package com.gabs.cubo3x3.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.data.backup.BackupSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    statusMessage: String?,
    pendingRestore: BackupSummary?,
    onExport: () -> Unit,
    onChooseImport: () -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    onBack: () -> Unit,
    onOpenTimerTransfer: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Backup e restauração") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Voltar") }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "Seus dados continuam locais. O arquivo só é criado ou lido quando você escolhe um documento.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                BackupActionCard(
                    title = "Exportar backup",
                    body = "Salva tema, lembretes, metas, plano de treino, progresso, tempos, recordes, respostas CFOP, fórmulas preferidas e algoritmos personalizados em .3x3backup.",
                    action = "EXPORTAR",
                    onClick = onExport,
                )
            }
            item {
                BackupActionCard(
                    title = "Importar backup",
                    body = "Valida o arquivo antes de mostrar a confirmação para substituir os dados atuais.",
                    action = "IMPORTAR",
                    onClick = onChooseImport,
                )
            }
            item {
                BackupActionCard(
                    title = "Transferir tempos e sessões",
                    body = "Importe e exporte históricos 3x3 compatíveis com o csTimer, com prévia e confirmação.",
                    action = "ABRIR",
                    onClick = onOpenTimerTransfer,
                )
            }
            statusMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Text(message, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            item {
                Text("Incluído no arquivo", style = MaterialTheme.typography.titleMedium)
            }
            items(
                listOf(
                    "Preferência de tema",
                    "Agenda de lembretes locais",
                    "Favoritos e concluídos do CFOP",
                    "Histórico do cronômetro",
                    "Recordes do Quiz",
                    "Respostas por caso do reconhecimento CFOP",
                    "Configuração reutilizável do treino CFOP",
                    "Fórmulas preferidas por caso",
                    "Configuração das metas diárias",
                    "Algoritmos personalizados",
                ),
            ) { label ->
                Text("• $label", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text(
                    "A importação não aceita arquivos .cfop nem backups com versão incompatível.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    pendingRestore?.let { summary ->
        AlertDialog(
            onDismissRequest = onCancelRestore,
            title = { Text("Substituir os dados atuais?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("O arquivo foi validado antes desta confirmação.")
                    Text("Criado no app ${summary.appVersion} em ${formatBackupDate(summary.exportedAtEpochMillis)}.")
                    Text(
                        "${summary.progressCount} marcações · " +
                            "${summary.solveTimeCount} tempos em ${summary.timerSessionCount} sessões · " +
                            "${summary.quizRecordCount} recordes · " +
                            "${summary.customAlgorithmCount} personalizados · " +
                            "${summary.cfopAttemptCount} respostas CFOP · " +
                            "${summary.preferredFormulaCount} fórmulas preferidas",
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        if (summary.reminderEnabled) {
                            "O arquivo contém um lembrete local ativado."
                        } else {
                            "O arquivo contém lembretes locais desativados."
                        },
                    )
                    Text("Esta ação substituirá os dados locais correspondentes.")
                    Text(if (summary.dailyGoalsEnabled)
                        "Metas também serão substituídas: ${summary.dailyRecognitionTarget} respostas CFOP e " +
                            "${summary.dailyExecutionTarget} tentativas dirigidas por dia (zero = desativada)."
                        else "Metas diárias serão desativadas. Backups anteriores não possuem essa configuração.")
                    Text("O treino salvo também será substituído: " + when (summary.trainingPlanMode) {
                        "COMPLETED" -> "concluídos e casos novos."
                        "CATEGORY" -> "categoria completa."
                        "CASE" -> "caso específico."
                        else -> "treino livre (padrão de backups anteriores)."
                    })
                    if (summary.cfopAttemptCount == 0) {
                        Text("O arquivo não contém respostas CFOP; o histórico CFOP atual também será substituído por vazio.")
                    }
                    if (summary.preferredFormulaCount == 0) {
                        Text("O arquivo não contém fórmulas preferidas; suas escolhas atuais também serão removidas, mantendo as originais do catálogo.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmRestore) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = onCancelRestore) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun BackupActionCard(
    title: String,
    body: String,
    action: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private val backupDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private fun formatBackupDate(epochMillis: Long): String = Instant
    .ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(backupDateFormatter)
