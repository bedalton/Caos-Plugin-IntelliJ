package com.openc2e.plugins.intellij.caos.def.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefTypeDefName
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefTypeDefinitionElement

class CaosDefTypeNameReference(element: CaosDefTypeDefName) : PsiReferenceBase<CaosDefTypeDefName>(element, TextRange(1, element.textLength)) {

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