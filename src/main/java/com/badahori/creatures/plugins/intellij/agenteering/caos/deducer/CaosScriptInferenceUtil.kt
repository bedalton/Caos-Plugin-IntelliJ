package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedGameVarIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object CaosScriptInferenceUtil {

    /**
     * Infers the type of an rvalue used in a CAOS script
     */
    fun getInferredType(element: CaosScriptRvalue, resolveVars: Boolean = true): CaosExpressionValueType {

        // If rvalue has any of these values
        // It is an incomplete command call, and cannot be resolved
        if (anyNotNull(element.incomplete,element.errorRvalue, element.rvaluePrefixIncompletes))
            return UNKNOWN

        // If element has var token set,
        // infer value of variable at last assignment if desired
        element.varToken?.let {  return if (resolveVars) getInferredType(it) ?: VARIABLE else VARIABLE }

        // If element has named var present,
        // infer value of variable at last assignment if desired
        element.namedGameVar?.let { return if (resolveVars) getInferredType(it) ?: VARIABLE else VARIABLE }
        // Return inferred types for simple values
        return when {
            element.isInt -> INT
            element.isFloat -> FLOAT
            element.isNumeric -> DECIMAL
            element.isQuoteString -> STRING
            element.isC1String -> C1_STRING
            element.isToken -> TOKEN
            element.byteString != null -> BYTE_STRING
            element.animationString != null -> ANIMATION
            element.pictDimensionLiteral != null -> PICT_DIMENSION
            else -> null
        } ?: getInferredType(element.rvaluePrime) ?: UNKNOWN
    }

    /**
     * Infer type for rvalue command calls
     */
    fun getInferredType(prime: CaosScriptRvaluePrime?): CaosExpressionValueType? {
        return prime?.commandDefinition?.returnType
    }

    /**
     * Get inferred value for an rvalue
     * TODO
     */
    private fun getInferredType(element: CaosScriptRvalue): CaosExpressionValueType? {
        val variable = element.variable
                ?: return element.inferredType
        return getInferredType(variable)
    }


    /**
     * Gets inferred type for a given variable
     * Checks for previous assignments for last assigned value
     */
    fun getInferredType(element: CaosScriptIsVariable?): CaosExpressionValueType? {
        if (element == null)
            return null
        // Get all assignments for this variable
        return element.getAssignments()
                .mapNotNull map@{ assignment ->
                    ProgressIndicatorProvider.checkCanceled()
                    if (assignment.lvalue?.let { !isSimilar(element, it) }.orFalse())
                        return@map null
                    getInferredType(assignment)?.let { type ->
                        if (type in skipTypes)
                            null
                        else
                            type
                    }
                }
                .firstOrNull()
    }

    /**
     * Helper function to find assigned expression type for an assignment command
     * ie. SETA/SETV/ADDV/NEGV
     */
    private fun getInferredType(assignment: CaosScriptCAssignment): CaosExpressionValueType? {
        // Quick return for commands that can only return int in old variants
        assignment.variant?.let {
            // On Old variants, we do not need to actually check types
            // As only integers and agents can be assigned
            // Agents are coerced into int values as needed
            // So if any commands require an integer
            // Return so early
            if (it.isOld) {
                // Check if any of the numeric assignment commands are used
                if (anyNotNull(assignment.cKwAssignNumber, assignment.cKwAssignAlter))
                    return INT
            }
        }

        // Checks for assign alter command,
        // This should really only operator on numbers
        assignment.cKwAssignAlter?.let {
            // Get lvalue
            val lvalue = (assignment.arguments.getOrNull(0) as? CaosScriptLvalue)
                    ?: return null
            // Get Lvalue vars
            val variable: CaosScriptIsVariable = lvalue.varToken as? CaosScriptIsVariable
                    ?: lvalue.namedGameVar
                    // If lvalue is not numbered var or named var
                    // It is probably an lvalue command
                    // Get its type and return it simply
                    ?: return lvalue.commandDefinition?.returnType

            // Find last value of variable being altered
            // Mostly used to check if it was an int or a float
            // Infer type for this sub variable
            return getInferredType(variable)?.let {
                // TODO: Should we make assumptions about the value stored within
                //  Only numbers can be alteredAssigned, so should we
                //  Assume it cannot be anything else at this point
                if (it != INT && it != FLOAT)
                    DECIMAL
                else
                    it
            }
        }

        // Check for String-like assignments
        if (anyNotNull(assignment.cKwAssignString, assignment.cKwAdds, assignment.cKwAssignChar, assignment.cAssignKwNetUnik))
            return STRING

        // Check for gene assignments
        if (assignment.cKwAssignGene != null)
            return INT

        // If SETA, engine asserts this must be an agent
        // TODO: should type still be checked.
        if (assignment.cKwAssignAgent != null)
            return AGENT

        // Get second argument as assigned rvalue
        // TODO: see if there are any exceptions to this rule in the BNF grammar
        val rvalue = (assignment.arguments.getOrNull(1) as? CaosScriptRvalue)
                ?: return null

        // Get the inferred type of this rvalue
        // Should really only be numeric at this point
        // Or possibly agent, if variant is C1e
        return getInferredType(rvalue)
    }

    /**
     * Checks that on variable matches another lvalue
     */
    private fun isSimilar(element: CaosScriptIsVariable, otherElement: CaosScriptLvalue): Boolean {
        if (element.getParentOfType(CaosScriptScriptElement::class.java)?.isEquivalentTo(otherElement.getParentOfType(CaosScriptScriptElement::class.java)).orFalse())
            return false
        if (element is CaosScriptVarToken) {
            if (otherElement !is CaosScriptVarToken)
                return false
            if (element.varGroup != otherElement.varGroup)
                return false
            return element.varIndex == otherElement.varIndex
        }
        if (element is CaosScriptNamedGameVar) {
            if (otherElement !is CaosScriptNamedGameVar)
                return false
            if (element.varType != otherElement.varType)
                return false
            return element.name == (otherElement as CaosScriptNamedGameVar).name
        }
        return false
    }

    /**
     * Types to skip when checking for valid inferred types
     * If inferred type is in this list, it keeps searching
     */
    private val skipTypes = listOf(
            ANY,
            UNKNOWN,
            NULL,
            VARIABLE
    )
}

