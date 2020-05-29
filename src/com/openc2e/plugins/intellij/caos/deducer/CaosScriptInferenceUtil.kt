package com.openc2e.plugins.intellij.caos.deducer

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptPsiImplUtil
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER

object CaosScriptInferenceUtil {

    fun getInferredType(element: CaosScriptIsVariable): CaosExpressionValueType {
        return getInferredValue(element).let {
            (it as? CaosVar.CaosCommandCall)?.returnType
                    ?: it.simpleType
        }
    }

    fun getInferredValue(element: CaosScriptIsVariable): CaosVar {
        val endRange = element.textRange.startOffset
        var enclosingBlock = element.getParentOfType(CaosScriptCodeBlock::class.java)
        if (element is CaosScriptNamedConstant) {
            return PsiTreeUtil.getParentOfType(element.reference.resolve(), CaosScriptConstantAssignment::class.java)
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
        val scope = CaosScriptPsiImplUtil.getScope(element)
        while (enclosingBlock != null) {
            val value = getValueInBlock(name, enclosingBlock, scope, endRange)
            if (value != null)
                return value
            enclosingBlock = enclosingBlock.getParentOfType(CaosScriptCodeBlock::class.java)
        }
        return CaosVar.CaosVarNone
    }

    private fun getValueInBlock(name:String, enclosingBlock:CaosScriptCodeBlock, scope:CaosScope, endRange:Int) : CaosVar? {
        val rndvVal = enclosingBlock
                .codeBlockLineList
                .mapNotNull { line ->
                    line.commandCall?.cRndv
                }
                .filter {
                    val thisScope = CaosScriptPsiImplUtil.getScope(it)
                    when (thisScope.blockType) {
                        CaosScriptBlockType.DOIF -> scope.blockType == CaosScriptBlockType.DOIF || thisScope.enclosingScope.intersect(scope.enclosingScope).size != scope.enclosingScope.size
                        CaosScriptBlockType.ELIF -> scope.blockType == CaosScriptBlockType.ELIF || thisScope.enclosingScope.intersect(scope.enclosingScope).size != scope.enclosingScope.size
                        CaosScriptBlockType.ELSE -> scope.blockType == CaosScriptBlockType.ELSE || thisScope.enclosingScope.intersect(scope.enclosingScope).size != scope.enclosingScope.size
                        else -> true
                    }
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
                    when (thisScope.blockType) {
                        CaosScriptBlockType.DOIF -> scope.blockType == CaosScriptBlockType.DOIF || thisScope.enclosingScope.intersect(scope.enclosingScope).size != scope.enclosingScope.size
                        CaosScriptBlockType.ELIF -> scope.blockType == CaosScriptBlockType.ELIF || thisScope.enclosingScope.intersect(scope.enclosingScope).size != scope.enclosingScope.size
                        CaosScriptBlockType.ELSE -> scope.blockType == CaosScriptBlockType.ELSE || thisScope.enclosingScope.intersect(scope.enclosingScope).size != scope.enclosingScope.size
                        else -> true
                    }
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
        val caosVar = rvalue.rvaluePrime?.let {
            getFromRValuePrime(it)
        } ?: rvalue.toCaosVar()
        return (caosVar as? CaosVar.CaosCommandCall)?.returnType
                ?: caosVar.simpleType
    }

    private fun getFromRValuePrime(prime: CaosScriptRvaluePrime): CaosVar? {
        return prime.getChildOfType(CaosScriptIsCommandToken::class.java)?.let { token ->
            token.reference.multiResolve(true)
                    .mapNotNull { resolved ->
                        PsiTreeUtil.getParentOfType(resolved.element, CaosDefCommandDefElement::class.java)
                                ?.returnTypeStruct
                                ?.type
                                ?.type
                                ?.let {
                                    LOGGER.info("Type found for ${prime.text}. Type: $it")
                                    CaosVar.CaosCommandCall(token.text, CaosExpressionValueType.fromSimpleName(it))
                                }
                    }
                    .firstOrNull { it.returnType != null && it.returnType !in skipTypes}
        }
    }

    private val skipTypes = listOf(
            CaosExpressionValueType.ANY,
            CaosExpressionValueType.UNKNOWN,
            CaosExpressionValueType.NULL,
            CaosExpressionValueType.VARIABLE
    )

}