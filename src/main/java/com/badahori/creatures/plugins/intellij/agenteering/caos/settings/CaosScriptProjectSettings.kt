package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosInjectorNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex

object CaosScriptProjectSettings {
    // Allows persisting ignore setting.
    // Disabled for now, as there would be no way to enable the check again in the future
    /*private const val INJECTION_CHECK_DISABLED_DEFAULT = false
    private const val INJECTION_CHECK_DISABLED_KEY = "INJECTION_CHECK_DISABLED"
    private val INJECTION_CHECK_DISABLED_SETTING = CaosPluginSettingsUtil.BooleanSetting(INJECTION_CHECK_DISABLED_KEY, false)
    var injectionCheckDisabled: Boolean
        get() = INJECTION_CHECK_DISABLED_SETTING.value ?: INJECTION_CHECK_DISABLED_DEFAULT
        set(value) { INJECTION_CHECK_DISABLED_SETTING.value = value }*/

    var injectionCheckDisabled: Boolean = false

    // ==== VARIANT ===== //
    private const val VARIANT_KEY = "BASE_VARIANT"
    private val DEFAULT_VARIANT = CaosVariant.UNKNOWN
    private val VARIANT_SETTING = CaosPluginSettingsUtil.StringSetting(VARIANT_KEY, DEFAULT_VARIANT.code)
    val variant: CaosVariant? get() = VARIANT_SETTING.value?.let { CaosVariant.fromVal(it) } ?: DEFAULT_VARIANT.let {
        if (it == CaosVariant.UNKNOWN)
            null
        else
            it
    }

    fun setVariant(variant: CaosVariant) {
        VARIANT_SETTING.value = variant.code
    }
    fun isVariant(variant: CaosVariant): Boolean = variant == this.variant

    // ==== INDENT ===== //
    private const val INDENT_KEY = "INDENT"
    private const val DEFAULT_INDENT = true
    private val INDENT_SETTING = CaosPluginSettingsUtil.BooleanSetting(INDENT_KEY, DEFAULT_INDENT)
    val indent:Boolean get() = INDENT_SETTING.value ?: DEFAULT_INDENT

    fun setIndent(indent:Boolean) {
        INDENT_SETTING.value = indent
    }


    private const val LABELS_KEY = "att.LABELS"
    private const val DEFAULT_SHOW_LABELS = true
    private val SHOW_LABELS_SETTING = CaosPluginSettingsUtil.BooleanSetting(LABELS_KEY, DEFAULT_SHOW_LABELS)
    var showLabels:Boolean
        get() = SHOW_LABELS_SETTING.value ?: DEFAULT_SHOW_LABELS
        set(show) {
            SHOW_LABELS_SETTING.value = show
        }

    // === CAOS Injection URLS === //
    fun getInjectURL(project:Project) : String? {//Map<String,String>?  {
        //val invalidNameCharsRegex = "[<>]".toRegex() // Used when doing multiple injection urls
        val contents = FilenameIndex.getAllFilesByExt(project, "caosurl")
                .firstOrNull()
                ?.contents
                .nullIfEmpty()
                ?: return null
        return contents
                .split("\n")
                .mapNotNull { line ->
                    line.trim().nullIfEmpty()?.let { nonNullLine ->
                        if (nonNullLine.startsWith("#"))
                            null
                        else
                            nonNullLine
                    }
                }
                .firstOrNull()
    /*
                .mapIndexedNotNull { i, text ->
                    text.split("=")
                            .map { it.trim() }
                            .let mapper@{ parts ->
                                if (parts.isEmpty()) {
                                    return@mapper null
                                }
                                when {
                                    parts.size > 2 -> {
                                        CaosInjectorNotifications.createWarningNotification(
                                                project = project,
                                                title = "CAOS URL format error",
                                                content = "The CAOS URL line with contents: $text is invalid. Format should be the 'name=url'\nWhere url begins with 'http' and name is an arbitrary reference key"
                                        )
                                        parts
                                                .firstOrNull { part -> part.startsWith("http") }
                                                ?.let { url ->
                                                    parts.first() to url
                                                }
                                    }
                                    parts.size == 2 -> {
                                        if (parts[0].contains(invalidNameCharsRegex)) {
                                            CaosInjectorNotifications.createWarningNotification(
                                                    project = project,
                                                    title = "CAOS URL format error",
                                                    content = "CAOS URL name key '${parts[0]}' is invalid. Name keys cannot contain '<' or '>' characters.\nThey have been automatically stripped out"
                                            )
                                            parts[0].replace(invalidNameCharsRegex, "") to parts[1]
                                        }
                                        parts[0] to parts[1]
                                    }
                                    else -> {
                                        "Injector $i - '${parts[0]}'" to parts[0]
                                    }
                                }
                            }

                }
                .toMap()*/

    }
}
