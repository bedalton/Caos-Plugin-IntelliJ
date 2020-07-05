@file:Suppress("DEPRECATION")

package com.openc2e.plugins.intellij.agenteering.caos.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.openc2e.plugins.intellij.agenteering.caos.lang.variant
import com.openc2e.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.CaosScript_CODE_BLOCK_LINE
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlock
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptHasCodeBlock
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptMacro
import com.openc2e.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptyNode
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.openc2e.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.agenteering.caos.utils.orFalse


class CaosScriptBlock internal constructor(
        node: ASTNode,
        wrap: Wrap?,
        alignment: Alignment?,
        val settings: CommonCodeStyleSettings
) : AbstractBlock(node, wrap, alignment) {

    private val spacingProcessor: CaosScriptSpacingProcessor by lazy {
        CaosScriptSpacingProcessor(node, settings)
    }
    private val indentProcessor: CaosScriptIndentProcessor by lazy {
        val caosSettings = CodeStyleSettingsManager.getSettings(node.psi.project).getCustomSettings(CaosScriptCodeStyleSettings::class.java)
        CaosScriptIndentProcessor(settings, caosSettings)
    }

    override fun buildChildren(): List<Block> {
        val blocks: MutableList<Block> = mutableListOf()
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
            child = child.treeNext
        }
        return blocks
    }

    override fun getChildAttributes(newIndex: Int): ChildAttributes {
        if ((node.psi.containingFile as? CaosScriptFile)?.variant == CaosVariant.C1) {
            return noneIndent
        }
        val elementType = myNode.elementType
        val previousType = myNode.getPreviousNonEmptyNode(true)?.elementType
        if (myNode.psi is CaosScriptHasCodeBlock)
            return normalIndent
        val canIndent = elementType == CaosScript_CODE_BLOCK_LINE
                || previousType == CaosScript_CODE_BLOCK_LINE
                || myNode.treeParent?.elementType == CaosScript_CODE_BLOCK_LINE
                || myNode.psi is CaosScriptHasCodeBlock
        if (canIndent || myNode.psi is CaosScriptHasCodeBlock) {
            val isMacroOrEventScript = myNode.psi.getSelfOrParentOfType(CaosScriptCodeBlock::class.java)
                    ?.parent
                    ?.let { it is CaosScriptMacro || it is CaosScriptEventScript }
                    .orFalse()
            if (isMacroOrEventScript)
                return noneIndent
            return normalIndent
        }
        if (elementType in CaosScriptTokenSets.BLOCK_ENDS)
            return noneIndent
        return super.getChildAttributes(newIndex)
    }

    override fun getIndent(): Indent? {
        if ((node.psi.containingFile as? CaosScriptFile)?.variant == CaosVariant.C1 || !CaosScriptProjectSettings.indent)
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