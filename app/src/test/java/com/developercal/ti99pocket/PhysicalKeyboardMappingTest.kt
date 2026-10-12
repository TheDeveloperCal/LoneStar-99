// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

package com.developercal.ti99pocket

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhysicalKeyboardMappingTest {
    @Test fun arrowsUseTheSameTiFctnCombinationsAsTheTouchDpad() {
        assertEquals(listOf(43, 18), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_DPAD_LEFT, 0))
        assertEquals(listOf(43, 3), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_DPAD_RIGHT, 0))
        assertEquals(listOf(43, 4), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_DPAD_UP, 0))
        assertEquals(listOf(43, 23), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_DPAD_DOWN, 0))
    }

    @Test fun printableCharactersFollowTheAndroidKeyboardLayout() {
        assertEquals(listOf(0), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_Q, 'a'.code))
        assertEquals(listOf(16), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_A, 'Q'.code))
        assertEquals(listOf(27), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_UNKNOWN, '!'.code))
        assertEquals(listOf(40), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_UNKNOWN, '?'.code))
    }

    @Test fun modifiersAndEditingKeysReachTheTiMatrix() {
        assertEquals(listOf(44), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_SHIFT_LEFT, 0))
        assertEquals(listOf(45), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_CTRL_RIGHT, 0))
        assertEquals(listOf(43), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_ALT_LEFT, 0))
        assertEquals(listOf(43, 27), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_DEL, 0))
        assertEquals(listOf(43, 29), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_FORWARD_DEL, 0))
        assertEquals(listOf(43, 36), mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_ESCAPE, 0))
    }

    @Test fun unsupportedKeysAreNotInventedAsTiInputs() {
        assertNull(mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_F1, 0))
        assertNull(mapPhysicalKeyToTiKeys(KeyEvent.KEYCODE_UNKNOWN, 0))
    }

    @Test fun simultaneousSourcesDoNotReleaseAKeyUntilTheLastOwnerLifts() {
        val events = mutableListOf<Pair<Int, Boolean>>()
        val router = TiInputRouter { key, down -> events += key to down }

        router.set("touch:one", 18, true)
        router.set("touch:one", 18, true) // duplicate/repeat from the same source
        router.set("physical:two", 18, true)
        router.set("touch:one", 18, false)
        assertEquals(listOf(18 to true), events)

        router.set("physical:two", 18, false)
        assertEquals(listOf(18 to true, 18 to false), events)
    }

    @Test fun focusLossReleasesEveryHeldKeyAndLateKeyUpIsHarmless() {
        val events = mutableListOf<Pair<Int, Boolean>>()
        val router = TiInputRouter { key, down -> events += key to down }
        router.set("touch:one", 43, true)
        router.set("touch:one", 18, true)
        router.releaseAll()
        router.set("touch:one", 18, false)

        assertEquals(listOf(43 to true, 18 to true, 43 to false, 18 to false), events)
    }
}
