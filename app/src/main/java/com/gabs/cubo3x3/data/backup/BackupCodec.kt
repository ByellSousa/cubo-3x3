package com.gabs.cubo3x3.data.backup

import com.gabs.cubo3x3.cube.CubeColorScheme
import com.gabs.cubo3x3.cube.CubeViewpoint
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.domain.quiz.QuizLevel
import com.gabs.cubo3x3.domain.quiz.QuizMode
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.domain.algorithm.PreferredCaseFormula
import com.gabs.cubo3x3.domain.algorithm.CaseFormulaValidation
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlanCodec
import com.gabs.cubo3x3.domain.training.DailyGoalSettings
import com.gabs.cubo3x3.domain.training.DailyGoalSettingsCodec
import com.gabs.cubo3x3.domain.reminder.ReminderDay
import com.gabs.cubo3x3.domain.reminder.ReminderSettings
import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.DEFAULT_TIMER_SESSION_ID
import com.gabs.cubo3x3.data.timer.DEFAULT_TIMER_SESSION_NAME
import com.gabs.cubo3x3.data.timer.TimerSessionRepository
import com.gabs.cubo3x3.ui.theme.ThemeMode
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException

object BackupCodec {
    const val FORMAT_ID = "com.gabs.cubo3x3.backup"
    const val SCHEMA_VERSION = 9
    const val MAX_BACKUP_BYTES = 5 * 1024 * 1024

    private const val MAX_PROGRESS = 119
    private const val MAX_SOLVE_TIMES = 100_000
    private const val MAX_TIMER_SESSIONS = 1_000
    private const val MAX_QUIZ_RECORDS = 6
    private const val MAX_CUSTOM_ALGORITHMS = 10_000
    private const val MAX_TEXT_LENGTH = 20_000

    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    fun encode(backup: BackupPackage): String = gson.toJson(backup.toDocument())

    fun decode(json: String): BackupPackage {
        val document = try {
            gson.fromJson(json, BackupDocument::class.java)
        } catch (error: JsonParseException) {
            throw BackupException.Invalid(cause = error)
        } catch (error: RuntimeException) {
            throw BackupException.Invalid(cause = error)
        } ?: throw BackupException.Invalid()

        if (document.format != FORMAT_ID) throw BackupException.Invalid()
        if (document.schemaVersion !in 1..SCHEMA_VERSION) {
            throw BackupException.Incompatible(document.schemaVersion)
        }
        return document.validate(document.schemaVersion ?: 0)
    }

