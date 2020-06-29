package com.openc2e.plugins.intellij.agenteering.caos.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.openc2e.plugins.intellij.agenteering.caos.indices.CaosScriptConstAssignmentIndex
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptConstantAssignment
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedConstant
import com.openc2e.plugins.intellij.agenteering.caos.utils.hasParentOfType

class CaosScriptNamedConstReference(element:CaosScriptNamedConstant) : PsiPolyVariantReferenceBase<CaosScriptNamedConstant>(element, TextRange.create(1, element.textLength)) {

    private val isDeclaration by lazy {
        myElement.hasParentOfType(CaosScriptConstantAssignment::class.java)
    }

    private val name:String by lazy {
        element.name
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is CaosScriptNamedConstant) {
            return false
        }
        if (element.name != name)
            return false
        if (element.hasParentOfType(CaosScriptConstantAssignment::class.java) && isDeclaration) {
            return false
        }
        if (!element.hasParentOfType(CaosScriptConstantAssignment::class.java) && !isDeclaration) {
            return false
        }
        return element.containingFile.isEquivalentTo(myElement.containingFile)
    }

    override fun multiResolve(p0: Boolean): Array<ResolveResult> {
        val scope = GlobalSearchScope.fileScope(myElement.containingFile)
        val text = element.name
        val items = CaosScriptConstAssignmentIndex
                .instance[text, myElement.project,scope]
        return PsiElementResolveResult.createResults(items)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }
}