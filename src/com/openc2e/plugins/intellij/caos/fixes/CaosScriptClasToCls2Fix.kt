package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptArgument
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCAssignment
import com.openc2e.plugins.intellij.caos.utils.CaosAgentClassUtils
import com.openc2e.plugins.intellij.caos.utils.EditorUtil
import com.openc2e.plugins.intellij.caos.utils.matchCase

class CaosScriptClasToCls2Fix(element:CaosScriptCAssignment) : IntentionAction {

    private val element = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = "CaosScript"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean
            = element.element?.text?.toLowerCase() == "cls2"

    override fun getText(): String = "Convert C1 CLAS statement to C2"

    override fun invoke(project: Project, editorIn: Editor?, file: PsiFile?) {
        val editor = editorIn ?: return
        val element = element.element
                ?: return
        val familyAndGenus = element.lvalue?.getChildrenOfType(CaosScriptArgument::class.java)
                ?: return
        if (familyAndGenus.size != 2)
            return
        val clasText = "CLAS".matchCase(element.commandString)
        val clas:Int
        try {
            val family = familyAndGenus[0].text.toInt()
            val genus = familyAndGenus[1].text.toInt()
            val species = element.getChildrenOfType(CaosScriptArgument::class.java).last().text.toInt()
            clas = CaosAgentClassUtils.toClas(family, genus, species)
        } catch (e:Exception) {
            return
        }
        val range = element.textRange
        EditorUtil.deleteText(editor.document, range)
        EditorUtil.insertText(editor, "$clasText $clas", range.startOffset, true)
    }
}