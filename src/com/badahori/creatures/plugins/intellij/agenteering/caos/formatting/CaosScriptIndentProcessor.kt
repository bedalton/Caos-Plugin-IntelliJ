package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.utils.editor
import kotlin.math.abs

class CaosScriptIndentProcessor(private val settings: CommonCodeStyleSettings, private val caosSettings: CaosScriptCodeStyleSettings) {
    fun getChildIndent(node: ASTNode): Indent? {
        if (!caosSettings.INDENT_BLOCKS) {
            return Indent.getNoneIndent()
        }
        val firstChild: ASTNode? = node

        // Handle indent if cursor is at block end (ie. NEXT, ENDI)
        if (firstChild?.elementType in CaosScriptTokenSets.BLOCK_START_AND_ENDS) {
            val cursor = firstChild?.psi?.editor?.caretModel?.offset
                    ?: -1
            // Cursor is actually at token, not just some newline before end
            if (abs(firstChild!!.startOffset - cursor) <= 2) {
                return Indent.getNoneIndent()
            }
        }
        val parent = node.psi
        if (parent is CaosScriptCodeBlock) {
            val parentBlock = parent.parent
            if (parentBlock is CaosScriptScriptElement) {
                return Indent.getNoneIndent()
            }
            return Indent.getNormalIndent()
        }
        return Indent.getNoneIndent()
    }
}