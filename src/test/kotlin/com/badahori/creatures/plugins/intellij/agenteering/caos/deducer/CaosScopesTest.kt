package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.intellij.openapi.util.TextRange
import org.junit.Test

class CaosScopesTest {


    @Test
    fun `scopes are not shared with same file`() {
        val file = "test.cos"
        val parent = CaosScope(
            file = file,
            TextRange(0, 100),
            CaosScriptBlockType.SCRP,
            null
        )

        val scope1 = CaosScope(
            file = file,
            TextRange(10, 50),
            CaosScriptBlockType.DOIF,
            parent
        )

        val scope2 = CaosScope(
            file = file,
            TextRange(51, 90),
            CaosScriptBlockType.ELSE,
            parent
        )

        val sharedScope =
    }
}