package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
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

class GenerateClasIntegerAction(element:CaosScriptRvalue) : LocalQuickFix, IntentionAction {

    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getText(): String = name

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        ClasForm(project, pointer).showAndGet()
    }

    override fun getName(): String = "Generate CLAS value"
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
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

    constructor(project: Project, pointer:SmartPsiElementPointer<CaosScriptRvalue>) : this(project) {
        insert = { clas: Int ->
            runUndoTransparentWriteAction action@{
                try {
                    val element = pointer.element
                            ?: return@action
                    val newValue = CaosScriptPsiElementFactory.createNumber(project, clas)
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

    override fun createCenterPanel(): JComponent? {
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
