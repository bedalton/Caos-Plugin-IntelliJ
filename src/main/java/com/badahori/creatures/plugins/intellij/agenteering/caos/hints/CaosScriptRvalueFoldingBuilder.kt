package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.commandToken2ElementType
import com.badahori.creatures.plugins.intellij.agenteering.utils.isFolded
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptRvalueFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val rvalues = PsiTreeUtil
            .collectElementsOfType(root, CaosScriptRvalue::class.java)
        if (quick) {
            return rvalues
                .mapNotNull map@{ rvalue ->
                    if (rvalue.isFolded) {
                        return@map null
                    }
                    val (oldText, foldData) = rvalue.getUserData(RVALUE_FOLDING_KEY)
                        ?: return@map null
                    return@map if (oldText == rvalue.text) {
                        if (foldData != null) {
                            FoldingDescriptor(rvalue, foldData.second)
                        } else {
                            null
                        }
                    } else {
                        rvalue.putUserData(RVALUE_FOLDING_KEY, null)
                        null
                    }
                }
                .toTypedArray()
        }
        return rvalues
            .mapNotNull map@{ rvalue ->
//                if (rvalue.isFolded) {
//                    return@map null
//                }
                val text = rvalue.text
                // Filter by cached value first
                rvalue.getUserData(RVALUE_FOLDING_KEY)?.let { (oldText, value) ->
                    if (oldText == text) {
                        return@map if (value != null) {
                             FoldingDescriptor(rvalue, value.second)
                        } else {
                            null
                        }
                    }
                }
                val resolved = getRvalueTextCaching(rvalue)
                if (resolved != null) {
                    FoldingDescriptor(rvalue, resolved.second)
                } else {
                    null
                }
            }
            .toTypedArray()
    }

    /**
     * Get the text to replace a region with
     */
    override fun getPlaceholderText(node: ASTNode): String? {
        if (node.elementType != CaosScriptTypes.CaosScript_RVALUE) {
            return null
        }
        val rvalue = node.psi as? CaosScriptRvalue
            ?: return null
        return getRvalueTextCaching(rvalue)?.first
    }


    /**
     * Get the replacement text and range, then store it for faster retrieval
     */
    private fun getRvalueTextCaching(rvalue: CaosScriptRvalue): Pair<String, TextRange>? {
        val text = rvalue.text
        rvalue.getUserData(RVALUE_FOLDING_KEY)?.let { (oldText, foldData) ->
            if (oldText == text) {
                return foldData
            }
        }
        val foldData = formatRvalue(rvalue)?.let { foldData ->
            if (foldData.first.isEmpty()) {
                null
            } else {
                foldData
            }
        }
        rvalue.putUserData(RVALUE_FOLDING_KEY, Pair(text, foldData))
        return foldData
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        val psi = node.psi as? CaosScriptRvalue
            ?: return false
        val firstToken = psi.commandTokenElementType
        if (firstToken in FOLD_BY_DEFAULT) {
            return true
        }

        if (psi.getUserData(RVALUE_FOLDING_KEY)?.second == null)
            return false
        return true
    }

    companion object {
//        private val EMPTY_FOLDING_DESCRIPTOR_ARRAY: Array<FoldingDescriptor> = emptyArray()
        private val RVALUE_FOLDING_KEY = Key<Pair<String, Pair<String, TextRange>?>?>("caos.rvalue-folding.value")

        private val FOLD_BY_DEFAULT = listOf(
            CaosScriptTypes.CaosScript_K_CHEM,
            CaosScriptTypes.CaosScript_K_DRIV,
            CaosScriptTypes.CaosScript_K_ATTR,
            CaosScriptTypes.CaosScript_K_CLIK,
            CaosScriptTypes.CaosScript_K_KEYD,
            CaosScriptTypes.CaosScript_K_LCUS,
            CaosScriptTypes.CaosScript_K_LOCI,
            CaosScriptTypes.CaosScript_K_RAIN,
            CaosScriptTypes.CaosScript_K_RATE,
            CaosScriptTypes.CaosScript_K_SCOL,
            CaosScriptTypes.CaosScript_K_SORC,
            CaosScriptTypes.CaosScript_K_SORQ,
            CaosScriptTypes.CaosScript_K_SOUL,
            CaosScriptTypes.CaosScript_K_WOLF,
            CaosScriptTypes.CaosScript_K_PROP,
            CaosScriptTypes.CaosScript_K_MUTE,
            CaosScriptTypes.CaosScript_K_LORP,
            CaosScriptTypes.CaosScript_K_HIRP,
            CaosScriptTypes.CaosScript_K_DOOR,

        )
    }
}