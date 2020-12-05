package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.editor
import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import kotlin.math.abs

class CaosScriptIndentProcessor(private val caosSettings: CaosScriptCodeStyleSettings) {
    fun getChildIndent(node: ASTNode): Indent? {
        if (!caosSettings.indentBlocks) {
            return Indent.getNoneIndent()
        }
        val element = node.psi
            ?: return Indent.getNoneIndent()

        // Handle indent if cursor is at block end (ie. NEXT, ENDI)
        if (node.elementType in CaosScriptTokenSets.BLOCK_START_AND_ENDS) {
            val cursor = element.editor?.caretModel?.offset
                ?: -1
            // Cursor is actually at token, not just some newline before end
            if (abs(node.startOffset - cursor) <= 2) {
                return Indent.getNoneIndent()
            }
        } else if (node.elementType == CaosScriptTypes.CaosScript_COMMENT) {
            val previousText = node.previous?.text
                ?: return Indent.getAbsoluteNoneIndent()
            when {
                previousText.endsWith("\n") -> return Indent.getAbsoluteNoneIndent()
                else -> Indent.getNoneIndent()
            }
        } else if (element is CaosScriptCodeBlock) {
            val parentBlock = element.parent
            if (parentBlock is CaosScriptScriptElement) {
                val indent = when (parentBlock) {
                    is CaosScriptEventScript -> parentBlock.scriptTerminator != null
                    is CaosScriptInstallScript -> parentBlock.scriptTerminator != null
                    is CaosScriptRemovalScript -> parentBlock.scriptTerminator != null
                    else -> false
                }
                return if (indent)
                    Indent.getNormalIndent()
                else
                    Indent.getNoneIndent()
            }
            return Indent.getNormalIndent()
        }

        return Indent.getNoneIndent()
    }

    companion object {
        private val afterNewlineRegex = ".*\n$".toRegex()
        private val spacesBeforeRegex = ".*[ ]$".toRegex()
    }
}