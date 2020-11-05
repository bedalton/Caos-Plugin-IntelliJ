package com.badahori.creatures.plugins.intellij.agenteering.caos.def.generator

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.splitByLength

/**
 * Generates a CaosDef command definition
 */
internal fun generateCommandDefinition(variant: CaosVariant, command: CaosCommand): String {

    // Split comment on word by max length
    val comment = command.description
            ?.splitByLength(120)
            ?.flatMap { it.split("\n") }
            .orEmpty()
            .toMutableList()
    // Add group hashtag
    comment.add("#" + command.commandGroup)

    // Add empty newline after #group
    comment.add("")

    // If requires owner, add tag for it
    if (command.requiresOwnr) {
        comment.add("@Ownr")
    }

    // Add rvalue tag command is rvalue
    if (command.rvalue) {
        comment.add("@rvalue")
    }

    // Add lvalue tag if command is lvalue
    if (command.lvalue) {
        comment.add("@lvalue")
    }

    // Format parameters in comment
    for (parameter in command.parameters) {
        var type = parameter.type.simpleName

        // Add tail to type and wrap if type does not start with '[' (ie. [animation], [byte_string], [string])
        // Types starting with '[' should never have a tail, so ignore it
        if (!type.startsWith("[")) {
            // If has value list, append @valuelist
            val tail = parameter.valuesList[variant]?.name?.let {
                "@$it"

            // If has both min and max, append [min to max]
            } ?: parameter.min?.let { min ->
                parameter.max?.let { max ->
                    "[$min to $max]"
                }
            }
            ?: ""
            type = "($type$tail)"
        }
        // Add parameter description if any
        val description = parameter.description.nullIfEmpty()?.let { description ->
            "- $description"
        } ?: ""
        // Append formatted parameter tag to comment lines
        comment.add("@param {${parameter.name}} $type $description".trim())
    }

    // Format and add return type if necessary
    if (command.returnType == CaosExpressionValueType.COMMAND) {
        val returnType = command.returnType.simpleName
        // return staring with '[' should most likely never happen
        // They are [animation], [byte_string] and [string] no methods return these
        val returnText = if (returnType.startsWith("[")) {
            // Types starting with '[' should not ever have a tail, s
            // so do not try to find one
            "@return $returnType"
        } else {
            val tail = command.returnValuesList[variant]?.name?.let { name ->
                "@$name"
            } ?: ""
            "@return ($returnType$tail)"
        }
        comment.add(returnText)
    }

    // Format comment body
    val commentBody = comment.joinToString("\n * ")

    // Create start of definition by wrapping comment body in its start and end characters
    val builder = StringBuilder("/*\n * ").append(commentBody).append("\n */\n")

    // Append command (type). Type will never start with '[' so no need to check
    builder.append(command.command).append(" (").append(command.returnType.simpleName).append(")")

    // Format parameters with type for command definition
    for (parameter in command.parameters) {
        val type = parameter.type.simpleName
        builder.append(" ").append(parameter.name).append(" ")
        if (type.startsWith("["))
            builder.append(type)
        else
            builder.append("(").append(type).append(")")
    }
    // Close and return command definition
    return builder.append(";").toString()
}