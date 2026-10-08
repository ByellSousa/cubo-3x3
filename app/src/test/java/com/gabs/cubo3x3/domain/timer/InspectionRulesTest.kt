package com.gabs.cubo3x3.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class InspectionRulesTest {
    @Test
    fun inspectionBeforeFifteenSecondsHasNoPenalty() {
        assertEquals(InspectionPenalty.NONE, InspectionRules.penaltyAt(14_999L))
        assertEquals(1L, InspectionRules.remainingMillis(14_999L))
    }

    @Test
    fun inspectionFromFifteenUntilBeforeSeventeenSecondsGetsPlusTwo() {
        assertEquals(InspectionPenalty.PLUS_TWO, InspectionRules.penaltyAt(15_000L))
        assertEquals(InspectionPenalty.PLUS_TWO, InspectionRules.penaltyAt(16_999L))
        assertEquals(0L, InspectionRules.remainingMillis(15_000L))
    }

    @Test
    fun inspectionAtSeventeenSecondsIsDnf() {
        assertEquals(InspectionPenalty.DNF, InspectionRules.penaltyAt(17_000L))
        assertEquals(InspectionPenalty.DNF, InspectionRules.penaltyAt(30_000L))
    }
}
