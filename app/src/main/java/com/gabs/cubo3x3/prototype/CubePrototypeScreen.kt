package com.gabs.cubo3x3.prototype

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.cube.CubeRenderer
import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.Move
import com.gabs.cubo3x3.cube.MoveNotation
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CubePrototypeScreen() {
    var notation by rememberSaveable { mutableStateOf("R U R' U'") }
    var cubeState by remember { mutableStateOf(CubeState.solved()) }
    var currentMove by remember { mutableStateOf<Move?>(null) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var speedMillis by rememberSaveable { mutableFloatStateOf(520f) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val parseResult = remember(notation) {
        runCatching { MoveNotation.parseAlgorithm(notation) }
    }
    val moves = parseResult.getOrDefault(emptyList())
    val parseError = parseResult.exceptionOrNull()?.message

    LaunchedEffect(isPlaying, currentIndex, moves, speedMillis) {
        if (!isPlaying) return@LaunchedEffect
        if (currentIndex >= moves.size) {
            isPlaying = false
            currentMove = null
            return@LaunchedEffect
        }

        val move = moves[currentIndex]
        if (currentMove != move) {
            currentMove = move
            progress.snapTo(0f)
        }

        val remainingDuration = (speedMillis * (1f - progress.value))
            .roundToInt()
            .coerceAtLeast(1)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(remainingDuration, easing = LinearEasing),
        )
        cubeState = cubeState.apply(move)
        currentMove = null
        progress.snapTo(0f)
        currentIndex += 1
    }

    fun reset() {
        isPlaying = false
        currentMove = null
        currentIndex = 0
        cubeState = CubeState.solved()
        scope.launch { progress.snapTo(0f) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Protótipo do cubo")
                        Text(
                            text = "Canvas nativo · 54 movimentos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CubeRenderer(
                        state = cubeState,
                        animatedMove = currentMove,
                        progress = progress.value,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            OutlinedTextField(
                value = notation,
                onValueChange = { value ->
                    notation = value
                    reset()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Sequência") },
                supportingText = {
                    Text(parseError ?: "Ex.: R U R' U' · também aceita Rw, M, x, y e z")
                },
                isError = parseError != null,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        if (currentIndex >= moves.size) {
                            cubeState = CubeState.solved()
                            currentIndex = 0
                            currentMove = null
                            scope.launch { progress.snapTo(0f) }
                        }
                        isPlaying = true
                    },
                    enabled = moves.isNotEmpty() && !isPlaying,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (currentMove == null) "Reproduzir" else "Continuar")
                }
                OutlinedButton(
                    onClick = { isPlaying = false },
                    enabled = isPlaying,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Pausar")
                }
                OutlinedButton(
                    onClick = ::reset,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reiniciar")
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Velocidade", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = speedMillis.roundToInt().toString() + " ms",
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Teste rápido", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("R", "U", "F", "L", "D", "B").forEach { token ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (!isPlaying) {
                                    cubeState = cubeState.apply(MoveNotation.parseToken(token))
                                }
                            },
                            label = {
                                Text(token, fontFamily = FontFamily.Monospace)
                            },
                            modifier = Modifier.size(width = 48.dp, height = 44.dp),
                        )
                    }
                }
            }

            Text(
                text = if (moves.isEmpty()) {
                    "Nenhum movimento válido."
                } else {
                    "Passo " + currentIndex.coerceAtMost(moves.size) + " de " + moves.size +
                        if (cubeState.isSolved()) " · cubo resolvido" else " · cubo embaralhado"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
