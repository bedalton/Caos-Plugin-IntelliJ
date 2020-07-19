package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosAgentClassUtils
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.NUMBER_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import com.intellij.openapi.application.invokeLater
import javax.swing.*


internal const val GENERATE_CLAS_LOOKUP_STRING = "Generate CLAS value"

internal object GenerateClasIntegerAction : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        val position = context.editor.caretModel.currentCaret.offset
        invokeLater {
            ClasForm(position, context.editor).showAndGet()
        }
    }
}

class ClasForm(val position: Int, private val editor: Editor) : DialogWrapper(editor.project, true) {

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
        runWriteAction {
            try {
                val clas = CaosAgentClassUtils.toClas(fVal, gVal, sVal)
                EditorUtil.insertText(editor, "$clas", position, true)
            } catch (e: Exception) {
            }
        }
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
