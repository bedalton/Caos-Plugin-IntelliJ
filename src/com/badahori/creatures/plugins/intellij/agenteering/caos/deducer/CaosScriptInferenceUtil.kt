package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
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
        val startOffset = element.startOffset
        val scope = CaosScriptPsiImplUtil.getScope(element)
        val sets = (element
                .getParentOfType(CaosScriptEventScript::class.java)?.let {
                    PsiTreeUtil.collectElementsOfType(it, CaosScriptCAssignment::class.java)
                } ?: PsiTreeUtil.collectElementsOfType(element.containingFile, CaosScriptCAssignment::class.java))
                .filter { it.endOffset < startOffset && it.scope.sharesScope(scope) }
                .sortedByDescending { it.endOffset }
        sets.firstOrNull()?.let { assignment ->
            val argument = assignment.arguments.lastOrNull() as? CaosScriptRvalue
                    ?: return@let null
            if (argument.namedGameVar != null || argument.expression?.namedGameVar != null) {
                return argument.toCaosVar()
            }
            val variable = argument.varToken
                    ?: argument.expression?.varToken
            variable?.let varLet@{
                inferPreviouslyAssignedType(it)?.let { value ->
                    if (value.simpleType !in skipTypes)
                        return value
                }
            }
            val caosVar = argument.toCaosVar()
            if (caosVar is CaosVar.CaosNumberedVar || caosVar is CaosVar.CaosNamedGameVar) {
                when (assignment.firstChild?.text?.toUpperCase()) {
                    "SETV" -> if (element.containingCaosFile.variant.isNotOld)
                        CaosVar.CaosInferredVariableType(assignment.arguments.firstOrNull()?.text
                                ?: "???", CaosExpressionValueType.DECIMAL)
                    else
                        return CaosVar.CaosLiteralVal
                    "SETA" -> return CaosVar.CaosInferredVariableType(assignment.arguments.firstOrNull()?.text
                            ?: "???", CaosExpressionValueType.AGENT)
                    "SETS" -> return CaosVar.CaosInferredVariableType(assignment.arguments.firstOrNull()?.text
                            ?: "???", CaosExpressionValueType.STRING)
                    else -> null
                }
            } else if (caosVar.simpleType !in skipTypes) {
                return caosVar
            } else {
                null
            }
        }

        return sets
                .asSequence()
                .mapNotNull {
                    it.arguments.lastOrNull() as? CaosScriptRvalue
                }
                .mapNotNull { arg ->
                    (arg.varToken ?: arg.expression?.varToken)?.let {
                        return@mapNotNull inferPreviouslyAssignedType(it)
                    }
                    val value = arg.toCaosVar()
                    if (value is CaosVar.CaosCommandCall) {
                        if (value.returnType != null)
                            value
                        else
                            null
                    } else {
                        value
                    }
                }
                .firstOrNull { it.simpleType !in skipTypes } ?: CaosVar.CaosVarNone
    }

    private fun inferPreviouslyAssignedType(element: CaosScriptVarToken): CaosVar? {
        val startOffset = element.startOffset
        val scope = element.scope
        val varText = element.text.toLowerCase()
        return (element
                .getParentOfType(CaosScriptEventScript::class.java)?.let {
                    PsiTreeUtil.collectElementsOfType(it, CaosScriptCAssignment::class.java)
                } ?: PsiTreeUtil.collectElementsOfType(element.containingFile, CaosScriptCAssignment::class.java))
                .filter { it.endOffset < startOffset && it.lvalue?.text?.toLowerCase() == varText && it.scope.sharesScope(scope) }
                .sortedByDescending { it.endOffset }
                .mapNotNull {
                    val argument = it.arguments.lastOrNull() as? CaosScriptRvalue
                            ?: return@mapNotNull null
                    val varToken = argument.varToken
                            ?: argument.expression?.varToken
                    varToken?.let {
                        return@mapNotNull inferPreviouslyAssignedType(varToken)
                    }
                    argument.toCaosVar()
                }
                .firstOrNull {
                    it.simpleType !in skipTypes
                }

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