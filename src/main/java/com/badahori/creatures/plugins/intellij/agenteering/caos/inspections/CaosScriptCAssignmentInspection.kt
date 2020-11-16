package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptC1ClasToCls2Fix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCls2ToClasFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceWordFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getAssignedType
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class CaosScriptCAssignmentInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Assigned value is of expected type"
    override fun getGroupDisplayName(): String = "CaosScript"
    override fun getShortName(): String = "ExpectedAssigmentType"
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitCAssignment(o: CaosScriptCAssignment) {
                super.visitCAssignment(o)
                annotateAssignment(o, holder)
            }
        }
    }

    private fun annotateAssignment(element: CaosScriptCAssignment, problemsHolder: ProblemsHolder) {
        val variant = element.containingCaosFile?.variant
                ?: return
        val type = element.getAssignedType()
                ?: return
        when (element.commandString.toUpperCase()) {
            "SETV" -> annotateSetv(variant, element, type, problemsHolder)
            "SETA" -> annotateSeta(variant, element, type, problemsHolder)
            "SETS" -> annotateSets(variant, element, type, problemsHolder)
        }
    }



    private fun annotateSetv(variant: CaosVariant, element: CaosScriptCAssignment, actualType: CaosExpressionValueType, problemsHolder: ProblemsHolder) {
        if (variant.isOld) {
            val lvalue = element.lvalue
                    ?: return
            annotateSetvClassic(variant, element, lvalue, problemsHolder)
            return
        }
        val setv = element.commandToken
                ?: return
        annotateSetvNew(setv, actualType, problemsHolder)
    }

    private fun annotateSetvClassic(variant: CaosVariant, element: CaosScriptCAssignment, lvalue: CaosScriptLvalue, problemsHolder: ProblemsHolder) {
        val lvalueCommand = lvalue.commandString?.toUpperCase()
                ?: return
        if (variant == CaosVariant.C2 && lvalueCommand == "CLAS") {
            problemsHolder.registerProblem(lvalue, CaosBundle.message("caos.annotator.command-annotator.setv-clas-replaced-in-c2"), CaosScriptC1ClasToCls2Fix(element))
        } else if (variant == CaosVariant.C1 && lvalueCommand == "CLS2") {
            problemsHolder.registerProblem(lvalue, CaosBundle.message("caos.annotator.command-annotator.setv-cl2-is-c2-only"), CaosScriptCls2ToClasFix(element))
        }
    }

    private fun annotateSetvNew(setv: CaosScriptIsCommandToken, type: CaosExpressionValueType, problemsHolder: ProblemsHolder) {
        if (type.isNumberType || type.isAnyType)
            return
        annotateUnexpectedType(setv, CaosExpressionValueType.DECIMAL, type, problemsHolder)
    }

    private fun annotateSets(variant: CaosVariant, element: CaosScriptCAssignment, type: CaosExpressionValueType, problemsHolder: ProblemsHolder) {
        if (variant.isOld)
            return
        val commandToken = element.commandToken
                ?: return
        annotateUnexpectedType(commandToken, CaosExpressionValueType.STRING, type, problemsHolder)
    }


    private fun annotateSeta(variant: CaosVariant, element: CaosScriptCAssignment, type: CaosExpressionValueType, problemsHolder: ProblemsHolder) {
        if (variant.isOld)
            return
        val commandToken = element.commandToken
                ?: return
        annotateUnexpectedType(commandToken, CaosExpressionValueType.AGENT, type, problemsHolder)
    }

    private fun annotateUnexpectedType(element: CaosScriptIsCommandToken, expectedType: CaosExpressionValueType, actualType: CaosExpressionValueType, problemsHolder: ProblemsHolder) {
        if (actualType == expectedType)
            return
        val replacement = when {
            actualType.isNumberType -> "SETV"
            actualType.isStringType -> "SETS"
            actualType.isAgentType -> "SETA"
            else -> null
        }
        val typeName = when {
            expectedType.isNumberType -> "a numeric"
            expectedType.isStringType -> "a string"
            expectedType.isAgentType -> "an agent"
            else -> return
        }
        registerProblemWithFix(element, typeName, actualType, replacement, problemsHolder)
    }

    private fun registerProblemWithFix(targetElement: CaosScriptIsCommandToken, expectedType: String, actualType: CaosExpressionValueType, replacement: String?, problemsHolder: ProblemsHolder) {
        if (actualType.isAnyType || actualType == CaosExpressionValueType.VARIABLE)
            return
        // Create message using suggestion text if any
        var message = CaosBundle.message("caos.inspections.assignments.wrong-type", targetElement.commandString.toUpperCase(), expectedType, actualType.simpleName)
        if (replacement != null) {
            message += ". " + CaosBundle.message("caos.inspections.assignments.use-replacement", replacement, actualType.simpleName)
        }
        val replacementFix = replacement?.let {
            listOf(CaosScriptReplaceWordFix(replacement, targetElement))
        }.orEmpty().toTypedArray()
        problemsHolder.registerProblem(targetElement, message, *replacementFix)
    }
}