package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCharacter
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.utils.isOrHasParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.primaryCursorElement
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class IgnoreCharacterEscapeFix(private val add: Boolean, val char: Char) : LocalQuickFix, IntentionAction {

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getText(): String {
        return if (add) {
            CaosBundle.message("caos.annotator.syntax-error-annotator.char-escape-invalid.ignore-fix", char)
        } else {
            CaosBundle.message("caos.annotator.syntax-error-annotator.char-escape-invalid.un-ignore-fix", char)
        }
    }

    override fun getFamilyName(): String {
        return CaosBundle.message("caos.intentions.family")
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(add, char)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        applyFix(add, char)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        val element = editor
            ?.primaryCursorElement
            ?: return true
        return element.isOrHasParentOfType(CaosScriptCharacter::class.java)
    }


    fun applyFix(add: Boolean, char: Char) {
        val settings = CaosApplicationSettingsService.getInstance()
        if (add) {
            settings.ignoredCharacterEscapes += char
        } else {
            settings.ignoredCharacterEscapes -= char
        }
    }



//    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
//
//        if (file == null) {
//            return
//        }
//        val carats = editor?.caretModel
//            ?.allCarets
//            ?.nullIfEmpty()
//            ?: return
//
//        for (carat in carats) {
//            if (!carat.isValid) {
//                continue
//            }
//            val element = file.findElementAt(carat.offset)
//                ?: continue
//            applyFix(element)
//        }
//    }
//
//    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
//        val element = descriptor.psiElement
//            ?: return
//        applyFix(element)
//    }
//
//    private fun applyFix(element: PsiElement?) {
//        if (element == null || !element.isValid) {
//            return
//        }
//        val text = element
//            .getSelfOrParentOfType(CaosScriptCharacter::class.java)
//            ?.stringValue
//            ?: return
//        if (text[0] != '\\' || text.length < 2) {
//            return
//        }
//        if (text[0] in VALID_ESCAPE_CHARS) {
//            return
//        }
//        val settings = CaosApplicationSettingsService.getInstance()
//        applyFix(add, text[1])
//    }

}