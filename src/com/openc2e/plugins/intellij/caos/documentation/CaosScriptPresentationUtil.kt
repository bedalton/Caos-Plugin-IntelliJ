package com.openc2e.plugins.intellij.caos.documentation

import com.intellij.navigation.ItemPresentation
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptIsCommandToken
import com.openc2e.plugins.intellij.caos.psi.util.*
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import icons.CaosScriptIcons
import javax.swing.Icon

object CaosScriptPresentationUtil {
    fun getPresentation(element: CaosScriptIsCommandToken) : ItemPresentation {
        val text = getDescriptiveText(element)
        return object : ItemPresentation {
            override fun getPresentableText(): String {
                return text
            }

            override fun getLocationString(): String {
                return element.containingFile?.name ?: element.originalElement?.containingFile?.name ?: "..."
            }

            override fun getIcon(b: Boolean): Icon {
                val commandType = element.getEnclosingCommandType()
                return when (commandType) {
                    CaosCommandType.COMMAND -> CaosScriptIcons.COMMAND
                    CaosCommandType.RVALUE -> CaosScriptIcons.RVALUE
                    CaosCommandType.LVALUE -> CaosScriptIcons.LVALUE
                    CaosCommandType.UNDEFINED -> CaosScriptIcons.COMMAND
                }
            }
        }
    }
    fun getDescriptiveText(element:CaosScriptIsCommandToken) : String {
        val simpleDescription = "command [${element.text}]"
        val resolved = element
                .reference
                .multiResolve(true)
                .firstOrNull()
                ?.element
        if (resolved == null) {
            LOGGER.info("Resolved is null for command token: ${element.text}")
            return simpleDescription
        }
        val command = resolved?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
        if (command == null) {
            LOGGER.info("Resolved element does not have parent command def element. Self is ${resolved.elementType}. Parent is ${resolved.parent?.elementType}")
            return simpleDescription
        }
        return command.fullCommand

    }
}