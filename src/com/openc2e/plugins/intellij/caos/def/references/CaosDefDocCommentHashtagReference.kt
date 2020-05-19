package com.openc2e.plugins.intellij.caos.def.references

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefHashTagsIndex
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefDocCommentHashtag

class CaosDefDocCommentHashtagReference(hashtag: CaosDefDocCommentHashtag)
    : PsiPolyVariantReferenceBase<CaosDefDocCommentHashtag>(hashtag, TextRange.create(1, hashtag.name.length)) {

    private val hashtag by lazy { hashtag.name }

    private val variants:List<String> by lazy {
        myElement.variants
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        val otherHashtag = element as? CaosDefDocCommentHashtag
                ?: return false
        return otherHashtag.name != hashtag && otherHashtag.isVariant(variants, true)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        val project = myElement.project
        if (DumbService.isDumb(project)) {
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