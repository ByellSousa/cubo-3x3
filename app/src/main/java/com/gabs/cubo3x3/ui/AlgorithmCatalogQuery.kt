package com.gabs.cubo3x3.ui

import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import java.text.Normalizer
import java.util.Locale

enum class AlgorithmStudyFilter(val label: String) {
    ALL("Todos"),
    FAVORITES("Favoritos"),
    COMPLETED("Concluídos"),
    NOT_COMPLETED("Pendentes"),
}

/** Read-only intersection: keep the catalog order and never rewrite the original notation. */
object AlgorithmCatalogQuery {
    fun filter(
        entries: List<AlgorithmEntry>,
        progress: Map<String, AlgorithmProgress>,
        query: String = "",
        studyFilter: AlgorithmStudyFilter = AlgorithmStudyFilter.ALL,
        category: AlgorithmCategory? = null,
    ): List<AlgorithmEntry> {
        val text = query.replace('′', '\'').replace('’', '\'').trim()
            .replace(Regex("\\s+"), " ")
        val metadata = normalize(text)
        val numbered = Regex("^(?:(f2l|oll|pll)[ -]*)?(?:caso[ -]*)?#?(\\d+)$")
            .matchEntire(metadata)
        val notationTokens = text.split(" ").takeIf { tokens ->
            text.isNotEmpty() && tokens.all { token ->
                runCatching { MoveNotation.parseToken(token) }.isSuccess
            }
        }
        return entries.filter { entry ->
            val state = progress[entry.id] ?: AlgorithmProgress()
            (category == null || entry.category == category) &&
                when (studyFilter) {
                    AlgorithmStudyFilter.ALL -> true
                    AlgorithmStudyFilter.FAVORITES -> state.isFavorite
                    AlgorithmStudyFilter.COMPLETED -> state.isCompleted
                    AlgorithmStudyFilter.NOT_COMPLETED -> !state.isCompleted
                } && when {
                    text.isEmpty() -> true
                    numbered != null -> {
                        val requestedCategory = numbered.groupValues[1]
                        entry.number == numbered.groupValues[2].toIntOrNull() &&
                            (requestedCategory.isEmpty() ||
                                normalize(entry.category.label) == requestedCategory)
                    }
                    notationTokens != null -> entry.notation.trim().split(Regex("\\s+"))
                        .windowed(notationTokens.size).any { it == notationTokens }
                    else -> normalize(
                        "${entry.category.label} caso ${entry.number} ${entry.category.description}",
                    ).contains(metadata)
                }
        }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
}
