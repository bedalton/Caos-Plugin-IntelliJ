package com.badahori.creatures.plugins.intellij.agenteering.caos.documentation

import com.intellij.navigation.ItemPresentation
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getEnclosingCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
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
                return when (element.getEnclosingCommandType()) {
                    CaosCommandType.COMMAND -> CaosScriptIcons.COMMAND
                    CaosCommandType.CONTROL_STATEMENT -> CaosScriptIcons.COMMAND
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
                ?: return simpleDescription
        val command = resolved.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                ?: return simpleDescription
        return command.fullCommand

    }
}