    private fun BackupDocument.validate(schemaVersion: Int): BackupPackage {
        val validAppVersion = appVersion.requiredText("versão do aplicativo", 100)
        val exportedAt = exportedAtEpochMillis.requireNonNegative("data de exportação")
        val theme = enumValue<ThemeMode>(settings?.themeMode, "tema")
        val reminder = if (schemaVersion == 1) {
            ReminderSettings()
        } else {
            val enabled = settings?.reminderEnabled ?: invalid("Ativação do lembrete ausente")
            val hour = settings.reminderHour ?: invalid("Hora do lembrete ausente")
            val minute = settings.reminderMinute ?: invalid("Minuto do lembrete ausente")
            if (hour !in 0..23 || minute !in 0..59) invalid("Horário do lembrete inválido")
            val reminderDays = settings.reminderDays.requiredList(
                "dias do lembrete",
                ReminderDay.entries.size,
            )
            val days = reminderDays
                .map { enumValue<ReminderDay>(it, "dia do lembrete") }
                .toSet()
            if (days.size != reminderDays.size) invalid("Há duplicatas nos dias do lembrete")
            if (enabled && days.isEmpty()) invalid("Lembrete ativo sem dias selecionados")
            ReminderSettings(enabled, hour, minute, days)
        }
        val trainingPlan = if (schemaVersion >= 7) {
            try {
                CfopTrainingPlanCodec.decode(settings?.cfopTrainingPlan.requiredText("plano CFOP", 2_000))
            } catch (error: IllegalArgumentException) {
                throw BackupException.Invalid("Plano de treino CFOP inválido.", error)
            }
        } else CfopTrainingPlan()
        val goals = if (schemaVersion >= 9) {
            try {
                DailyGoalSettingsCodec.decode(settings?.dailyGoals.requiredText("metas diárias", 40))
            } catch (error: IllegalArgumentException) {
                throw BackupException.Invalid("Metas diárias inválidas.", error)
            }
        } else DailyGoalSettings()
        val progressRows = progress.requiredList("progresso", MAX_PROGRESS).mapIndexed { index, row ->
            val value = row ?: invalid("Registro de progresso $index ausente")
            val category = value.category.requiredText("categoria do progresso", 10)
            val maxCase = when (category) {
                "F2L" -> 41
                "OLL" -> 57
                "PLL" -> 21
                else -> invalid("Categoria de progresso desconhecida: $category")
            }
            val caseNumber = value.caseNumber ?: invalid("Caso do progresso ausente")
            if (caseNumber !in 1..maxCase) invalid("Caso $category $caseNumber inválido")
            val favorite = value.isFavorite ?: invalid("Favorito do progresso ausente")
            val completed = value.isCompleted ?: invalid("Conclusão do progresso ausente")
            if (!favorite && !completed) invalid("Progresso vazio para $category $caseNumber")
            BackupProgress(
                category = category,
                caseNumber = caseNumber,
                isFavorite = favorite,
                isCompleted = completed,
                updatedAtEpochMillis = value.updatedAtEpochMillis.requireNonNegative("data do progresso"),
            )
        }
        ensureUnique(progressRows.map { "${it.category}-${it.caseNumber}" }, "progresso")

        val sessions = if (schemaVersion >= 4) {
            timerSessions.requiredList("sessões do cronômetro", MAX_TIMER_SESSIONS)
                .mapIndexed { index, row ->
                    val value = row ?: invalid("Sessão do cronômetro $index ausente")
                    BackupTimerSession(
                        id = value.id.requirePositive("identificador da sessão"),
                        name = value.name
                            .requiredText("nome da sessão", TimerSessionRepository.MAX_NAME_LENGTH)
                            .trim(),
                        createdAtEpochMillis = value.createdAtEpochMillis
                            .requireNonNegative("data da sessão"),
                    )
                }
                .also { values ->
                    if (values.isEmpty()) invalid("Backup sem sessão do cronômetro")
                    ensureUnique(values.map(BackupTimerSession::id), "sessões do cronômetro")
                    ensureUnique(values.map { it.name.lowercase() }, "nomes das sessões")
                }
        } else {
            listOf(BackupTimerSession(DEFAULT_TIMER_SESSION_ID, DEFAULT_TIMER_SESSION_NAME, 0L))
        }
        val sessionIds = sessions.map(BackupTimerSession::id).toSet()

        val times = solveTimes.requiredList("tempos", MAX_SOLVE_TIMES).mapIndexed { index, row ->
            val value = row ?: invalid("Tempo $index ausente")
            val scramble = if (schemaVersion >= 3) {
                value.scramble.requireBoundedText("embaralhamento do tempo", 500)
            } else {
                ""
            }
            val comment = if (schemaVersion >= 3) {
                value.comment.requireBoundedText("comentário do tempo", 2_000)
            } else {
                ""
            }
            val penalty = if (schemaVersion >= 3) {
                enumValue<SolvePenalty>(value.penalty, "penalidade do tempo").name
            } else {
                SolvePenalty.NONE.name
            }
            val sessionId = if (schemaVersion >= 4) {
                value.sessionId.requirePositive("sessão do tempo")
            } else {
                DEFAULT_TIMER_SESSION_ID
            }
            if (sessionId !in sessionIds) invalid("Tempo associado a uma sessão inexistente")
            val trainingCategory = if (schemaVersion >= 5) value.trainingCategory else null
            val trainingCaseNumber = if (schemaVersion >= 5) value.trainingCaseNumber else null
            if ((trainingCategory == null) != (trainingCaseNumber == null)) {
                invalid("Vínculo de treino CFOP incompleto")
            }
            if (trainingCategory != null && trainingCaseNumber != null) {
                val maximum = when (trainingCategory) {
                    "F2L" -> 41
                    "OLL" -> 57
                    "PLL" -> 21
                    else -> invalid("Categoria de treino desconhecida: $trainingCategory")
                }
                if (trainingCaseNumber !in 1..maximum) {
                    invalid("Caso de treino $trainingCategory $trainingCaseNumber inválido")
                }
            }
            BackupSolveTime(
                id = value.id.requirePositive("identificador do tempo"),
                durationMillis = value.durationMillis.requireNonNegative("duração do tempo"),
                recordedAtEpochMillis = value.recordedAtEpochMillis.requireNonNegative("data do tempo"),
                scramble = scramble,
                comment = comment,
                penalty = penalty,
                sessionId = sessionId,
                trainingCategory = trainingCategory,
                trainingCaseNumber = trainingCaseNumber,
            )
        }
        ensureUnique(times.map(BackupSolveTime::id), "tempos")

        val records = quizRecords.requiredList("recordes do Quiz", MAX_QUIZ_RECORDS).mapIndexed { index, row ->
            val value = row ?: invalid("Recorde do Quiz $index ausente")
            val level = enumValue<QuizLevel>(value.level, "nível do Quiz").name
            val mode = enumValue<QuizMode>(value.mode, "modo do Quiz").name
            val accuracy = value.bestAccuracyPercent.requireNonNegative("precisão do Quiz")
            if (accuracy > 100) invalid("Precisão do Quiz fora do intervalo")
            BackupQuizRecord(
                level = level,
                mode = mode,
                bestScore = value.bestScore.requireNonNegative("pontuação do Quiz"),
                bestStreak = value.bestStreak.requireNonNegative("sequência do Quiz"),
                bestAccuracyPercent = accuracy,
                gamesPlayed = value.gamesPlayed.requireNonNegative("partidas do Quiz"),
                updatedAtEpochMillis = value.updatedAtEpochMillis.requireNonNegative("data do Quiz"),
            )
        }
        ensureUnique(records.map { "${it.level}-${it.mode}" }, "recordes do Quiz")

        val custom = customAlgorithms.requiredList(
            "algoritmos personalizados",
            MAX_CUSTOM_ALGORITHMS,
        ).mapIndexed { index, row ->
            val value = row ?: invalid("Algoritmo personalizado $index ausente")
            val name = value.name.requiredText("nome do algoritmo", 200).trim()
            val notation = value.notation.requiredText("notação do algoritmo", MAX_TEXT_LENGTH)
            val moves = try {
                MoveNotation.parseAlgorithm(notation)
            } catch (error: IllegalArgumentException) {
                throw BackupException.Invalid("Notação inválida em $name.", error)
            }
            if (moves.isEmpty()) invalid("Algoritmo sem movimentos: $name")
            val tags = value.tags.requiredList("tags", 100)
                .map { tag ->
                    tag.requiredText("tag", 100).trim().also { normalized ->
                        if ('\u001F' in normalized) invalid("Tag contém caractere inválido")
                    }
                }
                .distinctBy(String::lowercase)
            val createdAt = value.createdAtEpochMillis.requireNonNegative("data de criação")
            val updatedAt = value.updatedAtEpochMillis.requireNonNegative("data de atualização")
            if (updatedAt < createdAt) invalid("Datas inválidas no algoritmo $name")
            BackupCustomAlgorithm(
                id = value.id.requirePositive("identificador do algoritmo"),
                name = name,
                notation = moves.joinToString(" ") { it.symbol },
                tags = tags,
                colorScheme = enumValue(value.colorScheme, "esquema de cores"),
                viewpoint = enumValue(value.viewpoint, "ponto de vista"),
                createdAtEpochMillis = createdAt,
                updatedAtEpochMillis = updatedAt,
            )
        }
        ensureUnique(custom.map(BackupCustomAlgorithm::id), "algoritmos personalizados")

        val attempts = if (schemaVersion >= 6) {
            cfopAttempts.requiredList("respostas CFOP", 100_000).map { row ->
                val value = row ?: invalid("Resposta CFOP ausente")
                val attempt = CfopAttempt(
                    value.id.requiredText("identificador CFOP", 100),
                    value.category.requiredText("categoria CFOP", 10),
                    value.caseNumber ?: invalid("Caso CFOP ausente"),
                    value.selectedCaseNumber ?: invalid("Resposta CFOP ausente"),
                    value.recordedAtEpochMillis.requireNonNegative("data CFOP"),
                )
                try { attempt.validate() } catch (error: IllegalArgumentException) {
                    throw BackupException.Invalid("Resposta CFOP inválida.", error)
                }
                attempt
            }.also { ensureUnique(it.map(CfopAttempt::id), "respostas CFOP") }
        } else emptyList()

        val formulas = if (schemaVersion >= 8) {
            preferredFormulas.requiredList("fórmulas preferidas", 119).map { row ->
                val value = row ?: invalid("Fórmula preferida ausente")
                try {
                    PreferredCaseFormula(
                        value.category.requiredText("categoria da fórmula", 10),
                        value.caseNumber ?: invalid("Caso da fórmula ausente"),
                        value.notation.requiredText("fórmula preferida", CaseFormulaValidation.MAX_TEXT_LENGTH),
                        value.updatedAtEpochMillis.requireNonNegative("data da fórmula"),
                    ).validated()
                } catch (error: IllegalArgumentException) {
                    throw BackupException.Invalid("Fórmula preferida inválida: ${error.message}", error)
                }
            }.also { ensureUnique(it.map(PreferredCaseFormula::caseId), "fórmulas preferidas") }
        } else emptyList()

        return BackupPackage(
            appVersion = validAppVersion,
            exportedAtEpochMillis = exportedAt,
            themeMode = theme,
            reminderSettings = reminder,
            progress = progressRows,
            timerSessions = sessions,
            solveTimes = times,
            quizRecords = records,
            customAlgorithms = custom,
            cfopAttempts = attempts,
            cfopTrainingPlan = trainingPlan,
            preferredFormulas = formulas,
            dailyGoals = goals,
        )
    }

