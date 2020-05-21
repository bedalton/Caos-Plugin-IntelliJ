package com.openc2e.plugins.intellij.caos.references

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommand
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandWord
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.caos.def.stubs.api.variants
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptIsCommandToken
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.CaosCommandType
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.getEnclosingCommandType

class CaosScriptCommandTokenReference(private val element: CaosScriptIsCommandToken) : PsiPolyVariantReferenceBase<CaosScriptIsCommandToken>(element, TextRange(0, element.text.length)) {

    private val renameRegex: Regex = "[a-zA-Z_][a-zA-Z_#!:]{3}".toRegex()

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        if (DumbService.isDumb(element.project))
            return emptyArray()
        if (element.parent?.parent is CaosDefCommandDefElement)
            return emptyArray()
        var elements = if (element is CaosDefCompositeElement)
            findFromDefElement()
        else
            findFromScriptElement()
        val commandText = element.name
                ?: return emptyArray()
        elements = elements
                .filter {
                    it.name.toLowerCase() == commandText.toLowerCase()
                }
        return PsiElementResolveResult.createResults(elements)
    }

    private fun findFromDefElement(): List<CaosDefCommandWord> {
        val parentCommand = element.parent as? CaosDefCommand
                ?: return emptyList()
        val index = parentCommand.commandWordList.indexOf(element)
        val variants = (element.containingFile as? CaosDefFile)?.variants
        return CaosDefCommandElementsByNameIndex
                .Instance[parentCommand.commandWordList.joinToString(" ") { it.text }, element.project]
                .filter {
                    it.variants.intersect(variants.orEmpty()).isNotEmpty()
                }.mapNotNull {
                    it.command.commandWordList.getOrNull(index)
                }
                .filter { it != element }
    }

    private fun findFromScriptElement(): List<CaosDefCommandWord> {
        val type = myElement.getEnclosingCommandType()
        val formattedName = element.name?.replace(EXTRA_SPACE_REGEX, " ")
                ?: return emptyList()
        val variant = myElement.containingCaosFile?.variant
        return CaosDefCommandElementsByNameIndex
                .Instance[formattedName, element.project]
                // Filter for type and variant
                .filter {
                    val isVariant = (variant == null || it.isVariant(variant))
                    if (isVariant)
                        LOGGER.info("Command '${it.commandName}'is variant")
                    else
                        return@filter false
                    val isForElement = when (type) {
                        CaosCommandType.COMMAND -> it.isCommand
                        CaosCommandType.RVALUE -> it.isRvalue
                        CaosCommandType.LVALUE -> it.isLvalue
                        CaosCommandType.UNDEFINED -> false
                    }
                    if (!isForElement) {
                        if (!(it.isLvalue || it.isRvalue || it.isCommand)) {
                            LOGGER.severe("Command element '${it.commandName}' is unknown return type")
                            return@filter false
                        }
                        when (type) {
                            CaosCommandType.COMMAND -> LOGGER.info("Element: ${it.commandName} is for type: $type. IsRValue: ${it.isRvalue} IsLValue: ${it.isLvalue}")
                            CaosCommandType.RVALUE -> LOGGER.info("Element: ${it.commandName} is not RValue. IsCommand: ${it.isCommand} IsLValue: ${it.isLvalue}")
                            CaosCommandType.LVALUE -> LOGGER.info("Element: ${it.commandName} is not LValue. IsCommand: ${it.isCommand} IsRValue: ${it.isRvalue}")
                            CaosCommandType.UNDEFINED -> LOGGER.info("myElement is invalid command type")
                        }

                    }
                    isForElement
                }
                .mapNotNull {
                    it.command.commandWordList.getOrNull(0)
                }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        if (renameRegex.matches(newElementName))
            return element.setName(newElementName)
        return element
    }

    companion object {
        val EXTRA_SPACE_REGEX = "\\s\\s+".toRegex()
    }

}