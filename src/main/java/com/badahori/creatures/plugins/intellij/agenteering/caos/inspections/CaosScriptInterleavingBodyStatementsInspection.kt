package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
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

    /**
     * Annotates a Macro script if necessary if it is found between event scripts
     */
    private fun annotate(thisScript: CaosScriptScriptElement, problemsHolder: ProblemsHolder) {
        val file = thisScript.containingCaosFile
                ?: return
        if (file.variant?.isOld.orFalse())
            return
        val thisScriptStart = thisScript.startOffset
        val isNotInterleavedBodyScript =
                PsiTreeUtil.collectElementsOfType(file, CaosScriptScriptElement::class.java)
                        .filter { aScript -> aScript !is CaosScriptMacro }
                        .none { macro ->
                            macro.startOffset < thisScriptStart
                        }
        if (isNotInterleavedBodyScript) {
            return
        }
        PsiTreeUtil.collectElementsOfType(thisScript, CaosScriptCodeBlockLine::class.java)
                .filter { codeBlockLine -> codeBlockLine.firstChild !is CaosScriptComment }
                .forEach { line ->
                    problemsHolder.registerProblem(line, CaosBundle.message("caos.inspections.interleaving-body-scripts.message"), CombineBodyScriptsToTopOfFile)
                }

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
            val firstMacro = macros.firstOrNull()
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
            CaosNotifications.showError(project, "Reformatting Failed", "Failed to move all body code blocks to head of file")
        }
    }
}