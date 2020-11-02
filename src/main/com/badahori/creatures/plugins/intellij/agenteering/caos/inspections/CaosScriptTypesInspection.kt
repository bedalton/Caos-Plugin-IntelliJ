package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getParentOfType
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class CaosScriptTypesInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Expected parameter type"
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = "ExpectedParameterType"
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitRvaluePrime(o: CaosScriptRvaluePrime) {
                annotateArgument(o, holder)
                super.visitRvaluePrime(o)
            }

            override fun visitLvalue(o: CaosScriptLvalue) {
                annotateArgument(o, holder)
                super.visitLvalue(o)
            }

            override fun visitCommandCall(o: CaosScriptCommandCall) {
                annotateArgument(o, holder)
                super.visitCommandCall(o)
            }

            override fun visitRvalue(o: CaosScriptRvalue) {
                // Ignore as checks are called on RvaluePrime
                super.visitRvalue(o)
            }
        }
    }

    private fun annotateArgument(element: CaosScriptCommandLike, holder: ProblemsHolder) {

        val token = element.commandToken
                ?: return

        val variant = element.containingCaosFile?.variant
                ?: return

        val matches = token
                .reference
                .multiResolve(true)
                .mapNotNull {
                    it.element?.getParentOfType(CaosDefCommandDefElement::class.java)
                }

        val match = when (matches.size) {
            0 -> return
            1 -> matches.first()
            2 -> {
                if (variant != CaosVariant.CV) {
                    LOGGER.warning("CAOS command ${token.text.toUpperCase()} has multiple matches in ${variant.code}")
                    return
                }
                when (token.text.toUpperCase()) {
                    "CHAR" -> matches.firstOrNull { it.simpleReturnType !== CaosExpressionValueType.STRING }
                            ?: matches.first()
                    "TRAN" -> matches.firstOrNull { it.simpleReturnType !== CaosExpressionValueType.STRING }
                            ?: matches.first()
                    else -> return
                }
            }
            else -> throw Exception("Unexpected number of command matches found for command: ${token.text.toUpperCase()}. Expected 1 or 2, found: ${matches.size}")
        }


        val arguments = element.getChildrenOfType(CaosScriptArgument::class.java)

        val parameters = match.parameterStructs
        if (parameters.size < arguments.size) {
            LOGGER.severe("BNF grammar definitions mismatched. BNF parse allowed ${arguments.size} arguments when expecting: ${parameters.size}")
            return
        }
        for (i in arguments.indices) {
            val argument = arguments[i]
            if (argument is CaosScriptLvalue)
                continue
            if (argument is CaosScriptTokenRvalue)
                continue
            if (argument !is CaosScriptRvalueLike)
                throw Exception("Unexpected argument type encountered")
            if (argument.varToken != null)
                continue
            if (argument.namedGameVar != null)
                continue
            val rvalue = argument as CaosScriptRvalueLike
            val actualType = rvalue.inferredType
                    .let {
                        if (it == CaosExpressionValueType.ANY || it == CaosExpressionValueType.UNKNOWN)
                            null
                        else
                            it
                    }
                    ?: getActualType(argument) { argument.toCaosVar() }
                    ?: throw Exception("Failed to understand expression value. Not all paths were handled with expression '${rvalue.text}'")
            val parameter = parameters[i]

            // Get simple type and convert it to analogous type if needed
            val expectedTypeSimple = parameter.simpleType

            // Actual
            if (actualType == expectedTypeSimple || fudge(variant, actualType, expectedTypeSimple)) {
                return
            }

            if (expectedTypeSimple == CaosExpressionValueType.TOKEN) {
                if (element.text.length == 4)
                    return
                else if (variant.isNotOld)
                    return
            }
            val message = CaosBundle.message("caos.annotator.command-annotator.incorrect-parameter-type-without-name-message", expectedTypeSimple.simpleName, actualType.simpleName)
            holder.registerProblem(element, message)
        }
    }

    private fun getActualType(element: CaosScriptRvalueLike, caosVar: () -> CaosVar): CaosExpressionValueType {
        val commandToken = element.commandToken
                ?: return caosVar().simpleType
        val returnTypeString = (commandToken
                .reference
                .multiResolve(true)
                .firstOrNull()
                ?.element as? CaosDefCompositeElement)
                ?.getParentOfType(CaosDefCommandDefElement::class.java)
                ?.returnTypeString
        return returnTypeString?.let { getCaosTypeStringAsType(it) } ?: caosVar().simpleType
    }

    companion object {
        fun getCaosTypeStringAsType(typeStringIn: String): CaosExpressionValueType {
            return CaosExpressionValueType.fromSimpleName(typeStringIn)
        }

        // Similar var types
        private val BYTE_STRING_LIKE = listOf(CaosExpressionValueType.BYTE_STRING, CaosExpressionValueType.C1_STRING, CaosExpressionValueType.ANIMATION)
        private val INT_LIKE = listOf(CaosExpressionValueType.INT, CaosExpressionValueType.DECIMAL)
        private val FLOAT_LIKE = listOf(CaosExpressionValueType.FLOAT, CaosExpressionValueType.DECIMAL, CaosExpressionValueType.INT)
        private val ANY_TYPE = listOf(CaosExpressionValueType.ANY, CaosExpressionValueType.VARIABLE, CaosExpressionValueType.UNKNOWN)
        private val NUMERIC = listOf(CaosExpressionValueType.FLOAT, CaosExpressionValueType.DECIMAL, CaosExpressionValueType.INT)
        private val AGENT_LIKE = listOf(CaosExpressionValueType.AGENT, CaosExpressionValueType.NULL)
        private val STRING_LIKE = listOf(CaosExpressionValueType.STRING, CaosExpressionValueType.C1_STRING, CaosExpressionValueType.HEXADECIMAL)

        /**
         * Determine whether types are similar despite having different types
         */
        fun fudge(variant: CaosVariant, actualType: CaosExpressionValueType, expectedType: CaosExpressionValueType): Boolean {
            if (actualType in ANY_TYPE || expectedType in ANY_TYPE)
                return true
            if (actualType in BYTE_STRING_LIKE && expectedType in BYTE_STRING_LIKE)
                return true
            if (actualType in INT_LIKE && expectedType in INT_LIKE)
                return true
            if (actualType in FLOAT_LIKE && expectedType in FLOAT_LIKE)
                return true
            if (actualType in AGENT_LIKE && expectedType in AGENT_LIKE)
                return true
            if (actualType in STRING_LIKE && expectedType in STRING_LIKE)
                return true
            if (variant.isNotOld)
                return actualType in NUMERIC && expectedType in NUMERIC
            return false
        }
    }
}