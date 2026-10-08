package com.gabs.cubo3x3.data.transfer

import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsTimerCodecTest {
    @Test
    fun readsPublicSessionShapeAndKeepsRawDurationSeparateFromPenalty() {
        val json = """{
          "session1":[[[0,12340],"R U R'","bom",1700000000],[[2000,10000],"U2'","+2",1700000010],
            [[-1,9000],"F2","DNF",1700000020]],
          "properties":{"sessionN":1,"sessionData":"{\"1\":{\"name\":\"Treino\",\"opt\":{\"scrType\":\"333\"}}}"}
        }"""
        val preview = CsTimerCodec.decode(json)
        assertEquals("Treino", preview.sessions.single().name)
        assertEquals(listOf(SolvePenalty.NONE, SolvePenalty.PLUS_TWO, SolvePenalty.DNF),
            preview.sessions.single().solves.map { it.penalty })
        assertEquals(10_000L, preview.sessions.single().solves[1].durationMillis)
        assertEquals(1_700_000_012_340L, preview.sessions.single().solves[0].recordedAtEpochMillis)
        assertEquals("U2'", preview.sessions.single().solves[1].scramble)
    }

    @Test
    fun roundTripPreservesFieldsAndOnlyLosesSubsecondDatePrecision() {
        val sessions = listOf(TransferredSession("Treino <A> & B", listOf(
            TransferredSolve(12_345, 1_700_000_013_123, "R U2' R'", "Comentário\ncom \"aspas\"; ç", SolvePenalty.PLUS_TWO),
            TransferredSolve(8_000, 1_700_000_028_000, "F2", "", SolvePenalty.DNF),
        )), TransferredSession("Vazia", emptyList()))
        val encoded = CsTimerCodec.encode(sessions)
        val root = JsonParser.parseString(encoded).asJsonObject
        assertEquals(2_000, root["session1"].asJsonArray[0].asJsonArray[0].asJsonArray[0].asInt)
        assertEquals(12_345, root["session1"].asJsonArray[0].asJsonArray[0].asJsonArray[1].asInt)
        assertEquals(2, root["properties"].asJsonObject["sessionN"].asInt)
        assertTrue(root["properties"].asJsonObject["sessionData"].asString.contains("&lt;A&gt; &amp; B"))
        val decoded = CsTimerCodec.decode(encoded)
        assertEquals(sessions.map { it.name }, decoded.sessions.map { it.name })
        val expected = sessions[0].solves[0]
        val actual = decoded.sessions[0].solves[0]
        assertEquals(expected.copy(recordedAtEpochMillis = actual.recordedAtEpochMillis), actual)
        assertTrue(expected.recordedAtEpochMillis - actual.recordedAtEpochMillis in 0..999)
        assertEquals(sessions[0].solves[1], decoded.sessions[0].solves[1])
    }

    @Test
    fun acceptsLegacyStringContainersAndMetadataObject() {
        val json = """{"session1":"[[[0,1234],\"R\",\"\",1700000000]]",
          "properties":"{\"sessionData\":{\"1\":{\"name\":1,\"scr\":\"333o\"}}}"}"""
        assertEquals("1", CsTimerCodec.decode(json).sessions.single().name)
    }

    @Test
    fun skipsOtherPuzzleSessionsWithExplicitCounts() {
        val json = """{"session1":[[[0,1234],"R","",1700000000]],"session2":[[[0,333],"invalid for 3x3","",0]],
          "properties":{"sessionData":{"1":{"opt":{"scrType":"333"}},"2":{"opt":{"scrType":"222so"}}}}}"""
        val preview = CsTimerCodec.decode(json)
        assertEquals(1, preview.sessions.size)
        assertEquals(1, preview.skippedSessionCount)
        assertEquals(1, preview.skippedSolveCount)
    }

    @Test
    fun rejectsFileWithoutSupportedSessions() {
        rejects("""{"session1":[],"properties":{"sessionData":{"1":{"scr":"444wca"}}}}""")
    }

    @Test
    fun respectsPuzzleIdentityInMixedSessionReconstruction() {
        val json = """{"session1":[[[0,1234],"R","",1700000000,["R","333"]],
            [[0,2000],"R","",1700000001,["R","222"]]]}"""
        val preview = CsTimerCodec.decode(json)
        assertEquals(1, preview.solveCount)
        assertEquals(1, preview.skippedSolveCount)
        assertEquals(0, preview.skippedSessionCount)
    }

    @Test
    fun rejectsMalformedValuesInsteadOfTruncatingOrCoercing() {
        listOf("[3000,1234]", "[0,-1]", "[0,1.25]", "[0,9223372036854775808]", "[\"0\",1234]")
            .forEach { timing -> rejects("""{"session1":[[$timing,"R","",1700000000]]}""") }
        rejects("""{"session1":[[[0,1234],"INVALID","",1700000000]]}""")
        rejects("""{"session1":[[[0,1234],"R","",-1]]}""")
        rejects("""{"session1":[[[0,1234],"R","",1.2]]}""")
    }

    @Test
    fun rejectsDuplicateFieldsLenientJsonAndTrailingContent() {
        rejects("""{"session1":[],"session1":[]}""")
        rejects("""{session1:[]}""")
        rejects("""{"session1":[]}{}""")
        rejects("""{"session1":[],}""")
    }

    @Test
    fun warnsAboutMissingDatesPartialsAndExtensions() {
        val json = """{"session1":[[[0,5000,3000,1000],"R","",0,{"reconstruction":"text"}],[[0,1234],"",""]]}"""
        val preview = CsTimerCodec.decode(json)
        assertEquals(3, preview.warnings.size)
        assertTrue(preview.sessions.single().solves.all { it.recordedAtEpochMillis == 0L })
        assertEquals(5000L, preview.sessions.single().solves.first().durationMillis)
        rejects("""{"session1":[[[0,5000,6000],"R","",0]]}""")
    }

    @Test
    fun enforcesFileAndFieldLimits() {
        rejects(" ".repeat(CsTimerCodec.MAX_BYTES + 1))
        rejects("""{"session1":[[[0,1],"R","${"a".repeat(2001)}",0]]}""")
        val preview = CsTimerCodec.decode("""{"session1":[],"properties":{"sessionData":{"1":{"name":"${"a".repeat(51)}"}}}}""")
        assertEquals(50, preview.sessions.single().name.length)
        assertEquals(1, preview.warnings.size)
    }

    private fun rejects(json: String) {
        assertTrue("Expected invalid JSON to be rejected", runCatching { CsTimerCodec.decode(json) }
            .exceptionOrNull() is TimerTransferException)
    }
}
