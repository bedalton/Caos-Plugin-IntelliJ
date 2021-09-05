package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isDump
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.utils.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptIgnoredScriptsAfterRemovalScriptInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspections.scripts-after-removal-script.display-name")
    override fun getGroupDisplayName(): String = CAOSScript
    override fun getShortName(): String = CaosBundle.message("caos.inspections.scripts-after-removal-script.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitInstallScript(o: CaosScriptInstallScript) {
                annotate(o, holder)
                super.visitInstallScript(o)
            }

            override fun visitEventScript(o: CaosScriptEventScript) {
                annotate(o, holder)
                super.visitEventScript(o)
            }

            override fun visitMacro(o: CaosScriptMacro) {
                annotate(o, holder)
                super.visitMacro(o)
            }
        }
    }

    private fun annotate(script: CaosScriptScriptElement, problemsHolder: ProblemsHolder) {
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
        problemsHolder.registerProblem(script, CaosBundle.message("caos.inspections.scripts-after-removal-script.message"), MoveElementBeforeRemovalScript)
    }
}

private object MoveElementBeforeRemovalScript : LocalQuickFix {
    override fun getName(): String = CaosBundle.message("caos.inspections.fixes.move-script-above-removal-script")
    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")
    override fun startInWriteAction(): Boolean = true
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement.getSelfOrParentOfType(CaosScriptScriptBodyElement::class.java)
                ?: return
        val firstRemovalScript = PsiTreeUtil.collectElementsOfType(element.containingFile, CaosScriptScriptElement::class.java)
                .sortedBy {
                    it.startOffset
                }
                .firstOrNull {
                    it is CaosScriptRemovalScript
                }
                ?.getSelfOrParentOfType(CaosScriptScriptBodyElement::class.java)
                ?: return
        if (element == firstRemovalScript)
            return
        runUndoTransparentWriteAction {
            val copy = element.copy()
            element.delete()
            firstRemovalScript.parent.addBefore(copy, firstRemovalScript).let { newElement ->
                newElement.parent.addAfter(CaosScriptPsiElementFactory.newLines(project, 2), newElement)
            }
        }
    }
}