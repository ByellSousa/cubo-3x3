package com.gabs.cubo3x3.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.CubeFacelets
import com.gabs.cubo3x3.cube.CubePlaybackCheckpoint
import com.gabs.cubo3x3.cube.Move
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.cube.StickerColor
import com.gabs.cubo3x3.cube.Vec3i
import com.gabs.cubo3x3.data.F2LAlgorithms
import com.gabs.cubo3x3.data.OLLAlgorithms
import com.gabs.cubo3x3.data.PLLAlgorithms
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import com.gabs.cubo3x3.domain.algorithm.PreferredCaseFormula
import com.gabs.cubo3x3.domain.algorithm.CaseFormulaValidation
import com.gabs.cubo3x3.ui.theme.F2LDark
import com.gabs.cubo3x3.ui.theme.F2LLight
import com.gabs.cubo3x3.ui.theme.OLLDark
import com.gabs.cubo3x3.ui.theme.OLLLight
import com.gabs.cubo3x3.ui.theme.PLLDark
import com.gabs.cubo3x3.ui.theme.PLLLight
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class AlgorithmCategory(
    val label: String,
    val description: String,
) {
    F2L("F2L", "Pares da primeira e segunda camadas"),
    OLL("OLL", "Orientação da última camada"),
    PLL("PLL", "Permutação da última camada"),
}

enum class SavedAlgorithmFilter(val title: String, val description: String) {
    FAVORITES("Favoritos", "Algoritmos marcados para revisar"),
    COMPLETED("Concluídos", "Casos que você já estudou"),
}

data class AlgorithmEntry(
    val category: AlgorithmCategory,
    val number: Int,
    val notation: String,
) {
    val id: String = category.name + "-" + number
}

object AlgorithmCatalog {
    private val entriesByCategory: Map<AlgorithmCategory, List<AlgorithmEntry>> = mapOf(
        AlgorithmCategory.F2L to F2LAlgorithms.all.map { case ->
            AlgorithmEntry(AlgorithmCategory.F2L, case.number, case.notation)
        },
        AlgorithmCategory.OLL to OLLAlgorithms.all.map { case ->
            AlgorithmEntry(AlgorithmCategory.OLL, case.number, case.notation)
        },
        AlgorithmCategory.PLL to PLLAlgorithms.all.map { case ->
            AlgorithmEntry(AlgorithmCategory.PLL, case.number, case.notation)
        },
    )

    fun entries(category: AlgorithmCategory): List<AlgorithmEntry> =
        entriesByCategory.getValue(category)

    fun allEntries(): List<AlgorithmEntry> = AlgorithmCategory.entries.flatMap(::entries)

    fun entry(category: AlgorithmCategory, number: Int): AlgorithmEntry =
        entries(category).single { it.number == number }

    fun initialState(entry: AlgorithmEntry): CubeState {
        val moves = MoveNotation.parseAlgorithm(entry.notation)
        val target = orientedSolvedTarget(moves)
        return target.apply(moves.asReversed().map(Move::inverse))
    }

    fun setupNotation(entry: AlgorithmEntry): String {
        val prefix = com.gabs.cubo3x3.cube.CubeOrientations.setupTo(
            orientedSolvedTarget(MoveNotation.parseAlgorithm(entry.notation)),
        )
        return listOf(prefix, inverseNotation(entry.notation)).filter(String::isNotBlank)
            .joinToString(" ")
    }

    private fun inverseNotation(notation: String): String = notation
        .trim()
        .split(Regex("\\s+"))
        .asReversed()
        .joinToString(" ") { token ->
            val move = MoveNotation.parseToken(token)
            when {
                move.repetitions == 2 -> token
                token.endsWith("'") -> token.dropLast(1)
                else -> token + "'"
            }
        }

