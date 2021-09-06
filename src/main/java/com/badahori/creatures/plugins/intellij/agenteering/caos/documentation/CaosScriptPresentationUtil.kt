package com.badahori.creatures.plugins.intellij.agenteering.caos.documentation

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandWord
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.variants
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.getNextNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPreviousNonEmptyNode
import com.intellij.navigation.ItemPresentation
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
        // get command from Command Element parent
        return (element.parent as? CaosScriptCommandElement)
                ?.commandDefinition
                ?.let {commandDefinition ->
                    return commandDefinition.fullCommandHeader
                }
                // Get command from parent command def element
                ?: (element.parent?.parent as? CaosDefCommandDefElement)
                        ?.fullCommandHeader
                // Get command from parent Caos Def Code Block
                ?: (element.parent as? CaosDefCodeBlock)?.let {
                    getCommandHeaderFromCodeBlock(element as CaosDefCommandWord)
                }
                // Return generic description of command on no matches
                ?: "command [${element.text}]"
    }

    private fun getCommandHeaderFromCodeBlock(element:CaosDefCommandWord) : String? {
        val elementText = element.text
        val previousWordElement
                = (element.getPreviousNonEmptyNode(false) as? CaosDefCommandWord)
        val commandCombinations = mutableListOf<String>()
        if (previousWordElement != null) {
            val withPrevious = "${previousWordElement.text} $elementText"
            (previousWordElement.getPreviousNonEmptyNode(false) as? CaosDefCommandWord)
                    ?.text
                    ?.let {previousPrevious ->
                        commandCombinations.add("$previousPrevious $withPrevious")
                    }

            commandCombinations.add(withPrevious)
        }
        val nextElement
            = (element.getNextNonEmptySibling(false) as? CaosDefCommandWord)
        if (nextElement != null) {
            val withNext = "$elementText ${nextElement.text}"
            (nextElement.getNextNonEmptySibling(false) as? CaosDefCommandWord)
                    ?.text
                    ?.let {nextNext ->
                        commandCombinations.add("$withNext $nextNext")
                    }
            if (previousWordElement != null) {
                commandCombinations.add("${previousWordElement.text} $withNext")
            }
            commandCombinations.add(withNext)
        }
        val variants = element.variants.map { it.code }
        return commandCombinations.mapNotNull {commandString ->
            CaosLibs.commands(commandString).firstOrNull {command ->
                command.variants.intersect(variants).isNotEmpty()
            }
        }.firstOrNull()?.fullCommandHeader
    }
}