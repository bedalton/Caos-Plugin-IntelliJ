@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex

object CaosScriptProjectSettings {

    // Transient property for disabling CAOS validation before inject
    // Transient to allow it to be reset each application start
    // Needs to reset as there is no way a user can control it
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
    @JvmStatic
    var showLabels:Boolean
        get() = SHOW_LABELS_SETTING.value ?: DEFAULT_SHOW_LABELS
        set(show) {
            SHOW_LABELS_SETTING.value = show
        }

    private const val DITHER_SPR_KEY = "sprites.DITHER_SPR"
    private const val DEFAULT_DITHER_SPR = true
    private val DITHER_SPR_SETTING = CaosPluginSettingsUtil.BooleanSetting(DITHER_SPR_KEY, DEFAULT_DITHER_SPR)
    var ditherSpr:Boolean
        get() = DITHER_SPR_SETTING.value ?: DEFAULT_DITHER_SPR
        set(show) {
            DITHER_SPR_SETTING.value = show
        }
    private const val ATT_SCALE_KEY = "att.SCALE"
    private const val DEFAULT_ATT_SCALE = 6
    private val ATT_SCALE_SETTING = CaosPluginSettingsUtil.IntegerSetting(ATT_SCALE_KEY, DEFAULT_ATT_SCALE)
    @JvmStatic
    var attScale:Int
        get() = ATT_SCALE_SETTING.value ?: DEFAULT_ATT_SCALE
        set(scale) {
            ATT_SCALE_SETTING.value = scale
        }

    private const val SHOW_ATT_POSE_VIEW_KEY = "att.POSE_VIEW"
    private const val DEFAULT_SHOW_ATT_POSE_VIEW = true
    private val SHOW_ATT_POSE_VIEW_SETTING = CaosPluginSettingsUtil.BooleanSetting(SHOW_ATT_POSE_VIEW_KEY, DEFAULT_SHOW_ATT_POSE_VIEW)
    @JvmStatic
    var showPoseView:Boolean
        get() = SHOW_ATT_POSE_VIEW_SETTING.value ?: DEFAULT_SHOW_ATT_POSE_VIEW
        set(show) {
            SHOW_ATT_POSE_VIEW_SETTING.value = show
        }

    private const val GAME_INTERFACE_NAMES_KEY = "caos.INJECTOR_INTERFACE_NAMES"
    private val GAME_INTERFACE_NAMES = CaosPluginSettingsUtil.StringSetting(GAME_INTERFACE_NAMES_KEY, "")
    @JvmStatic
    var gameInterfaceNames: List<GameInterfaceName>
        get() = GAME_INTERFACE_NAMES.value
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull {
                GameInterfaceName.fromString(it.trim())
            }
            ?: emptyList()
        set(names) {
            GAME_INTERFACE_NAMES.value = names.joinToString("\n")
        }


    private const val IGNORED_FILE_NAMES_KEY = "caos.IGNORED_FILE_NAMES"
    private val IGNORED_FILE_NAMES = CaosPluginSettingsUtil.StringSetting(IGNORED_FILE_NAMES_KEY, "")
    @JvmStatic
    var ignoredFileNames: List<GameInterfaceName>
        get() = IGNORED_FILE_NAMES.value
            ?.split("\n")
            ?.mapNotNull {
                GameInterfaceName.fromString(it)
            }
            ?: emptyList()
        set(names) {
            IGNORED_FILE_NAMES.value = names.joinToString("\n")
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
    }
    private const val DEFAULT_POSE_VIEW_KEY = "att.DEFAULT_POSE"
    private const val DEFAULT_DEFAULT_POSE_VIEW = "313122122111111"
    private val DEFAULT_POSE_VIEW_SETTING = CaosPluginSettingsUtil.StringSetting(DEFAULT_POSE_VIEW_KEY, DEFAULT_DEFAULT_POSE_VIEW)
    @JvmStatic
    var defaultPoseString:String
        get() = DEFAULT_POSE_VIEW_SETTING.value ?: DEFAULT_DEFAULT_POSE_VIEW
        set(show) {
            DEFAULT_POSE_VIEW_SETTING.value = show
        }
}

