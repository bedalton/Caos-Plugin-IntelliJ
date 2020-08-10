package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptHasCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptBlockFoldingBuilder : FoldingBuilderEx() {

    override fun getPlaceholderText(node: ASTNode): String? {
        return if (node.elementType == CaosScriptTypes.CaosScript_CODE_BLOCK) "..." else null
    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {

        return PsiTreeUtil.findChildrenOfType(root, CaosScriptHasCodeBlock::class.java).map {
            val group = FoldingGroup.newGroup("CaosScript_BLOCK_FOLDING")
            FoldingDescriptor(it.node, it.codeBlock?.textRange ?: TextRange.create(it.firstChild.startOffset, if (it.firstChild.startOffset != it.lastChild.startOffset) it.lastChild.startOffset else it.endOffset), group)
        }.toTypedArray()
    }

    override fun isCollapsedByDefault(root: ASTNode): Boolean {
        return false
    }
}