package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor


fun CaosScriptFile.copyAsOneLine() {
    ApplicationManager.getApplication().runWriteAction run@{
        val text = text
        var trimmedText = text.split("\n").joinToString(" ") {
            val line = it.trim()
            if (line.startsWith("*"))
                ""
            else
                line
        }
        trimmedText = trimmedText.replace(WHITESPACE, " ")
        trimmedText = trimmedText.replace("\\s*,\\s*".toRegex(), ",").trim()
        copyToClipboard(trimmedText)
    }
}

fun CaosScriptFile.trimErrorSpaces() {
    ApplicationManager.getApplication().runWriteAction {
        CommandProcessor.getInstance().executeCommand(project, run@{
            val text = text
            val trimmedText = CaosStringUtil.sanitizeCaosString(text)
            if (trimmedText == text)
                return@run
            val document = document
                    ?: return@run
            document.replaceString(0, text.length, trimmedText)
        }, "Strip Error Spaces", "Strip Error Spaces")
    }
}
