package com.openc2e.plugins.intellij.agenteering.caos.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.openc2e.plugins.intellij.agenteering.caos.utils.EditorUtil
import com.openc2e.plugins.intellij.agenteering.caos.utils.NUMBER_REGEX
import com.openc2e.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import javax.swing.*


internal const val GENERATE_DDE_PICT_LOOKUP_STRING = "Generate PICT dimensions"

internal object GeneratePictDimensionsAction : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        val position = context.editor.caretModel.currentCaret.offset
        invokeAndWaitIfNeeded {
            PictDimensionsForm(position, context.editor).showAndGet()
        }
    }
}

private class PictDimensionsForm(val position: Int, private val editor: Editor) : DialogWrapper(editor.project, true) {

    private val width = JFormattedTextField().apply {
        value = ""
        columns = 4
    }

    private val height = JFormattedTextField().apply {
        value = ""
        columns = 4
    }

    init {
        title = "DDE: PICT Dimensions Calculator"
        super.init()
    }

    override fun createCenterPanel(): JComponent? {
        val fieldsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.LINE_AXIS)
        }

        // Width
        fieldsPanel.add(JLabel("Width"))
        fieldsPanel.add(width)
        // Height
        fieldsPanel.add(JLabel("Height"))
        fieldsPanel.add(height)

        return fieldsPanel
    }

    override fun doOKAction() {
        val widthVal = width.text.toInt().toChar()
        val heightVal = height.text.toInt().toChar()
        runWriteAction {
            try {
                val dimensions = "${widthVal}|${heightVal}"
                EditorUtil.insertText(editor, dimensions, position, true)
            } catch (e: Exception) {
            }
        }
        super.doOKAction()
    }

    override fun doValidateAll(): List<ValidationInfo> {
        return listOfNotNull(
                validate(width),
                validate(height)
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
