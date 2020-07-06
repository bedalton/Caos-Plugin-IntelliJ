package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiImplUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.scope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset

object CaosScriptInferenceUtil {

    fun getInferredType(element: CaosScriptIsVariable): CaosExpressionValueType {
        return getInferredValue(element).let {
            (it as? CaosVar.CaosCommandCall)?.returnType
                    ?: it.simpleType
        }
    }

    fun getInferredValue(element: CaosScriptIsVariable): CaosVar {
        if (element is CaosScriptNamedConstant) {
            return PsiTreeUtil.getParentOfType(element.reference.resolve(), com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptConstantAssignment::class.java)
                    ?.constantValue?.let { constantValue ->
                        constantValue.int?.text?.toLong()?.let {
                            return CaosVar.CaosLiteral.CaosInt(it)
                        } ?: constantValue.float?.text?.toFloat()?.let {
                            return CaosVar.CaosLiteral.CaosFloat(it)
                        }
                    }
                    ?: CaosVar.CaosVarNone
        }
        val name = (element as? CaosScriptNamedVar)
                ?.let {
                    (element.reference.resolve() as? CaosScriptCompositeElement)
                            ?.getParentOfType(CaosScriptNamedVarAssignment::class.java)
                            ?.varToken
                            ?.text
                }
                ?: element.text
        val startOffset = element.startOffset
        val scope = CaosScriptPsiImplUtil.getScope(element)
        return (element
                .getParentOfType(CaosScriptEventScript::class.java)?.let {
                    PsiTreeUtil.collectElementsOfType(it, CaosScriptCAssignment::class.java)
                } ?: PsiTreeUtil.collectElementsOfType(element.containingFile, CaosScriptCAssignment::class.java))
                .asSequence()
                .mapNotNull {
                    it.arguments.lastOrNull()
                }
                .filter { it.endOffset < startOffset && it.scope.sharesScope(scope) }
                .sortedByDescending { it.endOffset }
                .mapNotNull { arg ->
                    val value = arg.toCaosVar()
                    val simpleType = value.simpleType
                    if (simpleType !in skipTypes) {
                        if (value is CaosVar.CaosCommandCall) {
                            if (value.returnType != null && value.returnType !in skipTypes)
                                value
                            else
                                null
                        } else {
                            value
                        }
                    } else
                        null
                }
                .filter { it.simpleType !in skipTypes }
                .firstOrNull() ?: CaosVar.CaosVarNone
    }

    private fun getValueInBlock(name: String, enclosingBlock: CaosScriptCodeBlock, scope: CaosScope, endRange: Int): CaosVar? {
        val rndvVal = enclosingBlock
                .codeBlockLineList
                .mapNotNull { line ->
                    line.commandCall?.cRndv
                }
                .filter {
                    CaosScriptPsiImplUtil.getScope(it).sharesScope(scope)
                }
                .map {
                    val minMax = it.rndvIntRange
                    val caosVar = CaosVar.CaosLiteral.CaosIntRange(minMax.first, minMax.second)
                    Pair(it.startOffset, caosVar)
                }
                .maxBy { it.first }
        val assignments = enclosingBlock
                .codeBlockLineList
                .mapNotNull {
                    it.commandCall?.cAssignment
                }
                .filter {
                    it.lvalue?.text == name && it.startOffset < endRange
                }
        val expressions = assignments
                .mapNotNull {
                    it.getChildOfType(CaosScriptExpectsValueOfType::class.java)
                }
                .filter {
                    val thisScope = CaosScriptPsiImplUtil.getScope(it)
                    scope.sharesScope(thisScope)
                }
                .sortedByDescending { it.startOffset }
        val rndvStart = rndvVal?.first ?: -1
        // Iterate through expressions to find type
        for (expression in expressions) {
            if (rndvStart > expression.startOffsetInParent) {
                rndvVal?.second?.let { return it }
            }
            val rvalue = expression.rvalue?.rvaluePrime
            if (rvalue != null) {
                getFromRValuePrime(rvalue)?.let {
                    return it
                }
            }
            val value = expression.toCaosVar()
            val simpleType = value.simpleType
            if (simpleType !in skipTypes) {
                if (value is CaosVar.CaosCommandCall) {
                    if (value.returnType != null && value.returnType !in skipTypes)
                        return value
                    else
                        continue
                }
                return value
            }
        }
        return rndvVal?.second
    }

    fun getInferredType(rvalue: CaosScriptRvalue): CaosExpressionValueType {
        val caosVar = rvalue.rvaluePrime?.toCaosVar() ?: rvalue.toCaosVar()
        return (caosVar as? CaosVar.CaosCommandCall)?.returnType
                ?: caosVar.simpleType
    }

    fun getFromRValuePrime(prime: CaosScriptRvaluePrime): CaosVar? {
        return prime.getChildOfType(CaosScriptIsCommandToken::class.java)?.let { token ->
            token.reference.multiResolve(true)
                    .mapNotNull { resolved ->
                        PsiTreeUtil.getParentOfType(resolved.element, CaosDefCommandDefElement::class.java)
                                ?.returnTypeStruct
                                ?.type
                                ?.type
                                ?.let {
                                    CaosVar.CaosCommandCall(token.text, CaosExpressionValueType.fromSimpleName(it))
                                }
                    }
                    .firstOrNull { it.returnType != null && it.returnType !in skipTypes }
        }
    }

    private val skipTypes = listOf(
            CaosExpressionValueType.ANY,
            CaosExpressionValueType.UNKNOWN,
            CaosExpressionValueType.NULL,
            CaosExpressionValueType.VARIABLE
    )
}