package com.openc2e.plugins.intellij.caos.def.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefTypeDefName
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefTypeDefinitionElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.containingCaosDefFile
import com.openc2e.plugins.intellij.caos.def.stubs.api.isVariant
import com.openc2e.plugins.intellij.caos.def.stubs.api.variants
import com.openc2e.plugins.intellij.caos.utils.orFalse

class CaosDefTypeNameReference(element: CaosDefTypeDefName) : PsiReferenceBase<CaosDefTypeDefName>(element, TextRange(1, element.textLength)) {


    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is CaosDefTypeDefName)
            return false
        return element.name == myElement.name && element.containingCaosDefFile.variants.intersect(element.containingCaosDefFile.variants).isNotEmpty()
    }


    override fun resolve(): PsiElement? {
        if (element.parent is CaosDefTypeDefinitionElement)
            return null
        return CaosDefTypeDefinitionElementsByNameIndex.Instance[element.name, element.project]
                .firstOrNull {
                    it.containingFile == element.containingFile
                }
                ?.typeDefName
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.setName(newElementName)
    }
}