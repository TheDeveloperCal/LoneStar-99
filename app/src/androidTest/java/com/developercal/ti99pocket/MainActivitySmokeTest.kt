// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

package com.developercal.ti99pocket

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
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
