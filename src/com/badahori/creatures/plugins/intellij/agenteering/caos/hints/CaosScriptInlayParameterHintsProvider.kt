@file:Suppress("UnstableApiUsage")
package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefParameterStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptClassifier
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptLvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.elementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType

enum class CaosScriptInlayParameterHintsProvider(description:String, override val enabled:Boolean, override val priority:Int = 0) : CaosScriptHintsProvider {
    PARAMETER_NAME_HINT("Show parameter names before expression", true) {
        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptCommandElement || element is CaosScriptClassifier
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            (element as? CaosScriptClassifier)?.let {
                return listOfNotNull(
                        element.family.let { InlayInfo("family:", it.startOffset) },
                        element.genus?.let { InlayInfo("genus:", it.startOffset) },
                        element.species?.let { InlayInfo("species:", it.startOffset) }
                ).toMutableList()
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
                    val lastParameter = firstCommand?.let {getCommand(it) }?.parameterStructs?.lastOrNull()
                    if (lastParameter != null && arg != null)
                        listOf(InlayInfo(lastParameter.name, arg.startOffset))
                    else
                        null
                }
            } else {
               null
            } ?: emptyList()
            val parameterStructs = referencedCommand.parameterStructs
            val parameters = getParametersAsStrings(parameterStructs, skipLast)
            return commandElement.arguments.mapIndexedNotNull{ i, it ->
                parameters.getOrNull(i)?.let { parameter -> InlayInfo("$parameter:", it.startOffset) }

            }.toList() + last
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element is CaosScriptClassifier) {
                return HintInfo.MethodInfo("family (integer) genus (integer) species (integer)", listOf("family", "genus", "species"), CaosScriptLanguage.instance)
            }
            val commandElement = element as? CaosScriptCommandElement
                    ?: return null
            val referencedCommand = getCommand(commandElement)
                    ?: return null
            val parameters = getParametersAsStrings(referencedCommand.parameterStructs, false)
            return HintInfo.MethodInfo(commandElement.commandString, parameters, CaosDefLanguage.instance)
        }
    }
    ;

    override val option: Option = Option("SHOW_${this.name}", description, enabled)

    companion object {

        private val setLike = listOf("SETV", "SETS", "SETA")

        private val SKIP_LAST = listOf("PUHL", "PUPT", "CLS2")

        private fun skipLast(element:CaosScriptCommandElement) : Boolean {
            return if (element.commandString.toUpperCase() in setLike) {
                val firstArg = element.argumentValues.firstOrNull()
                (firstArg is CaosVar.CaosCommandCall && firstArg.text.toUpperCase() in SKIP_LAST)
            } else
                false
        }

        private fun getParametersAsStrings(parameters:List<CaosDefParameterStruct>, skipLast:Boolean) : List<String> {
            return if (skipLast) {
                (0 until parameters.lastIndex).map { i -> parameters[i].name }
            } else {
                parameters.map { it.name }
            }
        }
    }
}