/**
 * Simplifies getting the variable from an rvalue
 */
val CaosScriptRvalue.variable: CaosScriptIsVariable?
    get() {
        return varToken as? CaosScriptIsVariable
                ?: namedGameVar
    }

/**
 * Finds all previous assignments matching this variable
 */
fun CaosScriptIsVariable?.getAssignments(): List<CaosScriptCAssignment> {
    if (this == null)
        return emptyList()
    val startOffset = startOffset
    val scope = CaosScriptPsiImplUtil.getScope(this)
    // Determine the assignments to return based on variable type
    if (this is CaosScriptVarToken) {
        // Only resolve event scripts assignments
        // TODO resolve variables types outside of event script scope
        if (this.varGroup != CaosScriptVarTokenGroup.VARx && this.varGroup != CaosScriptVarTokenGroup.VAxx)
            return emptyList()

        // At this point variable can only be an event variable
        // Limit returned assignments to those declared within the same event
        val assignments = this
                .getParentOfType(CaosScriptEventScript::class.java)?.let { parentEventScript ->
                    PsiTreeUtil.collectElementsOfType(parentEventScript, CaosScriptCAssignment::class.java)
                } ?: return emptyList()
        // Filter assignments by scope and when they are assigned
        // Conflicting variants out of scope should not be returned
        // Also later assignments should not be returned either
        return assignments
                .filter {
                    it.endOffset < startOffset && it.scope.sharesScope(scope)
                }
                .sortedByDescending { it.endOffset }
    } else if (this is CaosScriptNamedGameVar) {
        // Only find assignments to GAME and EAME variables, as they are universal
        // And not tied to an agent
        // TODO implement filtering on NAME and MAME variables by agent classifier
        if (this.varType != CaosScriptNamedGameVarType.EAME && this.varType != CaosScriptNamedGameVarType.GAME)
            return emptyList()
        return CaosScriptNamedGameVarIndex
                .instance[this.varType, this.key ?: "", this.project]
                .mapNotNull {
                    // Return this assignment only if its parent is an lvalue
                    // and that lvalue is a direct descendant of an assignment command
                    if (it.parent is CaosScriptLvalue)
                        it.parent?.parent as? CaosScriptCAssignment
                    else
                        null
                }
    } else
        return emptyList()
}

/**
 * Returns true if any element passed in is non-null
 */
private fun anyNotNull(vararg elements:PsiElement?) : Boolean {
    return elements.any{ it != null }
}