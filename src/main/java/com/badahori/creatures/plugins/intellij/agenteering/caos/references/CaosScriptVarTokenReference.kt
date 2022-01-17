package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandWord
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.collectElementsOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

/**
 * Find command variable reference
 */
class CaosScriptVarTokenReference(element: CaosScriptVarToken) :
    PsiPolyVariantReferenceBase<CaosScriptVarToken>(element, TextRange.create(0, element.text.length), true) {

    private val name: String by lazy {
        element.text.uppercase()
    }

    private val check: (element: PsiElement) -> Boolean by lazy {
        val variantCheck: (element: PsiElement) -> Boolean = myElement.variant?.let { variant ->
            { element: PsiElement -> element.variant == variant }
        } ?: { true }
        val varGroup = myElement.varGroup
        val varIndex = myElement.varIndex
        when (varGroup) {
            VARx, VAxx -> check@{ element ->
                if (!variantCheck(element)) {
                    return@check false
                }
                when (element) {
                    is CaosDefCommandWord, is CaosDefCommand -> element.text
                        .uppercase().let { it == "VARX" || it == "VAXX" }
                    is CaosScriptVarToken -> varIndex == element.varIndex && element.varGroup
                        .let { it == VARx || it == VAxx }
                    else -> false
                }
            }
            OBVx, OVxx -> check@{ element ->
                if (!variantCheck(element))
                    return@check false
                when (element) {
                    is CaosDefCommandWord, is CaosDefCommand -> element.text.uppercase()
                        .let { it == "OBVX" || it == "OVXX" }
                    is CaosScriptVarToken -> varIndex == element.varIndex && element
                        .varGroup.let { it == OBVx || it == OVxx }
                    else -> false
                }
            }

            MVxx -> check@{ element ->
                if (!variantCheck(element))
                    return@check false
                when (element) {
                    is CaosDefCommandWord, is CaosDefCommand -> element.text.uppercase() == "MVXX"
                    is CaosScriptVarToken -> varIndex == element.varIndex && element.varGroup == MVxx
                    else -> false
                }
            }
            UNKNOWN -> { _ -> false }
        }
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element == myElement) {
            return false
        }
        return check(element)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        val key = myElement.varGroup.value
        val variant = myElement.variant
            ?: return PsiElementResolveResult.createResults(myElement)
        val siblings: Collection<PsiElement> = myElement.getParentOfType(CaosScriptScriptElement::class.java)
            ?.collectElementsOfType(CaosScriptVarToken::class.java)
            ?.filter { otherVar ->
                check(otherVar)
            }
            .orEmpty()
        val commands = CaosDefCommandElementsByNameIndex
            .Instance[key, element.project]
            .filter {
                it.isVariant(variant)
            }
            .map { it.command }
        return PsiElementResolveResult.createResults(commands + siblings)
    }
}