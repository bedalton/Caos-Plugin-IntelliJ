package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.getRvalueTypeWithoutInference
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptC1ClasToCls2Fix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCls2ToClasFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceWordFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.matchCase
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class CaosScriptCAssignmentInspection : LocalInspectionTool(), DumbAware {

    override fun getDisplayName(): String = "Assigned value is of expected type"
    override fun getGroupDisplayName(): String = CAOSScript
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

        val commandElement = element.commandTokenElement
            ?: return


        val commandString = commandElement.commandString

        // Get expected type based on command name
        val expectedType = when (commandString.uppercase()) {
            "SETV" -> {
                if (variant.isOld) {
                    val lvalue = element.lvalue
                        ?: return
                    annotateSetvClassic(variant, element, lvalue, problemsHolder)
                    return
                }
                DECIMAL
            }
            "ADDS" -> {
                if (variant.isOld)
                    return
                STRING
            }
            "SETA" -> AGENT
            "SETS" -> STRING
            else -> return
        }

        // Get value element and return if not found
        val valueElement = element.arguments.lastOrNull() as? CaosScriptRvalue
            ?: return

        // Get actual type if possible, return if not
        val actualType = getRvalueTypeWithoutInference(valueElement, expectedType, fuzzy = true)
            ?: return

        annotateAssignment(
            variant = variant,
            commandString = commandString,
            element = commandElement,
            valueElement = valueElement,
            expectedType = expectedType,
            actualType = actualType,
            problemsHolder = problemsHolder
        )
    }


    private fun annotateSetvClassic(
        variant: CaosVariant,
        element: CaosScriptCAssignment,
        lvalue: CaosScriptLvalue,
        problemsHolder: ProblemsHolder
    ) {
        val lvalueCommand = lvalue.commandString?.uppercase()
            ?: return
        if (variant == CaosVariant.C2 && lvalueCommand == "CLAS") {
            problemsHolder.registerProblem(
                lvalue,
                CaosBundle.message("caos.annotator.syntax-error-annotator.setv-clas-replaced-in-c2"),
                ProblemHighlightType.WEAK_WARNING,
                CaosScriptC1ClasToCls2Fix(element)
            )
        } else if (variant == CaosVariant.C1 && lvalueCommand == "CLS2") {
            problemsHolder.registerProblem(
                lvalue,
                CaosBundle.message("caos.annotator.syntax-error-annotator.setv-cl2-is-c2-only"),
                CaosScriptCls2ToClasFix(element)
            )
        }
    }

    private fun annotateAssignment(
        variant: CaosVariant,
        commandString: String,
        element: CaosScriptIsCommandToken,
        valueElement: CaosScriptRvalue,
        expectedType: CaosExpressionValueType,
        actualType: CaosExpressionValueType,
        problemsHolder: ProblemsHolder
    ) {

        if (actualType like expectedType) {
            return
        }

        val replacement = when {
            actualType.isNumberType -> "SETV".matchCase(commandString, variant)
            actualType.isStringType -> "SETS".matchCase(commandString, variant)
            actualType.isAgentType -> "SETA".matchCase(commandString, variant)
            else -> null
        }
        val typeName = when {
            expectedType.isNumberType -> "a numeric"
            expectedType.isStringType -> "a string"
            expectedType.isAgentType -> "an agent"
            else -> return
        }
        registerProblemWithFix(
            commandString = commandString,
            commandElement = element,
            rvalueElement = valueElement,
            expectedType = expectedType,
            expectedTypeDescription = typeName,
            actualType = actualType,
            replacement = replacement,
            problemsHolder = problemsHolder
        )
    }

    private fun registerProblemWithFix(
        commandString: String,
        commandElement: CaosScriptIsCommandToken,
        rvalueElement: PsiElement,
        expectedType: CaosExpressionValueType,
        expectedTypeDescription: String,
        actualType: CaosExpressionValueType,
        replacement: String?,
        problemsHolder: ProblemsHolder
    ) {

        // Create message using suggestion text if any
        var message = CaosBundle.message(
            "caos.inspections.assignments.wrong-type",
            commandString.uppercase(),
            expectedTypeDescription,
            actualType.simpleName
        )

        // If there is a possible replacement, use it
        if (replacement != null) {
            message += ". " + CaosBundle.message(
                "caos.inspections.assignments.use-replacement",
                replacement,
                actualType.simpleName
            )
        }

        // Create fix if possible
        val replacementFix: Array<out LocalQuickFix> = replacement
            ?.let {
                val replaceWordFix = CaosScriptReplaceWordFix(replacement, commandElement)
                if (actualType.isNumberType && expectedType == STRING) {
                    val value = rvalueElement.text
                    arrayOf(
                        replaceWordFix,
                        CaosScriptReplaceElementFix(
                            rvalueElement,
                            "vtos $value",
                            "Prefix ${actualType.simpleName.lowercase()} '$value' with 'VTOS'"
                        )
                    )
                } else {
                    arrayOf(replaceWordFix)
                }
            } ?: emptyArray()
        problemsHolder.registerProblem(rvalueElement, message, *replacementFix)
    }
}