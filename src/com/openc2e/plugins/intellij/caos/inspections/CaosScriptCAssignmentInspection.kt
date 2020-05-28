package com.openc2e.plugins.intellij.caos.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.openc2e.plugins.intellij.caos.deducer.CaosScriptInferenceUtil
import com.openc2e.plugins.intellij.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptC1ClasToCls2Fix
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptCls2ToClasFix
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptReplaceWordFix
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.utils.matchCase

class CaosScriptCAssignmentInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Expected parameter type"
    override fun getGroupDisplayName(): String = "CaosScript"
    override fun getShortName(): String = "ExpectedParameterType"
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCAssignment(o: CaosScriptCAssignment) {
                super.visitCAssignment(o)
                annotateAssignment(o, holder)
            }
        }
    }

    private fun annotateAssignment(element: CaosScriptCAssignment, problemsHolder: ProblemsHolder) {
        val variant = element.containingCaosFile.variant
        when(element.commandString.toUpperCase()) {
            "SETV" -> annotateSetv(variant, element, problemsHolder)
            "SETA" -> annotateSeta(variant, element, problemsHolder)
            "SETS" -> annotateSets(variant, element, problemsHolder)
        }
    }

    private fun annotateSetv(variant:String, element:CaosScriptCAssignment, problemsHolder: ProblemsHolder) {
        val lvalue = element.lvalue
                ?: return
        if (variant in VARIANT_OLD) {
            annotateSetvClassic(variant, element, lvalue, problemsHolder)
            return
        }
        val setv = element.commandToken
                ?: return
        annotateSetvNew(setv, lvalue, problemsHolder)
    }

    private fun annotateSetvClassic(variant:String, element:CaosScriptCAssignment, lvalue:CaosScriptLvalue, problemsHolder: ProblemsHolder) {
        val lvalueCommand = lvalue.commandString.toUpperCase()
        if (variant == "C2" && lvalueCommand == "CLAS") {
            problemsHolder.registerProblem(lvalue, CaosBundle.message("caos.annotator.command-annotator.setv-clas-replaced-in-c2"), CaosScriptC1ClasToCls2Fix(element))
        } else if (variant == "C1" && lvalueCommand == "CLS2") {
            problemsHolder.registerProblem(lvalue, CaosBundle.message("caos.annotator.command-annotator.setv-cl2-is-c2-only"), CaosScriptCls2ToClasFix(element))
        }
    }

    private fun annotateSetvNew(setv: CaosScriptIsCommandToken, lvalue: CaosScriptLvalue, problemsHolder: ProblemsHolder) {
        val type = lvalue.inferredType
        if (type.isNumberType || type.isAnyType)
            return
        val replacement = when {
            type.isStringType -> "SETS"
            type.isAgentType -> "SETA"
            else -> null
        }
        registerProblemWithFix(setv, "a numeric", type, replacement, problemsHolder)
    }

    private fun annotateSets(variant:String, element:CaosScriptCAssignment, problemsHolder: ProblemsHolder) {
        if (variant in VARIANT_OLD)
            return
        val lvalue = element.lvalue
                ?: return
        val commandToken = element.commandToken
                ?: return

        // Get and validate actual value type
        val type = lvalue.inferredType
        if (type.isStringType)
            return
        val replacement = when {
            type.isNumberType -> "SETV"
            type.isAgentType -> "SETA"
            else -> null
        }?.matchCase(commandToken.text)
        registerProblemWithFix(commandToken, "a string", type, replacement, problemsHolder)
    }


    private fun annotateSeta(variant:String, element:CaosScriptCAssignment, problemsHolder: ProblemsHolder) {
        val lvalue = element.lvalue
                ?: return
        val seta = element.commandToken
                ?: return
        if (variant in VARIANT_OLD) {
            problemsHolder.registerProblem(element, CaosBundle.message("caos.inspections.assignments.invalid-seta-not-available", variant), CaosScriptReplaceWordFix("SETV".matchCase(seta.text), seta))
        }
        annotateSetaNewVariants(seta, lvalue, problemsHolder)
    }

    private fun annotateSetaNewVariants(seta:CaosScriptIsCommandToken, lvalue:CaosScriptLvalue, problemsHolder: ProblemsHolder) {
        val type = lvalue.varToken?.let {
            CaosScriptInferenceUtil.getInferredType(it)
        } ?: lvalue.toCaosVar().let {
            if (it is CaosVar.CaosCommandCall) {
                it.returnType ?: it.simpleType
            } else
                it.simpleType
        }
        if (type.isAgentType)
            return
        val replacement = when {
            type.isNumberType -> "SETV"
            type.isStringType -> "SETS"
            else -> null
        }?.matchCase(seta.text)
        registerProblemWithFix(seta, "an agent", type, replacement, problemsHolder)
    }

    private fun registerProblemWithFix(targetElement:CaosScriptIsCommandToken, expectedType:String, actualType:CaosExpressionValueType, replacement:String?, problemsHolder: ProblemsHolder) {
        // Create message using suggestion text if any
        var message = CaosBundle.message("caos.inspections.assignments.wrong-type", targetElement.commandString.toUpperCase(), expectedType)
        if (replacement != null) {
            message += " " + CaosBundle.message("caos.inspections.assignments.use-replacement", replacement, actualType.simpleName)
        }
        val replacementFix = replacement?.let {
            listOf(CaosScriptReplaceWordFix(replacement, targetElement))
        }.orEmpty().toTypedArray()
        problemsHolder.registerProblem(targetElement, CaosBundle.message("caos.inspections.assignments.seta-requires-agent", actualType.simpleName), *replacementFix)
    }

    companion object {
        private val VARIANT_OLD = listOf("C1", "C2")
    }

}