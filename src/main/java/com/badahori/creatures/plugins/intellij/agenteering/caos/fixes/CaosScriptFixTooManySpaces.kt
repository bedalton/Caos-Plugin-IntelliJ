package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager

class CaosScriptFixTooManySpaces(spaces: PsiElement) : IntentionAction, LocalQuickFix {

    private val spacesPointer = SmartPointerManager.createPointer(spaces)

    override fun startInWriteAction(): Boolean {
        return true
    }

    override fun getFamilyName(): String = CAOSScript

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is CaosScriptFile
    }

    override fun getText(): String = CaosBundle.message("caos.fixes.remove-extra-spaces")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val element = spacesPointer.element
            ?: return
        apply(file, element)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
            ?: return
        apply(element.containingFile, element)
    }

    fun apply(file: PsiFile?, space: PsiElement) {
        if (file == null)
            return
        var hasComma = false
        var hasNewline = false
        val siblings = mutableListOf(space)
        var sibling = space.previous
        while (sibling != null && WHITE_SPACE_OR_COMMAS.matches(sibling.text)) {
            if (sibling.text.contains(","))
                hasComma = true
            else if (sibling.text.contains("\n")) {
                hasNewline = true
                //break
            }
            siblings.add(0, sibling)
            sibling = sibling.previous
        }
        val trueNext = space.next
        sibling = trueNext
        while (sibling != null && WHITE_SPACE_OR_COMMAS.matches(sibling.text)) {
            if (sibling.text.contains(","))
                hasComma = true
            else if (sibling.text.contains("\n")) {
                hasNewline = true
                sibling = sibling.next
                break
            }
            siblings.add(sibling)
            sibling = sibling.next
        }
        // Needs this check, as WHITE_SPACE or comma will not catch new lines
        // Check should not match newlines, as it also matches the following tabs and spaces which are valid in c3+
        if (sibling?.text?.contains("\n").orFalse()) {
            hasNewline = true
        }
        if (siblings.isEmpty() && trueNext != null) {
            return
        }
        val hasNext = sibling?.text?.trim(' ','\t','\n',',')?.isNotEmpty().orFalse()
        val replacement = when {
            sibling == null -> ""
            hasNewline -> ""
            hasComma -> ","
            hasNext -> " "
            else -> ""
        }
        file.document?.replaceString(siblings.first().textRange.startOffset, siblings.last().textRange.endOffset, replacement)
    }

    companion object {
        private val WHITE_SPACE_OR_COMMAS = "[ ,\t\n]+".toRegex()
    }

}