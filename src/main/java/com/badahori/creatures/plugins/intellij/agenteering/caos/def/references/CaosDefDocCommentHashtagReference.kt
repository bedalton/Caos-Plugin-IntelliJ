package com.badahori.creatures.plugins.intellij.agenteering.caos.def.references

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefHashTagsIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefDocCommentHashtag
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant

class CaosDefDocCommentHashtagReference(hashtag: CaosDefDocCommentHashtag)
    : PsiPolyVariantReferenceBase<CaosDefDocCommentHashtag>(hashtag, TextRange.create(1, hashtag.name.length)) {

    private val hashtag by lazy { hashtag.name }

    private val variants:List<CaosVariant> by lazy {
        myElement.variants
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        val otherHashtag = element as? CaosDefDocCommentHashtag
                ?: return false
        return otherHashtag.name != hashtag && otherHashtag.isVariant(variants, true)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        val project = myElement.project
        if (project.isDisposed || DumbService.isDumb(project)) {
            return emptyArray()
        }
        val variants = variants
        return PsiElementResolveResult.createResults(CaosDefHashTagsIndex.Instance[hashtag, project].filter {
            it.isVariant(variants, true)
        })
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }

}