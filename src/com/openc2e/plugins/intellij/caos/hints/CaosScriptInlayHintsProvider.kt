@file:Suppress("UnstableApiUsage")

package com.openc2e.plugins.intellij.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptClassifier
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandElement
import com.openc2e.plugins.intellij.caos.psi.api.argumentsLength


class CaosScriptInlayHintsProvider : InlayParameterHintsProvider {

    override fun getParameterHints(elementIn: PsiElement): MutableList<InlayInfo> {
        val project = elementIn.project
        if (DumbService.isDumb(project))
            return mutableListOf()

        (elementIn as? CaosScriptClassifier)?.let {
            return listOfNotNull(
                    elementIn.family?.let { InlayInfo("family", it.startOffset) },
                    elementIn.genus?.let { InlayInfo("genus", it.startOffset) },
                    elementIn.species?.let { InlayInfo("species", it.startOffset) }
            ).toMutableList()
        }

        val element = elementIn as? CaosScriptCommandElement
                ?: return mutableListOf()
        val referencedCommand = getCommand(element)
                ?: return mutableListOf()
        val skipLast = skipLast(element)
        val parameters = getParametersAsStrings(referencedCommand.parameterStructs, skipLast)
        return element.arguments.mapIndexed{ i, it ->
            InlayInfo(parameters[i], it.startOffset)
        }.toMutableList()
    }

    override fun getDefaultBlackList(): MutableSet<String> {
        return mutableSetOf()
    }

    override fun getHintInfo(elementIn: PsiElement): HintInfo? {
        if (elementIn is CaosScriptClassifier) {
            return HintInfo.MethodInfo("family (integer) genus (integer) species (integer)", listOf("family", "genus", "species"), CaosScriptLanguage.instance)
        }
        val element = elementIn as? CaosScriptCommandElement
                ?: return null
        val referencedCommand = getCommand(element)
                ?: return null
        val skipLast = skipLast(element)
        val parameters = getParametersAsStrings(referencedCommand.parameterStructs, skipLast)
        return HintInfo.MethodInfo(element.commandString, parameters, CaosDefLanguage.instance)
    }

    companion object {

        private val setLike = listOf("SETV", "SETS", "SETA")

        private fun getCommand(element:PsiElement) : CaosDefCommandDefElement? {
            if (element !is CaosScriptCommandElement)
                return null
            val commandTokens = element.commandToken?.reference?.multiResolve(true)?.mapNotNull {
                (it.element as? CaosDefCompositeElement)?.getParentOfType(CaosDefCommandDefElement::class.java)
            } ?: return null
            val numParameters = element.argumentsLength
            if (commandTokens.isEmpty())
                return null
            return if (commandTokens.size == 1) {
                commandTokens[0]
            } else {
                commandTokens.filter { it.parameterStructs.size == numParameters }.ifEmpty { null }?.first()
                        ?: commandTokens.filter { it.parameterStructs.size > numParameters }.ifEmpty { null }?.first()
                        ?: return null
            }
        }

        private fun skipLast(element:CaosScriptCommandElement) : Boolean {
            return if (element.commandString.toUpperCase() in setLike) {
                val firstArg = element.argumentValues.firstOrNull()
                (firstArg is CaosVar.CaosCommandCall && firstArg.text.toUpperCase() == "CLS2")
            } else
                false
        }

        private fun getParametersAsStrings(parameters:List<CaosDefParameterStruct>, skipLast:Boolean) : List<String> {
            return if (skipLast && parameters.size == 2) {
                listOfNotNull(parameters.firstOrNull()?.name, "species")
            } else if (skipLast) {
                val lastIndex = parameters.lastIndex
                parameters.mapIndexed { i, parameter -> if (i == lastIndex) "species" else parameter.name }
            } else {
                parameters.map { it.name }
            }
        }
    }

}
