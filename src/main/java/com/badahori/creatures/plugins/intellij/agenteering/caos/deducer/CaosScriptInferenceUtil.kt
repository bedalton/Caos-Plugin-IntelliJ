@file:Suppress("UNUSED_PARAMETER", "unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedGameVarIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.isMVxxLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.isOVxxLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.isVAxxLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.collectElementsOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.parentParameter
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.PsiTreeUtil


val CaosScriptVarToken.isVAxxLike: Boolean get() =
    varGroup == CaosScriptVarTokenGroup.VAxx || varGroup == CaosScriptVarTokenGroup.VARx
val CaosScriptVarToken.isOVxxLike: Boolean get() =
    varGroup == CaosScriptVarTokenGroup.OVxx || varGroup == CaosScriptVarTokenGroup.OBVx
val CaosScriptVarToken.isMVxxLike: Boolean get() =
    varGroup == CaosScriptVarTokenGroup.MVxx

val CaosScriptNamedGameVar.isGameEngineVar: Boolean get() =
    varType == CaosScriptNamedGameVarType.GAME || varType == CaosScriptNamedGameVarType.EAME
val CaosScriptNamedGameVar.isObjectVar: Boolean get() =
    varType == CaosScriptNamedGameVarType.NAME || varType == CaosScriptNamedGameVarType.MAME


object CaosScriptInferenceUtil {

    private val SET_OF_ONLY_VARIABLE_TYPE = setOf(VARIABLE)
    private const val RESOLVE_RVALUE_VARS_DEFAULT = true

    /**
     * Infers the type of rvalue used in a CAOS script
     */
    fun getInferredType(element: CaosScriptRvalue, resolveVars: Boolean = true): Set<CaosExpressionValueType> {
        return getRvalueTypeWithoutInference(element, ANY, true)?.toListOf()?.toSet()
            ?: getInferredType(element, null, resolveVars, mutableListOf())?.toSet()
            ?: emptySet()
    }

    /**
     * Infers the type an rvalue used in a CAOS script
     */
    fun getInferredType(
        element: CaosScriptRvalue,
        bias: CaosExpressionValueType? = null,
        resolveVars: Boolean? = null,
        lastChecked: MutableList<CaosScriptIsVariable>,
    ): Set<CaosExpressionValueType>? {

        // It is an incomplete command call, and cannot be resolved
        if (element.isErrorElement) {
            return null
        }

        val doNotResolveVars = (resolveVars != null && !resolveVars) || (resolveVars == null && !RESOLVE_RVALUE_VARS_DEFAULT)

        // If element has var token set,
        // infer value of variable at last assignment if desired
        val varToken = element.varToken
        if (varToken != null) {
            // If we should not resolve vars, return list of generic variable type
            if (doNotResolveVars) {
                return SET_OF_ONLY_VARIABLE_TYPE
            }
            // Infer variable type
            getInferredRValueVariableType(
                varToken,
                bias,
                lastChecked = lastChecked
            ) ?: SET_OF_ONLY_VARIABLE_TYPE
        }

        val namedGameVar = element.namedGameVar

        if (namedGameVar != null) {
            if (doNotResolveVars) {
                return SET_OF_ONLY_VARIABLE_TYPE
            }
            // If element has named var present,
            // infer value of variable at last assignment if desired
            getInferredRValueVariableType(
                namedGameVar,
                bias,
                lastChecked = lastChecked
            ) ?: listOf(VARIABLE)
        }
        val type = getRvalueTypeWithoutInference(element, bias ?: ANY, false)?.let { setOf(it) }
            ?: getInferredType(element.rvaluePrime, bias)
            ?: emptySet()
        // Return inferred types for simple values
        return type
    }

    /**
     * Infer type for rvalue command calls
     */
    fun getInferredType(
        prime: CaosScriptRvaluePrime?,
        bias: CaosExpressionValueType? = null,
    ): Set<CaosExpressionValueType>? {
        return prime?.getCommandDefinition(bias)?.returnType?.let { listOf(it) }?.toSet()
    }

    /**
     * Gets inferred type for a given variable
     * Checks for previous assignments for last assigned value
     */
    private fun getInferredRValueVariableType(
        rvalueVariable: CaosScriptIsVariable?,
        bias: CaosExpressionValueType? = null,
        lastChecked: MutableList<CaosScriptIsVariable> = mutableListOf(),
    ): Set<CaosExpressionValueType>? {
        if (rvalueVariable == null) {
            return null
        }
        if (!rvalueVariable.isValid || rvalueVariable.project.isDisposed || DumbService.isDumb(rvalueVariable.project)) {
            return null
        }
        // Check variables by specific type
        return when (rvalueVariable) {
            is CaosScriptVarToken -> getIndexedVarInferredType(rvalueVariable, bias, lastChecked = lastChecked)
            is CaosScriptNamedGameVar -> getNamedGameVarInferredType(rvalueVariable, bias, lastChecked = lastChecked)
            else -> SET_OF_ONLY_VARIABLE_TYPE
        }
    }

    /**
     * Gets inferred type for a given variable
     * Checks for previous assignments for last assigned value
     */
    private fun getInferredRValueVariableType(
        rvalueVariable: CaosScriptVarToken?,
        bias: CaosExpressionValueType? = null,
        lastChecked: MutableList<CaosScriptIsVariable> = mutableListOf(),
    ): List<CaosExpressionValueType>? {
        if (rvalueVariable == null) {
            return null
        }
        if (!rvalueVariable.isValid || rvalueVariable.project.isDisposed || DumbService.isDumb(rvalueVariable.project)) {
            return null
        }
        val group = rvalueVariable.varGroup
        val scoped = when {
            group.isVAxxLike -> getVAxxAssignedValueTypes(rvalueVariable)
        }
    }


    private fun getVAxxAssignedValueTypes(rvalueVariable: CaosScriptVarToken): Set<CaosExpressionValueType> {
        val index = rvalueVariable.varIndex
        val maxOffset = rvalueVariable.startOffset
        val scope = rvalueVariable.scope
        val otherVariables = rvalueVariable.getParentOfType(CaosScriptScriptElement::class.java)
            ?.collectElementsOfType(CaosScriptVarToken::class.java)
            ?.filter { aVar ->
                aVar.isVAxxLike && aVar.varIndex == index && aVar.startOffset < maxOffset && scope.sharesScope(aVar.scope)
            }
            ?.sortedByDescending { it.startOffset }
            ?: return emptySet()
        for (otherVariable in otherVariables) {
            val parentLvalue = otherVariable.parent as? CaosScriptLvalue
                ?: continue
            val parentCommand = parentLvalue.parent as? CaosScriptCommandLike
                ?: continue

        }

    }

    private fun getNamedGameVarInferredType(
        element: CaosScriptNamedGameVar,
        bias: CaosExpressionValueType? = null,
        lastChecked: MutableList<CaosScriptIsVariable>,
    ): Set<CaosExpressionValueType> {

        // Short out this check due to inability to ascertain agent class
        if (element.isObjectVar) {
            return SET_OF_ONLY_VARIABLE_TYPE
        }

        // Get valid project or return
        val project = element.project
        if (project.isDisposed) {
            return SET_OF_ONLY_VARIABLE_TYPE
        }

        // Get named game var type. i.e. NAME, GAME, EAME, MAME
        val varType = element.varType

        // Get key
        // Usually string, but could be any caos type
        val key = element.key
            ?: return emptySet()

        val startIndex = element.startOffset
        val scope = element.scope
        val isObjectVar = element.isObjectVar
        val scriptNamedVars = element.getParentOfType(CaosScriptScriptElement::class.java)
            ?.collectElementsOfType(CaosScriptNamedGameVar::class.java)
            ?.filter { lastChecked.contains(element) }
            .orEmpty()
        lastChecked.addAll(scriptNamedVars)


        val immediate = scriptNamedVars
            .filter { it.startOffset < startIndex && (it.varType == varType || it.isObjectVar && element.isObjectVar) && it.sharesScope(scope) }
            .sortedByDescending { it.startOffset }
            .mapNotNull map@{
                val rValues = (it.parent as? CaosScriptLvalue)
                    ?.rvalueList
                    ?: return@map null
                if (rValues.size != 1) {
                    return@map null
                }
                getInferredType(rValues.first(), bias, true, lastChecked)
            }

        if (immediate.isNotEmpty()) {
            immediate.filter { it.isNotEmpty() && (it.size != 1 || it[0] != VARIABLE)}.firstOrNull()?.let {
                return it
            }
        }

        //
        var vars: Collection<CaosScriptNamedGameVar>? = try {
            if (DumbService.isDumb(project)) {
                null
            } else {
                CaosScriptNamedGameVarIndex.instance.get(element.varType, key, element.project)
            }
        } catch (e: Exception) {
            null
        }

        // If index is null from thrown exception or not yet indexed
        if (vars == null) {
            vars = getAllNamedVarsFromRawFiles(project) {
                it.varType == varType && it.key == key
            }
        }

        // Make sure we are only checking new variables
        vars = vars.filter { thisVar ->
            lastChecked.none { it.isEquivalentTo(thisVar) }
        }
        lastChecked.addAll(vars)

        return getInferredVariableTypeForVariablesList(vars, bias)
    }

    private inline fun getAllNamedVarsFromRawFiles(
        project: Project,
        check: (element: CaosScriptNamedGameVar) -> Boolean,
    ): List<CaosScriptNamedGameVar> {
        return (FilenameIndex.getAllFilesByExt(project, "cos") + FilenameIndex.getAllFilesByExt(project, "COS"))
            .distinctBy { it.path.lowercase() }
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
        lastChecked: MutableList<CaosScriptIsVariable>,
    ): Set<CaosExpressionValueType> {
        return when {
            element.varGroup.isVAxxLike -> {
                val maxOffset = element.endOffset
                val parentScript = element.getParentOfType(CaosScriptScriptElement::class.java)
                    ?: return SET_OF_ONLY_VARIABLE_TYPE
                val varIndex = element.varIndex
                val scriptVariables = PsiTreeUtil
                    .collectElementsOfType(parentScript, CaosScriptVarToken::class.java)
                val result: Set<CaosExpressionValueType>? = scriptVariables
                    .sortedByDescending {
                        it.startOffset
                    }
                    .filter { otherVariable ->
                        varIndex == otherVariable.varIndex &&
                                otherVariable.isVAxxLike &&
                                otherVariable.startOffset < maxOffset &&
                                lastChecked.none { it.isEquivalentTo(element) } &&
                                element.scope.sharesScope(otherVariable.scope)
                    }
                    .firstNotNullOfOrNull check@{ aVar ->
                        lastChecked.add(aVar)
                        getInferredVariableAssignedType(element, bias)
                    }
                    ?.toSet()
                result ?: SET_OF_ONLY_VARIABLE_TYPE
            }

            element.varGroup.isOVxxLike -> {
                //TODO implement after learning to resolve targ
                return SET_OF_ONLY_VARIABLE_TYPE
            }

            element.varGroup.isMVxxLike -> {
                //TODO implement after learning to resolve targ
                // We can only check previously set mv vars or vars directly after targ'ing
                SET_OF_ONLY_VARIABLE_TYPE
            }
            else -> SET_OF_ONLY_VARIABLE_TYPE
        }
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
    rvalue: CaosScriptRvalueLike?,
    bias: CaosExpressionValueType,
    fuzzy: Boolean = true,
): CaosExpressionValueType? {
    if (rvalue == null)
        return null

    // If is not actual rvalue, then it is a token or subroutine name
    if (rvalue !is CaosScriptRvalue) {
        if (rvalue !is CaosScriptTokenRvalue && rvalue !is CaosScriptSubroutineName) {
            throw Exception("Unexpected rvalue like type: ${rvalue.className}")
        }
        return TOKEN
    }

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
            CaosLibs[variant].rvalues.filter { command -> command.command == commandString }
                .nullIfEmpty()
                ?.let { commands ->
                    if (fuzzy && commands.any { command -> command.returnType like bias })
                        bias
                    else if (!fuzzy && commands.any { command -> command.returnType == bias })
                        bias
                    else
                        commands.firstOrNull()?.returnType
                }
        }
    }
}


val LIST_OF_VARIABLE_VALUE_TYPE = listOf(VARIABLE)

val CaosScriptRvalue.isErrorElement: Boolean
    get() {
        return swiftEscapeLiteral != null ||
                jsElement != null ||
                rvaluePrefixIncompletes != null ||
                incomplete != null ||
                pictDimensionLiteral != null
    }