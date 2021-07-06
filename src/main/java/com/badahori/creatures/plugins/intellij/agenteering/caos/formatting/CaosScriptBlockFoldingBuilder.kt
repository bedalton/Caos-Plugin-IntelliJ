package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.progress.ProgressIndicatorProvider
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
    return element.tokenType in CaosScriptTokenSets.WHITESPACES || element is CaosScriptWhiteSpaceLike
}
/**
 * Class for folding code blocks in a CaosScriptFile
 */
class CaosScriptBlockFoldingBuilder : FoldingBuilderEx() {

    /**
     * Gets placeholder text, for now it is simply ellipsis
     */
    override fun getPlaceholderText(node: ASTNode): String? {
        return if (node.elementType.let { it == CaosScriptTypes.CaosScript_CODE_BLOCK || it == CaosScriptTypes.CaosScript_DOIF_STATEMENT }) "..." else null
    }

    /**
     * Gets folding regions for all code blocks in the given element
     */
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        return (PsiTreeUtil.findChildrenOfType(root, CaosScriptHasCodeBlock::class.java).mapNotNull blocks@{ parent ->
            if (parent is CaosScriptMacro)
                    return@blocks null

            (parent.parent as? CaosScriptDoifStatement)?.let {doif->
                // If doif has no sub code blocks,
                // defer collapsing to the doif specific method
                if (doif.elseIfStatementList.isEmpty() && (doif.elseStatement?.codeBlock?.codeBlockLineList?.firstOrNull() == null))
                    return@blocks null
            }

            ProgressIndicatorProvider.checkCanceled()
            val group = FoldingGroup.newGroup("CaosScript_BLOCK_FOLDING")
            val codeBlock = parent.codeBlock
                ?: return@blocks null // If block is empty, there is nothing to fold
            // Determine whether to start fold at end of start expression, or start of
            val startAtNextLine = parent is CaosScriptDoifStatementStatement
            val rangeStart = getBlockStart(codeBlock, startAtNextLine)
            val rangeEnd = getBlockEnd(codeBlock)
            if (rangeStart < 0 || rangeStart >= rangeEnd)
                return@blocks null
            FoldingDescriptor(parent.node, TextRange(rangeStart, rangeEnd), group)
        } + PsiTreeUtil.findChildrenOfType(root, CaosScriptDoifStatement::class.java).mapNotNull {doif ->
            doif.doifStatementStatement.equalityExpression?.endOffset?.let { foldStart ->
                if (doif.cEndi == null)
                    return@let null
                val foldEnd = doif.cEndi?.startOffset ?: doif.endOffset
                if (foldStart >= foldEnd)
                    null
                else
                    FoldingDescriptor(doif.node, TextRange(foldStart, foldEnd))
            }
        }).toTypedArray()
    }

    /**
     * Gets block start by consuming all previous whitespace elements
     */
    private fun getBlockStart(codeBlock: CaosScriptCodeBlock, startAtNextLine:Boolean): Int {
        if (startAtNextLine) {
            codeBlock.codeBlockLineList.firstOrNull()?.startOffset?.let {
                return it
            }
        }
        var previous = codeBlock.previous
        while (previous != null) {
            ProgressIndicatorProvider.checkCanceled()
            val temp = previous.previous ?: break
            if (temp.tokenType in CaosScriptTokenSets.WHITESPACES || temp is CaosScriptWhiteSpaceLike)
                previous = temp
            else
                break
        }
        return if (consume(previous))
            // This starts fold at start of last leading whitespace,
            // TODO: perhaps this should be at end of last leading whitespace
                //// this though would push it the start down to the next line which may not be what we want
            previous.startOffset
        else
            codeBlock.startOffset
    }


    /**
     * Gets block end consuming all trailing whitespace elements
     */
    private fun getBlockEnd(codeBlock: CaosScriptCodeBlock): Int {
        var next = codeBlock.next
        while (next != null) {
            ProgressIndicatorProvider.checkCanceled()
            val temp = next.next ?: break
            if (temp.tokenType in CaosScriptTokenSets.WHITESPACES || temp is CaosScriptWhiteSpaceLike)
                next = temp
            else
                break
        }
        return if (next != null && (next.tokenType in CaosScriptTokenSets.WHITESPACES || next is CaosScriptWhiteSpaceLike))
            next.endOffset
        else
            codeBlock.endOffset
    }

    override fun isCollapsedByDefault(root: ASTNode): Boolean {
        return false
    }
}