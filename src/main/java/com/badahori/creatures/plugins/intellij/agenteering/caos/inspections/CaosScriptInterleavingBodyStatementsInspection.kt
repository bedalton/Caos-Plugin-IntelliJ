package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isDump
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.utils.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.previous
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInspection.*
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptInterleavingBodyStatements : LocalInspectionTool() {

    override fun getDisplayName(): String =
        CaosBundle.message("caos.inspections.interleaving-body-scripts.display-name")

    override fun getGroupDisplayName(): String = CAOSScript
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
     * Annotates a Macro script if necessary it is found between event scripts
     */
    private fun annotate(thisScript: CaosScriptScriptElement, problemsHolder: ProblemsHolder) {
        val file = thisScript.containingCaosFile
            ?: return
        if (file.isDump)
            return
        val isCaos2Cob = file.variant?.isOld.orFalse() && thisScript.containingCaosFile?.isCaos2Cob.orFalse()
        if (file.variant?.isOld.orFalse() && !isCaos2Cob) {
            return
        }
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
            .filter { codeBlockLine -> codeBlockLine.firstChild !is PsiComment }
            .forEach each@{ line ->
                if (isCaos2Cob) {
                    problemsHolder.registerProblem(
                        line,
                        "CAOS commands outside of ISCR, RSCR and SCRP scripts are ignored by CAOS2Cob",
                        ProblemHighlightType.WARNING,
                        CombineBodyScriptsIntoInstallScript,
                        CombineBodyScriptsIntoRemovalScript
                    )
                    return@each
                }
                problemsHolder.registerProblem(
                    line,
                    CaosBundle.message("caos.inspections.interleaving-body-scripts.message"),
                    CombineBodyScriptsToTopOfFile
                )
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
            for (macro in macros) {
                ProgressIndicatorProvider.checkCanceled()
                previous = macro.element?.getParentOfType(CaosScriptScriptBodyElement::class.java)?.let { parent ->
                    val parentCopy = parent.copy()
                    parent.delete()
                    previous.parent.addBefore(parentCopy, previous)
                        ?.getSelfOrParentOfType(CaosScriptScriptBodyElement::class.java)?.apply {
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
            CaosNotifications.showError(
                project,
                "Reformatting Failed",
                "Failed to move all body code blocks to head of file"
            )
        }
    }
}

private val CombineBodyScriptsIntoInstallScript = CombineBodyScriptsToScript("iscr", "an install")
private val CombineBodyScriptsIntoRemovalScript = CombineBodyScriptsToScript("rscr", "a removal")

private class CombineBodyScriptsToScript(private val tag: String, private val commonName: String) : LocalQuickFix {
    override fun getName(): String =
        CaosBundle.message("caos.inspections.fixes.move-body-scripts-to-script", commonName)

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")
    override fun startInWriteAction(): Boolean = true
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement.containingFile
        val allBodyElements = PsiTreeUtil.collectElementsOfType(file, CaosScriptScriptElement::class.java)
            .sortedBy { it.startOffset }

        val macros = allBodyElements
            .filterIsInstance<CaosScriptMacro>()
            .sortedBy { it.startOffset }
            .map {
                SmartPointerManager.createPointer(it)
            }

        val start = macros.mapNotNull { it.element?.startOffset }.minOrNull()
            ?: return

        val firstNonMacro = allBodyElements.filterNot { it is CaosScriptMacro && it.endOffset < start }.maxByOrNull { it.startOffset }
            ?: return
        var baseScript = CaosScriptPsiElementFactory.createScriptElement(project, tag)?.let {
            if (tag == "iscr")
                firstNonMacro.parent.addBefore(it, firstNonMacro)
            else
                firstNonMacro.parent.add(it)
        } ?: return
        val baseScriptPointer = SmartPointerManager.createPointer(baseScript)
        val didMove = runUndoTransparentWriteAction action@{
            var previous = baseScript.firstChild.next
                ?: return@action false
            for (macro in macros) {
                ProgressIndicatorProvider.checkCanceled()
                previous = macro.element?.getParentOfType(CaosScriptScriptBodyElement::class.java)?.let prev@{ parent ->
                    PsiTreeUtil.collectElementsOfType(parent, CaosScriptScriptTerminator::class.java)
                        .forEach { terminator ->
                            val spacePointer = (terminator.next as? CaosScriptWhiteSpaceLike)?.let { SmartPointerManager.createPointer(it)}
                            terminator.delete()
                            spacePointer?.element?.delete()
                        }
                    val parentCopy = parent.copy()
                    parent.delete()
                    if (parentCopy.text?.trim() like "endm")
                        return@prev previous
                    baseScript.addAfter(parentCopy, previous)?.getSelfOrParentOfType(CaosScriptScriptBodyElement::class.java)?.let {
                        baseScript.addAfter(CaosScriptPsiElementFactory.newLine(project), it)
                    }
                } ?: return@action false
                macro.element?.scriptTerminator?.delete()
            }
            val newline = CaosScriptPsiElementFactory.newLines(project, 2)
            baseScript = baseScriptPointer.element
                ?: return@action false
            baseScript.parent.addAfter(newline, baseScript)
            (file.document ?: file.originalFile.document)?.let { document ->
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
                CodeStyleManager.getInstance(project).reformat(file, false)
            }

            baseScript = baseScriptPointer.element
                ?: return@action false
            // Remove all lead
            val previousSpace:PsiElement? = baseScript.previous?.previous
            if (previousSpace?.tokenType in CaosScriptTokenSets.WHITESPACES) {
                previousSpace?.delete()
            }
            (file.document ?: file.originalFile.document)?.let { document ->
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
                CodeStyleManager.getInstance(project).reformat(file, false)
            }
            runWriteAction {
                DaemonCodeAnalyzer.getInstance(project).restart(file)
            }
            return@action true
        }
        if (!didMove) {
            CaosNotifications.showError(
                project,
                "Reformatting Failed",
                "Failed to move all body code blocks into $commonName script"
            )
        }
    }
}