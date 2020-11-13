package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiImplUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getAssignedType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.psi.util.PsiTreeUtil

object CaosScriptInferenceUtil {

    fun getInferredType(element: CaosScriptRvalue): CaosExpressionValueType {
        if (element.incomplete != null)
            return UNKNOWN
        element.varToken?.let { return getInferredType(it) }
        element.namedGameVar?.let { return getInferredType(it) }
        return when {
            element.isInt -> INT
            element.isFloat -> FLOAT
            element.isNumeric -> DECIMAL
            element.isQuoteString -> STRING
            element.isC1String -> C1_STRING
            element.byteString != null -> BYTE_STRING
            element.isToken -> TOKEN
            element.animationString != null -> ANIMATION
            element.pictDimensionLiteral != null -> PICT_DIMENSION
            else -> getInferredType(element.rvaluePrime) ?: ANY
        }
    }

    fun getInferredType(prime: CaosScriptRvaluePrime?): CaosExpressionValueType? {
        if (prime == null)
            return null
        return prime.commandDefinition?.returnType
    }

    fun getInferredValue(element: CaosScriptRvalue): CaosExpressionValueType? {
        val variable = element.variable
                ?: return element.inferredType
        return getInferredValue(variable)
    }


    fun getInferredValue(assignment: CaosScriptCAssignment): CaosExpressionValueType? {
        val assignAlter = assignment.cKwAssignAlter
        if (assignAlter != null) {
            val lvalue = (assignment.arguments.getOrNull(0) as? CaosScriptLvalue)
                    ?: return null
            val variable: CaosScriptIsVariable = lvalue.varToken as? CaosScriptIsVariable
                    ?: lvalue.namedGameVar
                    ?: return null
            return getInferredValue(variable)
        }

        if (assignment.cKwAdds != null || assignment.cKwAssignChar != null || assignment.cAssignKwNetUnik != null)
            return STRING
        if (assignment.cKwAssignGene != null)
            return INT
        val rvalue = (assignment.arguments.getOrNull(1) as? CaosScriptRvalue)
                ?: return null
        return getInferredValue(rvalue)
    }

    fun getInferredValue(element: CaosScriptIsVariable?): CaosExpressionValueType? {
        if (element == null)
            return UNKNOWN
        return element.getAssignments()
                .mapNotNull map@{ assignment ->
                    if (assignment.lvalue?.let { isSimilar(element, it) }.orFalse())
                        return@map null
                    getInferredValue(assignment)
                }
                .firstOrNull()
    }


    fun getInferredType(element: CaosScriptIsVariable): CaosExpressionValueType {
        return element.getAssignments()
                .mapNotNull map@{ assignment ->
                    if (assignment.lvalue?.let { isSimilar(element, it) }.orFalse())
                        return@map null
                    assignment.getAssignedType()?.let { type ->
                        if (type in skipTypes)
                            null
                        else
                            type
                    }
                }
                .firstOrNull()
                ?: ANY
    }

    internal fun isSimilar(element: CaosScriptIsVariable, otherElement: CaosScriptLvalue): Boolean {
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

    private val skipTypes = listOf(
            ANY,
            UNKNOWN,
            NULL,
            VARIABLE
    )
}


val CaosScriptRvalue.variable: CaosScriptIsVariable?
    get() {
        return varToken as? CaosScriptIsVariable
                ?: namedGameVar
    }

fun CaosScriptIsVariable?.getAssignments(): List<CaosScriptCAssignment> {
    if (this == null)
        return emptyList()
    val startOffset = startOffset
    val scope = CaosScriptPsiImplUtil.getScope(this)
    val assignments = this
            .getParentOfType(CaosScriptEventScript::class.java)?.let {
                PsiTreeUtil.collectElementsOfType(it, CaosScriptCAssignment::class.java)
            } ?: emptyList()
    return assignments
            .filter { it.endOffset < startOffset && it.scope.sharesScope(scope) }
            .sortedByDescending { it.endOffset }
}