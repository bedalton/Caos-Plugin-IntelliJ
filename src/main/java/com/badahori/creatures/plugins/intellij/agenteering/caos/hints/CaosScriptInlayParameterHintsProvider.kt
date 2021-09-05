@file:Suppress("UnstableApiUsage", "SpellCheckingInspection")

package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosParameter
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement

enum class CaosScriptInlayParameterHintsProvider(description: String, override val enabled: Boolean, override val priority: Int = 0) : CaosScriptHintsProvider {
    PARAMETER_NAME_HINT("Show parameter names before expression", true) {
        override fun isApplicable(element: PsiElement): Boolean {
            return element.parent !is CaosScriptEqualityExpressionPrime && (element is CaosScriptCommandElement || element is CaosScriptClassifier)
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            (element as? CaosScriptClassifier)?.let { classifier ->
                if (classifier.getPreviousNonEmptySibling(false)?.text?.equalsIgnoreCase("scrp").orFalse()) {
                    return listOfNotNull(
                            InlayInfo("family:", element.family.startOffset),
                            element.genus?.let { InlayInfo("genus:", it.startOffset) },
                            element.species?.let { InlayInfo("species:", it.startOffset) }
                    ).toMutableList()
                }
            }
            val commandElement = element as? CaosScriptCommandElement
                    ?: return mutableListOf()
            if (commandElement is CaosScriptCAssignment && commandElement.commandString like "SETV" )
                return inlayHintsForCAssignment(commandElement)

            // If direct parent is assignment, its parameters have been modified set c_assignment element pass
            if (commandElement.parent.let { it is CaosScriptCAssignment && it.commandString like "SETV" })
                return emptyList()

            val referencedCommand = getCommand(commandElement)
                    ?: return mutableListOf()
            val skipLast = skipLast(commandElement)
            val parameterStructs = referencedCommand.parameters
            val parameters = getParametersAsStrings(parameterStructs, skipLast)
            val arguments = commandElement.arguments
            return arguments.mapIndexedNotNull { i, it ->
                parameters.getOrNull(i)?.let { parameter -> parameter.nullIfEmpty()?.let { parameterNotEmpty -> InlayInfo("$parameterNotEmpty:", it.startOffset) } }
            }.toList()
        }

        private fun inlayHintsForCAssignment(assignment:CaosScriptCAssignment) : List<InlayInfo> {
            val arguments = assignment.arguments
            val lvalueElement = (arguments.firstOrNull() as? CaosScriptLvalue)
                    ?: return emptyList()
            val command = lvalueElement.commandDefinition
            if (command == null) {
                val parameters = assignment.commandDefinition?.parameters?.map { it.name.nullIfEmpty() }
                return arguments.mapIndexedNotNull {i, argument ->
                    parameters?.getOrNull(i)?.let {parameter ->
                        InlayInfo("$parameter:", argument.startOffset)
                    }
                }
            }
            val parameters = command.parameters
            val lastArgument = arguments.lastOrNull()

            // Build list of parameter names, appending lvalueName from command definition as needed
            val parameterNames = if (!skipLast(lvalueElement) && command.lvalueName.nullIfEmpty() != null)
                parameters.map { it.name.nullIfEmpty() } + command.lvalueName!!
            else
                parameters.map { it.name.nullIfEmpty() }
            return (lvalueElement.arguments + lastArgument).mapIndexedNotNull { i, nullableArg ->
                nullableArg?.let {argument ->
                    parameterNames.getOrNull(i)?.let { parameter ->
                        InlayInfo("$parameter:", argument.startOffset)
                    }
                }
            } + assignment.commandDefinition?.parameters?.map{ it.name }?.let { parameterStrings ->
                assignment.arguments.dropLast(1).mapIndexedNotNull { i, argument ->
                    parameterStrings.getOrNull(i)?.let { parameter ->
                        InlayInfo("$parameter:", argument.startOffset)
                    }
                }
            }.orEmpty()

        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element is CaosScriptClassifier) {
                return HintInfo.MethodInfo("family (integer) genus (integer) species (integer)", listOf("family", "genus", "species"), CaosScriptLanguage)
            }
            val commandElement = element as? CaosScriptCommandElement
                    ?: return null
            val commandString = commandElement.commandString
                    ?: return null
            val referencedCommand = getCommand(commandElement)
                    ?: return null
            val parameters = getParametersAsStrings(referencedCommand.parameters, false)
            return HintInfo.MethodInfo(commandString, parameters, CaosDefLanguage.instance)
        }
    }
    ;

    override val option: Option = Option("SHOW_${this.name}", description, enabled)

    companion object {

        private val setLike = listOf("SETV", "SETS", "SETA")

        private val SKIP_LAST = listOf("PUHL", "PUPT", "CLS2")

        private fun skipLast(element: CaosScriptCommandElement): Boolean {
            return if (element.commandString?.toUpperCase() in setLike) {
                val firstArg = element.argumentValues.firstOrNull()
                val commandString = (firstArg as? CaosScriptRvalue)?.commandString ?: (firstArg as? CaosScriptLvalue)?.commandString
                return commandString in SKIP_LAST
            } else
                false
        }

        private fun getParametersAsStrings(parameters: List<CaosParameter>, skipLast: Boolean): List<String> {
            return if (skipLast) {
                (0 until parameters.lastIndex).map { i -> parameters[i].name }
            } else {
                parameters.map { it.name }
            }
        }
    }
}