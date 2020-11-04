package com.badahori.creatures.plugins.intellij.agenteering.caos.def.generator

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.utils.splitByLength

/**
 * Generates a CaosDef command definition
 */
internal fun generateCommandDefinition(command: CaosCommand): String {
    val comment = command.description
            ?.split("\n")
            ?.flatMap { it.splitByLength(120) }
            .orEmpty()
            .toMutableList()
    comment.add("#" + command.commandGroup)
    comment.add("")
    if (command.requiresOwnr)
        comment.add("@Ownr")
    if (command.rvalue)
        comment.add("@rvalue")
    if (command.lvalue)
        comment.add("@lvalue")
    val formattedCommand = comment.joinToString("\n * ")
    val builder = StringBuilder("/*\n").append(formattedCommand).append(" */\n")
    builder.append(command.command).append(" (").append(command.returnType.simpleName).append(")")
    for (parameter in command.parameters) {
        val type = parameter.type.simpleName
        builder.append(" ").append(parameter.name).append(" ")
        if (type.startsWith("["))
            builder.append(type)
        else
            builder.append("(").append(type).append(")")
    }
    return builder.append(";").toString()
}