    private fun BackupPackage.toDocument() = BackupDocument(
        format = FORMAT_ID,
        schemaVersion = SCHEMA_VERSION,
        appVersion = appVersion,
        exportedAtEpochMillis = exportedAtEpochMillis,
        cfopAttempts = cfopAttempts.map {
            BackupCfopAttemptDocument(it.id, it.category, it.caseNumber,
                it.selectedCaseNumber, it.recordedAtEpochMillis)
        },
        preferredFormulas = preferredFormulas.map {
            BackupPreferredFormulaDocument(it.category, it.caseNumber, it.notation, it.updatedAtEpochMillis)
        },
        settings = BackupSettingsDocument(
            cfopTrainingPlan = CfopTrainingPlanCodec.encode(cfopTrainingPlan),
            dailyGoals = DailyGoalSettingsCodec.encode(dailyGoals),
            themeMode = themeMode.storageValue,
            reminderEnabled = reminderSettings.enabled,
            reminderHour = reminderSettings.hour,
            reminderMinute = reminderSettings.minute,
            reminderDays = ReminderDay.entries
                .filter(reminderSettings.days::contains)
                .map(ReminderDay::name),
        ),
        progress = progress.map {
            BackupProgressDocument(
                it.category, it.caseNumber, it.isFavorite, it.isCompleted, it.updatedAtEpochMillis,
            )
        },
        timerSessions = timerSessions.map {
            BackupTimerSessionDocument(it.id, it.name, it.createdAtEpochMillis)
        },
        solveTimes = solveTimes.map {
            BackupSolveTimeDocument(
                it.id,
                it.durationMillis,
                it.recordedAtEpochMillis,
                it.scramble,
                it.comment,
                it.penalty,
                it.sessionId,
                it.trainingCategory,
                it.trainingCaseNumber,
            )
        },
        quizRecords = quizRecords.map {
            BackupQuizRecordDocument(
                it.level,
                it.mode,
                it.bestScore,
                it.bestStreak,
                it.bestAccuracyPercent,
                it.gamesPlayed,
                it.updatedAtEpochMillis,
            )
        },
        customAlgorithms = customAlgorithms.map {
            BackupCustomAlgorithmDocument(
                it.id,
                it.name,
                it.notation,
                it.tags,
                it.colorScheme.name,
                it.viewpoint.name,
                it.createdAtEpochMillis,
                it.updatedAtEpochMillis,
            )
        },
    )

