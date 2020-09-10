@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefValuesListValueStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosAgentClassUtils
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement


enum class CaosScriptInlayTypeHint(description: String, override val enabled: Boolean, override val priority: Int = 0) : CaosScriptHintsProvider {

    ATTRIBUTE_BITFLAGS_RETURN_VALUE_HINT("Show bit flag for outer expression", true, 100) {
        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && (element as? CaosScriptExpression)?.isInt.orFalse() && usesBitFlags(element as CaosScriptExpression)
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
                    return (parent.parent.parent !is CaosScriptCommandCall)
            }
            return getTypeList(caosCommandToken) != null
        }

        private fun getTypeList(token: CaosScriptIsCommandToken): CaosDefValuesListElement? {
            val valuesList = token.reference.resolve()
                    ?.getParentOfType(CaosDefCommandDefElement::class.java)
                    ?.returnTypeStruct
                    ?.type
                    ?.valuesList
                    ?: return null
            val variant = token.containingCaosFile?.variant
                    ?: return null
            return CaosDefValuesListElementsByNameIndex
                    .Instance[valuesList, token.project]
                    .firstOrNull check@{
                        if (!it.isVariant(variant))
                            return@check false
                        val typeNote = it.typeNoteStatement?.typeNote?.text
                                ?: return@check false
                        typeNote.toLowerCase().contains("bitflags").orFalse()
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
            for (key in typeList.valuesListValues) {
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
                HintInfo.MethodInfo(parent.text, listOf(), CaosScriptLanguage.instance)
            }
        }
    },
    ATTRIBUTE_BITFLAGS_ARGUMENT_HINT("Show bit flag for argument value", true, 100) {
        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && (element as? CaosScriptExpression)?.isInt.orFalse() && usesBitFlags(element as CaosScriptExpression)
        }

        private fun usesBitFlags(element: CaosScriptExpression): Boolean {
            val parent = element.parent
            val caosCommandToken = when {
                parent is CaosScriptComparesEqualityElement -> getCommandTokenFromEquality(parent, element)
                parent.parent.parent is CaosScriptCommandElement -> getCommandTokenFromCommand(parent.parent.parent as CaosScriptCommandElement, element)
                else -> null
            } ?: return false
            val argument = element.getParentOfType(CaosScriptArgument::class.java)
                    ?: return false
            return getTypeList(caosCommandToken, argument.index) != null
        }

        private fun getTypeList(token: CaosScriptIsCommandToken, index: Int): CaosDefValuesListElement? {
            val valuesList = token.reference.resolve()
                    ?.getParentOfType(CaosDefCommandDefElement::class.java)
                    ?.parameterStructs
                    ?.getOrNull(index)
                    ?.type
                    ?.valuesList
                    ?: return null
            val variant = token.containingCaosFile?.variant
                    ?: return null
            return CaosDefValuesListElementsByNameIndex
                    .Instance[valuesList, token.project].firstOrNull check@{
                if (!it.isVariant(variant))
                    return@check false
                val typeNote = it.typeNoteStatement?.typeNote?.text
                        ?: return@check false
                typeNote.toLowerCase().contains("bitflags").orFalse()
            }
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val expression = element as? CaosScriptExpression
                    ?: return emptyList()
            val attr = expression.intValue
                    ?: return emptyList()
            val parent = element.parent
            val argumentIndex: Int = element.getParentOfType(CaosScriptArgument::class.java)
                    ?.index
                    ?: return emptyList()
            val commandToken = when {
                parent.parent.parent is CaosScriptCommandElement -> getCommandTokenFromCommand(parent.parent.parent as CaosScriptCommandElement, expression)
                else -> null
            } ?: return emptyList()
            val typeList = getTypeList(commandToken, argumentIndex)
                    ?: return emptyList()
            val values = mutableListOf<String>()
            for (key in typeList.valuesListValues) {
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
                HintInfo.MethodInfo(parent.text, listOf(), CaosScriptLanguage.instance)
            }
        }
    },
    ASSUMED_GENUS_NAME_HINT("Show genus simple name", true, 102) {

        override fun isApplicable(element: PsiElement): Boolean {
            if (!option.isEnabled())
                return false
            if (element !is CaosScriptExpression || !element.isInt)
                return false
            if (element.parent?.parent?.parent is CaosScriptGenus) {
                return true
            }
            val argument = element.getParentOfType(CaosScriptArgument::class.java)
                    ?: return false
            val commandToken = argument.getParentOfType(CaosScriptCommandElement::class.java)?.commandToken
                    ?: return false
            val command = commandToken.reference.resolve()?.getParentOfType(CaosDefCommandDefElement::class.java)
                    ?: return false
            val index = argument.index
            if (index < 1) // Cannot have preceding FAMILY value if index is 0
                return false
            val parameters = command.parameterStructs
            val thisParameter = parameters.getOrNull(index) ?: return false
            if (!thisParameter.name.equalsIgnoreCase("genus"))
                return false
            val previousParameter = parameters.getOrNull(index - 1)
                    ?: return false
            return (previousParameter.name.equalsIgnoreCase("family"))
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            return HintInfo.MethodInfo("Genus", listOf("family", "genus"), CaosScriptLanguage.instance)
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val variant = (element.containingFile as? CaosScriptFile)?.variant
                    ?: return emptyList()
            val genus = element.text.toIntSafe()
                    ?: return emptyList()
            val family = element.getPreviousNonEmptySibling(true)?.text?.toIntSafe()
                    ?: return emptyList()
            return getGenusInlayInfo(variant, element, family, genus)
        }

        private fun getGenusInlayInfo(variant: CaosVariant, element: PsiElement, family: Int, genus: Int): List<InlayInfo> {
            val value: CaosDefValuesListValueStruct = CaosDefValuesListElementsByNameIndex
                    .Instance["Genus", element.project]
                    .filter {
                        it.isVariant(variant)
                    }
                    .map {
                        it.getValueForKey("$family $genus")
                    }
                    .firstOrNull()
                    ?: return emptyList()
            return listOf(InlayInfo("(${value.value})", element.endOffset))
        }
    },
    ASSUMED_VALUE_NAME_HINT("Show assumed value name", true) {
        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && element is CaosScriptExpression
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val project = element.project
            val list = mutableListOf<InlayInfo>()
            if (DumbService.isDumb(project))
                return list
            if (element !is CaosScriptExpression)
                return list
            val valuesListValue = element.getValuesListValue()
                    ?: (element.parent?.parent?.parent as? CaosScriptCommandElement)?.let letCommand@{
                        val commandDef = getCommandTokenFromCommand(it, element)
                                ?.reference
                                ?.multiResolve(true)
                                ?.firstOrNull()
                                ?.element
                                ?.getParentOfType(CaosDefCommandDefElement::class.java)
                                ?: return@letCommand null

                        val valuesList = commandDef.returnTypeStruct?.type?.valuesList ?: return@letCommand null
                        val variant = element.containingCaosFile?.variant
                                ?: return emptyList()
                        getListValue(variant, valuesList, element.project, element.text)
                    } ?: return list
            list.add(InlayInfo("(" + valuesListValue.value + ")", element.endOffset))
            return list
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptExpression) {
                return null
            }
            val parent = element.getParentOfType(CaosScriptExpectsValueOfType::class.java)
                    ?: element.getParentOfType(CaosScriptEqualityExpression::class.java)
            return parent?.let {
                HintInfo.MethodInfo(parent.text, listOf(), CaosScriptLanguage.instance)
            }
        }
    },
    ASSUMED_EVENT_SCRIPT_NAME_HINT("Show assumed event script name", true) {

        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && element is CaosScriptEventNumberElement
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val eventElement = element as? CaosScriptEventNumberElement
                    ?: return emptyList()
            val items = listOf(
                    InlayInfo("event", eventElement.startOffset)
            )
            val variant = element.containingCaosFile?.variant
                    ?: return items
            val typeList = CaosDefValuesListElementsByNameIndex
                    .Instance["EventNumbers", element.project]
                    .firstOrNull {
                        it.containingCaosDefFile.isVariant(variant)
                    }
                    ?: return items
            val value = typeList.getValueForKey(eventElement.text)
                    ?: return items
            return items + InlayInfo("(${value.value})", eventElement.endOffset)
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptExpression) {
                return null
            }
            val parent = element.getParentOfType(CaosScriptExpectsValueOfType::class.java)
                    ?: element.getParentOfType(CaosScriptEqualityExpression::class.java)
            return parent?.let {
                HintInfo.MethodInfo(parent.text, listOf(), CaosScriptLanguage.instance)
            }
        }
    },
    DDE_PIC_DIMENSIONS("Show DDE: PICT dimensions", true) {

        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && element is CaosScriptPictDimensionLiteral
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
            return HintInfo.MethodInfo("DDE: PICT " + element.text, listOf(), CaosScriptLanguage.instance)
        }
    },
    RVALUE_RETURN_TYPE_HINT("Show rvalue return type", true) {

        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && element is CaosScriptRvalue
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val rvalue = element as? CaosScriptRvalue
                    ?: return emptyList()
            (rvalue.parent.parent as? CaosScriptCAssignment)?.let { assignment ->
                if (assignment.commandString.toUpperCase() == "SETV") {
                    val lvalue = assignment.lvalue?.text?.toLowerCase()
                    if (lvalue == "clas") {
                        rvalue.expression?.intValue?.let {
                            val agentClass = CaosAgentClassUtils.parseClas(it)
                                    ?: return emptyList()
                            return listOf(
                                    InlayInfo("family:${agentClass.family} genus:${agentClass.genus} species:${agentClass.species}", rvalue.endOffset)
                            )
                        }
                        return emptyList()
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
                HintInfo.MethodInfo(parent.text, listOf(), CaosScriptLanguage.instance)
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
    val other: CaosScriptExpression? = if (parent.first == expression) parent.second else parent.first
    return other?.varToken?.lastAssignment ?: other?.rvaluePrime?.commandToken
}

private val GENUS_KEY = Key<String>("com.badahori.creatures.plugins.intellij.agenteering.caos.psi.hints.GENUS")