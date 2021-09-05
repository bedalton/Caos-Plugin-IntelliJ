@file:Suppress("UNUSED_PARAMETER", "unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedGameVarIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.isMVxxLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.isOVxxLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.isVAxxLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.isObjectVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.isVAxxLike
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.toListOf
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.PsiTreeUtil

object CaosScriptInferenceUtil {

    private const val RESOLVE_RVALUE_VARS_DEFAULT = true

    /**
     * Infers the type of an rvalue used in a CAOS script
     */
    fun getInferredType(element: CaosScriptRvalue, resolveVars: Boolean = true): List<CaosExpressionValueType> {
        return getRvalueTypeWithoutInference(element, ANY)?.toListOf() ?: emptyList() //getInferredType(element, null, resolveVars, mutableListOf())
    }

    /**
     * Infers the type of an rvalue used in a CAOS script
     */
    @Suppress("UNUSED_PARAMETER")
    fun getInferredType(
        element: CaosScriptRvalue,
        bias: CaosExpressionValueType? = null,
        resolveVars: Boolean? = null,
        lastChecked: MutableList<CaosScriptIsVariable>
    ): List<CaosExpressionValueType> {
//
//        // If rvalue has any of these values
//        // It is an incomplete command call, and cannot be resolved
//        if (anyNotNull(element.incomplete, element.errorRvalue, element.rvaluePrefixIncompletes))
//            return emptyList()
//
//        // If element has var token set,
//        // infer value of variable at last assignment if desired
//        element.varToken?.let {
//            return if (resolveVars ?: RESOLVE_RVALUE_VARS_DEFAULT) getInferredType(
//                it,
//                bias,
//                true,
//                lastChecked = lastChecked
//            ) ?: listOf(VARIABLE) else listOf(VARIABLE)
//        }
//
//        // If element has named var present,
//        // infer value of variable at last assignment if desired
//        element.namedGameVar?.let {
//            return if (resolveVars ?: RESOLVE_RVALUE_VARS_DEFAULT) getInferredType(
//                it,
//                bias,
//                true,
//                lastChecked = lastChecked
//            ) ?: listOf(VARIABLE) else listOf(VARIABLE)
//        }
        val type = getRvalueTypeWithoutInference(element, bias ?: ANY)?.let { listOf(it) }
            ?: getInferredType(element.rvaluePrime, bias)
            ?: emptyList()
        // Return inferred types for simple values
        return type
    }

    /**
     * Infer type for rvalue command calls
     */
    fun getInferredType(
        prime: CaosScriptRvaluePrime?,
        bias: CaosExpressionValueType? = null
    ): List<CaosExpressionValueType>? {
        return prime?.getCommandDefinition(bias)?.returnType?.let { listOf(it) }
    }

    /**
     * Gets inferred type for a given variable
     * Checks for previous assignments for last assigned value
     */
    fun getInferredType(
        element: CaosScriptIsVariable?,
        bias: CaosExpressionValueType? = null,
        resolveVars: Boolean = false,
        lastChecked: MutableList<CaosScriptIsVariable> = mutableListOf()
    ): List<CaosExpressionValueType>? {
        if (element == null)
            return null
        if (!resolveVars)
            return listOf(VARIABLE)
        if (DumbService.isDumb(element.project))
            return null
        return when (element) {
            is CaosScriptVarToken -> {
                getIndexedVarInferredType(element, bias, lastChecked = lastChecked)
            }
            is CaosScriptNamedGameVar -> {
                getNamedGameVarInferredType(element, bias, lastChecked = lastChecked)
            }
            else -> {
                listOf(VARIABLE)
            }
        }

    }

    private fun getNamedGameVarInferredType(
        element: CaosScriptNamedGameVar,
        bias: CaosExpressionValueType? = null,
        lastChecked: MutableList<CaosScriptIsVariable>
    ): List<CaosExpressionValueType> {

        // Short out this check due to inability to accertain agent class
        if (element.isObjectVar) {
            // TODO filter by object class
            return listOf(VARIABLE)
        }

        val project = element.project
        val varType = element.varType
        val key = element.key
            ?: return emptyList()

        var vars: Collection<CaosScriptNamedGameVar>? = try {
            if (!DumbService.isDumb(project))
                CaosScriptNamedGameVarIndex.instance.get(element.varType, key, element.project)
            else
                null
        } catch (e: Exception) {
            null
        }
        if (vars == null) {
            vars = getAllNamedVarsFromRawFiles(project) {
                it.varType == varType && it.key == key
            }
        }
        vars = vars.filter { thisVar ->
            lastChecked.none { it.isEquivalentTo(thisVar) }
        }
        lastChecked.addAll(vars)

        return getInferredType(vars, bias)
    }

    private inline fun getAllNamedVarsFromRawFiles(
        project: Project,
        check: (element: CaosScriptNamedGameVar) -> Boolean
    ): List<CaosScriptNamedGameVar> {
        return (FilenameIndex.getAllFilesByExt(project, "cos") + FilenameIndex.getAllFilesByExt(project, "COS"))
            .distinctBy { it.path.toLowerCase() }
            .flatMap map@{
                val psiFile = it.getPsiFile(project)
                    ?: return@map emptyList()
                PsiTreeUtil.collectElementsOfType(psiFile, CaosScriptNamedGameVar::class.java)
                    .filter(check)
            }
    }

    private fun getIndexedVarInferredType(
        element: CaosScriptVarToken,
        bias: CaosExpressionValueType? = null,
        lastChecked: MutableList<CaosScriptIsVariable>
    ): List<CaosExpressionValueType> {
//        if (element.varGroup.isVAxxLike) {
//            val parentScript = element.getParentOfType(CaosScriptScriptElement::class.java)
//                ?: return arrayListOf(VARIABLE)
//            val varIndex = element.varIndex
//            val otherAssignments: List<CaosScriptVarToken> = PsiTreeUtil
//                .collectElementsOfType(parentScript, CaosScriptVarToken::class.java)
//                .filter { otherVariable ->
//                    varIndex == otherVariable.varIndex &&
//                            otherVariable.isVAxxLike &&
//                            lastChecked.none { it.isEquivalentTo(element) } &&
//                            element.scope.sharesScope(otherVariable.scope)
//                }
//            lastChecked.addAll(otherAssignments)
//            return getInferredType(otherAssignments, bias)
//        }
//
//        if (element.varGroup.isOVxxLike) {
//            //TODO implement after learning to resolve targ
//            return arrayListOf(VARIABLE)
//        }
//
//        if (element.varGroup.isMVxxLike) {
//            //TODO implement after learning to resolve targ
//            return arrayListOf(VARIABLE)
//        }

        return arrayListOf(VARIABLE)
    }

    private fun getInferredType(
        elements: List<CaosScriptIsVariable>,
        bias: CaosExpressionValueType? = null
    ): List<CaosExpressionValueType> {
        for (element in elements.filter { it.parent is CaosScriptLvalue }.sortedByDescending { it.endOffset }) {
            val parentLvalue = element.parent as? CaosScriptLvalue
                ?: continue
            val parentCommandCall = element.parent as? CaosScriptCommandCall
                ?: continue
            val parameter = parentCommandCall.commandDefinition?.parameters?.getOrNull(parentLvalue.index)
            when (val type = parameter?.variableSetToType) {
                "1" -> return listOf(INT)
                "4" -> return listOf(STRING)
                "4|1" -> return if (bias == STRING)
                    listOf(STRING)
                else if (bias?.isNumberType ?: false)
                    listOf(INT)
                else
                    listOf(STRING, INT)
                "16" -> return listOf(DECIMAL)
                "#" -> return listOf(DECIMAL)
                else -> {
                    if (type == null)
                        LOGGER.severe("VARIABLE 'SetTo' value not set on variable parameter definition")
                    else
                        LOGGER.severe("VARIABLE 'SetToType' value '$type' was not handled by when clause")
                    return listOf(VARIABLE)
                }
            }
        }
        return listOf(VARIABLE)
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
 * Returns true if any element passed in is non-null
 */
private fun anyNotNull(vararg elements: PsiElement?): Boolean {
    return elements.any { it != null }
}


internal fun getRvalueTypeWithoutInference(
    rvalue: CaosScriptRvalue?,
    bias: CaosExpressionValueType
): CaosExpressionValueType? {
    if (rvalue == null)
        return null
    return when {
        rvalue.isInt -> INT
        rvalue.isFloat -> FLOAT
        rvalue.isNumeric -> DECIMAL
        rvalue.isQuoteString -> STRING
        rvalue.isByteString || rvalue.animationString != null || rvalue.isC1String -> {
            when (bias) {
                STRING, C1_STRING -> C1_STRING
                BYTE_STRING -> BYTE_STRING
                ANIMATION -> ANIMATION
                else -> {
                    if (rvalue.isC1String)
                        C1_STRING
                    else
                        BYTE_STRING
                }
            }
        }
        rvalue.isToken -> TOKEN
        rvalue.isString -> STRING
        else -> rvalue.variant?.let { variant ->
            val commandString = rvalue.commandStringUpper?.replace(WHITESPACE, " ")
                ?: return null
            CaosLibs[variant].rvalues.filter { command -> command.command == commandString }.let { commands ->
                if (commands.any { command -> command.returnType == bias }) {
                    bias
                } else
                    commands.firstOrNull()?.returnType
            }
        }
    }
}


val LIST_OF_VARIABLE_VALUE_TYPE = listOf(VARIABLE)