    private fun invalid(message: String): Nothing = throw BackupException.Invalid(message)

    private fun String?.requiredText(field: String, maxLength: Int): String {
        if (this == null || isBlank() || length > maxLength) invalid("Campo $field inválido")
        return this
    }

    private fun String?.requireBoundedText(field: String, maxLength: Int): String {
        if (this == null || length > maxLength) invalid("Campo $field inválido")
        return this
    }

    private fun Long?.requireNonNegative(field: String): Long {
        if (this == null || this < 0) invalid("Campo $field inválido")
        return this
    }

    private fun Int?.requireNonNegative(field: String): Int {
        if (this == null || this < 0) invalid("Campo $field inválido")
        return this
    }

    private fun Long?.requirePositive(field: String): Long {
        if (this == null || this <= 0) invalid("Campo $field inválido")
        return this
    }

    private fun <T> List<T>?.requiredList(field: String, maximum: Int): List<T> {
        if (this == null || size > maximum) invalid("Coleção $field inválida")
        return this
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String?, field: String): T {
        return enumValues<T>().firstOrNull { candidate ->
            candidate.name == value || (candidate is ThemeMode && candidate.storageValue == value)
        } ?: invalid("Campo $field inválido")
    }

    private fun <T> ensureUnique(values: List<T>, field: String) {
        if (values.distinct().size != values.size) invalid("Há duplicatas em $field")
    }
}

