package com.badahori.creatures.plugins.intellij.agenteering.caos.def.generator

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.BUNDLE_DEFINITIONS_FOLDER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem

/**
 * Object class for generating CaosDef files for caos variants
 */
object CaosDefinitionsGenerator {

    fun ensureVariantCaosDef(variant: CaosVariant) {
        val libsFolder: CaosVirtualFile = CaosVirtualFileSystem.instance.getOrCreateRootChildDirectory(BUNDLE_DEFINITIONS_FOLDER)
        val fileName = "${variant.code}-Lib.caosdef"
        val existing = libsFolder[fileName]
                ?.contents
                ?.startsWith("@variant")
                .orFalse()
        if (existing)
            return
        val lib = CaosLibs[variant]
        val commands = lib.commands.joinToString("\n\n") { command ->
            generateCommandDefinition(variant, command)
        }
        val valuesLists = lib.valuesLists
                .filterNot { it.name.startsWith("File.") }
                .joinToString("\n\n") { valuesList ->
                    generatorValuesListDefinition(valuesList)
                }
        val fileText = """
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
        val file = libsFolder.createChildWithContent(fileName, fileText, true)
        LOGGER.info("Created lib at: ${file.path}")
    }

}

