package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrBlank
import com.badahori.creatures.plugins.intellij.agenteering.utils.rerunAnalyzer
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class DisableSpellcheckForCommandFix(command: String, private val disable: Boolean) : LocalQuickFix, IntentionAction {

    private val command: String = command.uppercase()

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getText(): String {
        return if (disable) {
            CaosBundle.message("caos.annotator.spellcheck.disable.command", command)
        } else {
            CaosBundle.message("caos.annotator.spellcheck.enable.command", command)
        }
    }

    override fun getFamilyName(): String {
        return CaosBundle.message("caos.intentions.family")
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(command, descriptor.psiElement, disable)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        applyFix(command, file, disable)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return if (command.isNotNullOrBlank()) {
            false
        } else if (disable) {
            command.uppercase() !in CaosApplicationSettingsService
                .getInstance()
                .noSpellcheckCommands
        } else {
            command.uppercase() in CaosApplicationSettingsService
                .getInstance()
                .noSpellcheckCommands
        }
    }

    fun applyFix(command: String, element: PsiElement?, add: Boolean) {
        val settings = CaosApplicationSettingsService.getInstance()
        if (add) {
            settings.noSpellcheckCommands += command
        } else {
            settings.noSpellcheckCommands -= command
        }
        if (element != null) {
            rerunAnalyzer(element)
        }
    }

}