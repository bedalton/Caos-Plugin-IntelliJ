package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptyNode
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.lineNumber
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.editor
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
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

        val elementType = node.elementType
        // Handle indent if cursor is at block end (ie. NEXT, ENDI)
        return when {
            elementType in CaosScriptTokenSets.BLOCK_STARTS_AND_ENDS -> {
                Indent.getNoneIndent()
            }
            elementType == CaosScriptTypes.CaosScript_COMMENT -> {
                val previousText = node.previous?.text
                    ?: return Indent.getAbsoluteNoneIndent()
                when {
                    previousText.endsWith("\n") -> Indent.getAbsoluteNoneIndent()
                    else -> Indent.getNoneIndent()
                }
            }

            // C1 String indents... which should be none on new line as it adds the spaces to the output
            elementType == CaosScriptTypes.CaosScript_CLOSE_BRACKET -> Indent.getAbsoluteNoneIndent()
            elementType == CaosScriptTypes.CaosScript_TEXT_LITERAL -> Indent.getAbsoluteNoneIndent()

            elementType == CaosScriptTypes.CaosScript_COMMENT_BLOCK -> Indent.getAbsoluteNoneIndent()

            elementType in CaosScriptTokenSets.WHITESPACES -> Indent.getNormalIndent()

            node.getPreviousNonEmptyNode(true)?.elementType == CaosScriptTypes.CaosScript_COMMENT_BLOCK ->
                Indent.getAbsoluteNoneIndent()

            element is CaosScriptCommentBlock || element.hasParentOfType(CaosScriptCommentBlock::class.java) ->
                Indent.getAbsoluteNoneIndent()


            element is CaosScriptCodeBlockLine -> {
                (element.parent?.parent as? CaosScriptScriptElement)?.let {
                    getIndent(it)
                } ?: Indent.getNormalIndent()
            }
            element is CaosScriptCodeBlock -> {
                if (element.firstChild?.firstChild?.firstChild is CaosScriptComment) {
                    Indent.getNormalIndent()
                } else
                    Indent.getNoneIndent()
            }
            element is CaosScriptCaos2Block -> Indent.getAbsoluteNoneIndent()
            element is CaosScriptCaos2BlockComment -> Indent.getAbsoluteNoneIndent()
            element.hasParentOfType(CaosScriptCaos2BlockComment::class.java) -> Indent.getAbsoluteNoneIndent()
            else -> Indent.getNoneIndent()
        }

    }

    companion object {
        private val afterNewlineRegex = ".*\n$".toRegex()
        private val spacesBeforeRegex = ".*[ ]$".toRegex()
    }
}

private fun getIndent(parentBlock:CaosScriptScriptElement) : Indent {
    val indent = when (parentBlock) {
        is CaosScriptEventScript -> parentBlock.scriptTerminator != null
        is CaosScriptInstallScript -> parentBlock.scriptTerminator != null
        is CaosScriptRemovalScript -> parentBlock.scriptTerminator != null
        is CaosScriptMacro -> false
        else -> true
    }
    return if (indent)
        Indent.getNormalIndent()
    else
        Indent.getNoneIndent()
}