package com.openc2e.plugins.intellij.caos.references

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommand
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandWord
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.caos.def.stubs.api.variants
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptIsCommandToken
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.CaosCommandType
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.getEnclosingCommandType

class CaosScriptCommandTokenReference(element: CaosScriptIsCommandToken) : PsiPolyVariantReferenceBase<CaosScriptIsCommandToken>(element, TextRange(0, element.text.length)) {

    private val renameRegex: Regex = "[a-zA-Z_][a-zA-Z_#!:]{3}".toRegex()

    private val name:String? by lazy {
        element.name
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        if (DumbService.isDumb(myElement.project))
            return emptyArray()
        if (myElement.parent?.parent is CaosDefCommandDefElement) {
            return PsiElementResolveResult.createResults(myElement)
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
        val variant = myElement.containingCaosFile.variant
        return CaosDefCommandElementsByNameIndex
                .Instance[formattedName, myElement.project]
                // Filter for type and variant
                .filter {
                    val isVariant = it.isVariant(variant)
                    if (!isVariant)
                        return@filter false
                    val isForElement = when (type) {
                        CaosCommandType.COMMAND -> it.isCommand
                        CaosCommandType.RVALUE -> it.isRvalue
                        CaosCommandType.LVALUE -> it.isLvalue
                        CaosCommandType.UNDEFINED -> it.isRvalue
                    }
                    if (!isForElement) {
                        if (!(it.isLvalue || it.isRvalue || it.isCommand)) {
                            LOGGER.severe("Command element '${it.commandName}' is unknown return type")
                            return@filter false
                        }
                    }
                    isForElement
                }
                .mapNotNull {
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