@file:Suppress("UnstableApiUsage")

package com.openc2e.plugins.intellij.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.containingCaosDefFile
import com.openc2e.plugins.intellij.caos.def.stubs.api.isVariant
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.getSelfOrParentOfType

enum class CaosScriptInlayTypeHint(description:String, override val enabled: Boolean) : CaosScriptHintsProvider {
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
                    ?: return list
            list.add(InlayInfo("("+typeDefValue.value+")", element.endOffset))
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
            val variant = element.containingCaosFile?.variant ?: CaosScriptProjectSettings.variant
            val typeList = CaosDefTypeDefinitionElementsByNameIndex
                    .Instance["EventNumbers", element.project]
                    .filter {
                        it.containingCaosDefFile.isVariant(variant)
                    }
                    .firstOrNull()
                    ?: return emptyList()
            val value = typeList.getValueForKey(eventElement.text)
                    ?: return emptyList()
            return listOf(InlayInfo("(${value.value})", eventElement.endOffset))
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
    RVALUE_RETURN_TYPE_HINT("Show rvalue return type", true) {

        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptRvalue
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val rvalue = element as? CaosScriptRvalue
                    ?: return emptyList()
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
            val parameterStruct = containingCommand.parameterStructs?.getOrNull(index)
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
