package com.openc2e.plugins.intellij.caos.deducer

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptPsiImplUtil

object CaosScriptVarDeducer {

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
                    ?.constantValue?.let {
                        it.int?.text?.toInt()?.let {
                            return CaosVar.CaosLiteral.CaosInt(it)
                        } ?: it.float?.text?.toFloat()?.let {
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
            // Iterate through expressions to find type
            for (expression in expressions) {
                val rvalue = expression.rvalue?.rvaluePrime
                if (rvalue != null) {
                    getFromRValuePrime(rvalue)?.let {
                        return it
                    }
                }
                val value = expression.toCaosVar()
                val simpleType = value.simpleType
                if (simpleType != CaosExpressionValueType.ANY && simpleType != CaosExpressionValueType.VARIABLE && simpleType != CaosExpressionValueType.UNKNOWN) {
                    if (value is CaosVar.CaosCommandCall) {
                        if (value.returnType != null)
                            return value
                        else
                            continue
                    }
                    return value
                }
            }
            enclosingBlock = enclosingBlock.getParentOfType(CaosScriptCodeBlock::class.java)
        }
        return CaosVar.CaosVarNone
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
                                    CaosVar.CaosCommandCall(token.text, CaosExpressionValueType.fromSimpleName(it))
                                }
                    }
                    .firstOrNull { it.simpleType != CaosExpressionValueType.COMMAND && it.simpleType != CaosExpressionValueType.ANY && it.simpleType != CaosExpressionValueType.VARIABLE && it.simpleType != CaosExpressionValueType.UNKNOWN }
        }
    }


}