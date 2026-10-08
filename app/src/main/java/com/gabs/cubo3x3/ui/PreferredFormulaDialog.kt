package com.gabs.cubo3x3.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.domain.algorithm.CaseFormulaValidation
import com.gabs.cubo3x3.domain.algorithm.ValidatedCaseFormula
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PreferredFormulaDialog(
    entry: AlgorithmEntry,
    initialNotation: String?,
    onSave: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val starting = initialNotation ?: entry.notation
    var draft by rememberSaveable(entry.id) { mutableStateOf(starting) }
    var discard by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("IDLE") }
    var message by remember { mutableStateOf<String?>(null) }
    var verified by remember { mutableStateOf<ValidatedCaseFormula?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    fun cancelCheck() {
        generation++
        job?.cancel()
        job = null
        phase = "IDLE"
    }
    fun dismiss() {
        if (phase == "SAVING") return
        cancelCheck()
        if (draft != starting) discard = true else onDismiss()
    }
    AlertDialog(
        onDismissRequest = ::dismiss,
        title = { Text("Minha fórmula · ${entry.id}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("A fórmula original e a preparação do caso serão preservadas. " +
                    "Confira sua alternativa na orientação amarelo acima e vermelho à frente.")
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        cancelCheck()
                        verified = null
                        message = null
                        if (it.length <= CaseFormulaValidation.MAX_TEXT_LENGTH) draft = it
                        else message = "Use no máximo 2.000 caracteres."
                    },
                    enabled = phase == "IDLE",
                    label = { Text("Fórmula de resolução") },
                    minLines = 3, maxLines = 7,
                    modifier = Modifier.fillMaxWidth().testTag("preferred-formula-input"),
                )
                Text("Até 200 movimentos. U2' e demais variantes são mantidos.")
                message?.let { Text(it, Modifier.testTag("preferred-formula-message")) }
                verified?.let {
                    Text("Conferida: resolve a etapa ${entry.category.label} deste caso.",
                        Modifier.testTag("preferred-formula-valid"))
                    if (!it.finalState.isSolved()) Text(
                        "A última camada pode terminar diferente. Use a preparação original para montar o caso desde resolvido.")
                }
            }
        },
        confirmButton = {
            Column {
                Button(onClick = {
                    cancelCheck()
                    verified = null
                    message = null
                    phase = "CHECKING"
                    val request = generation
                    val input = draft
                    job = scope.launch {
                        try {
                            val result = withContext(Dispatchers.Default) {
                                CaseFormulaValidation.validate(entry, input)
                            }
                            if (generation == request) verified = result
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            if (generation == request) message = error.message ?: "Não foi possível conferir."
                        } finally {
                            if (generation == request) { phase = "IDLE"; job = null }
                        }
                    }
                }, enabled = phase == "IDLE", modifier = Modifier.testTag("preferred-formula-check")) {
                    Text(if (phase == "CHECKING") "Conferindo…" else "Conferir fórmula")
                }
                TextButton(onClick = {
                    val value = verified ?: return@TextButton
                    phase = "SAVING"
                    message = null
                    job = scope.launch {
                        try {
                            onSave(value.notation)
                            onDismiss()
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            message = error.message ?: "Não foi possível salvar. Sua escolha anterior foi mantida."
                        } finally { phase = "IDLE"; job = null }
                    }
                }, enabled = verified != null && phase == "IDLE",
                    modifier = Modifier.testTag("preferred-formula-save")) {
                    Text(if (phase == "SAVING") "Salvando…" else "Salvar preferida")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = ::dismiss, enabled = phase != "SAVING") { Text("Cancelar") }
        },
    )
    if (discard) AlertDialog(
        onDismissRequest = { discard = false },
        title = { Text("Descartar alterações?") },
        text = { Text("O rascunho não foi salvo. A fórmula anterior será mantida.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Continuar editando") } },
    )
}
