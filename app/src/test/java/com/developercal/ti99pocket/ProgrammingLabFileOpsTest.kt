// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

package com.developercal.ti99pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProgrammingLabFileOpsTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun copyPreservesSourceAndCreatesDistinctLocalFile() {
        val source = temp.newFile("lesson.tibasic").apply { writeText("10 PRINT \"HI\"\n") }

        val copy = duplicateSource(source)

        assertEquals("10 PRINT \"HI\"\n", source.readText())
        assertEquals(source.readText(), copy.readText())
        assertTrue(copy.name.startsWith("lesson copy"))
    }

    @Test fun renameSanitizesTheNameAndKeepsItsExtensionWhenOmitted() {
        val source = temp.newFile("draft.tipy")

        val renamed = renameSource(source, "Texas Trail source")

        assertEquals("Texas_Trail_source.tipy", renamed.name)
        assertFalse(source.exists())
        assertTrue(renamed.exists())
    }

    @Test fun renameRefusesToOverwriteAnExistingSource() {
        val source = temp.newFile("draft.tibasic")
        val collision = temp.newFile("taken.tibasic")

        val failure = runCatching { renameSource(source, collision.name) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(source.exists())
        assertTrue(collision.exists())
    }

    @Test fun tiPythonRunnerRejectsInputTheConsoleCannotRepresent() {
        assertEquals(null, validateTiPythonSource("PRINT(\"HELLO\")\n2+3*4\n"))
        assertTrue(validateTiPythonSource("# LoneStar 99 TI PYTHON example")!!.contains("28-character"))
        assertTrue(validateTiPythonSource("PRINT(\"HELLO, TEXAS!\")")!!.contains("cannot type"))
        assertTrue(validateTiPythonSource("1".repeat(29))!!.contains("28-character"))
    }

    @Test fun tiPythonCharacterMappingUsesConsoleModifiers() {
        assertEquals(listOf(44, 36), tiPythonKeys('+'))
        assertEquals(listOf(43, 15), tiPythonKeys('"'))
        assertEquals(listOf(44, 31), tiPythonKeys('%'))
        assertEquals(listOf(0), tiPythonKeys('a'))
        assertEquals(null, tiPythonKeys('!'))
    }
}
