@file:Suppress("UnstableApiUsage", "SpellCheckingInspection")
package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosParameter
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptClassifier
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptLvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement

enum class CaosScriptInlayParameterHintsProvider(description:String, override val enabled:Boolean, override val priority:Int = 0) : CaosScriptHintsProvider {
    PARAMETER_NAME_HINT("Show parameter names before expression", true) {
        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && element is CaosScriptCommandElement || element is CaosScriptClassifier
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
            val referencedCommand = getCommand(commandElement)
                    ?: return mutableListOf()
            val skipLast = skipLast(commandElement)
            val last = if (skipLast) {
                commandElement.getSelfOrParentOfType(CaosScriptCAssignment::class.java)?.let { assignment ->
                    val firstCommand = (assignment.arguments.firstOrNull() as? CaosScriptLvalue)
                    val arg = assignment.arguments.lastOrNull()
                    val lastParameter = firstCommand?.let { getCommand(it) }?.parameters?.lastOrNull()
                    if (lastParameter != null && arg != null)
                        listOf(InlayInfo(lastParameter.name, arg.startOffset))
                    else
                        null
                }
            } else {
               null
            } ?: emptyList()
            val parameterStructs = referencedCommand.parameters
            val parameters = getParametersAsStrings(parameterStructs, skipLast)
            return commandElement.arguments.mapIndexedNotNull{ i, it ->
                parameters.getOrNull(i)?.let { parameter -> InlayInfo("$parameter:", it.startOffset) }

            }.toList() + last
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

        private fun skipLast(element:CaosScriptCommandElement) : Boolean {
            return if (element.commandString?.toUpperCase() in setLike) {
                val firstArg = element.argumentValues.firstOrNull()
                (firstArg is CaosVar.CaosCommandCall && firstArg.text.toUpperCase() in SKIP_LAST)
            } else
                false
        }

        private fun getParametersAsStrings(parameters:List<CaosParameter>, skipLast:Boolean) : List<String> {
            return if (skipLast) {
                (0 until parameters.lastIndex).map { i -> parameters[i].name }
            } else {
                parameters.map { it.name }
            }
        }
    }
}