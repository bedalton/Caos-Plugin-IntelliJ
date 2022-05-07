@file:Suppress("DEPRECATION")

package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.utils.getNextNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.util.elementType


class CaosScriptBlock internal constructor(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    val settings: CommonCodeStyleSettings
) : AbstractBlock(node, wrap, alignment) {

    private val caosSettings by lazy {
        CodeStyleSettingsManager.getSettings(node.psi.project)
            .getCustomSettings(CaosScriptCodeStyleSettings::class.java)
    }

    private val spacingProcessor: CaosScriptSpacingProcessor by lazy {
        CaosScriptSpacingProcessor(node, settings)
    }
    private val indentProcessor: CaosScriptIndentProcessor by lazy {
        CaosScriptIndentProcessor(caosSettings)
    }

    override fun buildChildren(): List<Block> {
        val blocks: MutableList<Block> = mutableListOf()
        var child: ASTNode? = myNode.firstChildNode
        while (child != null) {
            if (child.elementType !in CaosScriptTokenSets.WHITESPACES || child.text.isNotBlank()) {
                val block: Block = CaosScriptBlock(
                    child,
                    NONE_WRAP,
                    Alignment.createAlignment(),
                    settings
                )
                blocks.add(block)
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getChildAttributes(newIndex: Int): ChildAttributes {
        if (caosSettings.indentNone()) {
            return noneIndent
        }
        val psi = myNode.psi
            ?: return noneIndent

        if (psi is CaosScriptCommentBlock || psi.hasParentOfType(CaosScriptCommentBlock::class.java) || psi.getPreviousNonEmptySibling(
                true
            ) is CaosScriptCommentBlock
        )
            return ChildAttributes(Indent.getAbsoluteNoneIndent(), null)

        val cursorElement = psi.editor?.let { it.cursorElementInside(psi.textRange) ?: it.primaryCursorElement }
            ?: return noneIndent
        if (psi is CaosScriptHasCodeBlock) {
            if (cursorElement.tokenType == TokenType.WHITE_SPACE) {
                val previous = cursorElement.getPreviousNonEmptySibling(true)
                if (previous is CaosScriptCodeBlockLine || previous is CaosScriptCodeBlock || previous?.parent is CaosScriptCodeBlock)
                    return normalIndent
                val next = cursorElement.getNextNonEmptySibling(true)
                if (next is CaosScriptCodeBlockLine || next is CaosScriptCodeBlock || next?.parent is CaosScriptCodeBlock)
                    return normalIndent
                if (cursorElement.parent is CaosScriptScriptElement) {
                    return if ((cursorElement.parent as CaosScriptScriptElement).scriptTerminator != null)
                        normalIndent
                    else
                        noneIndent
                }
            }
            return if (psi is CaosScriptScriptElement) {
                if (cursorElement.parent == psi)
                    noneIndent
                else
                    normalIndent
            } else {
                if (cursorElement.isOrHasParentOfType(CaosScriptCommentBlock::class.java))
                    absoluteNoneIndent
                else {
                    val previousElement = cursorElement.getPreviousNonEmptySibling(true)
                    if (previousElement == null || previousElement.isOrHasParentOfType(CaosScriptCommentBlock::class.java)
                            .orFalse()
                    )
                        absoluteNoneIndent
                    else
                        normalIndent
                }
            }
        }

        if (psi.parent is CaosScriptScriptElement) {
            val parent = psi.parent as CaosScriptScriptElement
            if (parent.scriptTerminator == null || parent is CaosScriptMacro)
                return noneIndent
            if (!caosSettings.INDENT_SCRP)
                return absoluteNoneIndent
            return normalIndent
        }

        // Needs to check if my node is a DoifStatement parent
        // Because the end of any child (internal doif block, elif, else) are considered the end,
        // meaning the last line would never be indented
        if (psi is CaosScriptDoifStatement)
            return normalIndent
        if (psi.parent?.tokenType in listOf(
                CaosScriptTypes.CaosScript_DOIF_STATEMENT_STATEMENT,
                CaosScriptTypes.CaosScript_ELSE_IF_STATEMENT,
                CaosScriptTypes.CaosScript_ELSE_STATEMENT
            )
        )
            return normalIndent
        if (psi is CaosScriptCodeBlockLine)
            return normalIndent
        if (psi.tokenType in CaosScriptTokenSets.WHITESPACES)
            return normalIndent

        if (cursorElement.tokenType == TokenType.WHITE_SPACE) {
            val previous = cursorElement.getPreviousNonEmptySibling(true)
            if (previous is CaosScriptCodeBlockLine || previous is CaosScriptCodeBlock || previous?.parent is CaosScriptCodeBlock)
                return normalIndent
            val next = cursorElement.getNextNonEmptySibling(true)
            if (next is CaosScriptCodeBlockLine || next is CaosScriptCodeBlock || next?.parent is CaosScriptCodeBlock)
                return normalIndent
        }
        return noneIndent
    }

    override fun getIndent(): Indent? {
        if (caosSettings.indentNone()) {
            return Indent.getNoneIndent()
        }
        return indentProcessor.getChildIndent(node)
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingProcessor.getSpacing(child1, child2)
    }

    override fun isLeaf(): Boolean {
        return myNode.firstChildNode == null
    }

    companion object {
        private val normalIndent = ChildAttributes(Indent.getNormalIndent(), null)
        private val noneIndent = ChildAttributes(Indent.getNoneIndent(), null)
        private val absoluteNoneIndent = ChildAttributes(Indent.getAbsoluteNoneIndent(), null)
    }

}


internal val NONE_WRAP: Wrap = Wrap.createWrap(WrapType.NONE, false)