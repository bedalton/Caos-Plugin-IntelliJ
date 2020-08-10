package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.variants
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getEnclosingCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.equalsIgnoreCase
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CaosScriptCommandTokenReference(element: CaosScriptIsCommandToken) : PsiPolyVariantReferenceBase<CaosScriptIsCommandToken>(element, TextRange(0, element.text.length)) {

    private val name: String by lazy {
        element.name ?: "{{UNDEF}}"
    }

    private val variants:List<CaosVariant> by lazy {
        if (myElement is CaosDefCompositeElement)
            myElement.variants
        else if (myElement is CaosScriptCompositeElement)
            myElement.containingCaosFile?.variant?.let { listOf(it) } ?: emptyList()
        else
            emptyList()
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element is CaosScriptVarToken && element.varGroup.value.toUpperCase() == name.toUpperCase()) {
            return true
        }
        if (element.text.equalsIgnoreCase(name)) {
            return if (element is CaosDefCompositeElement)
                element.variantsIntersect(variants)
            else if (element is CaosScriptCompositeElement)
                element.containingCaosFile?.variant in variants
            else
                false
        }
        return super.isReferenceTo(element)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        if (DumbService.isDumb(myElement.project))
            return emptyArray()
        if (myElement.parent?.parent is CaosDefCommandDefElement) {
            return PsiElementResolveResult.EMPTY_ARRAY
        }
        val elements = if (myElement is CaosDefCompositeElement)
            findFromDefElement()
        else
            findFromScriptElement()
        return PsiElementResolveResult.createResults(elements)
    }

    private fun findFromDefElement(): List<CaosDefCommandWord> {
        val parentCommand = myElement.parent as? CaosDefCommand
                ?: return emptyList()
        val index = parentCommand.commandWordList.indexOf(myElement)
        val variants = (myElement.containingFile as? CaosDefFile)?.variants
        return CaosDefCommandElementsByNameIndex
                .Instance[parentCommand.commandWordList.joinToString(" ") { it.text }, myElement.project]
                .filter {
                    ProgressIndicatorProvider.checkCanceled()
                    it.variants.intersect(variants.orEmpty()).isNotEmpty()
                }.mapNotNull {
                    it.command.commandWordList.getOrNull(index)
                }
                .filter { it != myElement }
    }

    private fun findFromScriptElement(): List<CaosDefCommandWord> {
        val type = myElement.getEnclosingCommandType()
        val formattedName = name?.replace(EXTRA_SPACE_REGEX, " ")
                ?: return emptyList()
        val variant = myElement.containingCaosFile?.variant
                ?: return emptyList()
        val matches = CaosDefCommandElementsByNameIndex
                .Instance[formattedName, myElement.project]
                // Filter for type and variant
                .filter {
                    ProgressIndicatorProvider.checkCanceled()
                    val isVariant = it.isVariant(variant)
                    if (!isVariant)
                        return@filter false
                    val isForElement = when (type) {
                        CaosCommandType.COMMAND -> it.isCommand
                        CaosCommandType.CONTROL_STATEMENT -> it.isCommand
                        CaosCommandType.RVALUE -> it.isRvalue
                        CaosCommandType.LVALUE -> it.isLvalue
                        CaosCommandType.UNDEFINED -> it.isRvalue
                    }
                    if (!isForElement) {
                        if (!(it.isLvalue || it.isRvalue || it.isCommand)) {
                            return@filter false
                        }
                    }
                    isForElement
                }
        if (matches.size <= 1)
            return matches
                    .mapNotNull {
                        it.command.commandWordList.getOrNull(0)
                    }
        val parentArgument = myElement.getParentOfType(CaosScriptArgument::class.java)
                ?: return matches
                        .mapNotNull {
                            it.command.commandWordList.getOrNull(0)
                        }
        val out = if (parentArgument is CaosScriptExpectsInt) {
            matches.filter { it.returnTypeString.toLowerCase().startsWith("int") }
        } else if (parentArgument is CaosScriptExpectsDecimal) {
            matches.filter { it.returnTypeString.toLowerCase() == "decimal" || it.returnTypeString.toLowerCase().startsWith("int") || it.returnTypeString.toLowerCase() == "float" }
        } else if (parentArgument is CaosScriptExpectsFloat) {
            matches.filter { it.returnTypeString.toLowerCase() == "decimal" || it.returnTypeString.toLowerCase().startsWith("int") || it.returnTypeString.toLowerCase() == "float" }
        } else if (parentArgument is CaosScriptExpectsQuoteString) {
            matches.filter { it.returnTypeString.toLowerCase() == "string"}
        } else {
            matches
        }
        return out.mapNotNull {
            it.command.commandWordList.getOrNull(0)
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        //if (renameRegex.matches(newElementName))
        //  return myElement.setName(newElementName)
        return myElement
    }

    companion object {
        val EXTRA_SPACE_REGEX = "\\s\\s+".toRegex()
    }

}