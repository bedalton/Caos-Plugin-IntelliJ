package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.document

class TransposeEqOp(val element: com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqOpNew)  : IntentionAction {

    private val originalText = element.text
    private val replacement = when (element.text) {
        "=" -> "eq"
        "<>" -> "ne"
        ">" -> "gt"
        ">=" -> "ge"
        "<" -> "lt"
        "<=" -> "le"
        else -> null
    }

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is CaosScriptFile && replacement != null
    }

    override fun getText(): String = CaosBundle.message("caos.fixes.replace-eq", originalText, replacement ?: "????")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (replacement == null)
            return
        file?.document?.replaceString(element.textRange.startOffset, element.textRange.endOffset, replacement)
    }

}