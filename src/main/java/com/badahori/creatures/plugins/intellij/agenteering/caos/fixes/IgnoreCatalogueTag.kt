package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.actions.UndoableQuickFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.IgnoreCatalogueTag.IgnoreScope.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.HasIgnoredCatalogueTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.utils.settings
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class IgnoreCatalogueTag(
    private val tag: String,
    private val add: Boolean,
    private val ignoreIn: IgnoreScope = PROJECT
) : UndoableQuickFix() {


    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getFamilyName(): String {
        return CAOSScript
    }

    override fun getName(): String {
        return if (add) {
            CaosBundle.message("caos.fixes.ignore-catalogue.ignore.title", tag, ignoreIn.name.lowercase())
        } else {
            CaosBundle.message("caos.fixes.ignore-catalogue.un-ignore.title", tag, ignoreIn.name.lowercase())
        }
    }

    override fun undoFix(project: Project, descriptor: ProblemDescriptor) {
        val settings = when (ignoreIn) {
            PROJECT -> project.settings
            MODULE -> descriptor.psiElement.module?.settings
            APPLICATION -> CaosApplicationSettingsService.getInstance()
        } ?: return
        updateInSettings(settings, !add)
        reload(descriptor.psiElement.containingFile ?: descriptor.psiElement.originalElement?.containingFile)
    }

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val settings = when (ignoreIn) {
            PROJECT -> project.settings
            MODULE -> descriptor.psiElement.module?.settings
            APPLICATION -> CaosApplicationSettingsService.getInstance()
        } ?: return
        updateInSettings(settings, add)
        reload(descriptor.psiElement.containingFile ?: descriptor.psiElement.originalElement?.containingFile)
    }

    private fun reload(file: PsiFile?) {
        if (file == null) {
            return
        }
        runWriteAction {
            DaemonCodeAnalyzer.getInstance(file.project).restart(file)
        }
    }

    private fun updateInSettings(settings: HasIgnoredCatalogueTags, add: Boolean) {
        if (add) {
            if (tag in settings.ignoredCatalogueTags) {
                return
            }
            settings.ignoredCatalogueTags += tag
        } else if (tag in settings.ignoredCatalogueTags) {
            settings.ignoredCatalogueTags -= tag
        }
    }

    enum class IgnoreScope {
        PROJECT,
        MODULE,
        APPLICATION;
    }

}