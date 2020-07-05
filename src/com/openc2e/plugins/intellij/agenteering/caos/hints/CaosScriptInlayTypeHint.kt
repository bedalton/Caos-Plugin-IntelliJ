@file:Suppress("UnstableApiUsage")

package com.openc2e.plugins.intellij.agenteering.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.agenteering.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.openc2e.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.api.CaosDefTypeDefinitionElement
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.api.isVariant
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.api.isVariant
import com.openc2e.plugins.intellij.agenteering.caos.lang.variant
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.*
import com.openc2e.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.getParentOfType
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.openc2e.plugins.intellij.agenteering.caos.utils.CaosAgentClassUtils
import com.openc2e.plugins.intellij.agenteering.caos.utils.orFalse


enum class CaosScriptInlayTypeHint(description: String, override val enabled: Boolean, override val priority: Int = 0) : CaosScriptHintsProvider {

    ATTRIBUTE_BITFLAGS_HINT("Show bit flag for outer expression", true, 100) {


        override fun isApplicable(element: PsiElement): Boolean {
            return (element as? CaosScriptExpression)?.isInt.orFalse() && usesBitFlags(element as CaosScriptExpression)
        }

        private fun usesBitFlags(element: CaosScriptExpression): Boolean {
            val parent = element.parent
            val caosCommandToken = when {
                parent is CaosScriptComparesEqualityElement -> getCommandTokenFromEquality(parent, element)
                parent.parent.parent is CaosScriptCommandElement -> getCommandTokenFromCommand(parent.parent.parent as CaosScriptCommandElement, element)
                else -> null
            } ?: return false
            caosCommandToken.commandString.toUpperCase().let {
                if (it == "ATTR" || it == "BUMP")
                    return true
            }
            return getTypeList(caosCommandToken) != null
        }

        private fun getTypeList(token: CaosScriptIsCommandToken): CaosDefTypeDefinitionElement? {
            val typeDef = token.reference.resolve()
                    ?.getParentOfType(CaosDefCommandDefElement::class.java)
                    ?.returnTypeStruct
                    ?.type
                    ?.typedef
                    ?: return null
            val variant = token.containingCaosFile.variant
            return CaosDefTypeDefinitionElementsByNameIndex
                    .Instance[typeDef, token.project].firstOrNull {
                it.isVariant(variant) && it.typeNote?.text?.toLowerCase() == "bitflags"
            }
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val expression = element as? CaosScriptExpression
                    ?: return emptyList()
            val attr = expression.intValue
                    ?: return emptyList()
            val parent = element.parent
            val commandToken = when {
                parent is CaosScriptComparesEqualityElement -> getCommandTokenFromEquality(parent, element)
                parent.parent.parent is CaosScriptCommandElement -> getCommandTokenFromCommand(parent.parent.parent as CaosScriptCommandElement, expression)
                else -> null
            } ?: return emptyList()
            val typeList = getTypeList(commandToken)
                    ?: return emptyList()
            val values = mutableListOf<String>()
            for (key in typeList.keys) {
                try {
                    if (attr and key.key.toInt() > 0)
                        values.add(key.value)
                } catch (e: Exception) {
                }
            }
            return listOf(InlayInfo("(${values.joinToString()})", element.endOffset))
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptExpression) {
                return null
            }
            val parent = element.getParentOfType(CaosScriptExpectsValueOfType::class.java)
                    ?: element.getParentOfType(CaosScriptEqualityExpression::class.java)
            return parent?.let {
                HintInfo.MethodInfo(parent.text, listOf(), CaosDefLanguage.instance)
            }
        }
    },
    ASSUMED_VALUE_NAME_HINT("Show assumed value name", true) {
        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptExpression
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val project = element.project
            val list = mutableListOf<InlayInfo>()
            if (DumbService.isDumb(project))
                return list
            if (element !is CaosScriptExpression)
                return list
            val typeDefValue = element.getTypeDefValue()
                    ?: (element.parent?.parent?.parent as? CaosScriptCommandElement)?.let letCommand@{
                        val commandDef = getCommandTokenFromCommand(it, element)
                                ?.reference
                                ?.multiResolve(true)
                                ?.firstOrNull()
                                ?.element
                                ?.getParentOfType(CaosDefCommandDefElement::class.java)
                                ?: return@letCommand null

                        val typeDef = commandDef.returnTypeStruct?.type?.typedef ?: return@letCommand null
                        getListValue(element.containingCaosFile.variant, typeDef, element.project, element.text)
                    } ?: return list
            list.add(InlayInfo("(" + typeDefValue.value + ")", element.endOffset))
            return list
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptExpression) {
                return null
            }
            val parent = element.getParentOfType(CaosScriptExpectsValueOfType::class.java)
                    ?: element.getParentOfType(CaosScriptEqualityExpression::class.java)
            return parent?.let {
                HintInfo.MethodInfo(parent.text, listOf(), CaosDefLanguage.instance)
            }
        }
    },
    ASSUMED_EVENT_SCRIPT_NAME_HINT("Show assumed event script name", true) {

        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptEventNumberElement
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val eventElement = element as? CaosScriptEventNumberElement
                    ?: return emptyList()
            val variant = element.containingCaosFile.variant
            val typeList = CaosDefTypeDefinitionElementsByNameIndex
                    .Instance["EventNumbers", element.project]
                    .firstOrNull {
                        it.containingCaosDefFile.isVariant(variant)
                    }
                    ?: return emptyList()
            val value = typeList.getValueForKey(eventElement.text)
                    ?: return emptyList()
            return listOf(
                    InlayInfo("event", eventElement.startOffset),
                    InlayInfo("(${value.value})", eventElement.endOffset)
            )
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptExpression) {
                return null
            }
            val parent = element.getParentOfType(CaosScriptExpectsValueOfType::class.java)
                    ?: element.getParentOfType(CaosScriptEqualityExpression::class.java)
            return parent?.let {
                HintInfo.MethodInfo(parent.text, listOf(), CaosDefLanguage.instance)
            }
        }
    },
    DDE_PIC_DIMENSIONS("Show DDE: PICT dimensions", true) {

        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptPictDimensionLiteral
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val dimensions = element as? CaosScriptPictDimensionLiteral
                    ?: return emptyList()
            val text = dimensions.dimensions.let {
                "${it.first}x${it.second}"
            }
            return listOf(InlayInfo(text, element.endOffset))
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptPictDimensionLiteral) {
                return null
            }
            return HintInfo.MethodInfo("DDE: PICT " + element.text, listOf(), CaosDefLanguage.instance)
        }
    },
    RVALUE_RETURN_TYPE_HINT("Show rvalue return type", true) {

        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptRvalue
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val rvalue = element as? CaosScriptRvalue
                    ?: return emptyList()
            (rvalue.parent.parent as? CaosScriptCAssignment)?.let { assignment ->
                if (assignment.commandString.toUpperCase() == "SETV") {
                    val lvalue = assignment.lvalue?.text
                    if (lvalue == "clas") {
                        rvalue.expression?.intValue?.let {
                            val agentClass = CaosAgentClassUtils.parseClas(it)
                                    ?: return emptyList()
                            return listOf(
                                    InlayInfo("family:${agentClass.family} genus:${agentClass.genus} species:${agentClass.species}", rvalue.endOffset)
                            )
                        }
                    }
                }
            }
            val token = rvalue.commandToken
                    ?: return emptyList()
            val resolved = getCommand(token)
                    ?: return emptyList()
            val type = resolved.returnTypeStruct?.type?.type
                    ?: return emptyList()
            val inlayInfo = listOf(InlayInfo("($type)", token.endOffset))
            val expectsParentOfType = (rvalue.parent as? CaosScriptExpectsValueOfType)
                    ?: return inlayInfo
            val index = expectsParentOfType.index
            val containingCommand = (expectsParentOfType.parent as? CaosScriptCommandElement)
                    ?.commandToken
                    ?.reference
                    ?.multiResolve(false)
                    ?.firstOrNull()
                    ?.element
                    ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                    ?: return inlayInfo
            val parameterStruct = containingCommand.parameterStructs.getOrNull(index)
            if (parameterStruct != null) {
                val parameterName = parameterStruct.name.toLowerCase()
                val typeName = type.toLowerCase()
                if (parameterName == typeName)
                    return emptyList()
            }
            return inlayInfo
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptExpression) {
                return null
            }
            val parent = element.getParentOfType(CaosScriptExpectsValueOfType::class.java)
                    ?: element.getParentOfType(CaosScriptEqualityExpression::class.java)
            return parent?.let {
                HintInfo.MethodInfo(parent.text, listOf(), CaosDefLanguage.instance)
            }
        }
    };

    override val option: Option = Option("SHOW_${this.name}", description, enabled)
}


private fun getCommandTokenFromCommand(command: CaosScriptCommandElement, expression: CaosScriptExpression): CaosScriptIsCommandToken? {
    if (command is CaosScriptCAssignment) {
        if (expression.parent.parent is CaosScriptLvalue) {
            return null
        }
        val lvalue = command.lvalue
                ?: return null
        return lvalue.varToken?.lastAssignment
                ?: command.lvalue?.commandToken
    }
    val argument = expression.parent.parent as? CaosScriptArgument
            ?: return null
    if (argument.parent != command) {
        LOGGER.severe("Expression command pair mismatch in command ${command.text} with expression: ${expression.text}")
        return null
    }
    return command.commandToken
}


private fun getCommandTokenFromEquality(parent: CaosScriptComparesEqualityElement, expression: CaosScriptExpression): CaosScriptIsCommandToken? {
    val other = if (parent.first == expression) parent.second else parent.first
    return other.varToken?.lastAssignment ?: other.rvaluePrime?.commandToken
}