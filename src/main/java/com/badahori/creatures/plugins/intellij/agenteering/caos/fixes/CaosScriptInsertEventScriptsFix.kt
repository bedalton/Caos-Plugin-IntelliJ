package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRemovalScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptBodyElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.collectElementsOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.bedalton.common.util.className
import com.bedalton.common.util.formatted
import com.bedalton.creatures.common.structs.AgentClass
import com.bedalton.log.Log
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.max

class CaosScriptInsertEventScriptsFix(
    psiFile: PsiFile,
    private val fixText: String,
    private val insertAfter: Boolean,
    element: PsiElement? = null,
    private val scripts: List<Triple<AgentClass, Int, String>>,
    private val postInsert: ((editor: Editor?) -> Unit)? = null,
) : LocalQuickFix, IntentionAction {

    private val filePointer: SmartPsiElementPointer<PsiFile> = SmartPointerManager.createPointer(psiFile)

    private val elementPointer: SmartPsiElementPointer<PsiElement>? = if (element != null) {
        SmartPointerManager.createPointer(element)
    } else {
        null
    }

    override fun getFamilyName(): String = CAOSScript

    override fun getText(): String = fixText

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return filePointer.element != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null) {
            return
        }
        invoke(editor, file, null)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

        val file = filePointer.element
            ?: return

        val element = elementPointer?.element

        val editor = element?.editor ?: file.editor

        try {
            invoke(editor, file, element)
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            LOGGER.severe("Failed to insert event scripts. ${e.formatted(true)}")
        }
    }

    private fun invoke(editor: Editor?, file: PsiFile, element: PsiElement?) {

        if (scripts.isEmpty()) {
            return
        }

        val scripts = scripts
            .sortedBy { it.second }

        val project = file.project

        if (project.isDisposed || !project.isOpen) {
            return
        }

        val actualFile = if (element != null) {
            element.containingFile
        } else {
            file
        }

        val filePointer = SmartPointerManager.createPointer(actualFile)

        val classifiers = scripts.map { it.first }
            .distinct()
            .nullIfEmpty()
            ?: return

        val firstClassifier = classifiers[0]

        for (classifier in classifiers) {
            insertScripts(
                file,
                if (classifiers.size == 1) element else null,
                classifier,
                insertAfter,
            )
        }

        commit(file, element ?: file)

        navigateTo(filePointer.element ?: this.filePointer.element ?: file, firstClassifier)

        postInsert?.invoke(editor)
    }

    private fun navigateTo(file: PsiFile, classifier: AgentClass) {
        val events = scripts.filter { it.first == classifier }.map { it.second }

        val inserted = PsiTreeUtil
            .collectElementsOfType(
                file,
                CaosScriptEventScript::class.java
            )
            .first {
                classifier.family == it.family
                        && classifier.genus == it.genus
                        && classifier.species == it.species
                        && it.eventNumber in events
            }
        inserted?.navigate(true)
    }

    private fun insertScripts(
        file: PsiFile,
        element: PsiElement?,
        agentClass: AgentClass?,
        insertAfter: Boolean,
    ): PsiElement {

        val scriptText = scripts
            .filter { it.first == agentClass }
            .joinToString("\n\n\n") {
                getScriptText(it.first, it.second, it.third)
            }

        return if (element != null && element.isValid) {
            insertScriptsNearElement(
                file,
                element,
                scriptText,
                insertAfter
            )
        } else {
            insertScriptsIntoFile(
                file,
                agentClass,
                scriptText,
            )
        }
    }


    private fun insertScriptsNearElement(
        file: PsiFile,
        element: PsiElement,
        scriptText: String,
        insertAfter: Boolean,
    ): PsiElement {

        val parentScript = element.getSelfOrParentOfType(CaosScriptScriptElement::class.java)
            ?: element

        return if (insertAfter) {
            insertScriptsAfter(file, parentScript, scriptText)
        } else {
            insertScriptsBefore(file, parentScript, scriptText)
        }
    }

    private fun insertScriptsIntoFile(
        file: PsiFile,
        agentClass: AgentClass?,
        scriptText: String,
    ): PsiElement {

        val (anchor, insertAfterForSure) = getAnchorElementFromFileOnly(
            file = file,
            agentClass = agentClass
        )

        if (anchor == null) {
            return insertAtEndOfFile(file, scriptText)
        }

        return if (insertAfterForSure) {
            insertScriptsAfter(file, anchor, scriptText)
        } else {
            insertScriptsBefore(
                file,
                anchor,
                scriptText,
            )
        }
    }

    private fun insertAtEndOfFile(
        file: PsiFile,
        scriptText: String,
    ): PsiElement {
        // Add newlines before sibling script element
        val text = "\n".repeat(2) + scriptText.trim() + "\n\n"
        val elements = CaosScriptPsiElementFactory.createFileFromText(
            file.project,
            text,
            file.variant ?: CaosVariant.DS,
        )
        return file.addRange(elements.firstChild, elements.lastChild)
    }

    private fun insertScriptsAfter(
        file: PsiFile,
        beforeElement: PsiElement,
        scriptText: String,
    ): PsiElement {

        val bodyElement = beforeElement.getParentOfType(CaosScriptScriptBodyElement::class.java)

        // Add newlines before sibling script element
        val newlines = getNewlinesNeededAfterElement(beforeElement, 4)
        val newLinesBefore = "\n\n"
        val newLinesAfter = if ((newlines - 2) < 0) {
            "\n".repeat(newlines - 2)
        } else {
            ""
        }

        val text = newLinesBefore + scriptText.trim() + newLinesAfter
        val elements = CaosScriptPsiElementFactory.createFileFromText(
            file.project,
            text,
            beforeElement.variant ?: CaosVariant.DS,
        )
        return file.addRangeAfter(elements.firstChild, elements.lastChild, bodyElement)
    }

    private fun insertScriptsBefore(
        file: PsiFile,
        afterElement: PsiElement,
        scriptText: String,
    ): PsiElement {

        val bodyElement = afterElement.getParentOfType(CaosScriptScriptBodyElement::class.java)

        // Add newlines before sibling script element
        val newlines = getNewlinesNeededAfterElement(afterElement, 2)
        val text = "\n".repeat(newlines) + scriptText.trim() + "\n\n"
        val elements = CaosScriptPsiElementFactory.createFileFromText(
            file.project,
            text,
            afterElement.variant ?: CaosVariant.DS,
        )

        return file.addRangeBefore(elements.firstChild, elements.lastChild, bodyElement)
    }

    private fun getNewlinesNeededAfterElement(element: PsiElement, count: Int): Int {
        var whitespace = element.getNextSiblingOfType(*CaosScriptTokenSets.WHITESPACES.types)
        var newlinesNeeded = count
        while (whitespace != null && newlinesNeeded > 0) {
            newlinesNeeded -= whitespace.text.count { it == '\n' }
            whitespace = whitespace.getNextSiblingOfType(*CaosScriptTokenSets.WHITESPACES.types)
        }
        return newlinesNeeded
    }

    private fun getScriptText(agentClass: AgentClass, event: Int, body: String): String {
        return getScriptText(agentClass.family, agentClass.genus, agentClass.species, event, body)
    }

    private fun getScriptText(family: Int, genus: Int, species: Int, event: Int, body: String): String {
        return "scrp $family $genus $species $event\n$body\nendm"
    }

    private fun getScript(
        project: Project,
        scriptDescriptor: Triple<AgentClass, Int, String>,
    ): CaosScriptScriptElement? {

        val (family, genus, species) = scriptDescriptor.first
        val event = scriptDescriptor.second
        val body = scriptDescriptor.third

        return CaosScriptPsiElementFactory.createScriptElement(
            project,
            "scrp $family $genus $species $event",
            body
        )
    }

    private fun getAnchorElementFromFileOnly(
        file: PsiFile,
        agentClass: AgentClass? = null,
    ): Pair<PsiElement?, Boolean> {

        // Get all scripts in file
        val scripts = file
            .collectElementsOfType(CaosScriptScriptElement::class.java)
            .filter { it.isValid }

        // Filter for event scripts
        val eventScripts = scripts
            .filterIsInstance<CaosScriptEventScript>()

        // If agent class is provided, get all event scripts for agent
        if (agentClass != null) {
            val (family, genus, species) = agentClass
            val event = eventScripts
                .filter {
                    it.family == family && it.genus == genus && it.species == species
                }
                .maxByOrNull { it.startOffset }
            if (event != null) {
                return Pair(event, true)
            }
        }

        // Insert after last event script of any agent class
        if (eventScripts.isNotEmpty()) {
            val anchor = eventScripts.maxBy { it.startOffset }
            return Pair(anchor, true)
        }

        // Get rscr scripts
        val rscr = scripts
            .filterIsInstance<CaosScriptRemovalScript>()
            .minByOrNull { it.startOffset }

        // Insert before first rscr
        if (rscr != null) {
            return Pair(rscr, false)
        }

        // Get last script period or last element
        val anchor = if (scripts.isNotEmpty()) {
            scripts.maxByOrNull { it.startOffset }
        } else {
            file.lastChild
        }

        return Pair(anchor, true)
    }


    private fun commit(file: PsiFile, element: PsiElement, editor: Editor? = null) {
        // Commit document if possible
        val document = editor?.document
            ?: file.document
            ?: element.document

        if (document != null) {
            commit(file.project, document)
        }
    }

    private fun commit(project: Project, document: Document) {
        if (project.isDisposed) {
            return
        }
        if (!ApplicationManager.getApplication().isDispatchThread) {
            com.intellij.openapi.application.invokeLater { commit(project, document) }
            return
        }
        com.intellij.openapi.application.runWriteAction {
            try {
                val manager = PsiDocumentManager.getInstance(project)
                manager.commitDocument(document)
                manager.doPostponedOperationsAndUnblockDocument(document)
            } catch (e: Throwable) {
                e.rethrowAnyCancellationException()
                LOGGER.severe("Failed to commit document with throwable; ${e.className}: ${e.message ?: ""}")
            } catch (e: Error) {
                LOGGER.severe("Failed to commit document with error; ${e.className}: ${e.message ?: ""}")
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                LOGGER.severe("Failed to commit document with exception; ${e.className}: ${e.message ?: ""}")
            }
        }
    }

}