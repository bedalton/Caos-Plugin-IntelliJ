package com.badahori.creatures.plugins.intellij.agenteering.caos.def.generator

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesList
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty

/**
 * Generates a CaosDef definition for a values list
 */
internal fun generatorValuesListDefinition(valuesList: CaosValuesList) : String {
    val builder = StringBuilder("@${valuesList.name}")
    valuesList.superType?.let {superType ->
        builder.append(":").append(superType)
    }
    builder.append("{")
    for(value in valuesList.values) {
        builder
                .append("\n\t").append(value.value)
                .append(" = ")
                .append(value.name)
        value.description?.nullIfEmpty()?.let {description ->
            builder.append(" - ").append(description)
        }
    }
    return builder.append("};").toString()
}