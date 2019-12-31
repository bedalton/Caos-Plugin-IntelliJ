package com.openc2e.plugins.intellij.caos.lang

import com.intellij.CommonBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

object CaosBundle {

    private const val BUNDLE = "com.openc2e.plugins.intellij.caos-bundle"
    private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE) }

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
            CommonBundle.message(bundle, key, *params)
}