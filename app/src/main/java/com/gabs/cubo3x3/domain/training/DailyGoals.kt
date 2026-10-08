package com.gabs.cubo3x3.domain.training

import com.gabs.cubo3x3.data.timer.SolveTime
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyGoalSettings(
    val enabled: Boolean = false,
    val recognitionTarget: Int = 5,
    val executionTarget: Int = 5,
) {
    init {
        require(recognitionTarget in 0..100 && executionTarget in 0..100)
        require(!enabled || recognitionTarget > 0 || executionTarget > 0)
    }
}

object DailyGoalSettingsCodec {
    fun encode(value: DailyGoalSettings): String =
        "1|${if (value.enabled) 1 else 0}|${value.recognitionTarget}|${value.executionTarget}"

    fun decode(value: String): DailyGoalSettings {
        require(value.length <= 40)
        val parts = value.split("|")
        require(parts.size == 4 && parts[0] == "1" && parts[1] in setOf("0", "1"))
        val result = DailyGoalSettings(parts[1] == "1", parts[2].toInt(), parts[3].toInt())
        require(encode(result) == value)
        return result
    }
}

data class DailyGoalProgress(
    val date: LocalDate,
    val recognitionCount: Int,
    val executionCount: Int,
) {
    fun reached(settings: DailyGoalSettings): Boolean = settings.enabled &&
        (settings.recognitionTarget == 0 || recognitionCount >= settings.recognitionTarget) &&
        (settings.executionTarget == 0 || executionCount >= settings.executionTarget)
}

object DailyGoals {
    fun progress(attempts: List<CfopAttempt>, times: List<SolveTime>,
        nowEpochMillis: Long, zone: ZoneId): DailyGoalProgress {
        require(nowEpochMillis >= 0)
        val date = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()
        fun today(at: Long): Boolean = at in 0..nowEpochMillis &&
            Instant.ofEpochMilli(at).atZone(zone).toLocalDate() == date
        attempts.forEach { it.validate() }
        val recognition = attempts.distinctBy { it.id }.count { today(it.recordedAtEpochMillis) }
        val limits = mapOf("F2L" to 41, "OLL" to 57, "PLL" to 21)
        val execution = times.distinctBy { it.id }.count {
            val maximum = limits[it.trainingCategory]
            it.id > 0 && it.durationMillis >= 0 && maximum != null &&
                it.trainingCaseNumber != null && it.trainingCaseNumber in 1..maximum &&
                today(it.recordedAtEpochMillis)
        }
        return DailyGoalProgress(date, recognition, execution)
    }
}
