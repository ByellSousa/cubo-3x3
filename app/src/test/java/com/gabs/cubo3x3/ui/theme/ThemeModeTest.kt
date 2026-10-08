package com.gabs.cubo3x3.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun storageValuesRoundTrip() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorage(mode.storageValue))
        }
    }

    @Test
    fun unknownStorageValueFallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("unknown"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
    }
}
