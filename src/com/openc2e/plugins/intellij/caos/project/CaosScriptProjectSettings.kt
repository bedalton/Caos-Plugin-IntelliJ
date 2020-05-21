package com.openc2e.plugins.intellij.caos.project

object CaosScriptProjectSettings {

    // ==== VARIANT ===== //
    private const val VARIANT_KEY = "BASE_VARIANT"
    private const val DEFAULT_VARIANT = "DS"
    private val VARIANT_SETTING = CaosPluginSettingsUtil.StringSetting(VARIANT_KEY, DEFAULT_VARIANT)
    val variant:String get() = VARIANT_SETTING.value ?: DEFAULT_VARIANT

    fun setVariant(variant:String) {
        VARIANT_SETTING.value = variant
    }
    fun isVariant(variant:String): Boolean = variant == variant

    // ==== INDENT ===== //
    private const val INDENT_KEY = "INDENT"
    private const val DEFAULT_INDENT = true
    private val INDENT_SETTING = CaosPluginSettingsUtil.BooleanSetting(INDENT_KEY, DEFAULT_INDENT)
    val indent:Boolean get() = INDENT_SETTING.value ?: DEFAULT_INDENT

    fun setIndent(indent:Boolean) {
        INDENT_SETTING.value = indent
    }


}
