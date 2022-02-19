    package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.formatting.CaosScriptBlock
import com.intellij.codeInspection.IntentionAndQuickFixAction
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.text.BlockSupport

    class CaosScriptFixQuoteType(element:PsiElement, private val quoteStart:Char, private val quoteEnd:Char = quoteStart) : IntentionAndQuickFixAction() {
    private val pointer = SmartPointerManager.createPointer(element)

    override fun getName(): String = CaosBundle.message("caos.fixes.fix-quote-type")

    override fun getFamilyName(): String = CAOSScript

    override fun applyFix(project: Project, element: PsiFile?, editor: Editor?) {
        pointer.element?.let {
            applyFix(project, it)
        }
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(project, descriptor.psiElement)
    }

    private fun applyFix(project:Project, element:PsiElement) {
        val expression = element.getSelfOrParentOfType(CaosScriptStringLike::class.java)
                ?: return
        val document = element.document
            ?: return
        val startOffset = element.startOffset
        val endOffset = element.endOffset
        val text = expression.stringValue
        val replaceWith = "$quoteStart$text$quoteEnd"
        runUndoTransparentWriteAction {
            EditorUtil.replaceText(document, TextRange(startOffset, endOffset), replaceWith)
        }
    }
}