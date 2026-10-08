package com.gabs.cubo3x3.data.transfer

import androidx.room.withTransaction
import com.gabs.cubo3x3.data.progress.CuboDatabase
import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTimeEntity
import com.gabs.cubo3x3.data.timer.TimerSessionEntity
import com.gabs.cubo3x3.data.timer.TimerSessionRepository
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TimerTransferRepository(
    private val database: CuboDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun inspect(input: InputStream): TimerTransferPreview = withContext(Dispatchers.IO) {
        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                if (total > CsTimerCodec.MAX_BYTES) throw TimerTransferException("O arquivo excede 5 MB.")
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val json = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: java.nio.charset.CharacterCodingException) {
            throw TimerTransferException("O arquivo não contém texto UTF-8 válido.")
        }
        CsTimerCodec.decode(json)
    }

    suspend fun prepareExport(): PreparedTimerExport = withContext(Dispatchers.Default) {
        val snapshot = database.withTransaction {
            database.backupDao().readTimerSessions() to database.backupDao().readSolveTimes()
        }
        val grouped = snapshot.second.groupBy { it.sessionId }
        val sessions = snapshot.first.map { session ->
            TransferredSession(
                name = session.name,
                solves = grouped[session.id].orEmpty()
                    .sortedWith(compareBy<SolveTimeEntity> { it.recordedAtEpochMillis }.thenBy { it.id })
                    .map { solve ->
                        TransferredSolve(
                            solve.durationMillis, solve.recordedAtEpochMillis, solve.scramble,
                            solve.comment, SolvePenalty.valueOf(solve.penalty),
                        )
                    },
            )
        }
        val json = CsTimerCodec.encode(sessions)
        PreparedTimerExport(
            json,
            TimerTransferPreview(
                sessions = sessions,
                warnings = listOf(
                    "A data usa precisão de segundos no csTimer.",
                    "Tema, favoritos, Quiz, personalizados e vínculo de treino CFOP ficam apenas no backup .3x3backup.",
                ),
            ),
        )
    }

    suspend fun exportTo(prepared: PreparedTimerExport, output: OutputStream) = withContext(Dispatchers.IO) {
        output.use { stream ->
            stream.write(prepared.json.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    suspend fun importAsNewSessions(preview: TimerTransferPreview): List<Long> {
        // Validate again at the write boundary; no partial writes if any insertion fails.
        val validated = withContext(Dispatchers.Default) {
            CsTimerCodec.decode(CsTimerCodec.encode(preview.sessions))
        }
        return database.withTransaction {
            val names = database.timerSessionDao().readAll().map { it.name }.toMutableList()
            if (names.size + validated.sessions.size > CsTimerCodec.MAX_SESSIONS ||
                database.backupDao().readSolveTimes().size + validated.solveCount > CsTimerCodec.MAX_SOLVES
            ) {
                throw TimerTransferException("A importação excederia os limites do backup completo (1.000 sessões ou 100.000 tempos).")
            }
            validated.sessions.map { session ->
                var name = session.name
                var suffix = 2
                while (names.any { it.equals(name, ignoreCase = true) }) {
                    val ending = " ($suffix)"
                    name = session.name.take(TimerSessionRepository.MAX_NAME_LENGTH - ending.length) + ending
                    suffix++
                }
                names += name
                val id = database.timerSessionDao().insert(TimerSessionEntity(name = name, createdAtEpochMillis = now()))
                session.solves.forEach { solve ->
                    database.solveTimeDao().insert(
                        SolveTimeEntity(
                            durationMillis = solve.durationMillis,
                            recordedAtEpochMillis = solve.recordedAtEpochMillis,
                            scramble = solve.scramble,
                            comment = solve.comment,
                            penalty = solve.penalty.name,
                            sessionId = id,
                        ),
                    )
                }
                id
            }
        }
    }
}
