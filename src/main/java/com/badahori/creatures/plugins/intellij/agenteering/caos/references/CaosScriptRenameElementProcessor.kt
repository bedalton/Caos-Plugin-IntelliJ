package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedGameVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutineName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.nullIfUndefOrBlank
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenamePsiElementProcessor

class CaosScriptRenameElementProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is CaosScriptSubroutine
                || element is CaosScriptSubroutineName
                || element is CaosScriptNamedGameVar
    }

    override fun isInplaceRenameSupported(): Boolean {
        return true
    }

    override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? {
        if (element is CaosScriptSubroutineName) {
            val name = element.name.nullIfUndefOrBlank()
                    ?: return null
            val containingScript = element.getParentOfType(CaosScriptScriptElement::class.java)
                    ?: return null
            return PsiTreeUtil.collectElementsOfType(containingScript, CaosScriptSubroutine::class.java)
                    .filter {
                        it.name == name
                    }
                    .mapNotNull {
                        it.subroutineHeader.subroutineName
                    }
                    .firstOrNull()
        }
        if (element is CaosScriptSubroutine) {
            return element.subroutineHeader.subroutineName
        }
        return super.substituteElementToRename(element, editor)
    }
}