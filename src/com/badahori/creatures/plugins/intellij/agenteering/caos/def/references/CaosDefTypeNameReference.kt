package com.badahori.creatures.plugins.intellij.agenteering.caos.def.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefTypeDefName
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefTypeDefinitionElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.variantsIntersect
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.variants

class CaosDefTypeNameReference(element: CaosDefTypeDefName) : PsiReferenceBase<CaosDefTypeDefName>(element, TextRange(1, element.textLength)) {

    private val variants by lazy {
        myElement.containingCaosDefFile.variants
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is CaosDefTypeDefName)
            return false
        return element.name == myElement.name && element.variantsIntersect(variants)
    }


    override fun resolve(): PsiElement? {
        if (element.parent is CaosDefTypeDefinitionElement)
            return null
        return CaosDefTypeDefinitionElementsByNameIndex.Instance[element.name, element.project]
                .firstOrNull {
                    it.variantsIntersect(variants) && it.containingFile == element.containingFile
                }
                ?.typeDefName
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.setName(newElementName)
    }
}