package com.gabs.cubo3x3.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.cube.*
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class CaseSequence(
    val initialState: CubeState,
    val notation: String,
    val title: String,
    val instructions: String,
    val completedText: String = "Concluído · caso preparado",
)

private val paintHistorySaver = listSaver<CubePaintHistory, String>(
    save = { it.checkpoint() },
    restore = { CubePaintHistory.restore(it) },
)

@Composable
internal fun CasePreparationFlow(
    entry: AlgorithmEntry,
    onBack: () -> Unit,
    calculate: suspend (String, CubeState) -> CaseSolution = CaseSolver::calculate,
) {
    var page by rememberSaveable(entry.id) { mutableStateOf("setup") }
    var painting by rememberSaveable(entry.id, stateSaver = paintHistorySaver) {
        mutableStateOf(CubePaintHistory.solved())
    }
    val draft = painting.facelets
    var resultNotation by rememberSaveable(entry.id) { mutableStateOf<String?>(null) }
    var elapsed by rememberSaveable(entry.id) { mutableLongStateOf(0L) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var requestId by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val target = remember(entry.id) { AlgorithmCatalog.initialState(entry) }
    val setup = remember(entry.id) { AlgorithmCatalog.setupNotation(entry) }
    val screenStates = rememberSaveableStateHolder()

    fun cancel() {
        requestId++
        job?.cancel()
        job = null
        busy = false
    }
    fun back() {
        cancel()
        when (page) {
            "editor" -> page = "setup"
            "result" -> page = "editor"
            else -> onBack()
        }
    }
    BackHandler { back() }

    screenStates.SaveableStateProvider(page) {
    if (page == "editor") {
        CubeCaseEditor(entry, painting, busy, message,
            onPaintingChange = { painting = it; resultNotation = null; message = null },
            onBack = ::back,
            onCancel = { cancel(); message = "Cálculo cancelado. A pintura foi mantida." },
            onCalculate = {
                message = CaseSolver.validationError(draft)
                if (message == null) {
                    cancel()
                    screenStates.removeState("result")
                    busy = true
                    val request = requestId
                    val source = draft
                    job = scope.launch {
                        try {
                            val result = calculate(source, target)
                            if (request == requestId && painting.facelets == source) {
                                resultNotation = result.notation
                                elapsed = result.elapsedMillis
                                page = "result"
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            if (request == requestId) message = error.message ?: "Não foi possível calcular."
                        } finally {
                            if (request == requestId) { busy = false; job = null }
                        }
                    }
                }
            },
        )
    } else {
        val result = page == "result" && resultNotation != null
        AlgorithmPlayerScreen(
            category = entry.category, number = entry.number,
            progress = AlgorithmProgress(),
            onFavoriteChange = { _, _ -> }, onCompletedChange = { _, _ -> },
            onBack = ::back,
            sequence = if (result) {
                CaseSequence(CubeFacelets.decode(draft), resultNotation.orEmpty(),
                    "Do meu cubo ao caso",
                    if (resultNotation!!.isBlank()) "Seu cubo já está neste caso."
                    else "Sequência conferida nos 54 adesivos. Cálculo: $elapsed ms. " +
                        "Execute no cubo físico mantendo a mesma orientação inicial.",
                    if (resultNotation!!.isBlank()) "Seu cubo já está neste caso"
                    else "Concluído · caso preparado")
            } else {
                CaseSequence(CubeState.solved(), setup, "Preparar desde resolvido",
                    "Comece com o cubo resolvido: amarelo acima, branco abaixo, vermelho à frente " +
                        "e verde à direita. Execute toda a preparação; depois use a fórmula do caso. " +
                        "Rotações x, y e z, quando presentes, fazem parte da preparação.")
            },
            onConfigureCube = { page = "editor"; message = null },
            onReturnToCase = { cancel(); onBack() },
        )
    }
    }
}

private val faceNames = listOf("Acima", "Direita", "Frente", "Abaixo", "Esquerda", "Atrás")
private val colorNames = listOf("Amarelo", "Verde", "Vermelho", "Branco", "Azul", "Laranja")
private val colorCodes = listOf("AM", "VD", "VM", "BR", "AZ", "LR")
// Vizinhos na borda de cada face vista de frente: cima, esquerda, direita, baixo.
private val neighbors = listOf("BLRF", "UFBD", "ULRD", "FLRB", "UBFD", "URLD")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CubeCaseEditor(
    entry: AlgorithmEntry,
    painting: CubePaintHistory,
    busy: Boolean,
    message: String?,
    onPaintingChange: (CubePaintHistory) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onCalculate: () -> Unit,
) {
    var face by rememberSaveable(entry.id) { mutableIntStateOf(0) }
    var selectedColor by rememberSaveable(entry.id) { mutableIntStateOf(0) }
    var confirmReset by remember { mutableStateOf(false) }
    val draft = painting.facelets
    val editorScope = rememberCoroutineScope()
    val gridIntoView = remember { BringIntoViewRequester() }
    val cube = remember(draft) { CubeFacelets.decode(draft) }
    val validation = remember(draft) { CubeConfigurationValidation.inspect(draft) }
    // Giro apenas da previa: a configuracao original enviada ao solver nao muda.
    val preview = remember(cube, face) {
        if (face == 3) cube.apply(MoveNotation.parseAlgorithm("x2")) else cube
    }
    val view = when (face) {
        // O yaw positivo do renderizador traz a face L para a camera.
        4 -> CubeViewpoint.RIGHT
        5 -> CubeViewpoint.BACK
        else -> CubeViewpoint.FRONT
    }
    Scaffold(topBar = {
        TopAppBar(title = { Column {
            Text("Configurar meu cubo")
            Text("Destino: ${entry.category.label} ${entry.number}",
                style = MaterialTheme.typography.labelMedium)
        } }, navigationIcon = { TextButton(onClick = onBack) { Text("‹ Voltar") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Oriente seu cubo: amarelo acima, branco abaixo, vermelho à frente e verde à direita. " +
                "Pinte as seis faces olhando cada uma de frente. Os centros não mudam.")
            Text("Prévia 3D · face ${CubeFacelets.faces[face]} visível",
                style = MaterialTheme.typography.labelSmall)
            SolidCubeRenderer(preview, MoveNotation.parseToken("U"), 0f,
                Modifier.fillMaxWidth().height(180.dp), viewpoint = view)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CubeFacelets.faces.forEachIndexed { index, letter ->
                    FilterChip(face == index, onClick = { face = index }, enabled = !busy,
                        modifier = Modifier.semantics {
                            if (validation.reviewStickerIndices.any { it / 9 == index }) {
                                stateDescription = "Contém peças para revisar"
                            }
                        },
                        label = { Text("$letter · ${faceNames[index]}") })
                }
            }
            val adjacent = neighbors[face]
            fun neighborName(letter: Char) = colorNames[CubeFacelets.faces.indexOf(letter)]
            Text("Face ${CubeFacelets.faces[face]} · ${faceNames[face]}",
                fontWeight = FontWeight.Bold)
            Text("Acima: ${neighborName(adjacent[0])} · Esquerda: ${neighborName(adjacent[1])}\n" +
                "Direita: ${neighborName(adjacent[2])} · Abaixo: ${neighborName(adjacent[3])}",
                style = MaterialTheme.typography.bodySmall)
            Column(Modifier.align(Alignment.CenterHorizontally).bringIntoViewRequester(gridIntoView),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) { column ->
                            val cell = row * 3 + column
                            val index = face * 9 + cell
                            val colorIndex = CubeFacelets.faces.indexOf(draft[index])
                            val review = index in validation.reviewStickerIndices
                            Surface(
                                color = CubeFacelets.colors[colorIndex].toComposeColor(CubeColorScheme.STANDARD),
                                contentColor = Color.Black,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(if (review) 3.dp else 1.dp,
                                    if (review) MaterialTheme.colorScheme.error else Color.DarkGray),
                                modifier = Modifier.size(64.dp).semantics {
                                    contentDescription = "Face ${CubeFacelets.faces[face]} adesivo ${cell + 1}: " +
                                        colorNames[colorIndex] + if (cell == 4) ", centro fixo" else ""
                                    if (review) stateDescription = "Peça para revisar"
                                }.clickable(enabled = !busy && cell != 4) {
                                    val next = painting.paint(index, CubeFacelets.faces[selectedColor])
                                    if (next !== painting) onPaintingChange(next)
                                },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text((if (cell == 4) CubeFacelets.faces[face].toString()
                                    else colorCodes[colorIndex]) + if (review) " !" else "",
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Text("Escolha uma cor e toque nos adesivos", style = MaterialTheme.typography.labelLarge)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(2) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) { column ->
                            val index = row * 3 + column
                            FilterChip(selectedColor == index, { selectedColor = index },
                                enabled = !busy, modifier = Modifier.weight(1f),
                                label = { Text(colorNames[index], style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val next = painting.undo()
                    face = next.changedFaceFrom(painting) ?: face
                    onPaintingChange(next)
                }, enabled = !busy && painting.canUndo, modifier = Modifier.weight(1f)) {
                    Text("Desfazer pintura")
                }
                OutlinedButton(onClick = {
                    val next = painting.redo()
                    face = next.changedFaceFrom(painting) ?: face
                    onPaintingChange(next)
                }, enabled = !busy && painting.canRedo, modifier = Modifier.weight(1f)) {
                    Text("Refazer pintura")
                }
            }
            Text("Até ${CubePaintHistory.MAX_UNDO_STEPS} alterações, em qualquer face. " +
                "Uma nova pintura substitui o caminho de refazer.",
                style = MaterialTheme.typography.bodySmall)
            EditorValidationSummary(validation, busy, onReviewFace = {
                face = it
                editorScope.launch { gridIntoView.bringIntoView() }
            })
            if (message != null && message != validation.message) {
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Calculando offline… Na primeira vez, a preparação do motor pode demorar mais.")
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar cálculo")
                }
            } else {
                Button(onClick = onCalculate, enabled = validation.isValid,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Calcular até este caso")
                }
                OutlinedButton(onClick = { confirmReset = true },
                    enabled = draft != CubeFacelets.solved, modifier = Modifier.fillMaxWidth()) {
                    Text("Voltar à pintura resolvida")
                }
            }
            Text("A pintura é mantida ao voltar dentro deste caso. Não é salva no backup.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Reiniciar pintura?") },
        text = { Text("A configuração informada será substituída pelo cubo resolvido. " +
            "Você poderá desfazer esta reinicialização.") },
        confirmButton = { TextButton(onClick = {
            onPaintingChange(painting.reset()); confirmReset = false
        }) { Text("Reiniciar") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
    )
}

@Composable
private fun EditorValidationSummary(
    validation: CubeConfigurationReport,
    busy: Boolean,
    onReviewFace: (Int) -> Unit,
) {
    Text("Conferência da pintura", style = MaterialTheme.typography.titleSmall)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { column ->
                    val index = row * 2 + column
                    val count = validation.colorCounts[index]
                    val difference = when {
                        count == 8 -> "falta 1"
                        count == 10 -> "sobra 1"
                        count < 9 -> "faltam ${9 - count}"
                        count > 9 -> "sobram ${count - 9}"
                        else -> "completo"
                    }
                    Text("${colorNames[index]}: $count/9 · $difference",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (count == 9) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    Surface(
        color = if (validation.isValid) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(validation.message ?: "Configuração válida · pronta para calcular",
                style = MaterialTheme.typography.bodyMedium)
            if (!validation.isValid) {
                Text(if (validation.reviewStickerIndices.isNotEmpty())
                    "O sinal ! marca peças para conferir. Em peças repetidas, todos os exemplares " +
                        "são marcados; isso não identifica qual adesivo você deve alterar."
                    else if (validation.problem == CubeConfigurationProblem.COLOR_COUNT)
                        "Confira as contagens antes de calcular. Nenhum adesivo é alterado automaticamente."
                    else "Esse teste não identifica um adesivo específico para corrigir. " +
                        "Confira a orientação e a pintura de todas as faces.",
                    style = MaterialTheme.typography.bodySmall)
                validation.reviewStickerIndices.map { it / 9 }.distinct().sorted().forEach { face ->
                    TextButton(onClick = { onReviewFace(face) }, enabled = !busy) {
                        Text("Revisar face ${CubeFacelets.faces[face]} · ${faceNames[face]}")
                    }
                }
            }
        }
    }
}