    private fun orientedSolvedTarget(moves: List<Move>): CubeState {
        val solved = CubeState.solved()
        val centerResult = solved.apply(moves)
        val centers = centerResult.stickers
            .filter { it.position == it.normal && it.position.nonZeroCount() == 1 }
            .associate { it.color to it.normal }
        val right = requireNotNull(centers[StickerColor.GREEN])
        val up = requireNotNull(centers[StickerColor.YELLOW])
        val front = requireNotNull(centers[StickerColor.RED])

        fun oriented(vector: Vec3i): Vec3i = Vec3i(
            x = right.x * vector.x + up.x * vector.y + front.x * vector.z,
            y = right.y * vector.x + up.y * vector.y + front.y * vector.z,
            z = right.z * vector.x + up.z * vector.y + front.z * vector.z,
        )

        return CubeState(
            solved.stickers.map { sticker ->
                sticker.copy(
                    position = oriented(sticker.position),
                    normal = oriented(sticker.normal),
                )
            },
        )
    }

    private fun Vec3i.nonZeroCount(): Int = listOf(x, y, z).count { it != 0 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmListScreen(
    category: AlgorithmCategory,
    progress: Map<String, AlgorithmProgress>,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenAlgorithm: (AlgorithmEntry) -> Unit,
) {
    val entries = remember(category) { AlgorithmCatalog.entries(category) }
    val accent = algorithmAccent(category)
    var query by rememberSaveable { mutableStateOf("") }
    var studyFilterName by rememberSaveable { mutableStateOf(AlgorithmStudyFilter.ALL.name) }
    val studyFilter = AlgorithmStudyFilter.valueOf(studyFilterName)
    val visibleEntries = remember(entries, progress, query, studyFilter) {
        AlgorithmCatalogQuery.filter(entries, progress, query, studyFilter)
    }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    fun clearSearch() {
        query = ""
        studyFilterName = AlgorithmStudyFilter.ALL.name
        scope.launch { listState.scrollToItem(0) }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text("‹ Voltar")
                        }
                    },
                    title = {
                        Column {
                            Text(category.label)
                            Text(
                                category.description,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
                CatalogSearchControls(
                    query = query,
                    onQueryChange = { query = it; scope.launch { listState.scrollToItem(0) } },
                    studyFilter = studyFilter,
                    onStudyFilterChange = {
                        studyFilterName = it.name
                        scope.launch { listState.scrollToItem(0) }
                    },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("algorithm-case-list"),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    contentColor = accent,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            if (query.isBlank() && studyFilter == AlgorithmStudyFilter.ALL) {
                                "${entries.size} casos únicos"
                            } else {
                                "${visibleEntries.size} de ${entries.size} casos"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Toque em um caso para abrir a sequência e o cubo animado.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (visibleEntries.isEmpty()) {
                item { EmptyCatalogSearch(onClear = ::clearSearch) }
            }
            items(visibleEntries, key = AlgorithmEntry::id) { entry ->
                AlgorithmCaseCard(
                    entry = entry,
                    progress = progress[entry.id] ?: AlgorithmProgress(),
                    accent = accent,
                    onClick = { keyboard?.hide(); onOpenAlgorithm(entry) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAlgorithmListScreen(
    filter: SavedAlgorithmFilter,
    progress: Map<String, AlgorithmProgress>,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenAlgorithm: (AlgorithmEntry) -> Unit,
) {
    val entries = remember(filter, progress) {
        AlgorithmCatalog.allEntries().filter { entry ->
            when (filter) {
                SavedAlgorithmFilter.FAVORITES -> progress[entry.id]?.isFavorite == true
                SavedAlgorithmFilter.COMPLETED -> progress[entry.id]?.isCompleted == true
            }
        }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var studyFilterName by rememberSaveable { mutableStateOf(AlgorithmStudyFilter.ALL.name) }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val studyFilter = AlgorithmStudyFilter.valueOf(studyFilterName)
    val category = categoryName?.let(AlgorithmCategory::valueOf)
    val visibleEntries = remember(entries, progress, query, studyFilter, category) {
        AlgorithmCatalogQuery.filter(entries, progress, query, studyFilter, category)
    }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    fun clearSearch() {
        query = ""
        studyFilterName = AlgorithmStudyFilter.ALL.name
        categoryName = null
        scope.launch { listState.scrollToItem(0) }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("‹ Voltar") }
                    },
                    title = {
                        Column {
                            Text(filter.title)
                            Text(
                                filter.description,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
                CatalogSearchControls(
                    query = query,
                    onQueryChange = { query = it; scope.launch { listState.scrollToItem(0) } },
                    studyFilter = studyFilter,
                    onStudyFilterChange = {
                        studyFilterName = it.name
                        scope.launch { listState.scrollToItem(0) }
                    },
                    studyFilters = when (filter) {
                        SavedAlgorithmFilter.FAVORITES -> listOf(
                            AlgorithmStudyFilter.ALL, AlgorithmStudyFilter.COMPLETED,
                            AlgorithmStudyFilter.NOT_COMPLETED,
                        )
                        SavedAlgorithmFilter.COMPLETED -> listOf(
                            AlgorithmStudyFilter.ALL, AlgorithmStudyFilter.FAVORITES,
                        )
                    },
                    showCategories = true,
                    category = category,
                    onCategoryChange = {
                        categoryName = it?.name
                        scope.launch { listState.scrollToItem(0) }
                    },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("algorithm-case-list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "${visibleEntries.size} de ${entries.size} casos",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entries.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            if (filter == SavedAlgorithmFilter.FAVORITES) {
                                "Nenhum favorito ainda. Abra um caso e toque em Favorito."
                            } else {
                                "Nenhum caso concluído ainda. Abra um caso e marque como Concluído."
                            },
                            modifier = Modifier.padding(18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (visibleEntries.isEmpty()) {
                item { EmptyCatalogSearch(onClear = ::clearSearch) }
            } else {
                items(visibleEntries, key = AlgorithmEntry::id) { entry ->
                    AlgorithmCaseCard(
                        entry = entry,
                        progress = progress.getValue(entry.id),
                        accent = algorithmAccent(entry.category),
                        onClick = { keyboard?.hide(); onOpenAlgorithm(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogSearchControls(
    query: String,
    onQueryChange: (String) -> Unit,
    studyFilter: AlgorithmStudyFilter,
    onStudyFilterChange: (AlgorithmStudyFilter) -> Unit,
    studyFilters: List<AlgorithmStudyFilter> = AlgorithmStudyFilter.entries,
    showCategories: Boolean = false,
    category: AlgorithmCategory? = null,
    onCategoryChange: (AlgorithmCategory?) -> Unit = {},
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Buscar casos" },
            singleLine = true,
            label = { Text("Buscar número, categoria ou fórmula") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(onClick = { onQueryChange("") }) { Text("Limpar") }
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            studyFilters.forEach { option ->
                FilterChip(
                    selected = studyFilter == option,
                    onClick = { onStudyFilterChange(option) },
                    label = { Text(option.label) },
                    modifier = Modifier.semantics {
                        contentDescription = "Filtrar status: ${option.label}"
                    },
                )
            }
        }
        if (showCategories) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = category == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("Todas as etapas") },
                    modifier = Modifier.semantics { contentDescription = "Filtrar etapa: Todas" },
                )
                AlgorithmCategory.entries.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { onCategoryChange(option) },
                        label = { Text(option.label) },
                        modifier = Modifier.semantics {
                            contentDescription = "Filtrar etapa: ${option.label}"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalogSearch(onClear: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Nenhum caso encontrado com esta busca e estes filtros.")
            TextButton(onClick = onClear) { Text("Limpar busca e filtros") }
        }
    }
}

@Composable
private fun AlgorithmCaseCard(
    entry: AlgorithmEntry,
    progress: AlgorithmProgress,
    accent: Color,
    onClick: () -> Unit,
) {
    val initialState = remember(entry.id) { AlgorithmCatalog.initialState(entry) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Abrir ${entry.category.label} caso ${entry.number}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(112.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
            ) {
                AlgorithmCaseDiagram(
                    entry = entry,
                    state = initialState,
                    animatedMove = null,
                    progress = 0f,
                    isDetail = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Caso ${entry.number}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Surface(
                        color = accent.copy(alpha = 0.15f),
                        contentColor = accent,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            entry.category.label,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    entry.notation,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progress.isFavorite || progress.isCompleted) {
                    Text(
                        listOfNotNull(
                            "★ Favorito".takeIf { progress.isFavorite },
                            "✓ Concluído".takeIf { progress.isCompleted },
                        ).joinToString("  ·  "),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                }
                Text(
                    "Ver detalhes  ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
            }
        }
    }
}

@Composable
fun AlgorithmDetailScreen(
    category: AlgorithmCategory,
    number: Int,
    progress: AlgorithmProgress,
    onFavoriteChange: (AlgorithmEntry, Boolean) -> Unit,
    onCompletedChange: (AlgorithmEntry, Boolean) -> Unit,
    onBack: () -> Unit,
    preferredFormula: PreferredCaseFormula? = null,
    onPreferredFormulaChange: (suspend (AlgorithmEntry, String?) -> Unit)? = null,
) {
    val entry = remember(category, number) { AlgorithmCatalog.entry(category, number) }
    var preparationOpen by rememberSaveable(entry.id) { mutableStateOf(false) }
    val screenStates = rememberSaveableStateHolder()
    screenStates.SaveableStateProvider(entry.id + if (preparationOpen) ":prepare" else ":formula") {
        if (preparationOpen) {
            CasePreparationFlow(entry, onBack = { preparationOpen = false })
        } else {
            AlgorithmPlayerScreen(category, number, progress, onFavoriteChange, onCompletedChange,
                onBack, onPrepare = { preparationOpen = true },
                preferredFormula = preferredFormula, onPreferredFormulaChange = onPreferredFormulaChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlgorithmPlayerScreen(
    category: AlgorithmCategory,
    number: Int,
    progress: AlgorithmProgress,
    onFavoriteChange: (AlgorithmEntry, Boolean) -> Unit,
    onCompletedChange: (AlgorithmEntry, Boolean) -> Unit,
    onBack: () -> Unit,
    sequence: CaseSequence? = null,
    onPrepare: (() -> Unit)? = null,
    onConfigureCube: (() -> Unit)? = null,
    onReturnToCase: (() -> Unit)? = null,
    preferredFormula: PreferredCaseFormula? = null,
    onPreferredFormulaChange: (suspend (AlgorithmEntry, String?) -> Unit)? = null,
) {
    val entry = remember(category, number) { AlgorithmCatalog.entry(category, number) }
    require(preferredFormula == null || preferredFormula.caseId == entry.id)
    var showOriginal by rememberSaveable(entry.id, preferredFormula?.notation) { mutableStateOf(false) }
    var editFormula by rememberSaveable(entry.id) { mutableStateOf(false) }
    var removeFormula by remember { mutableStateOf(false) }
    var changingPreference by remember { mutableStateOf(false) }
    var preferenceMessage by remember { mutableStateOf<String?>(null) }
    val usingPreferred = sequence == null && preferredFormula != null && !showOriginal
    val notation = sequence?.notation ?: if (usingPreferred) preferredFormula.notation else entry.notation
    val moves = remember(notation) { MoveNotation.parseAlgorithm(notation) }
    val initialState = remember(entry.id, sequence?.initialState) {
        sequence?.initialState ?: AlgorithmCatalog.initialState(entry)
    }
    val playerKey = remember(entry.id, sequence?.title, notation, initialState) {
        listOf(entry.id, sequence?.title ?: "resolution", notation,
            CubeFacelets.encode(initialState)).joinToString("|")
    }
    val accent = algorithmAccent(category)
    val checkpointSaver = remember {
        listSaver<CubePlaybackCheckpoint, Any>(
            save = { listOf(it.sequenceId, it.completedMoves) },
            restore = { CubePlaybackCheckpoint(it[0] as String, it[1] as Int) },
        )
    }
    var checkpoint by rememberSaveable(playerKey, stateSaver = checkpointSaver) {
        mutableStateOf(CubePlaybackCheckpoint(playerKey, 0))
    }
    val currentIndex = checkpoint.indexFor(playerKey, moves.size)
    val cubeState = remember(initialState, moves, checkpoint, playerKey) {
        checkpoint.stateFor(initialState, moves, playerKey)
    }
    var currentMove by remember(playerKey) { mutableStateOf<Move?>(null) }
    var activeStep by remember(playerKey) { mutableIntStateOf(0) }
    var generation by remember(playerKey) { mutableIntStateOf(0) }
    var isPlaying by remember(playerKey) { mutableStateOf(false) }
    var isStepping by remember(playerKey) { mutableStateOf(false) }
    var manualStepJob by remember(playerKey) { mutableStateOf<Job?>(null) }
    var speedMillis by rememberSaveable(playerKey) { mutableFloatStateOf(520f) }
    val moveProgress = remember(playerKey) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun commitStep(index: Int) {
        checkpoint = CubePlaybackCheckpoint(playerKey, index.coerceIn(0, moves.size))
    }

    fun stopPlayback() {
        generation++
        manualStepJob?.cancel()
        manualStepJob = null
        isPlaying = false
        isStepping = false
        currentMove = null
        scope.launch { moveProgress.snapTo(0f) }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, playerKey) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) stopPlayback()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isPlaying, currentIndex, moves, speedMillis) {
        if (!isPlaying) return@LaunchedEffect
        if (currentIndex >= moves.size) {
            isPlaying = false
            currentMove = null
            return@LaunchedEffect
        }

        val move = moves[currentIndex]
        activeStep = currentIndex
        if (currentMove != move) {
            currentMove = move
            moveProgress.snapTo(0f)
        }
        val moveDuration = animationDurationMillis(move, speedMillis)
        val remainingDuration = (moveDuration * (1f - moveProgress.value))
            .roundToInt()
            .coerceAtLeast(1)
        moveProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(remainingDuration, easing = LinearEasing),
        )
        commitStep(currentIndex + 1)
        currentMove = null
        moveProgress.snapTo(0f)
    }

    fun jumpToStep(targetIndex: Int) {
        val safeIndex = targetIndex.coerceIn(0, moves.size)
        stopPlayback()
        commitStep(safeIndex)
    }

    fun reset() = jumpToStep(0)

    fun animateManualStep(move: Move, moveIndex: Int, onComplete: () -> Unit) {
        isPlaying = false
        isStepping = true
        currentMove = move
        activeStep = moveIndex
        generation++
        val request = generation
        manualStepJob = scope.launch {
            try {
                moveProgress.snapTo(0f)
                moveProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = animationDurationMillis(move, speedMillis)
                            .roundToInt()
                            .coerceAtLeast(1),
                        easing = LinearEasing,
                    ),
                )
                if (generation == request) onComplete()
            } finally {
                if (generation == request) {
                    currentMove = null
                    isStepping = false
                    manualStepJob = null
                    moveProgress.snapTo(0f)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("‹ Voltar")
                    }
                },
                title = {
                    Column {
                        Text("${category.label} ${entry.number}")
                        Text(
                            sequence?.title ?: "Detalhe do algoritmo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (sequence != null) {
                Text(sequence.instructions, style = MaterialTheme.typography.bodyMedium)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (sequence == null) {
                        AlgorithmCaseDiagram(
                            entry = entry, state = cubeState, animatedMove = currentMove,
                            progress = moveProgress.value, isDetail = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        com.gabs.cubo3x3.cube.SolidCubeRenderer(
                            state = cubeState,
                            animatedMove = currentMove ?: MoveNotation.parseToken("U"),
                            progress = if (currentMove == null) 0f else moveProgress.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = accent.copy(alpha = 0.13f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Sequência",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                    if (sequence == null) {
                        Text(
                            if (usingPreferred) "Preferida · caso → etapa resolvida"
                            else "Resolução · caso → resolvido",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        if (preferredFormula != null) Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(selected = !usingPreferred,
                                onClick = { reset(); showOriginal = true },
                                label = { Text("Original") }, modifier = Modifier.testTag("formula-original"))
                            FilterChip(selected = usingPreferred,
                                onClick = { reset(); showOriginal = false },
                                label = { Text("Preferida") }, modifier = Modifier.testTag("formula-preferred"))
                        }
                    }
                    val sequenceText = buildAnnotatedString {
                        moves.forEachIndexed { index, move ->
                            if (index > 0) append(" ")
                            val style = when {
                                index < currentIndex -> SpanStyle(
                                    color = accent,
                                    fontWeight = FontWeight.Bold,
                                )
                                index == activeStep && currentMove != null -> SpanStyle(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    background = accent,
                                    fontWeight = FontWeight.Bold,
                                )
                                else -> SpanStyle()
                            }
                            withStyle(style) { append(move.symbol) }
                        }
                    }
                    Text(
                        sequenceText,
                        modifier = Modifier.testTag("case-active-formula"),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        "$currentIndex/${moves.size}",
                        modifier = Modifier.align(Alignment.End),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (sequence == null) {
                        CaseFormulaCopyButton("Copiar resolução", notation, "formula-copy-resolution")
                        if (onPreferredFormulaChange != null) {
                            TextButton(onClick = {
                                stopPlayback(); preferenceMessage = null; editFormula = true
                            }, modifier = Modifier.testTag("formula-edit")) {
                                Text(if (preferredFormula == null) "Adicionar minha fórmula" else "Editar minha fórmula")
                            }
                            if (preferredFormula != null) TextButton(onClick = {
                                stopPlayback(); preferenceMessage = null; removeFormula = true
                            }, modifier = Modifier.testTag("formula-remove")) { Text("Remover preferida") }
                            preferenceMessage?.let { Text(it) }
                        }
                        if (usingPreferred) {
                            val inverse = remember(notation) { CaseFormulaValidation.inverseNotation(notation) }
                            HorizontalDivider()
                            Text("Voltar ao caso · resultado → caso",
                                style = MaterialTheme.typography.labelLarge, color = accent)
                            Text(inverse, Modifier.testTag("formula-personal-inverse"),
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace))
                            Text("Aplique após terminar esta fórmula, na orientação em que ela terminou. " +
                                "Não é necessariamente uma preparação desde o cubo totalmente resolvido.",
                                style = MaterialTheme.typography.bodySmall)
                            CaseFormulaCopyButton("Copiar inversa pessoal", inverse, "formula-copy-inverse")
                        }
                        val preparation = remember(entry.id) {
                            AlgorithmCatalog.setupNotation(entry)
                        }
                        val context = androidx.compose.ui.platform.LocalContext.current
                        var preparationCopied by remember(entry.id) { mutableStateOf(false) }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = accent.copy(alpha = 0.25f),
                        )
                        Text(
                            "Preparação · resolvido → caso",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                        )
                        Text(
                            preparation,
                            modifier = Modifier.semantics {
                                contentDescription = "Fórmula de preparação de ${entry.id}"
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                        Text(
                            "Fórmula inversa: comece resolvido, com amarelo acima, branco abaixo, " +
                                "vermelho à frente e verde à direita. Os ajustes de orientação " +
                                "já estão incluídos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(
                                    android.content.Context.CLIPBOARD_SERVICE,
                                ) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                                    "Preparar " + entry.id, preparation,
                                ))
                                preparationCopied = true
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(if (preparationCopied) "Preparação copiada" else "Copiar preparação")
                        }
                    }
                }
            }

            if (sequence == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilterChip(
                    selected = progress.isFavorite,
                    onClick = { onFavoriteChange(entry, !progress.isFavorite) },
                    label = { Text(if (progress.isFavorite) "★ Favorito" else "☆ Favorito") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = progress.isCompleted,
                    onClick = { onCompletedChange(entry, !progress.isCompleted) },
                    label = { Text(if (progress.isCompleted) "✓ Concluído" else "Concluir") },
                    modifier = Modifier.weight(1f),
                )
            }

            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = ::reset,
                    enabled = currentIndex > 0 || currentMove != null,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Reiniciar algoritmo" },
                ) {
                    Text("↶")
                }
                OutlinedButton(
                    onClick = {
                        val targetIndex = currentIndex - 1
                        val reverseMove = moves[targetIndex].copy(
                            quarterTurns = -moves[targetIndex].quarterTurns,
                        )
                        animateManualStep(reverseMove, targetIndex) {
                            commitStep(targetIndex)
                        }
                    },
                    enabled = !isPlaying && !isStepping && currentMove == null && currentIndex > 0,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Movimento anterior" },
                ) {
                    Text("−")
                }
                Button(
                    onClick = {
                        val move = moves[currentIndex]
                        animateManualStep(move, currentIndex) {
                            commitStep(currentIndex + 1)
                        }
                    },
                    enabled = !isPlaying && !isStepping && currentMove == null &&
                        currentIndex < moves.size,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Próximo movimento" },
                ) {
                    Text("+")
                }
                Button(
                    onClick = {
                        if (currentIndex >= moves.size) {
                            commitStep(0)
                            currentMove = null
                            scope.launch { moveProgress.snapTo(0f) }
                        }
                        isPlaying = !isPlaying
                    },
                    enabled = moves.isNotEmpty() && !isStepping,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = if (isPlaying) {
                                "Pausar algoritmo"
                            } else {
                                "Reproduzir algoritmo"
                            }
                        },
                ) {
                    Text(if (isPlaying) "Ⅱ" else "▶")
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Velocidade", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${speedMillis.roundToInt()} ms",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Slider(
                    value = speedMillis,
                    onValueChange = { speedMillis = it },
                    valueRange = 180f..900f,
                )
            }

            Text(
                text = when {
                    currentIndex >= moves.size -> sequence?.completedText ?:
                        if (usingPreferred) "Concluído · caso resolvido" else "Concluído · cubo resolvido"
                    currentMove != null -> "Movimento ${activeStep + 1} de ${moves.size}: ${currentMove?.symbol}"
                    else -> "Passo $currentIndex de ${moves.size}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (onPrepare != null) {
                Button(onClick = onPrepare, modifier = Modifier.fillMaxWidth()) {
                    Text("Preparar este caso")
                }
            }
            if (sequence != null) {
                val context = androidx.compose.ui.platform.LocalContext.current
                var copied by remember(playerKey) { mutableStateOf(false) }
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                        "Preparar " + entry.id, sequence.notation,
                    ))
                    copied = true
                }, enabled = sequence.notation.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(if (copied) "Sequência copiada" else "Copiar sequência")
                }
                if (onConfigureCube != null) {
                    Button(onClick = onConfigureCube, modifier = Modifier.fillMaxWidth()) {
                        Text("Configurar meu cubo")
                    }
                }
                if (onReturnToCase != null) {
                    OutlinedButton(onClick = onReturnToCase, modifier = Modifier.fillMaxWidth()) {
                        Text("Voltar à fórmula do caso")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
    val updatePreference = onPreferredFormulaChange
    if (sequence == null && editFormula && updatePreference != null) {
        PreferredFormulaDialog(entry, preferredFormula?.notation,
            onSave = { value -> updatePreference(entry, value); reset(); showOriginal = false },
            onDismiss = { editFormula = false })
    }
    if (sequence == null && removeFormula && updatePreference != null) AlertDialog(
        onDismissRequest = { if (!changingPreference) removeFormula = false },
        title = { Text("Remover a fórmula preferida?") },
        text = { Text(preferenceMessage ?: "A original voltará a ser o padrão. Seus tempos e marcações serão mantidos.") },
        confirmButton = {
            TextButton(onClick = {
                changingPreference = true
                scope.launch {
                    try {
                        updatePreference(entry, null)
                        reset()
                        showOriginal = false
                        removeFormula = false
                    } catch (error: kotlinx.coroutines.CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        preferenceMessage = error.message ?: "Não foi possível remover."
                    } finally { changingPreference = false }
                }
            }, enabled = !changingPreference, modifier = Modifier.testTag("formula-confirm-remove")) {
                Text(if (changingPreference) "Removendo…" else "Remover")
            }
        },
        dismissButton = { TextButton(onClick = { removeFormula = false },
            enabled = !changingPreference) { Text("Cancelar") } },
    )
}

@Composable
private fun CaseFormulaCopyButton(label: String, notation: String, tag: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var copied by remember(notation, label) { mutableStateOf(false) }
    TextButton(onClick = {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, notation))
        copied = true
    }, modifier = Modifier.testTag(tag)) { Text(if (copied) "Copiado" else label) }
}

@Composable
private fun algorithmAccent(category: AlgorithmCategory): Color {
    val dark = MaterialTheme.colorScheme.background.red < 0.2f
    return when (category) {
        AlgorithmCategory.F2L -> if (dark) F2LDark else F2LLight
        AlgorithmCategory.OLL -> if (dark) OLLDark else OLLLight
        AlgorithmCategory.PLL -> if (dark) PLLDark else PLLLight
    }
}

private fun animationDurationMillis(move: Move, speedMillis: Float): Float =
    speedMillis * if (move.repetitions == 2) 1.5f else 1f
