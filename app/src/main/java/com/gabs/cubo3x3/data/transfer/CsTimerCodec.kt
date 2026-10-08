package com.gabs.cubo3x3.data.transfer

import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTimeRepository
import com.gabs.cubo3x3.data.timer.TimerSessionRepository
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/** Independent adapter for the public, uncompressed csTimer session JSON format. */
object CsTimerCodec {
    const val MAX_BYTES = 5 * 1024 * 1024
    const val MAX_SESSIONS = 1_000
    const val MAX_SOLVES = 100_000
    private const val MAX_EPOCH_MILLIS = 253_402_300_799_000L
    private val sessionKey = Regex("session([1-9][0-9]*)")

    fun decode(json: String): TimerTransferPreview {
        if (json.toByteArray(Charsets.UTF_8).size > MAX_BYTES) invalid("O arquivo excede 5 MB.")
        val root = parse(json).objectValue("arquivo")
        val keys = root.keySet().filter(sessionKey::matches)
            .sortedBy { it.removePrefix("session").toIntOrNull() ?: invalid("Sessão inválida.") }
        if (keys.isEmpty()) invalid("Não há sessões no JSON do csTimer.")
        if (keys.size > MAX_SESSIONS) invalid("O arquivo excede 1.000 sessões.")
        val properties = root["properties"]?.let { unwrap(it).objectValue("preferências") }
        val metadata = properties?.get("sessionData")?.let { unwrap(it).objectValue("sessões") }
        val sessions = mutableListOf<TransferredSession>()
        val warnings = linkedSetOf<String>()
        var total = 0
        var skippedSessions = 0
        var skippedSolves = 0
        for (key in keys) {
            val number = key.removePrefix("session")
            val rows = unwrap(root[key]).arrayValue(key)
            total += rows.size()
            if (total > MAX_SOLVES) invalid("O arquivo excede 100.000 tempos.")
            val detail = metadata?.get(number)?.objectValue("sessão $number")
            val options = detail?.get("opt")?.objectValue("opções da sessão $number")
            val type = (options?.get("scrType") ?: detail?.get("scr"))
                ?.textValue("tipo da sessão $number") ?: "333"
            if (type !in setOf("333", "333o")) {
                skippedSessions++
                skippedSolves += rows.size()
                continue
            }
            val originalName = detail?.get("name")?.let {
                if (it.isJsonPrimitive) it.asString else invalid("Nome da sessão $number inválido.")
            } ?: "csTimer $number"
            val decodedName = decodeName(originalName).trim().ifBlank { "csTimer $number" }
            val name = decodedName.take(TimerSessionRepository.MAX_NAME_LENGTH)
            if (name != decodedName) warnings += "Nomes longos serão limitados a 50 caracteres."
            val solves = rows.mapIndexedNotNull { index, element ->
                val context = "$key, tempo ${index + 1}"
                val row = element.arrayValue(context)
                if (row.size() !in 3..5) invalid("Estrutura inválida em $context.")
                val extension = if (row.size() == 5) row[4] else JsonNull.INSTANCE
                if (extension.isJsonArray && extension.asJsonArray.size() > 1 &&
                    extension.asJsonArray[1].isJsonPrimitive && extension.asJsonArray[1].asJsonPrimitive.isString &&
                    extension.asJsonArray[1].asString != "333"
                ) {
                    skippedSolves++
                    warnings += "Tempos identificados como outro puzzle na reconstrução ficam de fora."
                    return@mapIndexedNotNull null
                }
                val timing = row[0].arrayValue("duração em $context")
                if (timing.size() !in 2..20) invalid("Duração inválida em $context.")
                val penalty = when (timing[0].integerValue("penalidade em $context")) {
                    0L -> SolvePenalty.NONE
                    2_000L -> SolvePenalty.PLUS_TWO
                    -1L -> SolvePenalty.DNF
                    else -> invalid("Penalidade desconhecida em $context.")
                }
                val duration = timing[1].integerValue("duração em $context")
                if (duration !in 0..MAX_EPOCH_MILLIS) invalid("Duração inválida em $context.")
                if (timing.size() > 2) {
                    var previous = duration
                    timing.drop(2).forEach { phase ->
                        val value = phase.integerValue("parcial em $context")
                        if (value !in 0..previous) invalid("Parciais inválidas em $context.")
                        previous = value
                    }
                    warnings += "Parciais de etapas não serão importadas; o tempo total será preservado."
                }
                val scramble = row[1].textValue("embaralhamento em $context")
                if (scramble.length > SolveTimeRepository.MAX_SCRAMBLE_LENGTH) {
                    invalid("Embaralhamento longo demais em $context.")
                }
                try {
                    MoveNotation.parseAlgorithm(scramble)
                } catch (_: IllegalArgumentException) {
                    invalid("Notação de embaralhamento não suportada em $context.")
                }
                val comment = row[2].textValue("comentário em $context")
                if (comment.length > SolveTimeRepository.MAX_COMMENT_LENGTH) {
                    invalid("Comentário longo demais em $context.")
                }
                val startSeconds = if (row.size() >= 4) row[3].integerValue("data em $context") else 0L
                if (startSeconds !in 0..(MAX_EPOCH_MILLIS - duration) / 1_000L) {
                    invalid("Data inválida em $context.")
                }
                if (startSeconds == 0L) warnings += "Há tempos sem data; eles ficarão sem data registrada."
                if (row.size() == 5 && !row[4].isJsonNull) {
                    warnings += "Extensões e reconstruções do csTimer não serão importadas."
                }
                TransferredSolve(
                    durationMillis = duration,
                    recordedAtEpochMillis = if (startSeconds == 0L) 0L else startSeconds * 1_000L + duration,
                    scramble = scramble,
                    comment = comment,
                    penalty = penalty,
                )
            }
            sessions += TransferredSession(name, solves)
        }
        if (sessions.isEmpty()) invalid("O arquivo não contém sessões 3x3 compatíveis (333 ou 333o).")
        return TimerTransferPreview(sessions, skippedSessions, skippedSolves, warnings.toList())
    }

