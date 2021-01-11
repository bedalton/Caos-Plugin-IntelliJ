package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.copyToClipboard
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CopyAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAware

/**
 * Creates a file
 * @todo implement multiple file types (ie. implementations or protocols)
 */
class CaosScriptCopyForBoBCobAction : CopyAction(), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val dataContext = event.dataContext
        (dataContext.getData(PlatformDataKeys.PSI_FILE) as? CaosScriptFile)?.let {
            val formattedText = formatForBobCob(it.text)
            copyToClipboard(formattedText)
        }
    }

    override fun update(event: AnActionEvent) {
        val presentation = event.presentation
        val dataContext = event.dataContext
        val enabled = (dataContext.getData(PlatformDataKeys.PSI_FILE) as? CaosScriptFile)?.variant?.isOld.orFalse()
        presentation.isEnabled = enabled
        presentation.isVisible = enabled
        presentation.text = CaosBundle.message("caos.actions.copy-for-bob-cob.title")
        presentation.description = CaosBundle.message("caos.actions.copy-for-bob-cob.description")
        presentation.icon = AllIcons.Actions.Copy
    }

    companion object {
        @Suppress("SpellCheckingInspection")
        // Randomish pattern that hopefully never exists in a users code
        private const val replacePattern = ";;;;__xZZZZx___&&&&___xZZZx__;;;"
        private val stringRegex = "([ \t]*\\[[^\\]]+][ \t]*)".toRegex(RegexOption.MULTILINE)
        private val multiWhitespace = "[, \t\n][, \t\n]+".toRegex(RegexOption.MULTILINE)
        private val spacesAroundCommas = "(\\s+[,]\\s*|\\s*[,]\\s+)".toRegex(RegexOption.MULTILINE)

        fun formatForBobCob(scriptIn: String): String {
            val matches = stringRegex.findAll(scriptIn).toList().nullIfEmpty()
            val script = if (matches != null) {
                stringRegex.replace(scriptIn, replacePattern)
            } else
                scriptIn
            val mapper: (string: String) -> String = { string ->
                string.trim()
                    .replace(spacesAroundCommas, ",")
                    .replace(multiWhitespace, " ")
                    .trim('\n','\t','\r',' ',',')
            }
            var out = script
                .split("\n")
                .filterNot {
                    it.isBlank() || it.trim().startsWith("*")
                }
                .joinToString("\n", transform = mapper)

            if (matches != null) {
                val pattern = replacePattern.toRegex()
                for(match in matches) {
                    val string = match.value
                    val prefix = if (string.startsWith(" ") || string.startsWith("\t")) " " else ""
                    val suffix = if (string.endsWith(" ") || string.endsWith("\t")) " " else ""
                    out = pattern.replaceFirst(out, prefix + match.value.trim() + suffix)
                }
            }
            return out.trim()
        }
    }
}