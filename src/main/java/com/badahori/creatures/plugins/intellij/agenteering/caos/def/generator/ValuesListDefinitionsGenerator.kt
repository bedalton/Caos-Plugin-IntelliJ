package com.badahori.creatures.plugins.intellij.agenteering.caos.def.generator

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesList
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.splitByLength

/**
 * Generates a CaosDef definition for a values list
 */
internal fun generatorValuesListDefinition(valuesList: CaosValuesList) : String {
    val builder = StringBuilder()
    valuesList.description.nullIfEmpty()?.let {description ->
        val commentBody = description
                .splitByLength(CaosDefinitionsGenerator.MAX_COMMENT_LENGTH)
                .flatMap { it.split("\n") }
                .joinToString("\n * ")
        builder.append("/*\n * ").append(commentBody).append("\n */\n")
    }

    builder.append("@${valuesList.name}")
    valuesList.extensionType?.let { superType ->
        builder.append(":").append(superType)
    }
    builder.append(" {")
    for(value in valuesList.values) {
        value.beforeRegion?.let {regionHeader ->
            builder.append("\n\t# ").append(regionHeader)
        }
        builder
                .append("\n\t").append(value.value)
                .append(" = ")
                .append(value.name)
        value.description?.nullIfEmpty()?.let {description ->
            builder.append(" - ").append(description)
        }
    }
    return builder.append("\n}").toString()
}