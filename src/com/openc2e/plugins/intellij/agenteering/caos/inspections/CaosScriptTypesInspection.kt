package com.openc2e.plugins.intellij.agenteering.caos.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.openc2e.plugins.intellij.agenteering.caos.annotators.AnnotationHolderWrapper
import com.openc2e.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.*

class CaosScriptTypesInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Expected parameter type"
    override fun getGroupDisplayName(): String = "CaosScript"
    override fun getShortName(): String = "ExpectedParameterType"
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitExpectsAgent(o: CaosScriptExpectsAgent) {
                super.visitExpectsAgent(o)
                annotateArgument(o, holder)
            }

            override fun visitExpectsByteString(o: CaosScriptExpectsByteString) {
                super.visitExpectsByteString(o)
                annotateArgument(o, holder)
            }

            override fun visitExpectsDecimal(o: CaosScriptExpectsDecimal) {
                super.visitExpectsDecimal(o)
                annotateArgument(o, holder)
            }
            override fun visitExpectsC1String(o: CaosScriptExpectsC1String) {
                super.visitExpectsC1String(o)
                annotateArgument(o, holder)
            }

            override fun visitExpectsFloat(o: CaosScriptExpectsFloat) {
                super.visitExpectsFloat(o)
                annotateArgument(o, holder)
            }

            override fun visitExpectsInt(o: CaosScriptExpectsInt) {
                super.visitExpectsInt(o)
                annotateArgument(o, holder)
            }

            override fun visitExpectsQuoteString(o: CaosScriptExpectsQuoteString) {
                super.visitExpectsQuoteString(o)
                annotateArgument(o, holder)
            }

            override fun visitExpectsToken(o: CaosScriptExpectsToken) {
                super.visitExpectsToken(o)
                annotateArgument(o, holder)
            }

            override fun visitExpectsValue(o: CaosScriptExpectsValue) {
                super.visitExpectsValue(o)
                annotateArgument(o, holder)
            }
        }
    }

    private fun annotateArgument(element: CaosScriptExpectsValueOfType, holder: ProblemsHolder) {
        val rvalue = element.rvalue ?: return
        val actualType = (rvalue.expression ?: rvalue).getChildOfType(CaosScriptIsVariable::class.java)
                ?.getInferredType()
                ?: getActualType(rvalue, rvalue.toCaosVar())
        var expectedTypeSimple = element.expectedType
        if (expectedTypeSimple == CaosExpressionValueType.DECIMAL && expectsInt(element)) {
            expectedTypeSimple = CaosExpressionValueType.INT
        }
        if (actualType == expectedTypeSimple || fudge(actualType, expectedTypeSimple)) {
            return
        }

        if (expectedTypeSimple == CaosExpressionValueType.TOKEN && element.text.length == 4) {
            return
        }
        val message = CaosBundle.message("caos.annotator.command-annotator.incorrect-parameter-type-without-name-message", expectedTypeSimple.simpleName, actualType.simpleName)
        holder.registerProblem(element,message)
        /*
        val index = element.index
        val matchingParameter = getMatchingParameter(element, index)
                ?: return
        val expectedType = getCaosTypeStringAsType(matchingParameter.type.type)
        validateType(element, matchingParameter.name, expectedType, actualType, annotationWrapper)*/
    }

    private fun expectsInt(element: CaosScriptExpectsValueOfType) : Boolean {
        val matchingParameter = getMatchingParameter(element, element.index)
                ?: return false
        return matchingParameter.type.type.let {
            it == "integer" || it == "int"
        }
    }

    private fun getMatchingParameter(element: CaosScriptCompositeElement, index: Int): CaosDefParameterStruct? {
        val parent = element.getParentOfType(CaosScriptCommandElement::class.java)
        val containingToken = parent
                ?.commandToken
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element as? CaosDefCompositeElement
                ?: return null
        val containingCommand = containingToken.getParentOfType(CaosDefCommandDefElement::class.java)
        return containingCommand?.parameterStructs?.getOrNull(index)
    }

    private fun getActualType(element: CaosScriptRvalue, caosVar: CaosVar): CaosExpressionValueType {
        val commandToken = element.commandToken
                ?: return caosVar.simpleType
        val returnTypeString = (commandToken
                .reference
                .multiResolve(true)
                .firstOrNull()
                ?.element as? CaosDefCompositeElement)
                ?.getParentOfType(CaosDefCommandDefElement::class.java)
                ?.returnTypeString
        return returnTypeString?.let { getCaosTypeStringAsType(it) } ?: caosVar.simpleType
    }

    private fun validateType(element: PsiElement, parameterName: String, expectedType: CaosExpressionValueType, actualType: CaosExpressionValueType, annotationWrapper: AnnotationHolderWrapper) {
        if (actualType == expectedType || fudge(actualType, expectedType))
            return
        val message = CaosBundle.message("caos.annotator.command-annotator.incorrect-parameter-type-message", parameterName, expectedType.simpleName, actualType.simpleName)
        annotationWrapper.newWarningAnnotation(message)
                .range(element)
                .create()
    }

    companion object {
        fun getCaosTypeStringAsType(typeStringIn: String): CaosExpressionValueType {
            return CaosExpressionValueType.fromSimpleName(typeStringIn)
        }

        // Similar var types
        private val BYTE_STRING_LIKE = listOf(CaosExpressionValueType.BYTE_STRING, CaosExpressionValueType.C1_STRING, CaosExpressionValueType.ANIMATION)
        private val INT_LIKE = listOf(CaosExpressionValueType.INT, CaosExpressionValueType.DECIMAL)
        private val FLOAT_LIKE = listOf(CaosExpressionValueType.FLOAT, CaosExpressionValueType.DECIMAL)
        private val ANY_TYPE = listOf(CaosExpressionValueType.ANY, CaosExpressionValueType.VARIABLE, CaosExpressionValueType.UNKNOWN)

        /**
         * Determine whether types are similar despite having different types
         */
        fun fudge(actualType: CaosExpressionValueType, expectedType: CaosExpressionValueType): Boolean {
            if (actualType in ANY_TYPE || expectedType in ANY_TYPE)
                return true
            if (actualType in BYTE_STRING_LIKE && expectedType in BYTE_STRING_LIKE)
                return true
            if (actualType in INT_LIKE && expectedType in INT_LIKE)
                return true
            if (actualType in FLOAT_LIKE && expectedType in FLOAT_LIKE)
                return true
            return false
        }
    }
}