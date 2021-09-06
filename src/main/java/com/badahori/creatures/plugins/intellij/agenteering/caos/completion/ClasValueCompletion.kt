package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlockLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosAgentClassUtils
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.NUMBER_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.TokenType
import com.intellij.psi.util.elementType
import javax.swing.*


internal const val GENERATE_CLAS_LOOKUP_STRING = "Generate CLAS value"

internal object GenerateClasIntegerInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        val position = context.editor.caretModel.currentCaret.offset
        invokeLater {
            ClasForm(position, context.editor).showAndGet()
        }
    }
}

class GenerateClasIntegerAction : PsiElementBaseIntentionAction(), IntentionAction, LocalQuickFix {

    override fun startInWriteAction(): Boolean = true

    override fun getText(): String = name

    override fun getFamilyName(): String = CAOSScript

    override fun getName(): String = "Generate CLAS value"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return isSetvClas(element)
    }

    private fun isSetvClas(element: PsiElement): Boolean {
        var assignment = element as? CaosScriptCAssignment
        if (assignment == null && element.tokenType == TokenType.WHITE_SPACE) {
            val previous = element.getPreviousNonEmptySibling(false) as? CaosScriptCodeBlockLine
                ?: return false
            assignment = previous.commandCall?.cAssignment
                ?: return false
        }
        if (assignment != null) {
            val arguments = assignment.arguments.nullIfEmpty()
                ?: return false
            if (arguments.isNotEmpty())
                return arguments.first().text?.toUpperCase() == "CLAS"
            return false
        }

        if (element !is CaosScriptRvalue)
            return false
        if (element.index != 1)
            return false

        val command = element.getParentOfType(CaosScriptCommandElement::class.java)
            ?: return false
        val token = command
            .commandString
            ?.toUpperCase()
            ?: return false
        if (token != "SETV")
            return false

        val previousToken = command.arguments.getOrNull(0)?.text?.toUpperCase()
            ?: return false
        return previousToken == "CLAS"
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (!isSetvClas(element))
            return
        applyFix(project, element)
    }


    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        applyFix(project, element)
    }

    private fun applyFix(project: Project, element: PsiElement) {
        val elementActual = when (element) {
            is CaosScriptCAssignment -> {
                val arguments = element.arguments.nullIfEmpty()
                    ?: return
                if (arguments.size != 1)
                    return
                if (arguments.first().text?.toUpperCase() != "CLAS")
                    return
                arguments.first().next
                    ?: return
            }
            is CaosScriptRvalue -> element
            else -> if (isSetvClas(element))
                element
            else
                return
        }
        val pointer = SmartPointerManager.createPointer(elementActual)
        invokeLater {
            ClasForm(project, pointer).showAndGet()
        }
    }

}

class ClasForm private constructor(project: Project) : DialogWrapper(project, true) {

    private var insert:((value:Int) -> Unit)? = null

    constructor(position: Int, editor: Editor) : this(editor.project!!) {
        insert = { clas: Int ->
            runWriteAction {
                try {
                    EditorUtil.insertText(editor, "$clas", position, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    LOGGER.severe("Failed to insert CLAS text with error: ${e.message}")
                }
            }
        }
    }

    constructor(project: Project, pointer:SmartPsiElementPointer<PsiElement>) : this(project) {
        insert = { clas: Int ->
            runUndoTransparentWriteAction action@{
                try {
                    val element = pointer.element
                            ?: return@action
                    val newValue = CaosScriptPsiElementFactory.createNumber(project, clas)
                    if (element.elementType == TokenType.WHITE_SPACE) {
                        val editor = element.editor
                            ?: return@action
                        EditorUtil.replaceText(editor, element.textRange, " $clas ")
                        return@action
                    }
                    element.replace(newValue)
                } catch (e: Exception) {
                    e.printStackTrace()
                    LOGGER.severe("Failed to create CLAS value with error: ${e.message}")
                }
            }
        }
    }


    private val family = JFormattedTextField().apply {
        value = ""
        columns = 3
    }

    private val genus = JFormattedTextField().apply {
        value = ""
        columns = 3
    }

    val species = JFormattedTextField().apply {
        value = ""
        columns = 3
    }

    init {
        title = "Clas Value Builder"
        super.init()
    }

    override fun createCenterPanel(): JComponent {
        val fieldsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.LINE_AXIS)
        }

        // Family
        fieldsPanel.add(JLabel("Family"))
        fieldsPanel.add(family)
        // Genus
        fieldsPanel.add(JLabel("Genus"))
        fieldsPanel.add(genus)
        // Species
        fieldsPanel.add(JLabel("Species"))
        fieldsPanel.add(species)
        return fieldsPanel
    }

    override fun doOKAction() {
        val fVal = family.text.toInt()
        val gVal = genus.text.toInt()
        val sVal = species.text.toInt()
        val clas = CaosAgentClassUtils.toClas(fVal, gVal, sVal)
        insert?.let { it(clas) }
        super.doOKAction()
    }

    override fun doValidateAll(): List<ValidationInfo> {
        return listOfNotNull(
                validate(family),
                validate(genus),
                validate(species)
        )
    }

    private fun validate(field: JTextField): ValidationInfo? {
        val text = field.text
        if (!NUMBER_REGEX.matches(text))
            return ValidationInfo("numeric value expected", field)
        val value = text.nullIfEmpty()?.let {
            try {
                it.toInt()
            } catch (e: Exception) {
                return ValidationInfo("Int value invalid", field)
            }
        }
        // Family
        if (value == null)
            return ValidationInfo("Value Required", field)
        else if (value < 1)
            return ValidationInfo("Value must be greater than 1", field)
        return null
    }
}
