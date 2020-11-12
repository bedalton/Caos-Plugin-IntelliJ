package com.badahori.creatures.plugins.intellij.agenteering.caos.generator

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile

/**
 * Object class for generating CaosDef files for caos variants
 */
object CaosDefinitionsGenerator {

    internal const val MAX_COMMENT_LENGTH = 80

    fun ensureVariantCaosDef(variant:CaosVariant, libsFolder:CaosVirtualFile) {
        val fileName = "${variant.code}-Lib.caosdef"
        val existing = libsFolder[fileName]
                ?.contents
                ?.startsWith("@variant")
                .orFalse()
        if (existing)
            return
    }

    fun getVariantCaosDef(variant: CaosVariant): String {
        val lib = CaosLibs[variant]
        val commands = lib.allCommands.sortedBy { it.command }.joinToString("\n\n") { command ->
            generateCommandDefinition(variant, command)
        }
        val valuesLists = lib.valuesLists
                .filterNot { it.name.startsWith("File.") }
                .joinToString("\n\n") { valuesList ->
                    generatorValuesListDefinition(valuesList)
                }
        return """
@variant(${variant.code} = ${variant.fullName})

// =============================== //
// ========== Commands =========== //
// =============================== //
$commands

// =============================== //
// ======== Values Lists ========= //
// =============================== //
$valuesLists

        """.trimIndent()
    }

}

