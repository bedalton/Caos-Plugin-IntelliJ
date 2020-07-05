package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedVarAssignmentIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedVarAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.hasParentOfType

class CaosScriptNamedVarReference(element: CaosScriptNamedVar) : PsiPolyVariantReferenceBase<CaosScriptNamedVar>(element, TextRange.create(1, element.textLength)) {

    private val isDeclaration by lazy {
        myElement.hasParentOfType(CaosScriptNamedVarAssignment::class.java)
    }

    private val name:String by lazy {
        element.name
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is CaosScriptNamedVar) {
            return false
        }
        if (element.name != name)
            return false
        if (element.hasParentOfType(CaosScriptNamedVarAssignment::class.java) && isDeclaration) {
            return false
        }
        if (!element.hasParentOfType(CaosScriptNamedVarAssignment::class.java) && !isDeclaration) {
            return false
        }
        return element.containingFile.isEquivalentTo(myElement.containingFile)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        val scope = GlobalSearchScope.fileScope(myElement.containingFile)
        val text = element.name
        val items = CaosScriptNamedVarAssignmentIndex
                .instance[text, myElement.project,scope]
        return PsiElementResolveResult.createResults(items)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }
}