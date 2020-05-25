package com.openc2e.plugins.intellij.caos.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.impl.source.tree.ElementType
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCodeBlock
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptMacro
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets
import com.openc2e.plugins.intellij.caos.psi.util.getParentOfType
import java.util.*


class CaosScriptBlock internal constructor(
        node: ASTNode,
        wrap: Wrap?,
        alignment: Alignment?,
        val settings:CommonCodeStyleSettings
) : AbstractBlock(node, wrap, alignment) {

    private val subFormattedBlocks by lazy {
        val mySubObjJFormattedBlocks = mutableListOf<CaosScriptBlock>()
        for (block in subBlocks) {
            mySubObjJFormattedBlocks.add(block as CaosScriptBlock)
        }
        if (!mySubObjJFormattedBlocks.isNotEmpty()) mySubObjJFormattedBlocks else EMPTY
    }

    private val spacingProcessor: CaosScriptSpacingProcessor by lazy {
        CaosScriptSpacingProcessor(node, settings)
    }
    private val indentProcessor: CaosScriptIndentProcessor by lazy {
        val caosSettings = CodeStyleSettingsManager.getSettings(node.psi.project).getCustomSettings(CaosScriptCodeStyleSettings::class.java)
        CaosScriptIndentProcessor(settings, caosSettings)
    }

    override fun buildChildren(): List<Block> {
        val blocks: MutableList<Block> = ArrayList<Block>()
        var child: ASTNode? = myNode.firstChildNode
        while (child != null) {
            if (child.text.trim().isNotBlank()) {
                val block: Block = CaosScriptBlock(
                        child,
                        NONE_WRAP,
                        Alignment.createAlignment(),
                        settings
                )
                blocks.add(block)
            }
            child = child.getTreeNext()
        }
        return blocks
    }

    override fun getChildAttributes(newIndex: Int): ChildAttributes {
        val elementType = myNode.elementType
        val elementTypeReduced = (if (node.psi.children.size == 1)
            (node.firstChildNode.psi as? LeafPsiElement)?.elementType
        else
            null) ?: elementType
        val previousBlock = if (newIndex == 0 || subFormattedBlocks.isEmpty()) null else subFormattedBlocks[newIndex - 1]
        val previousType = previousBlock?.node?.elementType
        if (elementType == ElementType.WHITE_SPACE && node.psi.getParentOfType(CaosScriptCodeBlock::class.java)?.parent !is CaosScriptMacro)
            return normalIndent
        if (previousType == CaosScriptTypes.CaosScript_CODE_BLOCK_LINE || elementType == CaosScriptTypes.CaosScript_CODE_BLOCK_LINE || elementType == CaosScriptTypes.CaosScript_SPACE_LIKE_OR_NEWLINE)
            return normalIndent
        if (elementType in CaosScriptTokenSets.BLOCK_ENDS)
            return ChildAttributes(Indent.getNoneIndent(), null)
        return super.getChildAttributes(newIndex)
    }

    override fun getIndent(): Indent? {
        return indentProcessor.getChildIndent(node)
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingProcessor.getSpacing(child1, child2)
    }

    override fun isLeaf(): Boolean {
        return myNode.firstChildNode == null
    }

    companion object {
        private val EMPTY:List<CaosScriptBlock> = emptyList()
        private val normalIndent = ChildAttributes(Indent.getNormalIndent(), null)
        private val noneIndent = ChildAttributes(Indent.getNoneIndent(), null)
    }

}


internal val  NONE_WRAP:Wrap = Wrap.createWrap(WrapType.NONE, false)