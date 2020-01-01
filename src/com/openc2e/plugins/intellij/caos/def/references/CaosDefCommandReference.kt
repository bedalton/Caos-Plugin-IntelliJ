package com.openc2e.plugins.intellij.caos.def.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommand
import com.openc2e.plugins.intellij.caos.def.stubs.api.variants

class CaosDefCommandReference(private val element: CaosDefCommand) : PsiPolyVariantReferenceBase<CaosDefCommand>(element, TextRange(0, element.text.length)) {

    override fun isReferenceTo(element: PsiElement): Boolean {
        return super.isReferenceTo(element)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        val raw = CaosDefCommandElementsByNameIndex
                .Instance[element.commandWordList.joinToString(" ") { it.text }, element.project]
        val variants = (element.containingFile as? CaosDefFile)?.variants
        val filtered = raw
                .filter {
                    it.variants.orEmpty().intersect(variants.orEmpty()).isNotEmpty()
                }.map {
                    it.command
                }
                .filter { it != element }
        return PsiElementResolveResult.createResults(filtered)
    }

}