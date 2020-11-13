package com.badahori.creatures.plugins.intellij.agenteering.caos.generator

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException

open class CaosDefGeneratorTask(
) : DefaultTask() {

    lateinit var targetFolder: File
    var createFolder: Boolean = false

    @TaskAction
    fun generateCaosDef() {
        val libJsonText = universalLibJsonText.nullIfEmpty()
                ?: throw IOException("Failed to load CAOS lib json in generator")
        val exists = targetFolder.exists()
        if (exists && targetFolder.isFile)
            throw IOException("Cannot generate CAOS def files into non-directory path: $targetFolder")
        if (!exists && !createFolder)
            throw IOException("Directory '$targetFolder' for CAOS def generation does not exist")
        if (!exists) {
            if (!targetFolder.mkdirs())
                throw IOException("Failed to create folder file '$targetFolder'")
        }
        val variants = CaosVariant.variants
        for (variant in variants) {
            val caosDef = CaosDefinitionsGenerator.getVariantCaosDef(variant)
            if (caosDef.isEmpty())
                throw IOException("Failed to generate CAOS def file for variant: ${variant.code}")
            val variantFileName = "${variant.code}-Lib.caosdef"
            val variantFile = File(targetFolder, variantFileName)
            if (!variantFile.exists() && !variantFile.createNewFile())
                throw IOException("Failed to create empty file at ${variantFile.path}")
            variantFile.writeText(caosDef, Charsets.UTF_8)
        }
        val libJsonFileName = "caos.universal.lib.json"
        val libJsonFile = File(targetFolder, libJsonFileName)
        if (!libJsonFile.exists() && !libJsonFile.createNewFile())
            throw IOException("Failed to create $libJsonFileName")
        libJsonFile.writeText(libJsonText)
    }

}