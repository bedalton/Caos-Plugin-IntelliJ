package com.openc2e.plugins.intellij.caos.settings

import com.openc2e.plugins.intellij.caos.lang.CaosVariant

object CaosScriptProjectSettings {

    // ==== VARIANT ===== //
    private const val VARIANT_KEY = "BASE_VARIANT"
    private val DEFAULT_VARIANT = CaosVariant.DS
    private val VARIANT_SETTING = CaosPluginSettingsUtil.StringSetting(VARIANT_KEY, DEFAULT_VARIANT.code)
    val variant:CaosVariant get() = VARIANT_SETTING.value?.let { CaosVariant.fromVal(it) } ?: DEFAULT_VARIANT

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
}
