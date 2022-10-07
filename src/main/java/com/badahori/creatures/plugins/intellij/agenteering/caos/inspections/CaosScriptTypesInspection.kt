package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.getRvalueTypeWithoutInference
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosParameter
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptRvalueLikeMixin
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType

/**
 * Inspection for validating expected parameter types with actual argument types
 */
class CaosScriptTypesInspection : LocalInspectionTool(), DumbAware {

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
                super.visitCommandCall(o)
                val command = o.firstChild as? CaosScriptCommandElement
                    ?: return
                annotateArgument(command, holder)
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
        val arguments = element.arguments
        // If there are less parameters than arguments
        // Grammar does not align with lib json
        // Needs to be checked
        // @todo should this be an error?
        if (parameters.size < arguments.size) {
            LOGGER.severe("BNF grammar definitions mismatched. BNF parse of ${commandDefinition.command} allowed ${arguments.size} arguments when expecting: ${parameters.size}")
            return
        }
        if (parameters.isEmpty())
            return

        if (arguments.isEmpty()) {
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
    @Suppress("unused")
    private fun annotateArgument(variant: CaosVariant, parameter: CaosParameter, argument: CaosScriptArgument, holder: ProblemsHolder) {
        // If is LValue, simply return as grammar forces lvalues where they need to be
        if (argument is CaosScriptLvalue)
            return
        // If token rvalue, return, no type checking needs to be made
        // As token place is hardcoded in grammar
        if (argument is CaosScriptRvalueLikeMixin<*>)
            return
        // If this argument is not a rvalue,
        // then this command needs to be updated after grammar change
        if (argument !is CaosScriptRvalueLike)
            throw Exception("Unexpected argument type encountered. Type: ${argument.elementType}(${argument.text})")


        // If value is a var token, return as it can be anything
        if (argument.varToken != null)
            return

        // If named variable, return as it can be anything
        if (argument.namedGameVar != null)
            return

        // Get simple type and convert it to analogous type if needed
        val expectedTypeSimple = parameter.type

        // Get actual type of this argument
        val actualType = getRvalueTypeWithoutInference(argument, expectedTypeSimple)
            ?: return


        // compare Actual type to expected type
        // Fudge types when similar enough
        if (expectedTypeSimple like actualType) {
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
        if (variant.isOld && expectedTypeSimple == INT && actualType like AGENT) {
            // This coercion will be handled by another inspection
            return
        }

        // Create error message
//        val message = if (actualType.size == 1) {
//            CaosBundle.message("caos.annotator.syntax-error-annotator.incorrect-parameter-type-without-name-message", expectedTypeSimple.simpleName, actualType[0].simpleName)
//        } else {
//            CaosBundle.message("caos.annotator.syntax-error-annotator.incorrect-parameter-type-without-name-message", expectedTypeSimple.simpleName, actualType.distinct().joinToString(" or ") { it.simpleName })
//        }
        val message = CaosBundle.message("caos.annotator.syntax-error-annotator.incorrect-parameter-type-without-name-message", expectedTypeSimple.simpleName, actualType.simpleName)
        // Annotate element with type error
        holder.registerProblem(argument, message)
    }

    /**
     * Gets the actual type for a variable
     */
    @Suppress("unused")
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
}