package com.badahori.creatures.plugins.intellij.agenteering.bundles.general.actions

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.formatting.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosModuleSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.addIgnoredFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.removeIgnoredFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.utils.addIgnoredFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.removeIgnoredFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.rerunAnalyzer
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project


class AddIgnoredModuleLevelFile(private val fileName: String) : UndoableQuickFix() {
    override fun getFamilyName(): String = CAOSScript
    override fun getName(): String = "Ignore filename in current module"

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val module = descriptor.psiElement?.containingFile?.module
        if (module == null) {
            LOGGER.severe("Module is null on apply fix on AddIgnoredModuleLevel file")
            return
        }
        val service = CaosModuleSettingsService.getInstance(module)
            ?: return
        service.addIgnoredFile(fileName)
        val element = descriptor.psiElement
            ?: return
        rerunAnalyzer(element)
    }

    override fun undoFix(project: Project, descriptor: ProblemDescriptor) {
        val module = descriptor.psiElement?.containingFile?.module
        if (module == null) {
            LOGGER.severe("Module is null on apply fix on AddIgnoredModuleLevel file")
            return
        }
        val service = CaosModuleSettingsService.getInstance(module)
            ?: return
        service.removeIgnoredFile(fileName)
        val element = descriptor.psiElement
            ?: return
        rerunAnalyzer(element)
    }
}

class AddIgnoredProjectLevelFile(private val fileName: String) : UndoableQuickFix() {
    override fun getFamilyName(): String = CAOSScript
    override fun getName(): String = "Ignore filename in project"

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        if (project.isDisposed)
            return
        project.settings.addIgnoredFile(fileName)
        val element = descriptor.psiElement
            ?: return
        rerunAnalyzer(element)
    }

    override fun undoFix(project: Project, descriptor: ProblemDescriptor) {
        if (project.isDisposed)
            return
        project.settings.removeIgnoredFile(fileName)
        val element = descriptor.psiElement
            ?: return
        rerunAnalyzer(element)
    }

}





