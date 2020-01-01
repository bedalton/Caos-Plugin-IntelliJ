package com.openc2e.plugins.intellij.caos.def.references

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
import com.openc2e.plugins.intellij.caos.def.stubs.api.variants

class CaosDefCommandWordReference(private val element: CaosDefCommandWord) : PsiPolyVariantReferenceBase<CaosDefCommandWord>(element, TextRange(0, element.text.length)) {

    private val RenameRegex:Regex = "[a-zA-Z_][a-zA-Z_#!:]{3}".toRegex()

    override fun isReferenceTo(element: PsiElement): Boolean {
        return super.isReferenceTo(element)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {

        if (element.parent?.parent is CaosDefCommandDefElement)
            return emptyArray()
        val parentCommand = element.parent as? CaosDefCommand
                ?: return emptyArray()
        val index = parentCommand.commandWordList.indexOf(element)
        val raw = CaosDefCommandElementsByNameIndex
                .Instance[parentCommand.commandWordList.joinToString(" ") { it.text }, element.project]
        val variants = (element.containingFile as? CaosDefFile)?.variants
        val filtered = raw
                .filter {
                    it.variants.orEmpty().intersect(variants.orEmpty()).isNotEmpty()
                }.map {
                    it.command.commandWordList.getOrNull(index) ?: it.command
                }
                .filter { it != element }
        return PsiElementResolveResult.createResults(filtered)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        if (RenameRegex.matches(newElementName))
            return element.setName(newElementName)
        return element
    }

}