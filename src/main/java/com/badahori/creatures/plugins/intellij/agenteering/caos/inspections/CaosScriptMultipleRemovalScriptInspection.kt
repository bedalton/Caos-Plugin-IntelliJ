package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isDump
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRemovalScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptMultipleRemovalScriptInspection : LocalInspectionTool(), DumbAware {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspections.multiple-removal-scripts.display-name")
    override fun getGroupDisplayName(): String = CAOSScript
    override fun getShortName(): String = CaosBundle.message("caos.inspections.multiple-removal-scripts.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitRemovalScript(o: CaosScriptRemovalScript) {
                annotate(o, holder)
                super.visitRemovalScript(o)
            }
        }
    }

    private fun annotate(script: CaosScriptRemovalScript, problemsHolder: ProblemsHolder) {
        val file = script.containingCaosFile
                ?: return
        if (file.isDump)
            return
        if (file.variant?.isOld.orFalse())
            return
        val thisScriptStart = script.startOffset
        val isNotDeclaredAfterEventScript =
                PsiTreeUtil.collectElementsOfType(file, CaosScriptRemovalScript::class.java)
                        .none {
                            it.startOffset < thisScriptStart
                        }
        if (isNotDeclaredAfterEventScript) {
            return
        }
        problemsHolder.registerProblem(script, CaosBundle.message("caos.inspections.multiple-removal-scripts.message"), DeleteRemovalScript)
    }
}

private object DeleteRemovalScript : LocalQuickFix {
    override fun getName(): String = CaosBundle.message("caos.inspections.fixes.delete-extra-removal-script")
    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")
    override fun startInWriteAction(): Boolean = true
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement.getSelfOrParentOfType(CaosScriptRemovalScript::class.java)
                ?: return
        element.delete()
    }
}