package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedGameVarIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedGameVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptQuoteStringLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

/**
 * Checks that a string literal is reference to other named game vars
 */
class CaosScriptQuoteStringLiteralReference(element:CaosScriptQuoteStringLiteral) : PsiPolyVariantReferenceBase<CaosScriptQuoteStringLiteral>(element, TextRange(1, 1 + element.stringValue.length)) {

    private val selfOnlyResult by lazy {
        if (myElement.isValid)
            PsiElementResolveResult.createResults(myElement)
        else
            ResolveResult.EMPTY_ARRAY
    }

    private val type:CaosScriptNamedGameVarType by lazy {
        (element.parent?.parent as? CaosScriptNamedGameVar)?.varType ?: CaosScriptNamedGameVarType.UNDEF
    }

    private val key:String by lazy {
        (element.parent?.parent as? CaosScriptNamedGameVar)?.key ?: UNDEF
    }

    private val canResolve by lazy {
        type != CaosScriptNamedGameVarType.UNDEF && key != UNDEF
    }

    /**
     * Returns true for any matching var in project
     * TODO: Mask NAME and MAME by agent class
     */
    override fun isReferenceTo(element: PsiElement): Boolean {
        // Initial check to see that this element is resolvable
        if (!canResolve) {
            return false
        }
        // Ensure that variables share the same variant,
        // otherwise the variables cannot be the same
        if (element.variant != myElement.variant)
            return false
        // Ensure other is or has named game var parent element
        val namedGameVarParent = element.parent?.parent as? CaosScriptNamedGameVar ?: return false
        // Check that type and key are the same
        return namedGameVarParent.varType == type && namedGameVarParent.key == key
    }

    /**
     * Resolves to itself to prevent weird ctrl+click behavior
     */
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!canResolve) {
            return selfOnlyResult
        }

        if (DumbService.isDumb(myElement.project))
            return selfOnlyResult
        val variant = myElement.variant
                ?: return selfOnlyResult
        val references = CaosScriptNamedGameVarIndex.instance[type, key, myElement.project]
                .filter {anElement ->
                    ProgressIndicatorProvider.checkCanceled()
                    anElement.variant == variant && anElement.isValid
                }
                .mapNotNull {namedGameVar ->
                    ProgressIndicatorProvider.checkCanceled()
                    namedGameVar.rvalue?.quoteStringLiteral?.let { stringLiteral ->
                        if (stringLiteral.isValid)
                            stringLiteral
                        else
                            null
                    }
                }
                .nullIfEmpty()
                ?.let {
                    it + myElement
                }
                ?: return selfOnlyResult
        return PsiElementResolveResult.createResults(references)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }

}