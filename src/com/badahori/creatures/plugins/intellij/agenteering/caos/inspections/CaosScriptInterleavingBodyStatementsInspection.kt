package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptMacro
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptBodyElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosInjectorNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptInterleavingBodyStatements : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspections.interleaving-body-scripts.display-name")
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = CaosBundle.message("caos.inspections.interleaving-body-scripts.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitMacro(o: CaosScriptMacro) {
                super.visitMacro(o)
                annotate(o, holder)
            }
        }
    }

    private fun annotate(script: CaosScriptScriptElement, problemsHolder: ProblemsHolder) {
        val file = script.containingCaosFile
                ?: return
        if (file.variant?.isOld.orFalse())
            return
        val thisScriptStart = script.startOffset
        val isNotInterleavedBodyScript =
                PsiTreeUtil.collectElementsOfType(file, CaosScriptScriptElement::class.java)
                        .filter { it !is CaosScriptMacro }
                        .none {
                            it.startOffset < thisScriptStart
                        }
        if (isNotInterleavedBodyScript) {
            return
        }
        problemsHolder.registerProblem(script, CaosBundle.message("caos.inspections.interleaving-body-scripts.message"), CombineBodyScriptsToTopOfFile)
    }
}

private object CombineBodyScriptsToTopOfFile : LocalQuickFix {
    override fun getName(): String = CaosBundle.message("caos.inspections.fixes.move-body-script-to-top")
    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")
    override fun startInWriteAction(): Boolean = true
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement.containingFile
        val allBodyElements = PsiTreeUtil.collectElementsOfType(file, CaosScriptScriptElement::class.java)
                .sortedBy { it.startOffset }

        val firstNonMacro = allBodyElements.firstOrNull { it !is CaosScriptMacro }
                ?: return
        val firstNonMacroStart = firstNonMacro.startOffset
        val macros = allBodyElements
                .filterIsInstance<CaosScriptMacro>()
                .filter { it.startOffset > firstNonMacroStart }
                .sortedByDescending { it.startOffset }
                .map {
                    SmartPointerManager.createPointer(it)
                }
        val didMove = runUndoTransparentWriteAction action@{
            var previous = firstNonMacro.parent as? CaosScriptScriptBodyElement
                    ?: return@action false
            for (macro in macros) {
                previous = macro.element?.getParentOfType(CaosScriptScriptBodyElement::class.java)?.let { parent ->
                    val parentCopy = parent.copy()
                    parent.delete()
                    previous.parent.addBefore(parentCopy, previous)?.getSelfOrParentOfType(CaosScriptScriptBodyElement::class.java)?.apply{
                        previous.parent.addAfter(CaosScriptPsiElementFactory.newLines(project, 2), this)
                    }
                } ?: return@action false
            }
            val newline = CaosScriptPsiElementFactory.newLines(project, 1)
            firstNonMacro.getParentOfType(CaosScriptScriptBodyElement::class.java)?.let {
                it.parent.addBefore(newline, it)
            }
            (file.document ?: file.originalFile.document)?.let { document ->
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
                CodeStyleManager.getInstance(project).reformat(file, false)
            }
            return@action true
        }
        if (!didMove) {
            CaosInjectorNotifications.show(project, "Reformatting Failed", "Failed to move all body code blocks to head of file", NotificationType.ERROR)
        }
    }
}