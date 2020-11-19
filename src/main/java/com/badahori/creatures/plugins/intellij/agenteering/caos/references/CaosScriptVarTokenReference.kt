package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandWord
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

/**
 * Find command variable reference
 */
class CaosScriptVarTokenReference(element: CaosScriptVarToken) : PsiPolyVariantReferenceBase<CaosScriptVarToken>(element, TextRange.create(0, element.text.length), true) {

    private val name: String by lazy {
        element.text.toUpperCase()
    }

    private val check: (element: PsiElement) -> Boolean by lazy {
        val variantCheck:(element:PsiElement)->Boolean = myElement.variant?.let { variant ->
            { element: PsiElement -> element.variant == variant }
        } ?: { true }
        val varGroup = myElement.varGroup
        val varIndex = myElement.varIndex
        when (varGroup) {
            VARx, VAxx -> check@{ element ->
                if (!variantCheck(element)) {
                    return@check false
                }
                if (element is CaosDefCommandWord || element is CaosDefCommand) {
                    LOGGER.info("Is '$name' reference to '${element.text}' in ${myElement.containingFile.name}")
                    element.text.toUpperCase().let { it == "VARX" || it == "VAXX" }
                } else if (element is CaosScriptVarToken) {
                    LOGGER.info("Is '$name' reference to '${element.text}' in ${myElement.containingFile.name}")
                    varIndex == element.varIndex && element.varGroup.let { it == VARx || it == VAxx }
                } else {
                    false
                }
            }
            OBVx, OVxx -> check@{ element ->
                if (!variantCheck(element))
                    return@check false
                if (element is CaosDefCommandWord || element is CaosDefCommand) {
                    LOGGER.info("Is '$name' reference to '${element.text}' in ${myElement.containingFile.name}")
                    element.text.toUpperCase().let { it == "OBVX" || it == "OVXX" }
                } else if (element is CaosScriptVarToken) {LOGGER.info("Is '$name' reference to '${element.text}' in ${myElement.containingFile.name}")
                    varIndex == element.varIndex && element.varGroup.let { it == OBVx || it == OVxx }
                } else {
                    false
                }
            }

            MVxx -> check@{ element ->
                if (!variantCheck(element))
                    return@check false
                if (element is CaosDefCommandWord || element is CaosDefCommand) {
                    LOGGER.info("Is '$name' reference to '${element.text}' in ${myElement.containingFile.name}")
                    element.text.toUpperCase() == "MVXX"
                } else if (element is CaosScriptVarToken) {
                    LOGGER.info("Is '$name' reference to '${element.text}' in ${myElement.containingFile.name}")
                    varIndex == element.varIndex && element.varGroup == MVxx
                } else {
                    false
                }
            }
            UNKNOWN -> { _ -> false }
        }
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return check(element)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        val key = myElement.varGroup.value
        val variant = myElement.variant
                ?: return PsiElementResolveResult.createResults(myElement)
        val commands = CaosDefCommandElementsByNameIndex
                .Instance[key, element.project]
                .filter {
                    it.isVariant(variant)
                }
                .map { it.command }
                .nullIfEmpty()
                ?: return PsiElementResolveResult.createResults(myElement)
        return PsiElementResolveResult.createResults(commands)
    }
}