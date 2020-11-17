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
        if (node.elementType == CaosScriptTypes.CaosScript_COMMENT) {
            val previousText = node.previous?.text
                    ?: return Indent.getAbsoluteNoneIndent()
            when {
                previousText.endsWith("\n") -> return Indent.getAbsoluteNoneIndent()
                previousText.endsWith("\t") || previousText.endsWith(" ") -> {
                    val lastLine = previousText.split("\n").last()
                    val precedingSpace = if (lastLine.contains("\t")) {
                        val psi = node.psi
                                ?: return Indent.getAbsoluteNoneIndent()
                        val tabSize = EditorUtil.tabSize(psi) ?: 4
                        lastLine.replace("\t", "".padStart(tabSize, ' ')).length
                    } else {
                        lastLine.length
                    }
                    return Indent.getSpaceIndent(precedingSpace, false)
                }
                else -> Indent.getNormalIndent()
            }
        }

        return Indent.getNoneIndent()
    }

    companion object {
        private val afterNewlineRegex = ".*\n$".toRegex()
        private val spacesBeforeRegex = ".*[ ]$".toRegex()
    }
}