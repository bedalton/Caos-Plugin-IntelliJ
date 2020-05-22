@file:Suppress("UnstableApiUsage")

package com.openc2e.plugins.intellij.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.util.CaosCommandType
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.getEnclosingCommandType
import com.openc2e.plugins.intellij.caos.references.CaosScriptCommandTokenReference
import com.openc2e.plugins.intellij.caos.utils.orElse


class CaosScriptInlayHintsProvider : InlayParameterHintsProvider {


    override fun getParameterHints(elementIn: PsiElement): MutableList<InlayInfo> {
        val project = elementIn.project;
        if (DumbService.isDumb(project))
            return mutableListOf()
        val element = elementIn as? CaosScriptCommandElement
                ?: return mutableListOf()
        val referencedCommand = getCommand(element)
                ?: return mutableListOf()
        LOGGER.info("${referencedCommand.fullCommand} - parameters: ${referencedCommand.parameterStructs}")
        val parameters = referencedCommand.parameterStructs
        return element.arguments.mapIndexed{ i, it ->
            InlayInfo(parameters[i].name, it.startOffset)
        }.toMutableList()
    }

    override fun getDefaultBlackList(): MutableSet<String> {
        return mutableSetOf()
    }

    override fun getHintInfo(elementIn: PsiElement): HintInfo? {
        val element = elementIn as? CaosScriptCommandElement
                ?: return null
        val referencedCommand = getCommand(element)
                ?: return null
        val parameters = referencedCommand.parameterStructs
        return HintInfo.MethodInfo(element.commandString, parameters.map { it.name }, CaosDefLanguage.instance)
    }

    companion object {
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
    }

}
