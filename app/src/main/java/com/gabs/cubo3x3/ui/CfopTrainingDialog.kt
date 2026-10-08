package com.gabs.cubo3x3.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import com.gabs.cubo3x3.domain.timer.CfopTrainingMode
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan

@Composable
internal fun CfopTrainingDialog(
    initialPlan: CfopTrainingPlan,
    progress: Map<String, AlgorithmProgress>,
    onDismiss: () -> Unit,
    onApply: (CfopTrainingPlan) -> Unit,
) {
    var modeName by rememberSaveable { mutableStateOf(initialPlan.mode.name) }
    var categoryName by rememberSaveable { mutableStateOf(initialPlan.category?.name ?: "F2L") }
    var caseNumber by rememberSaveable { mutableIntStateOf(initialPlan.caseNumber ?: 1) }
    var selectedCategories by rememberSaveable {
        mutableStateOf(initialPlan.completedCategories.map { it.name })
    }
    var extras by rememberSaveable { mutableStateOf(initialPlan.extraCaseIds.toList()) }
    var newCategoryName by rememberSaveable { mutableStateOf("F2L") }
    val mode = CfopTrainingMode.valueOf(modeName)
    val category = AlgorithmCategory.valueOf(categoryName)
    val categories = selectedCategories.map(AlgorithmCategory::valueOf).toSet()
    val validExtras = extras.filter { id ->
        AlgorithmCatalog.allEntries().any { it.id == id && it.category in categories }
    }.toSet()
    val plan = when (mode) {
        CfopTrainingMode.FREE -> CfopTrainingPlan()
        CfopTrainingMode.CATEGORY -> CfopTrainingPlan(mode, category)
        CfopTrainingMode.CASE -> CfopTrainingPlan(mode, category, caseNumber)
        CfopTrainingMode.COMPLETED -> CfopTrainingPlan(
            mode = mode, completedCategories = categories, extraCaseIds = validExtras,
        )
    }
    val selectedEntries = if (categories.isEmpty()) emptyList() else plan.entries(progress)
    val canApply = mode != CfopTrainingMode.COMPLETED || selectedEntries.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Montar treino CFOP") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("A configuração fica salva para repetir em outro dia. O setup e o caso ficam junto do tempo.")
                listOf(
                    CfopTrainingMode.FREE to "Treino livre WCA",
                    CfopTrainingMode.CATEGORY to "Categoria completa",
                    CfopTrainingMode.CASE to "Um caso específico",
                    CfopTrainingMode.COMPLETED to "Meus concluídos + novos",
                ).forEach { (option, label) ->
                    FilterChip(
                        selected = mode == option, onClick = { modeName = option.name },
                        label = { Text(label) }, modifier = Modifier.testTag("training-mode-${option.name}"),
                    )
                }
                if (mode == CfopTrainingMode.CATEGORY || mode == CfopTrainingMode.CASE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlgorithmCategory.entries.forEach { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = { categoryName = option.name; caseNumber = 1 },
                                label = { Text(option.label) },
                            )
                        }
                    }
                    if (mode == CfopTrainingMode.CASE) {
                        CaseNumberChips(AlgorithmCatalog.entries(category), setOf("$categoryName-$caseNumber")) {
                            caseNumber = it.number
                        }
                        Text(AlgorithmCatalog.entry(category, caseNumber).notation,
                            style = MaterialTheme.typography.bodySmall)
                    } else Text("Todos os casos desta etapa podem ser sorteados.")
                }
                if (mode == CfopTrainingMode.COMPLETED) {
                    Text("Etapas do treino", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlgorithmCategory.entries.forEach { option ->
                            FilterChip(
                                selected = option in categories,
                                onClick = {
                                    selectedCategories = ArrayList(
                                        if (option.name in selectedCategories) selectedCategories - option.name
                                        else selectedCategories + option.name,
                                    )
                                },
                                label = { Text(option.label) },
                                modifier = Modifier.testTag("training-category-${option.name}"),
                            )
                        }
                    }
                    val completed = selectedEntries.count { progress[it.id]?.isCompleted == true }
                    val newCases = selectedEntries.size - completed
                    Text(
                        "$completed ${if (completed == 1) "concluído" else "concluídos"} + " +
                            "$newCases ${if (newCases == 1) "novo" else "novos"} · " +
                            "${selectedEntries.size} ${if (selectedEntries.size == 1) "caso" else "casos"}",
                        modifier = Modifier.testTag("training-selection-summary"),
                    )
                    Text("Uma rodada passa por todos, sem repetir os já praticados. " +
                        "Pular ou visualizar não conta; uma tentativa finalizada, inclusive DNF, conta como prática. " +
                        "Novas marcações entram só na próxima rodada.")
                    if (!canApply) Text(
                        if (categories.isEmpty()) "Selecione pelo menos uma etapa."
                        else "Ainda não há concluídos nestas etapas. Marque casos no catálogo ou acrescente novos abaixo.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text("Acrescentar casos novos", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlgorithmCategory.entries.forEach { option ->
                            FilterChip(
                                selected = newCategoryName == option.name,
                                enabled = option in categories,
                                onClick = { newCategoryName = option.name },
                                label = { Text(option.label) },
                                modifier = Modifier.testTag("training-new-category-${option.name}"),
                            )
                        }
                    }
                    val newCategory = AlgorithmCategory.valueOf(newCategoryName)
                    if (newCategory in categories) {
                        val pending = AlgorithmCatalog.entries(newCategory).filter {
                            progress[it.id]?.isCompleted != true
                        }
                        CaseNumberChips(pending, validExtras) {
                            extras = ArrayList(if (it.id in extras) extras - it.id else extras + it.id)
                        }
                        if (pending.isEmpty()) Text("Todos os casos desta etapa já estão concluídos.")
                    }
                    Text("Treinar não altera favoritos nem a marcação de concluído.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(plan) }, enabled = canApply,
                modifier = Modifier.testTag("training-apply")) { Text("Aplicar e iniciar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun CaseNumberChips(
    entries: List<AlgorithmEntry>,
    selectedIds: Set<String>,
    onSelect: (AlgorithmEntry) -> Unit,
) {
    entries.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            row.forEach { entry ->
                FilterChip(
                    selected = entry.id in selectedIds,
                    onClick = { onSelect(entry) }, label = { Text(entry.number.toString()) },
                    modifier = Modifier.testTag("training-case-${entry.id}"),
                )
            }
        }
    }
}
