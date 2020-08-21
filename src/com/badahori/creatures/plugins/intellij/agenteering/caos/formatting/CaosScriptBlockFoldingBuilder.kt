package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptHasCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptWhiteSpaceLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.contracts.contract

private fun consume(element:PsiElement?) : Boolean {
    contract {
        returns(true) implies (element != null)
    }
    if (element == null)
        return false
    return element.elementType in CaosScriptTokenSets.WHITESPACES || element is CaosScriptWhiteSpaceLike
}
/**
 * Class for folding code blocks in a CaosScriptFile
 */
class CaosScriptBlockFoldingBuilder : FoldingBuilderEx() {

    /**
     * Gets placeholder text, for now it is simply ellipsis
     */
    override fun getPlaceholderText(node: ASTNode): String? {
        return if (node.elementType == CaosScriptTypes.CaosScript_CODE_BLOCK) "..." else null
    }

    /**
     * Gets folding regions for all code blocks in the given element
     */
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        return PsiTreeUtil.findChildrenOfType(root, CaosScriptHasCodeBlock::class.java).mapNotNull blocks@{ parent ->
            val group = FoldingGroup.newGroup("CaosScript_BLOCK_FOLDING")
            val codeBlock = parent.codeBlock ?: return@blocks null // If block is empty, there is nothing to fold
            val rangeStart = getBlockStart(codeBlock)
            val rangeEnd = getBlockEnd(codeBlock)
            if (rangeStart < 0 || rangeStart >= rangeEnd)
                return@blocks null
            FoldingDescriptor(parent.node, TextRange(rangeStart, rangeEnd), group)
        }.toTypedArray()
    }

    /**
     * Gets block start by consuming all previous whitespace elements
     */
    private fun getBlockStart(codeBlock: CaosScriptCodeBlock): Int {
        var previous = codeBlock.previous
        while (previous != null) {
            val temp = previous.previous ?: break
            if (temp.elementType in CaosScriptTokenSets.WHITESPACES || temp is CaosScriptWhiteSpaceLike)
                previous = temp
            else
                break
        }
        return if (consume(previous))
            previous.startOffset
        else
            codeBlock.startOffset
    }


    /**
     * Gets block end consiming all trailing whitespace elements
     */
    private fun getBlockEnd(codeBlock: CaosScriptCodeBlock): Int {
        var next = codeBlock.next
        while (next != null) {
            val temp = next.next ?: break
            if (temp.elementType in CaosScriptTokenSets.WHITESPACES || temp is CaosScriptWhiteSpaceLike)
                next = temp
            else
                break
        }
        return if (next != null && (next.elementType in CaosScriptTokenSets.WHITESPACES || next is CaosScriptWhiteSpaceLike))
            next.endOffset
        else
            codeBlock.endOffset
    }

    override fun isCollapsedByDefault(root: ASTNode): Boolean {
        return false
    }
}