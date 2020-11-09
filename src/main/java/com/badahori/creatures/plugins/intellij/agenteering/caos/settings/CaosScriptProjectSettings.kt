package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex

object CaosScriptProjectSettings {

    // ==== VARIANT ===== //
    private const val VARIANT_KEY = "BASE_VARIANT"
    private val DEFAULT_VARIANT = CaosVariant.UNKNOWN
    private val VARIANT_SETTING = CaosPluginSettingsUtil.StringSetting(VARIANT_KEY, DEFAULT_VARIANT.code)
    val variant:CaosVariant? get() = VARIANT_SETTING.value?.let { CaosVariant.fromVal(it) } ?: DEFAULT_VARIANT.let {
        if (it == CaosVariant.UNKNOWN)
            null
        else
            it
    }

    fun setVariant(variant:CaosVariant) {
        VARIANT_SETTING.value = variant.code
    }
    fun isVariant(variant:CaosVariant): Boolean = variant == this.variant

    // ==== INDENT ===== //
    private const val INDENT_KEY = "INDENT"
    private const val DEFAULT_INDENT = true
    private val INDENT_SETTING = CaosPluginSettingsUtil.BooleanSetting(INDENT_KEY, DEFAULT_INDENT)
    val indent:Boolean get() = INDENT_SETTING.value ?: DEFAULT_INDENT

    fun setIndent(indent:Boolean) {
        INDENT_SETTING.value = indent
    }

    // === UndocumentedCommand === //
    private const val IGNORE_UNDOCUMENTED_COMMAND_KEY = "IGNORE_UNDOCUMENTED_COMMAND"
    private val IGNORE_UNDOCUMENTED_COMMAND_SETTING = CaosPluginSettingsUtil.StringSetting(IGNORE_UNDOCUMENTED_COMMAND_KEY, "")
    private val ignoredUndocumentedCommands by lazy {
        IGNORE_UNDOCUMENTED_COMMAND_SETTING.value?.split(";")?.toMutableList() ?: mutableListOf()
    }
    fun isIgnoredUndocumentedCommand(command:String) : Boolean {
        return command.toUpperCase() in ignoredUndocumentedCommands
    }

    fun getInjectURL(project:Project) = FilenameIndex.getAllFilesByExt(project, "caosurl").firstOrNull()?.contents

    fun ignoreUndocumentedCommand(commandIn:String, ignore:Boolean = true) {
        val command = commandIn.toUpperCase()
        if (ignore) {
            if (command in ignoredUndocumentedCommands)
                return
            ignoredUndocumentedCommands.add(command)
        } else {
            if (command !in ignoredUndocumentedCommands)
                return
            ignoredUndocumentedCommands.remove(command)
        }

        IGNORE_UNDOCUMENTED_COMMAND_SETTING.value = ignoredUndocumentedCommands.joinToString(";")
    }
}
