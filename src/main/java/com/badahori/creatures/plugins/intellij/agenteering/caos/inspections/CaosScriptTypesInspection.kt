package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosParameter
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

/**
 * Inspection for validating expected parameter types with actual argument types
 */
class CaosScriptTypesInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Expected parameter type"
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = "ExpectedParameterType"

    /**
     * Gets a visitor responsible for applying annotations as needed
     */
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
        }
    }

    /**
     * Annotates a command-like element's arguments
     */
    private fun annotateArgument(element: CaosScriptCommandElement, holder: ProblemsHolder) {
        // Get command definition for this element if found in universal lib
        val commandDefinition = element.commandDefinition
                ?: return
        // Get variant for this file
        val variant = element.variant
                ?: return
        // Get parameters for this command call
        val parameters = commandDefinition.parameters
        // Get arguments for this command call
        val arguments = element.getChildrenOfType(CaosScriptArgument::class.java)
        // If there are less parameters than arguments
        // Grammar does not align with lib json
        // Needs to be checked
        // @todo should this be an error?
        if (parameters.size < arguments.size) {
            LOGGER.severe("BNF grammar definitions mismatched. BNF parse of ${commandDefinition.command} allowed ${arguments.size} arguments when expecting: ${parameters.size}")
            return
        }
        // Iterate over the arguments and annotate as necessary
        for (i in arguments.indices) {
            val argument = arguments[i]
            val parameter = parameters[i]
            annotateArgument(variant, parameter, argument, holder)
        }
    }

    /**
     * Annotate argument for expected type vs. actual type
     */
    private fun annotateArgument(variant: CaosVariant, parameter: CaosParameter, argument: CaosScriptArgument, holder: ProblemsHolder) {
        // If is LValue, simply return as grammar forces lvalues where they need to be
        if (argument is CaosScriptLvalue)
            return
        // If token rvalue, return, no type checking needs to be made
        // As token place is hardcoded in grammar
        if (argument is CaosScriptTokenRvalue)
            return
        // If this argument is not a rvalue,
        // then this command needs to be updated after grammar change
        if (argument !is CaosScriptRvalueLike)
            throw Exception("Unexpected argument type encountered")

        // If value is a var token, return as it can be anything
        if (argument.varToken != null)
            return

        // If named variable, return as it can be anything
        if (argument.namedGameVar != null)
            return

        // Get actual type of this argument
        val actualType = getActualType(argument) { it.inferredType }.nullIfEmpty()
            ?: return

        // Get simple type and convert it to analogous type if needed
        val expectedTypeSimple = parameter.type

        // compare Actual type to expected type
        // Fudge types when similar enough
        if (expectedTypeSimple in actualType || fudge(variant, actualType, expectedTypeSimple)) {
            return
        }

        // If Expected type is TOKEN
        // Ensure that token is valid
        if (expectedTypeSimple == TOKEN) {
            // Tokens in C1/C2 must be 4 letters long
            if ((argument as CaosScriptArgument).text.length == 4)
                return
            // Tokens in CV+ can be any length
            else if (variant.isNotOld)
                return
        }

        // In old variants, agent return types are integers, and can be used as such
        // Check if expecting integer, and actual type is agent
        if (variant.isOld && expectedTypeSimple == INT && AGENT in actualType) {
            // This coercion will be handled by another inspection
            return
        }
        // Create error message
        val message = if (actualType.size == 1) {
            CaosBundle.message("caos.annotator.syntax-error-annotator.incorrect-parameter-type-without-name-message", expectedTypeSimple.simpleName, actualType[0].simpleName)
        } else {
            CaosBundle.message("caos.annotator.syntax-error-annotator.incorrect-parameter-type-without-name-message", expectedTypeSimple.simpleName, actualType.distinct().joinToString(" or ") { it.simpleName })
        }
        // Annotate element with type error
        holder.registerProblem(argument, message)
    }

    /**
     * Gets the actual type for a variable
     */
    private fun getActualType(element: CaosScriptRvalueLike, caosVar: (element:CaosScriptRvalueLike) -> List<CaosExpressionValueType>): List<CaosExpressionValueType> {
        return element.inferredType
                .let {
                    if (ANY in it || UNKNOWN in it)
                        null
                    else
                        it
                }
                // Type inference returned non-concrete type
                // Try for command definition
                ?: (element.rvaluePrime)?.let {
                    it.commandDefinition?.returnType?.toListOf()
                }
                // element is not a rvalue command call, get simple type
                ?: caosVar(element)
    }

    companion object {

        // Similar var types
        private val BYTE_STRING_LIKE = listOf(BYTE_STRING, C1_STRING, ANIMATION)
        private val INT_LIKE = listOf(INT, DECIMAL)
        private val FLOAT_LIKE = listOf(FLOAT, DECIMAL, INT)
        private val ANY_TYPE = listOf(ANY, VARIABLE, UNKNOWN)
        private val NUMERIC = listOf(FLOAT, DECIMAL, INT)
        private val AGENT_LIKE = listOf(AGENT, NULL)
        private val STRING_LIKE = listOf(STRING, C1_STRING, HEXADECIMAL)

        /**
         * Determine whether types are similar despite having actually different types
         */
        fun fudge(variant: CaosVariant, actualType: List<CaosExpressionValueType>, expectedType: CaosExpressionValueType): Boolean {
            if (actualType likeAny ANY_TYPE || expectedType in ANY_TYPE)
                return true
            if (actualType likeAny BYTE_STRING_LIKE && expectedType in BYTE_STRING_LIKE)
                return true
            if (actualType likeAny INT_LIKE && expectedType in INT_LIKE)
                return true
            if (actualType likeAny FLOAT_LIKE && expectedType in FLOAT_LIKE)
                return true
            if (actualType likeAny AGENT_LIKE && expectedType in AGENT_LIKE)
                return true
            if (actualType likeAny STRING_LIKE && expectedType in STRING_LIKE)
                return true
            if (variant.isNotOld)
                return actualType likeAny NUMERIC && expectedType in NUMERIC
            return false
        }
        /**
         * Determine whether types are similar despite having actually different types
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