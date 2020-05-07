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
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptIsCommandToken

class CaosScriptCommandTokenReference(private val element: CaosScriptIsCommandToken) : PsiPolyVariantReferenceBase<CaosScriptIsCommandToken>(element, TextRange(0, element.text.length)) {

    private val renameRegex: Regex = "[a-zA-Z_][a-zA-Z_#!:]{3}".toRegex()

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        if (DumbService.isDumb(element.project))
            return emptyArray()
        if (element.parent?.parent is CaosDefCommandDefElement)
            return emptyArray()
        var elements = if (element is CaosDefCompositeElement)
            findFromDefElement()
        else
            findFromScriptElement()
        val commandText = element.name
                ?: return emptyArray()
        elements = elements
                .filter {
                    it.name.toLowerCase() == commandText.toLowerCase()
                }
        return PsiElementResolveResult.createResults(elements)
    }

    private fun findFromDefElement(): List<CaosDefCommandWord> {
        val parentCommand = element.parent as? CaosDefCommand
                ?: return emptyList()
        val index = parentCommand.commandWordList.indexOf(element)
        val variants = (element.containingFile as? CaosDefFile)?.variants
        return CaosDefCommandElementsByNameIndex
                .Instance[parentCommand.commandWordList.joinToString(" ") { it.text }, element.project]
                .filter {
                    it.variants.intersect(variants.orEmpty()).isNotEmpty()
                }.mapNotNull {
                    it.command.commandWordList.getOrNull(index)
                }
                .filter { it != element }
    }

    private fun findFromScriptElement(): List<CaosDefCommandWord> {
        val formattedName = element.name?.replace(EXTRA_SPACE_REGEX, " ")
                ?: return emptyList()
        return CaosDefCommandElementsByNameIndex
                .Instance[formattedName, element.project].mapNotNull {
            it.command.commandWordList.getOrNull(0)
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        if (renameRegex.matches(newElementName))
            return element.setName(newElementName)
        return element
    }

    companion object {
        val EXTRA_SPACE_REGEX = "\\s\\s+".toRegex()
    }

}