private data class BackupDocument(
    val format: String? = null,
    val schemaVersion: Int? = null,
    val appVersion: String? = null,
    val exportedAtEpochMillis: Long? = null,
    val settings: BackupSettingsDocument? = null,
    val progress: List<BackupProgressDocument?>? = null,
    val timerSessions: List<BackupTimerSessionDocument?>? = null,
    val solveTimes: List<BackupSolveTimeDocument?>? = null,
    val quizRecords: List<BackupQuizRecordDocument?>? = null,
    val customAlgorithms: List<BackupCustomAlgorithmDocument?>? = null,
    val cfopAttempts: List<BackupCfopAttemptDocument?>? = null,
    val preferredFormulas: List<BackupPreferredFormulaDocument?>? = null,
)

private data class BackupPreferredFormulaDocument(
    val category: String? = null,
    val caseNumber: Int? = null,
    val notation: String? = null,
    val updatedAtEpochMillis: Long? = null,
)

private data class BackupCfopAttemptDocument(
    val id: String? = null,
    val category: String? = null,
    val caseNumber: Int? = null,
    val selectedCaseNumber: Int? = null,
    val recordedAtEpochMillis: Long? = null,
)

private data class BackupTimerSessionDocument(
    val id: Long? = null,
    val name: String? = null,
    val createdAtEpochMillis: Long? = null,
)

private data class BackupSettingsDocument(
    val dailyGoals: String? = null,
    val cfopTrainingPlan: String? = null,
    val themeMode: String? = null,
    val reminderEnabled: Boolean? = null,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val reminderDays: List<String?>? = null,
)

private data class BackupProgressDocument(
    val category: String? = null,
    val caseNumber: Int? = null,
    val isFavorite: Boolean? = null,
    val isCompleted: Boolean? = null,
    val updatedAtEpochMillis: Long? = null,
)

private data class BackupSolveTimeDocument(
    val id: Long? = null,
    val durationMillis: Long? = null,
    val recordedAtEpochMillis: Long? = null,
    val scramble: String? = null,
    val comment: String? = null,
    val penalty: String? = null,
    val sessionId: Long? = null,
    val trainingCategory: String? = null,
    val trainingCaseNumber: Int? = null,
)

private data class BackupQuizRecordDocument(
    val level: String? = null,
    val mode: String? = null,
    val bestScore: Int? = null,
    val bestStreak: Int? = null,
    val bestAccuracyPercent: Int? = null,
    val gamesPlayed: Int? = null,
    val updatedAtEpochMillis: Long? = null,
)

private data class BackupCustomAlgorithmDocument(
    val id: Long? = null,
    val name: String? = null,
    val notation: String? = null,
    val tags: List<String?>? = null,
    val colorScheme: String? = null,
    val viewpoint: String? = null,
    val createdAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long? = null,
)
