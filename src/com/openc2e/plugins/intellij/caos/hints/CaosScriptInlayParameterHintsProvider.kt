@file:Suppress("UnstableApiUsage")
package com.openc2e.plugins.intellij.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptClassifier
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandElement

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
            val parameters = getParametersAsStrings(referencedCommand.parameterStructs, skipLast)
            return commandElement.arguments.mapIndexedNotNull{ i, it ->
                parameters.getOrNull(i)?.let { parameter -> InlayInfo("$parameter:", it.startOffset) }

            }.toMutableList()
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element is CaosScriptClassifier) {
                return HintInfo.MethodInfo("family (integer) genus (integer) species (integer)", listOf("family", "genus", "species"), CaosScriptLanguage.instance)
            }
            val commandElement = element as? CaosScriptCommandElement
                    ?: return null
            val referencedCommand = getCommand(commandElement)
                    ?: return null
            val skipLast = skipLast(commandElement)
            val parameters = getParametersAsStrings(referencedCommand.parameterStructs, skipLast)
            return HintInfo.MethodInfo(commandElement.commandString, parameters, CaosDefLanguage.instance)
        }
    }
    ;

    override val option: Option = Option("SHOW_${this.name}", description, enabled)

    companion object {

        private val setLike = listOf("SETV", "SETS", "SETA")

        private fun skipLast(element:CaosScriptCommandElement) : Boolean {
            return if (element.commandString.toUpperCase() in setLike) {
                val firstArg = element.argumentValues.firstOrNull()
                (firstArg is CaosVar.CaosCommandCall && firstArg.text.toUpperCase() == "CLS2")
            } else
                false
        }

        private fun getParametersAsStrings(parameters:List<CaosDefParameterStruct>, skipLast:Boolean) : List<String> {
            return if (skipLast && parameters.size == 2) {
                listOfNotNull(parameters.firstOrNull()?.name, "species:")
            } else if (skipLast) {
                val lastIndex = parameters.lastIndex
                parameters.mapIndexed { i, parameter -> if (i == lastIndex) "species:" else parameter.name }
            } else {
                parameters.map { it.name }
            }
        }
    }
}