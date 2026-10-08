package com.gabs.cubo3x3.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.Move
import com.gabs.cubo3x3.cube.SolidCubeRenderer

/**
 * Repete um unico movimento usando o mesmo cubo do prototipo. Cada ciclo
 * comeca resolvido, executa o giro, segura brevemente o resultado e reinicia.
 */
@Composable
internal fun QuizMovePreview(
    move: Move,
    modifier: Modifier = Modifier,
) {
    val motionDurationMillis = if (move.repetitions == 2) 900 else 650
    val startHoldMillis = 220
    val endHoldMillis = 180
    val totalDurationMillis = startHoldMillis + motionDurationMillis + endHoldMillis
    val transition = rememberInfiniteTransition(label = "demonstracao do quiz")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalDurationMillis
                0f at 0
                0f at startHoldMillis
                1f at (startHoldMillis + motionDurationMillis) using LinearEasing
                1f at totalDurationMillis
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "progresso do movimento do quiz",
    )

    Box(
        modifier = modifier.semantics {
            contentDescription = if (move.repetitions == 2) {
                "Demonstração animada de movimento duplo, repetida continuamente"
            } else {
                "Demonstração animada do movimento, repetida continuamente"
            }
        },
    ) {
        SolidCubeRenderer(
            state = CubeState.solved(),
            animatedMove = move,
            progress = progress,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        )
        if (move.repetitions == 2) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Text(
                    text = "2×",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
