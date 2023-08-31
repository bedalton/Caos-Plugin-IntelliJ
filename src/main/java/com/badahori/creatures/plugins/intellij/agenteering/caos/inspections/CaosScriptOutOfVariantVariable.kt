package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.varIndexOrZero
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import java.util.concurrent.atomic.AtomicInteger


/**
 * Detects out of variant variables such as VAxx in C1 and VARx in CV+
 */
class CaosScriptOutOfVariantVariable : LocalInspectionTool(), DumbAware {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspection.invalid-var-type-for-variant.display-name")
    override fun getGroupDisplayName(): String = CAOSScript
    override fun getShortName(): String = CaosBundle.message("caos.inspection.invalid-var-type-for-variant.short-name")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitVarToken(o: CaosScriptVarToken) {
                super.visitVarToken(o)
                validate(o, holder)
            }
        }
    }

    private fun validate(element: CaosScriptVarToken, problemsHolder: ProblemsHolder) {
        val variant = element.variant
            ?: return
        if (isVarTokenValid(element, true, variant)) {
            return
        }

        // Get a string describing valid variants for used variable
        val variants = when {
            element.varX != null -> "C1,C2"
            element.obvX != null -> {
                if (element.varIndex.orElse(0) <= 2) {
                    "C1,C2"
                } else {
                    "C2"
                }
            }
            element.vaXx != null -> "C2+"
            element.ovXx != null -> "C2+"
            element.mvXx != null -> "CV+"
            else -> return
        }

        // Get used variable string
        val varName = when {
            element.varX != null -> "VARx"
            element.vaXx != null -> "VAxx"
            element.obvX != null -> if (variant == CaosVariant.C1 && element.varIndex.orElse(0) > 2)
                "OBVx[3-9]"
            else
                "OBVx"
            element.ovXx != null -> "OVxx"
            element.mvXx != null -> "MVxx"
            else -> element.text
        }

        // Create a fix if a fix is possible
        val fix = if (element.varIndex.orElse(0) <= 9 && element.mvXx == null) {
            arrayOf(CaosScriptVariableVariantFix(element))
        } else {
            emptyArray()
        }

        val error = CaosBundle.message("caos.inspection.invalid-var-type-for-variant.message", varName, variants)
        problemsHolder
            .registerProblem(
                element,
                error,
                *fix
            )
    }

}

/**
 * Fix for out of Variant variables. Doing conversions such as VAxx to VARx or OBVx to OVxx
 */
class CaosScriptVariableVariantFix(
    element: CaosScriptVarToken
)  : IntentionAction, LocalQuickFix {

    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CAOSScript

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return pointer.element?.let { variable ->
            variable.mvXx == null && variable.varIndexOrZero <= 9 && !isVarTokenValid(variable, true)
        } ?: false
    }

    override fun getText(): String = "Fix variable format"

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val element = pointer.element
            ?: return
        applyFix(element)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val token = (element as? CaosScriptVarToken) ?: (element as? CaosScriptRvalue)?.varToken
        ?: return
        if (token.varIndex.orElse(0) > 9) {
            return
        }
        if (isVarTokenValid(token, false)) {
            return
        }
        applyFix(token)
    }

    private fun applyFix(element: CaosScriptVarToken) {
        if (element.varIndex.orElse(0) > 9) {
            return
        }
        val project: Project = element.project
        if (project.isDisposed) {
            return
        }
        val document = element.document
            ?: return
        val replacementText = replacementText(element)
            ?: return
        val startOffset = element.startOffset
        val endOffset = element.endOffset

        val title = text[0].uppercaseChar() + text.substring(1).lowercase()

        CommandProcessor.getInstance().executeCommand(
            project,
            {
                runWriteAction {
                    if (project.isDisposed) {
                        return@runWriteAction
                    }
                    EditorUtil.replaceText(document, TextRange(startOffset, endOffset), replacementText)
                }
            },
            title,
            "FixVariableExpression${nextId.incrementAndGet()}"
        )
    }


    companion object {

        private val nextId = AtomicInteger(0)

        fun replacementText(element: CaosScriptVarToken): String? {
            // Fix switches are simplified as at this point,
            // we know things do not match up, so do not need to check variant
            return when {
                element.varX != null -> "va0${element.varIndex}"
                element.vaXx != null && element.varIndex in 0..9 ->
                    "var${element.varIndex}"
                element.obvX != null ->
                    "ov0${element.varIndex}"
                element.ovXx != null && element.varIndex in 0..9 ->
                    "obv${element.varIndex}"
                else -> null
            }
        }

    }

}



private fun isVarTokenValid(element: CaosScriptVarToken, validateIndex: Boolean, theVariant: CaosVariant? = null): Boolean {
    val variant = theVariant ?: element.variant
    ?: return true
    if (element.varX != null) {
        return variant.isOld
    } else if (element.vaXx != null) {
        return variant != CaosVariant.C1
    } else if (element.obvX != null) {
        return variant.isOld && (!validateIndex || (variant == CaosVariant.C1 && element.varIndex.orElse(0) <= 2))
    } else if (element.ovXx != null) {
        return variant != CaosVariant.C1
    } else if (element.mvXx != null) {
        return variant.isNotOld
    }
    return false
}