// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

package com.developercal.ti99pocket

import android.graphics.Bitmap
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun bundledCartridgeLoadsAndCanBeEjectedFromTheLibrary() {
        compose.onNodeWithText("SOFTWARE LIBRARY  ›").performClick()
        compose.onNodeWithText("The Texas Trail", substring = true).performClick()
        compose.onNodeWithText("LOAD").performClick()

        compose.onNodeWithText("SOFTWARE LIBRARY  ›").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("mounted", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("EJECT").performClick()
        assert(compose.onAllNodesWithText("mounted", substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun settingsAndProgrammingLabAreReachableFromTheLiveEmulator() {
        compose.onNodeWithText("LONESTAR 99").assertIsDisplayed()

        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Theme mode").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hardware skin").performScrollTo().assertIsDisplayed()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithContentDescription("About LoneStar 99").performClick()
        compose.onNodeWithText("PROGRAMMING LAB").performScrollTo().performClick()
        compose.onNodeWithText("Programming Lab").assertIsDisplayed()
        compose.onNodeWithText("NEW").performClick()
        compose.onNodeWithText("RUN ON CONSOLE").assertIsDisplayed()
        compose.onNodeWithText("DONE").performClick()
        compose.onNodeWithText("RESET").performClick()
        compose.onNodeWithText("Reset the TI-99/4A?").assertIsDisplayed()
        compose.onNodeWithText("CANCEL").performClick()
        assert(compose.onAllNodesWithText("Reset the TI-99/4A?").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun aboutAndSupportActionsAreVisible() {
        compose.onNodeWithContentDescription("About LoneStar 99").performClick()
        compose.onNodeWithText("Unofficial TI-99/4A emulator", substring = true).assertIsDisplayed()
        compose.onNodeWithText("HISTORY OF THE TI-99/4A").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("SUPPORT LONESTAR 99 ON LIBERAPAY").performScrollTo().assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
class HomebrewCartridgeInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test fun starlightLoadsAndVisibleGameplayChangesAfterFireAndCollision() = exercise(
        "Starlight Patrol", listOf(28, 29, 30), "starlight-patrol")

    @Test fun solarLoadsAndVisibleGameplayChangesThroughPickupAndWin() = exercise(
        "Solar Salvage 99", listOf(28, 29, 30, 29, 29, 30, 29, 29), "solar-salvage-99")

    @Test fun oilfieldLoadsAndVisibleGameplayChangesThroughScoringAndCollision() = exercise(
        "Oilfield Outrun", listOf(28, 28, 28, 28, 28, 28, 28, 28, 28), "oilfield-outrun")

    @Test fun titrisLoadsAndRespondsToRealJoystickMatrixInput() = exercise(
        "Titris", listOf(49, 49, 47, 48), "titris")

    @Test fun sokobanLoadsAndMovesThroughItsFirstLevel() = exercise(
        "Sokoban", listOf(50, 48, 48, 49), "sokoban")

    @Test fun homebrewTitlesAndControlsRemainVisibleInPortraitAndLandscape() {
        compose.onNodeWithText("SOFTWARE LIBRARY  ›").performClick()
        listOf("Starlight Patrol", "Solar Salvage 99", "Oilfield Outrun").forEach { title ->
            compose.onNodeWithText(title, substring = true).assertIsDisplayed()
        }
        compose.onNodeWithText("Space shooter", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Maze puzzle", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Driving challenge", substring = true).assertIsDisplayed()
        save(screenshot(), "homebrew-library-portrait.png")
        compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
        listOf("Starlight Patrol", "Solar Salvage 99", "Oilfield Outrun").forEach { title ->
            compose.onNodeWithText(title, substring = true).assertIsDisplayed()
        }
        compose.onNodeWithText("Space shooter", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Maze puzzle", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Driving challenge", substring = true).assertIsDisplayed()
        save(screenshot(), "homebrew-library-landscape.png")
        compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }

    private fun exercise(title: String, keys: List<Int>, fileStem: String) {
        compose.onNodeWithText("SOFTWARE LIBRARY  ›").performClick()
        compose.onNodeWithText(title, substring = true).performClick()
        compose.onNodeWithText("LOAD").performClick()
        SystemClock.sleep(2_000)

        // Follow the real firmware startup sequence to the selected cartridge.
        press(41) // Space
        SystemClock.sleep(2_000)
        press(28) // 2: start selected cartridge
        SystemClock.sleep(3_000)
        save(screenshot(), "$fileStem-title.png")
        press(41) // Start game from its title/instruction screen.
        SystemClock.sleep(500)
        val before = screenshot()

        keys.forEach { code ->
            press(code)
            SystemClock.sleep(250)
        }
        SystemClock.sleep(500)
        val after = screenshot()
        assertTrue("$title must change the real emulator display after scripted TI key input", changedPixels(before, after) > 200)
        save(before, "$fileStem-before.png")
        save(after, "$fileStem-after.png")
        compose.onNodeWithText("SOFTWARE LIBRARY  ›").performClick()
        compose.onNodeWithText("EJECT").performClick()
        compose.onNodeWithText("SOFTWARE LIBRARY  ›").assertIsDisplayed()
    }

    private fun press(code: Int) {
        compose.activityRule.scenario.onActivity {
            NativeCore.nativeSetKey(NativeCore.handle, code, true)
        }
        SystemClock.sleep(100)
        compose.activityRule.scenario.onActivity {
            NativeCore.nativeSetKey(NativeCore.handle, code, false)
        }
    }

    private fun screenshot(): Bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation
        .takeScreenshot().copy(Bitmap.Config.ARGB_8888, false)

    private fun changedPixels(a: Bitmap, b: Bitmap): Int {
        if (a.width != b.width || a.height != b.height) return Int.MAX_VALUE
        val left = IntArray(a.width * a.height)
        val right = IntArray(b.width * b.height)
        a.getPixels(left, 0, a.width, 0, 0, a.width, a.height)
        b.getPixels(right, 0, b.width, 0, 0, b.width, b.height)
        return left.indices.count { left[it] != right[it] }
    }

    private fun save(bitmap: Bitmap, name: String) {
        val directory = compose.activity.cacheDir.resolve("homebrew-gameplay")
        directory.mkdirs()
        directory.resolve(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
