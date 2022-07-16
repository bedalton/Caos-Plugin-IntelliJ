@file:Suppress("MemberVisibilityCanBePrivate")

package com.badahori.creatures.plugins.intellij.agenteering.caos.generator

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.time.Instant.ofEpochSecond
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * A Gradle task for generating the CAOS def files needed by the plugin
 * to support element resolution to docs files for browsing by the user
 */
open class CaosDefGeneratorTask : DefaultTask() {

    /**
     * The folder into which to generate all files
     */
    @OutputDirectory
    lateinit var targetFolder: File

    /**
     * Whether or not to create the target file, if it does not exist.
     */
    @Input
    var createFolder: Boolean = false

    /**
     * The task used for CAOS def generation
     */
    @TaskAction
    fun generateCaosDef() {
        val libJsonText = universalLibJsonText.nullIfEmpty()
                ?: throw IOException("Failed to load CAOS lib json in generator")
        val version = versionString

        // Ensure target folder exists
        val exists = targetFolder.exists()
        if (exists && targetFolder.isFile) {
            throw IOException("Cannot generate CAOS def files into non-directory path: $targetFolder")
        }
        if (!exists && !createFolder) {
            throw IOException("Directory '$targetFolder' for CAOS def generation does not exist")
        }
        if (!exists) {
            if (!targetFolder.mkdirs()) {
                throw IOException("Failed to create folder file '$targetFolder'")
            }
        }

        // Get all variants
        val variants = CaosVariant.variants

        // Create CAOS def files for all variants
        for (variant in variants) {
            // Generate CAOS def file text
            val caosDef = CaosDefinitionsGenerator.getVariantCaosDef(variant, version)
            // Ensure CAOS def file text is not empyt
            if (caosDef.isEmpty())
                throw IOException("Failed to generate CAOS def file for variant: ${variant.code}")

            // Create File
            val variantFileName = "${variant.code}-Lib.caosdef"
            val variantFile = File(targetFolder, variantFileName)

            // Ensure file exists for writing
            if (!variantFile.exists() && !variantFile.createNewFile())
                throw IOException("Failed to create empty file at ${variantFile.path}")
            // Write text to file
            variantFile.writeText(caosDef, Charsets.UTF_8)
        }
        // Write LIB json file to resources folder
        // Do this last, because if the definitions files were not created
        // This will show up as an error before those do
        writeLibJson(libJsonText)
        // Lastly write a version.txt file to show when things were last updated
        writeVersionFile(versionString)
    }

    /**
     * Write version to text file for quick reference
     */
    private fun writeVersionFile(version:String) {
        val versionTextFile = File(targetFolder, "version.txt")
        if (!versionTextFile.exists() && !versionTextFile.createNewFile())
            throw IOException("Failed to create 'version.txt' file at path ${versionTextFile.path};")
        versionTextFile.writeText(version)
    }

    /**
     * Writes lib json to text file for use in Plugin
     */
    private fun writeLibJson(libJsonText:String) {
        val libJsonFileName = "caos.universal.lib.json"
        val libJsonFile = File(targetFolder, libJsonFileName)
        if (!libJsonFile.exists() && !libJsonFile.createNewFile())
            throw IOException("Failed to create $libJsonFileName")
        libJsonFile.writeText(libJsonText)

    }

    companion object {

        /**
         * Date formatter for version string
         */
        private val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd.")
                .withLocale(Locale.US)
                .withZone(ZoneId.of("America/Los_Angeles"))

        /**
         * Gets CAOS def version string
         */
        private val versionString:String get() {
            val modDateUnix = universalLib.modDate
            val date = ofEpochSecond(modDateUnix)
            return formatter.format(date) + (modDateUnix % SECONDS_A_DAY)
        }

        /**
         * Seconds a day for use in version string
         */
        private val SECONDS_A_DAY = 24 * 60 * 60
    }

}