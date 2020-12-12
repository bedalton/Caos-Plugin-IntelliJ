@file:Suppress("DEPRECATION")

package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.CaosScript_CODE_BLOCK_LINE
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.elementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.utils.editor
import com.badahori.creatures.plugins.intellij.agenteering.utils.isOrHasParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
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
            if (child.elementType !in CaosScriptTokenSets.WHITESPACES && child.text.isNotBlank()) {
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

        val codeBlock = myNode.psi as? CaosScriptCodeBlock
            ?: return if (myNode.psi.parent !is CaosScriptCodeBlock)
                normalIndent
            else
                noneIndent

        (codeBlock.parent as? CaosScriptScriptElement)?.let {scriptParent ->
            return if (scriptParent is CaosScriptMacro || (scriptParent is CaosScriptEventScript && scriptParent.scriptTerminator == null))
                noneIndent
            else
                normalIndent
        }
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
    }

}


internal val NONE_WRAP: Wrap = Wrap.createWrap(WrapType.NONE, false)