package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil

class Caos2CobMoveFileToCommandFix(element: PsiElement, private val command: CobCommand) : IntentionAction,
    LocalQuickFix {

    private val fileName = (element.text.trim('\n', '\r', '\t', ' ', '"'))
    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("cob.caos2cob.inspections.group")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return pointer.element != null && file is CaosScriptFile
    }

    override fun getText(): String =
        CaosBundle.message("cob.caos2cob.fixes.move-file-to-command", fileName, command.keyString)

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val element = pointer.element
            ?: return
        applyFix(element)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(descriptor.psiElement)
    }

    private fun applyFix(element: PsiElement) {
        val project = element.project
        val documentManager = PsiDocumentManager.getInstance(project)
        val block = element
            .getSelfOrParentOfType(CaosScriptCaos2Block::class.java)
            ?: return
        val containingCommandPointer = element.getParentOfType(CaosScriptCaos2Command::class.java)?.let {
            SmartPointerManager.createPointer(it)
        } ?: return
        val commandPointer = PsiTreeUtil.collectElementsOfType(block, CaosScriptCaos2Command::class.java)
            .lastOrNull { CobCommand.fromString(it.commandName) == command }
            ?.let {
                SmartPointerManager.createPointer(it)
            }

        val text = element.text
        element.delete()
        var document = containingCommandPointer.element?.document
            ?: return
        documentManager.doPostponedOperationsAndUnblockDocument(document)
        document = containingCommandPointer.element?.document!!
        if (commandPointer == null) {
            val lastCommand = PsiTreeUtil
                .collectElementsOfType(block, CaosScriptCaos2::class.java)
                .lastOrNull()
            val case = lastCommand
                ?.text
                ?.case
                ?: Case.CAPITAL_FIRST
            var prefix = "\n*# "
            val insertionPoint = if (lastCommand != null) {
                lastCommand.endOffset
            } else {
                val index = document.text.indexOf("*#")
                if (index < 0)
                    block.endOffset
                else {
                    prefix = " "
                    index
                }
            }
            EditorUtil
                .insertText(document, prefix + command.keyString.matchCase(case, element.variant) + " " + element.text, insertionPoint)
        } else {
            val commandElement = commandPointer.element!!
            val commandRange = commandElement.textRange
            EditorUtil.insertText(document, " $text", commandRange.endOffset)
        }

        val containingCommand = containingCommandPointer.element
            ?: return
        document = containingCommand.document
            ?: return
        documentManager.doPostponedOperationsAndUnblockDocument(document)
        if (containingCommand.commandArgs.isEmpty()) {
            val parentPointer = containingCommandPointer
                .element
                ?.getParentOfType(CaosScriptCaos2BlockComment::class.java)
                ?.let {
                    SmartPointerManager.createPointer(it)
                } ?: return
            val nextPointer = (parentPointer.element?.next as? CaosScriptSpaceLikeOrNewline)?.let {
                SmartPointerManager.createPointer(it)
            }
            document = parentPointer.element?.document
                ?: return
            documentManager.commitDocument(document)
            documentManager.doPostponedOperationsAndUnblockDocument(document)
            parentPointer.element?.delete()
            nextPointer?.element?.let { next ->
                document = next.document
                    ?: return
                PsiDocumentManager.getInstance(next.project).doPostponedOperationsAndUnblockDocument(document)
                val numNewlines = next.text.count { it == '\n'}
                if (numNewlines > 1) {
                    val space = CaosScriptPsiElementFactory.newLines(project, numNewlines - 1)
                    next.replace(space)
                } else {
                    next.delete()
                }
            }
        }
    }


}