    fun encode(sessions: List<TransferredSession>): String {
        if (sessions.isEmpty() || sessions.size > MAX_SESSIONS) invalid("Quantidade de sessões inválida.")
        val root = JsonObject()
        val metadata = JsonObject()
        sessions.forEachIndexed { index, session ->
            if (session.name.isBlank() || session.name.length > TimerSessionRepository.MAX_NAME_LENGTH) {
                invalid("Nome de sessão inválido para exportação.")
            }
            val number = (index + 1).toString()
            metadata.add(number, JsonObject().apply {
                addProperty("name", encodeName(session.name))
                addProperty("rank", index + 1)
                add("opt", JsonObject().apply { addProperty("scrType", "333") })
            })
            root.add("session$number", JsonArray().apply {
                session.solves.forEach { solve ->
                    if (solve.durationMillis !in 0..MAX_EPOCH_MILLIS ||
                        solve.recordedAtEpochMillis !in 0..MAX_EPOCH_MILLIS
                    ) invalid("Tempo ou data inválidos para exportação.")
                    add(JsonArray().apply {
                        add(JsonArray().apply {
                            add(when (solve.penalty) {
                                SolvePenalty.NONE -> 0
                                SolvePenalty.PLUS_TWO -> 2_000
                                SolvePenalty.DNF -> -1
                            })
                            add(solve.durationMillis)
                        })
                        add(solve.scramble)
                        add(solve.comment)
                        add(if (solve.recordedAtEpochMillis == 0L) 0L else {
                            (solve.recordedAtEpochMillis - solve.durationMillis).coerceAtLeast(0L) / 1_000L
                        })
                    })
                }
            })
        }
        root.add("properties", JsonObject().apply {
            addProperty("sessionN", sessions.size)
            addProperty("session", 1)
            addProperty("sessionData", metadata.toString())
        })
        val json = GsonBuilder().disableHtmlEscaping().create().toJson(root)
        decode(json)
        return json
    }

    private fun unwrap(value: JsonElement): JsonElement =
        if (value.isJsonPrimitive && value.asJsonPrimitive.isString) parse(value.asString) else value

    private fun parse(json: String): JsonElement = try {
        JsonReader(StringReader(json)).use { reader ->
            reader.strictness = Strictness.STRICT
            val value = readValue(reader, 0)
            if (reader.peek() != JsonToken.END_DOCUMENT) invalid("Conteúdo extra após o JSON.")
            value
        }
    } catch (error: TimerTransferException) {
        throw error
    } catch (_: Exception) {
        invalid("JSON inválido. Use a exportação local sem compressão do csTimer.")
    }

    private fun readValue(reader: JsonReader, depth: Int): JsonElement {
        if (depth > 12) invalid("O JSON tem níveis demais.")
        return when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> JsonObject().apply {
                reader.beginObject()
                while (reader.hasNext()) {
                    val key = reader.nextName()
                    if (has(key)) invalid("Campo JSON duplicado: $key.")
                    add(key, readValue(reader, depth + 1))
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> JsonArray().apply {
                reader.beginArray()
                while (reader.hasNext()) add(readValue(reader, depth + 1))
                reader.endArray()
            }
            JsonToken.STRING -> JsonPrimitive(reader.nextString())
            JsonToken.NUMBER -> JsonPrimitive(reader.nextString().toBigDecimal())
            JsonToken.BOOLEAN -> JsonPrimitive(reader.nextBoolean())
            JsonToken.NULL -> { reader.nextNull(); JsonNull.INSTANCE }
            else -> invalid("Valor JSON inválido.")
        }
    }

    private fun JsonElement.objectValue(label: String): JsonObject =
        if (isJsonObject) asJsonObject else invalid("Objeto inválido: $label.")

    private fun JsonElement.arrayValue(label: String): JsonArray =
        if (isJsonArray) asJsonArray else invalid("Lista inválida: $label.")

    private fun JsonElement.textValue(label: String): String =
        if (isJsonPrimitive && asJsonPrimitive.isString) asString else invalid("Texto inválido: $label.")

    private fun JsonElement.integerValue(label: String): Long {
        if (!isJsonPrimitive || !asJsonPrimitive.isNumber) invalid("Número inválido: $label.")
        return try { asBigDecimal.longValueExact() } catch (_: ArithmeticException) {
            invalid("Número inteiro inválido: $label.")
        }
    }

    private fun encodeName(name: String): String = name.replace("&", "&amp;")
        .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

    private fun decodeName(name: String): String = Regex("&(?:amp|lt|gt|quot|#39|#x27);").replace(name) {
        when (it.value) {
            "&amp;" -> "&"
            "&lt;" -> "<"
            "&gt;" -> ">"
            "&quot;" -> "\""
            else -> "'"
        }
    }

    private fun invalid(message: String): Nothing = throw TimerTransferException(message)
}
