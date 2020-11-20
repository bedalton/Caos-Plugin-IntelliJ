package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.editor
import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.ui.components.JBScrollPane
import javax.swing.*

/**
 * Creates an action to allow selection of bitflags by checkboxes
 */
internal class GenerateBitFlagIntegerAction(private val valuesListName: String, private val valuesListValues: List<CaosValuesListValue>, private val currentValue: Int = 0) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        val position = context.editor.caretModel.currentCaret.offset
        invokeLater {
            BitFlagsForm(position, 0, context.editor, valuesListName, valuesListValues.sortedBy { it.value.toIntSafe() ?: 1000 }, currentValue).showAndGet()
        }
    }
}

/**
 * Intention action to insert/change bit-flag integer
 */
class GenerateBitFlagIntegerIntentionAction(element: PsiElement, private val valuesListName: String, private val valuesListValues: List<CaosValuesListValue>, private val currentValue: Int = 0) : IntentionAction, LocalQuickFix {

    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = false

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is CaosScriptFile && editor != null && valuesListValues.isNotEmpty()
    }

    override fun getText(): String = CaosBundle.message("com.actions.generate-bit-flags", valuesListName)

    override fun invoke(project: Project, editorIn: Editor?, file: PsiFile?) {
        val editor = editorIn
                ?: return
        val element = pointer.element
                ?: return
        invoke(element, editor)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
                ?: return
        val editor = element.editor
                ?: return
        invoke(element, editor)
    }

    private fun invoke(element: PsiElement, editor: Editor) {
        val position = element.startOffset
        val length = element.textRange.let { it.endOffset - it.startOffset }
        val values = valuesListValues
        BitFlagsForm(position, length, editor, valuesListName, values, currentValue).showAndGet()
    }
}

/**
 * Creates a simple form of bitflag checkboxes
 */
private class BitFlagsForm(
        val position: Int,
        val consume: Int,
        private val editor: Editor,
        private val valuesListName: String,
        private val valuesListValues: List<CaosValuesListValue>,
        private val currentValue: Int = 0
) : DialogWrapper(editor.project, true) {

    // Hold a list of checkboxes paired with their bit-flag values
    private lateinit var checkboxes: List<BitFlagSelection>

    init {
        title = "$valuesListName bit-flags builder"
        super.init()
    }

    override fun createCenterPanel(): JComponent? {
        val out = JBScrollPane()
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("Select BitFlags for $valuesListName"))
        // Loops through all values to create their checkboxes
        checkboxes = valuesListValues.mapNotNull { key ->
            // Gets the value as an int, as an int value is required for bitflags
            key.value.toIntSafe()?.let {
                // Create the checkbox
                val checkBox = JCheckBox(key.value + " - " + key.name)
                if (currentValue and it > 0)
                    checkBox.isSelected = true
                panel.add(checkBox)
                // Create a selection object to hold both the checkbox
                // and the value list int value
                BitFlagSelection(it, checkBox)
            }
        }
        // Set the checkbox panel as the main view
        out.setViewportView(panel)
        return out
    }

    /**
     * Called on okay button clicked
     */
    override fun doOKAction() {
        var value = 0
        // Find all checked boxes, and 'OR' their values together
        // to get the full bitflag value
        // as in value = 2 | 4 | 16
        for (selection in checkboxes) {
            if (selection.checkbox.isSelected) {
                value = value or selection.key
            }
        }
        // If consume > 0, it means replace the range
        // Used if there is already a bitflag present
        if (consume > 0) {
            val range = TextRange(position, position + consume)
            EditorUtil.replaceText(editor, range, "$value", true)
        } else {
            EditorUtil.insertText(editor, "$value", position, true)
        }
        super.doOKAction()
    }
}

private data class BitFlagSelection(val key: Int, val checkbox: JCheckBox)