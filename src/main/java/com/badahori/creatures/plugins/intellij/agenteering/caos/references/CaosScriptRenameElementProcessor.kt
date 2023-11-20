package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.nullIfUndefOrBlank
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

class CaosScriptRenameElementProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is CaosScriptSubroutine
                || element is CaosScriptSubroutineName
                || element is CaosScriptNamedGameVar
                || element is CaosScriptQuoteStringLiteral
                || element is CaosScriptStringLike
    }

    override fun isInplaceRenameSupported(): Boolean {
        return true
    }

    override fun findCollisions(
        element: PsiElement,
        newName: String,
        allRenames: MutableMap<out PsiElement, String>,
        result: MutableList<UsageInfo>
    ) {
        super.findCollisions(element, newName, allRenames, result)
        val containingScript = element.getParentOfType(CaosScriptScriptElement::class.java)
            ?: return
        return PsiTreeUtil.collectElementsOfType(containingScript, CaosScriptSubroutine::class.java)
            .filter {
                it.name == newName && it.subroutineHeader.subroutineName != null
            }.forEach{
                result.add(UsageInfo(it.subroutineHeader.subroutineName ?: it.subroutineHeader))
            }
    }

    override fun findExistingNameConflicts(
        element: PsiElement,
        newName: String,
        conflicts: MultiMap<PsiElement, String>
    ) {
        super.findExistingNameConflicts(element, newName, conflicts)
        val containingScript = element.getParentOfType(CaosScriptScriptElement::class.java)
            ?: return
        PsiTreeUtil.collectElementsOfType(containingScript, CaosScriptSubroutine::class.java)
            .filter {
                it.name == newName
            }.forEach each@{
                val name = it.subroutineHeader.subroutineName
                    ?: return@each
                conflicts.put(name, listOf(name.text))
            }
    }

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean
    ): Collection<PsiReference> {
        return super.findReferences(element, searchScope, searchInCommentsAndStrings)
            .distinct()

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
                }.firstNotNullOfOrNull {
                    it.subroutineHeader.subroutineName
                }
        }
        if (element is CaosScriptSubroutine) {
            return element.subroutineHeader.subroutineName
        }
        if (element is CaosScriptQuoteStringLiteral) {
            return element
        }
        return super.substituteElementToRename(element, editor)
    }


}