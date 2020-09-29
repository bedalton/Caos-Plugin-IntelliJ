package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.psi.util.PsiTreeUtil

object CaosScriptInferenceUtil {

    fun getInferredType(element: CaosScriptRvalue): CaosExpressionValueType {
        if (element.incomplete != null)
            return CaosExpressionValueType.UNKNOWN
        val expression = element.expression
        (element.varToken ?: expression?.varToken)?.let { return getInferredType(it) }
        (element.namedConstant ?: expression?.namedConstant)?.let { return getInferredType(it) }
        (element.namedGameVar ?: expression?.namedGameVar)?.let { return getInferredType(it) }
        (element.namedVar ?: expression?.namedVar)?.let { return getInferredType(it) }
        if (expression != null) {
            return when {
                expression.animation != null -> CaosExpressionValueType.ANIMATION
                expression.isC1String -> CaosExpressionValueType.C1_STRING
                expression.isFloat -> CaosExpressionValueType.FLOAT
                expression.isInt -> CaosExpressionValueType.INT
                expression.isNumeric -> CaosExpressionValueType.DECIMAL
                expression.isQuoteString -> CaosExpressionValueType.STRING
                expression.byteString != null -> CaosExpressionValueType.BYTE_STRING
                expression.isToken -> CaosExpressionValueType.TOKEN
                expression.isString -> CaosExpressionValueType.STRING
                expression.pictDimensionLiteral != null -> CaosExpressionValueType.PICT_DIMENSION
                else -> CaosExpressionValueType.ANY
            }
        }
        return getInferredType(element.rvaluePrime ?: expression?.rvaluePrime) ?: CaosExpressionValueType.UNKNOWN
    }

    fun getInferredType(prime: CaosScriptRvaluePrime?): CaosExpressionValueType? {
        if (prime == null)
            return null
        val commandString = prime.commandStringUpper
        val variant = prime.variant
                ?: return CaosExpressionValueType.UNKNOWN
        return CaosDefCommandElementsByNameIndex.Instance[commandString, prime.project]
                .filter {
                    variant in it.variants && it.isRvalue
                }
                .map { it.simpleReturnType }
                .firstOrNull()
    }

    fun getInferredValue(element: CaosScriptRvalue): CaosVar? {
        val variable = element.variable
                ?: return element.toCaosVar()
        return getInferredValue(variable)
    }


    fun getInferredValue(assignment: CaosScriptCAssignment): CaosVar? {
        if (assignment.cAssignKwL != null || assignment.cKwNegv != null) {
            val lvalue = (assignment.arguments.getOrNull(0) as? CaosScriptLvalue)
                    ?: return null
            val variable: CaosScriptIsVariable = lvalue.varToken as? CaosScriptIsVariable
                    ?: lvalue.namedGameVar
                    ?: lvalue.namedVar
                    ?: return null
            val value = getInferredValue(variable)
            if (assignment.cKwNegv != null) {
                return when (value) {
                    is CaosVar.CaosLiteral.CaosInt -> return CaosVar.CaosLiteral.CaosInt(-value.value)
                    is CaosVar.CaosLiteral.CaosFloat -> return CaosVar.CaosLiteral.CaosFloat(-value.value)
                    is CaosVar.CaosLiteral.CaosIntRange -> return CaosVar.CaosLiteral.CaosIntRange(value.min?.let { -it }, value.max?.let { -it })
                    is CaosVar.CaosLiteral.CaosFloatRange -> return CaosVar.CaosLiteral.CaosFloatRange(value.min?.let { -it }, value.max?.let { -it })
                    else -> value
                }
            }
            return value
        }

        if (assignment.cKwAdds != null || assignment.cKwAssignChar != null || assignment.cAssignKwNetUnik != null)
            return CaosVar.CaosLiteralStringVal
        if (assignment.cKwAssignGene != null)
            return CaosVar.CaosLiteralIntVal
        val rvalue = (assignment.arguments.getOrNull(1) as? CaosScriptRvalue)
                ?: return null
        return getInferredValue(rvalue)
    }

    fun getInferredValue(element: CaosScriptIsVariable?): CaosVar? {
        if (element == null)
            return null
        val startOffset = element.startOffset
        val scope = CaosScriptPsiImplUtil.getScope(element)
        return (element
                .getParentOfType(CaosScriptEventScript::class.java)?.let {
                    PsiTreeUtil.collectElementsOfType(it, CaosScriptCAssignment::class.java)
                } ?: PsiTreeUtil.collectElementsOfType(element.containingFile, CaosScriptCAssignment::class.java))
                .filter { it.endOffset < startOffset && it.scope.sharesScope(scope) }
                .sortedByDescending { it.endOffset }
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
                ?: CaosExpressionValueType.ANY
    }

    internal fun isSimilar(element: CaosScriptIsVariable, otherElement: CaosScriptLvalue): Boolean {
        if (element !is CaosScriptNamedConstant && element.getParentOfType(CaosScriptScriptElement::class.java)?.isEquivalentTo(otherElement.getParentOfType(CaosScriptScriptElement::class.java)).orFalse())
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
            return element.name == otherElement.name
        }
        if (element is CaosScriptNamedVar) {
            if (otherElement !is CaosScriptNamedVar)
                return false
            return element.name == otherElement.name
        }
        if (element is CaosScriptNamedConstant) {
            if (otherElement !is CaosScriptNamedConstant)
                return false
            return element.name == otherElement.name
        }
        return false
    }

    private val skipTypes = listOf(
            CaosExpressionValueType.ANY,
            CaosExpressionValueType.UNKNOWN,
            CaosExpressionValueType.NULL,
            CaosExpressionValueType.VARIABLE
    )
}


val CaosScriptRvalue.variable: CaosScriptIsVariable?
    get() {
        return varToken as? CaosScriptIsVariable
                ?: namedGameVar
                ?: namedVar
                ?: namedConstant
                ?: expression?.let {
                    it.varToken as? CaosScriptIsVariable
                            ?: it.namedGameVar
                            ?: it.namedVar
                            ?: it.namedConstant
                }
    }

fun CaosScriptIsVariable?.getAssignments(): List<CaosScriptCAssignment> {
    if (this == null)
        return emptyList()
    val startOffset = startOffset
    val scope = CaosScriptPsiImplUtil.getScope(this)
    val assignments = this
            .getParentOfType(CaosScriptEventScript::class.java)?.let {
                PsiTreeUtil.collectElementsOfType(it, CaosScriptCAssignment::class.java)
            } ?: PsiTreeUtil.collectElementsOfType(containingFile, CaosScriptCAssignment::class.java)
    return assignments
            .filter { it.endOffset < startOffset && it.scope.sharesScope(scope) }
            .sortedByDescending { it.endOffset }
}