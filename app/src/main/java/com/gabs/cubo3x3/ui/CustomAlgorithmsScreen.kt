package com.gabs.cubo3x3.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.cube.CubeColorScheme
import com.gabs.cubo3x3.cube.CubeRenderer
import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.CubeViewpoint
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.data.custom.CustomAlgorithm

private const val LIST_ID = Long.MIN_VALUE
private const val NEW_ID = -1L

private enum class MoveGroup(val label: String, val roots: List<String>) {
    FACES("Faces", listOf("U", "D", "L", "R", "F", "B")),
    WIDE("Largos", listOf("u", "d", "l", "r", "f", "b")),
    SLICES("Fatias", listOf("M", "E", "S")),
    ROTATIONS("Rotações", listOf("x", "y", "z")),
}

@Composable
fun CustomAlgorithmsScreen(
    algorithms: List<CustomAlgorithm>,
    onCreate: (String, String, List<String>, CubeColorScheme, CubeViewpoint) -> Unit,
    onUpdate: (Long, String, String, List<String>, CubeColorScheme, CubeViewpoint) -> Unit,
    onDuplicate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var editorId by rememberSaveable { mutableLongStateOf(LIST_ID) }
    var editorDirty by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    fun closeEditor() {
        editorId = LIST_ID
        editorDirty = false
    }

    BackHandler {
        if (editorId == LIST_ID) {
            onBack()
        } else if (editorDirty) {
            confirmDiscard = true
        } else {
            closeEditor()
        }
    }

    if (editorId == LIST_ID) {
        CustomAlgorithmList(
            algorithms = algorithms,
            onCreate = { editorId = NEW_ID },
            onEdit = { editorId = it },
            onDuplicate = onDuplicate,
            onDelete = onDelete,
            onBack = onBack,
        )
    } else {
        val initial = algorithms.singleOrNull { it.id == editorId }
        CustomAlgorithmEditor(
            editorKey = editorId,
            initial = initial,
            onDirtyChange = { editorDirty = it },
            onSave = { name, notation, tags, colorScheme, viewpoint ->
                if (editorId == NEW_ID) {
                    onCreate(name, notation, tags, colorScheme, viewpoint)
                } else {
                    onUpdate(editorId, name, notation, tags, colorScheme, viewpoint)
                }
                closeEditor()
            },
            onBack = {
                if (editorDirty) confirmDiscard = true else closeEditor()
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Descartar alterações?") },
            text = { Text("As mudanças ainda não salvas serão perdidas.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    closeEditor()
                }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Continuar editando") }
            },
        )
    }
}

@Composable
private fun CustomAlgorithmList(
    algorithms: List<CustomAlgorithm>,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    val tags = algorithms.flatMap(CustomAlgorithm::tags).distinctBy(String::lowercase).sorted()
    val filtered = algorithms.filter { algorithm ->
        val matchesQuery = query.isBlank() || algorithm.name.contains(query, ignoreCase = true)
        val matchesTag = selectedTag == null || algorithm.tags.any {
            it.equals(selectedTag, ignoreCase = true)
        }
        matchesQuery && matchesTag
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("‹ Voltar") }
                Text("Personalizados", style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Pesquisar por nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (tags.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Filtrar por tag", style = MaterialTheme.typography.labelLarge)
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { selectedTag = null },
                        label = { Text("Todas") },
                    )
                    tags.forEach { tag ->
                        FilterChip(
                            selected = tag == selectedTag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag) },
                        )
                    }
                }
            }
        }
        item {
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Text("Criar algoritmo")
            }
        }
        if (filtered.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            if (algorithms.isEmpty()) "Seu primeiro algoritmo começa aqui" else "Nenhum resultado",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (algorithms.isEmpty()) {
                                "Crie uma sequência, escolha a prévia e salve tudo localmente."
                            } else {
                                "Tente outro nome ou remova o filtro de tag."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        items(filtered, key = CustomAlgorithm::id) { algorithm ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(algorithm.name, style = MaterialTheme.typography.titleMedium)
                    Text(algorithm.notation, fontFamily = FontFamily.Monospace)
                    if (algorithm.tags.isNotEmpty()) {
                        Text(
                            algorithm.tags.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(onClick = { onEdit(algorithm.id) }) { Text("Editar") }
                        TextButton(onClick = { onDuplicate(algorithm.id) }) { Text("Duplicar") }
                        TextButton(onClick = { pendingDeleteId = algorithm.id }) { Text("Excluir") }
                    }
                }
            }
        }
    }

    val deleting = algorithms.singleOrNull { it.id == pendingDeleteId }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Excluir ${deleting.name}?") },
            text = { Text("Esta ação remove o algoritmo somente deste aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(deleting.id)
                    pendingDeleteId = null
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun CustomAlgorithmEditor(
    editorKey: Long,
    initial: CustomAlgorithm?,
    onDirtyChange: (Boolean) -> Unit,
    onSave: (String, String, List<String>, CubeColorScheme, CubeViewpoint) -> Unit,
    onBack: () -> Unit,
) {
    var name by rememberSaveable(editorKey) { mutableStateOf(initial?.name.orEmpty()) }
    var tagsText by rememberSaveable(editorKey) { mutableStateOf(initial?.tags?.joinToString(", ").orEmpty()) }
    var notation by rememberSaveable(editorKey) { mutableStateOf(initial?.notation.orEmpty()) }
    var colorSchemeName by rememberSaveable(editorKey) {
        mutableStateOf((initial?.colorScheme ?: CubeColorScheme.STANDARD).name)
    }
    var viewpointName by rememberSaveable(editorKey) {
        mutableStateOf((initial?.viewpoint ?: CubeViewpoint.FRONT).name)
    }
    var moveGroupName by rememberSaveable(editorKey) { mutableStateOf(MoveGroup.FACES.name) }
    val colorScheme = CubeColorScheme.valueOf(colorSchemeName)
    val viewpoint = CubeViewpoint.valueOf(viewpointName)
    val moveGroup = MoveGroup.valueOf(moveGroupName)
    val tokens = notation.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    val parsedMoves = runCatching { MoveNotation.parseAlgorithm(notation) }.getOrDefault(emptyList())
    val previewState = remember(notation) { CubeState.solved().apply(parsedMoves) }
    val isDirty = name != initial?.name.orEmpty() ||
        tagsText != initial?.tags?.joinToString(", ").orEmpty() ||
        notation != initial?.notation.orEmpty() ||
        colorScheme != (initial?.colorScheme ?: CubeColorScheme.STANDARD) ||
        viewpoint != (initial?.viewpoint ?: CubeViewpoint.FRONT)
    LaunchedEffect(isDirty) { onDirtyChange(isDirty) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("‹ Voltar") }
                Text(
                    if (initial == null) "Novo algoritmo" else "Editar algoritmo",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome obrigatório") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text("Tags separadas por vírgula") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                CubeRenderer(
                    state = previewState,
                    animatedMove = null,
                    progress = 0f,
                    colorScheme = colorScheme,
                    viewpoint = viewpoint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(235.dp)
                        .padding(8.dp),
                )
            }
        }
        item {
            Text(
                text = notation.ifBlank { "A sequência aparecerá aqui" },
                modifier = Modifier.fillMaxWidth(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { notation = tokens.dropLast(1).joinToString(" ") },
                    enabled = tokens.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text("Desfazer") }
                OutlinedButton(
                    onClick = { notation = "" },
                    enabled = tokens.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text("Limpar") }
            }
        }
        item {
            Text("Grupo de movimentos", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MoveGroup.entries.forEach { group ->
                    FilterChip(
                        selected = moveGroup == group,
                        onClick = { moveGroupName = group.name },
                        label = { Text(group.label) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        items(moveGroup.roots, key = { it }) { root ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(root, "$root'", "${root}2", "${root}2'").forEach { token ->
                    OutlinedButton(
                        onClick = { notation = (tokens + token).joinToString(" ") },
                        modifier = Modifier.weight(1f),
                    ) { Text(token, fontFamily = FontFamily.Monospace) }
                }
            }
        }
        item { Text("Cores da prévia", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CubeColorScheme.entries.forEach { scheme ->
                    FilterChip(
                        selected = colorScheme == scheme,
                        onClick = { colorSchemeName = scheme.name },
                        label = { Text(scheme.label) },
                    )
                }
            }
        }
        item { Text("Ponto de vista", style = MaterialTheme.typography.titleMedium) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CubeViewpoint.entries.forEach { option ->
                    FilterChip(
                        selected = viewpoint == option,
                        onClick = { viewpointName = option.name },
                        label = { Text(option.label) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    onSave(
                        name,
                        notation,
                        tagsText.split(',').map(String::trim),
                        colorScheme,
                        viewpoint,
                    )
                },
                enabled = name.isNotBlank() && parsedMoves.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (initial == null) "Salvar algoritmo" else "Atualizar algoritmo")
            }
        }
    }
}
