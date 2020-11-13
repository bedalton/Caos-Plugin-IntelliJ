package com.badahori.creatures.plugins.intellij.agenteering.caos.generator

/**
 * Object class for generating CaosDef files for caos variants
 */
internal object CaosDefinitionsGenerator {

    internal const val MAX_COMMENT_LENGTH = 80

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
#${universalLib.modDate}
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

