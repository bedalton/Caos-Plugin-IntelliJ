package com.badahori.creatures.plugins.intellij.agenteering.caos.def.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListName
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.variantsIntersect
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class CaosDefValuesListNameReference(element: CaosDefValuesListName) : PsiReferenceBase<CaosDefValuesListName>(element, TextRange(1, element.textLength)) {

    private val variants by lazy {
        myElement.containingCaosDefFile.variants
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is CaosDefValuesListName)
            return false
        return element.name == myElement.name && element.variantsIntersect(variants)
    }


    override fun resolve(): PsiElement? {
        if (element.parent is CaosDefValuesListElement)
            return null
        return CaosDefValuesListElementsByNameIndex.Instance[element.name, element.project]
                .firstOrNull {
                    it.variantsIntersect(variants) && it.containingFile == element.containingFile
                }
                ?.valuesListName
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.setName(newElementName)
    }
}