package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPreviousNonEmptyNode
import com.badahori.creatures.plugins.intellij.agenteering.utils.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType

interface CaosScriptIndentProcessor {
    fun getChildIndent(node: ASTNode): Indent?
}

class CaosScriptIndentProcessorImpl(private val caosSettings: CaosScriptCodeStyleSettings): CaosScriptIndentProcessor {
    override fun getChildIndent(node: ASTNode): Indent? {
        if (caosSettings.indentNone()) {
            return Indent.getNoneIndent()
        }
        val element = node.psi
            ?: return Indent.getNoneIndent()

        val blockIndent = if (caosSettings.INDENT_BLOCKS) {
            Indent.getNormalIndent()
        } else {
            Indent.getNoneIndent()
        }

        val elementType = node.elementType
        // Handle indent if cursor is at block end (i.e. NEXT, ENDI)
        return when {
            elementType in CaosScriptTokenSets.BLOCK_STARTS_AND_ENDS -> {
                Indent.getNoneIndent()
            }
            elementType in CaosScriptTokenSets.COMMENTS -> {
                val previous: ASTNode = node.previous
                    ?: return Indent.getAbsoluteNoneIndent()
                if (previous.elementType == TokenType.WHITE_SPACE && previous.text == "\n") {
                    Indent.getAbsoluteNoneIndent()
                } else {
                    blockIndent
                }
            }

            // C1 String indents... which should be none on new line as it adds the spaces to the output
            elementType == CaosScriptTypes.CaosScript_CLOSE_BRACKET -> Indent.getAbsoluteNoneIndent()
            elementType == CaosScriptTypes.CaosScript_TEXT_LITERAL -> Indent.getAbsoluteNoneIndent()

            elementType in CaosScriptTokenSets.WHITESPACES -> {
                blockIndent
            }

            node.getPreviousNonEmptyNode(true)?.elementType == CaosScriptTypes.CaosScript_COMMENT_BLOCK ->
                Indent.getAbsoluteNoneIndent()

            element is CaosScriptCommentBlock || element.hasParentOfType(CaosScriptCommentBlock::class.java) ->
                Indent.getAbsoluteNoneIndent()


            element is CaosScriptCodeBlockLine -> {
                (element.parent?.parent as? CaosScriptScriptElement)?.let {
                    getIndent(it, caosSettings)
                } ?: blockIndent
            }
            element is CaosScriptCodeBlock -> {
                if (element.firstChild?.firstChild?.firstChild is CaosScriptComment && caosSettings.INDENT_BLOCKS) {
                    Indent.getNormalIndent()
                } else {
                    Indent.getNoneIndent()
                }
            }
            element is CaosScriptCaos2Block -> Indent.getAbsoluteNoneIndent()
            element is CaosScriptCaos2BlockComment -> Indent.getAbsoluteNoneIndent()
            element.hasParentOfType(CaosScriptCaos2BlockComment::class.java) -> Indent.getAbsoluteNoneIndent()
            else -> Indent.getNoneIndent()
        }

    }

}

object CaosScriptNullIndentProcessor: CaosScriptIndentProcessor {
    override fun getChildIndent(node: ASTNode): Indent? {
        return null
    }

}

private fun getIndent(parentBlock: CaosScriptScriptElement, caosSettings: CaosScriptCodeStyleSettings): Indent {
    val indent = when (parentBlock) {
        is CaosScriptEventScript -> caosSettings.INDENT_SCRP && parentBlock.scriptTerminator != null
        is CaosScriptInstallScript -> caosSettings.INDENT_ISCR && parentBlock.scriptTerminator != null
        is CaosScriptRemovalScript -> caosSettings.INDENT_RSCR && parentBlock.scriptTerminator != null
        is CaosScriptMacro -> false
        else -> true
    }
    return if (indent)
        Indent.getNormalIndent()
    else
        Indent.getNoneIndent()
}