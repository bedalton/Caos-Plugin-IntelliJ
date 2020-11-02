package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.intellij.CommonBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

object CaosBundle {

    private const val BUNDLE = "com.badahori.creatures.plugins.intellij.caos-bundle"
    private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE) }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
            CommonBundle.message(bundle, key, *params)
}