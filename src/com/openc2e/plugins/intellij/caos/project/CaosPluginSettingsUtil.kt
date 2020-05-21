@file:Suppress("unused")

package com.openc2e.plugins.intellij.caos.project

import com.intellij.ide.util.PropertiesComponent

object CaosPluginSettingsUtil {
    private const val PREFIX = "com.openc2e.plugins.intellij.caos.settings."

    /**
     * Set value or unset if equals to default value
     */
    private fun setValue(key: String, `val`: String, defaultValue: String) {
        PropertiesComponent.getInstance().setValue(PREFIX + key, `val`, defaultValue)
    }

    /**
     * Set value or unset if equals to default value
     */
    private fun setValue(key: String, `val`: Boolean, defaultValue: Boolean) {
        PropertiesComponent.getInstance().setValue(PREFIX + key, `val`, defaultValue)
    }

    /**
     * Set value or unset if equals to default value
     */
    private fun setValue(key: String, val1: Float, defaultValue: Float) {
        PropertiesComponent.getInstance().setValue(PREFIX + key, val1, defaultValue)
    }

    /**
     * Set value or unset if equals to default value
     */
    private fun setValue(key: String, val1: Int, defaultValue: Int) {
        PropertiesComponent.getInstance().setValue(PREFIX + key, val1, defaultValue)
    }

    /**
     * Set value or unset if equals to default value
     */
    private fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return PropertiesComponent.getInstance().getBoolean(PREFIX + key, defaultValue)
    }


    /**
     * Get value if set, or get default value
     */
    private fun getInt(key: String, defaultValue: Int): Int {
        return PropertiesComponent.getInstance().getInt(PREFIX + key, defaultValue)
    }

    /**
     * Get value if set, or get default value
     */
    fun getFloat(key: String, defaultValue: Float): Float {
        return PropertiesComponent.getInstance().getFloat(PREFIX + key, defaultValue)
    }

    /**
     * Get value if set, or get default value
     */
    private fun getValue(key: String, defaultValue: String): String {
        return PropertiesComponent.getInstance().getValue(PREFIX + key, defaultValue)
    }

    class StringSetting internal constructor(private val key: String, private val defaultValue: String) : Setting<String> {
        override var value: String?
            get() = CaosPluginSettingsUtil.getValue(key, defaultValue)
            set(valueIn) {
                val value = valueIn ?: defaultValue
                CaosPluginSettingsUtil.setValue(key, value, defaultValue)
            }
    }

    class IntegerSetting internal constructor(private val key: String, private val defaultValue: Int) : Setting<Int> {

        override var value: Int?
            get() = CaosPluginSettingsUtil.getInt(key, defaultValue)
            set(valueIn) {
                val value = valueIn ?: defaultValue
                CaosPluginSettingsUtil.setValue(key, value, defaultValue)
            }
    }

    class BooleanSetting internal constructor(private val key: String, private val defaultValue: Boolean) : Setting<Boolean> {

        override var value: Boolean?
            get() = CaosPluginSettingsUtil.getBoolean(key, defaultValue)
            set(valueIn) {
                val value = valueIn ?: defaultValue
                CaosPluginSettingsUtil.setValue(key, value, defaultValue)
            }
    }

    class FloatSetting internal constructor(private val key: String, private val defaultValue: Float) : Setting<Float> {

        override var value: Float?
            get() = CaosPluginSettingsUtil.getFloat(key, defaultValue)
            set(valueIn) {
                val value = valueIn ?: defaultValue
                CaosPluginSettingsUtil.setValue(key, value, defaultValue)
            }
    }

    class AnnotationLevelSetting internal constructor(private val key: String, private val defaultValue: AnnotationLevel) : Setting<AnnotationLevel> {
        override var value: AnnotationLevel?
            get() {
                val rawValue = CaosPluginSettingsUtil.getInt(key, defaultValue.value)
                return AnnotationLevel.getAnnotationLevel(rawValue)
            }
            set(valueIn) {
                val value = valueIn ?: defaultValue
                CaosPluginSettingsUtil.setValue(key, value.value, defaultValue.value)
            }
    }


    enum class AnnotationLevel constructor(internal val value: Int) {
        ERROR(100),
        WARNING(75),
        WEAK_WARNING(25),
        IGNORE(0);

        companion object {
            internal fun getAnnotationLevel(value: Int): AnnotationLevel {
                return when {
                    value >= ERROR.value -> ERROR
                    value >= WARNING.value -> WARNING
                    value >= WEAK_WARNING.value -> WEAK_WARNING
                    else -> IGNORE
                }
            }
        }
    }


    private interface Setting<T> {
        var value: T?
    }


}
