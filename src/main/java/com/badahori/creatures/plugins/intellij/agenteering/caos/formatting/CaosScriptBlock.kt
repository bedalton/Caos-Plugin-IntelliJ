@file:Suppress("DEPRECATION")

package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock


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
        if (!caosSettings.indentBlocks) {
            return noneIndent
        }
        val psi = myNode.psi
            ?: return noneIndent

        if (psi is CaosScriptCommentBlock || psi.hasParentOfType(CaosScriptCommentBlock::class.java) || psi.getPreviousNonEmptySibling(
                true
            ) is CaosScriptCommentBlock
        )
            return ChildAttributes(Indent.getAbsoluteNoneIndent(), null)

        if (psi is CaosScriptHasCodeBlock)
            return if (psi is CaosScriptScriptElement && psi.scriptTerminator == null)
                noneIndent
            else {
                val cursorElement = psi.editor?.let { it.cursorElementInside(psi.textRange) ?: it.primaryCursorElement }
                    ?: return noneIndent
                if (cursorElement.isOrHasParentOfType(CaosScriptCommentBlock::class.java))
                    absoluteNoneIndent
                else {
                    val previousElement = cursorElement.getPreviousNonEmptySibling(true)
                    if (previousElement == null || previousElement.isOrHasParentOfType(CaosScriptCommentBlock::class.java).orFalse())
                        absoluteNoneIndent
                    else
                        normalIndent
                }
            }

        if (psi is CaosScriptCodeBlock && (psi.parent as? CaosScriptScriptElement)?.let { it !is CaosScriptMacro && it.scriptTerminator != null }
                .orTrue()) {
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
        return noneIndent
    }

    override fun getIndent(): Indent? {
        if (!(CaosScriptProjectSettings.indent || caosSettings.indentBlocks))
            return Indent.getNoneIndent()
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