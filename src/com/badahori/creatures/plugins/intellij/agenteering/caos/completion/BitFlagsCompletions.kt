package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefTypeDefValueStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.toIntSafe
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.ui.components.JBScrollPane
import javax.swing.*


internal class GenerateBitFlagIntegerAction(private val typeDefName: String, private val typeDefValues: List<CaosDefTypeDefValueStruct>, private val currentValue: Int = 0) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        val position = context.editor.caretModel.currentCaret.offset
        invokeLater {
            BitFlagsForm(position, 0, context.editor, typeDefName, typeDefValues.sortedBy { it.key.toIntSafe() ?: 1000 }, currentValue).showAndGet()
        }
    }
}

/**
 * Intention action to insert/change bit-flag integer
 */
class GenerateBitFlagIntegerIntentionAction(element: PsiElement, private val typeDefName: String, private val typeDefValues: List<CaosDefTypeDefValueStruct>, private val currentValue: Int = 0) : IntentionAction {

    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = false

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is CaosScriptFile && editor != null && typeDefValues.isNotEmpty()
    }

    override fun getText(): String = CaosBundle.message("com.actions.generate-bit-flags", typeDefName)

    override fun invoke(project: Project, editorIn: Editor?, file: PsiFile?) {
        val editor = editorIn
                ?: return
        val element = pointer.element
                ?: return
        val position = element.startOffset
        val length = element.textRange.let { it.endOffset - it.startOffset }
        val values = typeDefValues
        BitFlagsForm(position, length, editor, typeDefName, values, currentValue).showAndGet()
    }
}

private class BitFlagsForm(
        val position: Int,
        val consume: Int,
        private val editor: Editor,
        private val typeDefName: String,
        private val typeDefValues: List<CaosDefTypeDefValueStruct>,
        private val currentValue: Int = 0
) : DialogWrapper(editor.project, true) {

    private lateinit var checkboxes: List<BitFlagSelection>

    init {
        title = "$typeDefName bit-flags builder"
        super.init()
    }

    override fun createCenterPanel(): JComponent? {
        val out = JBScrollPane()
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("Select BitFlags for $typeDefName"))
        checkboxes = typeDefValues.mapNotNull { key ->
            key.key.toIntSafe()?.let {
                val checkBox = JCheckBox(key.key + " - " + key.value)
                if (currentValue and it > 0)
                    checkBox.isSelected = true
                panel.add(checkBox)
                BitFlagSelection(it, checkBox)
            }
        }
        out.setViewportView(panel)
        return out
    }

    override fun doOKAction() {
        var value = 0
        for (selection in checkboxes) {
            if (selection.checkbox.isSelected) {
                value = value or selection.key
            }
        }
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