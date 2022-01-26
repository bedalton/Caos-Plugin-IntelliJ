package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.AbstractBundle
import com.intellij.CommonBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

object CaosBundle {

    private const val BUNDLE = "com.badahori.creatures.plugins.intellij.caos-bundle"
    private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE) }

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
            AbstractBundle.messageOrDefault(bundle, key, "", *params).nullIfEmpty()!!
}


object AgentMessages {

    private const val BUNDLE = "com.badahori.creatures.plugins.intellij.agent-bundle"
    private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE) }

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        AbstractBundle.messageOrDefault(bundle, key, "", *params).nullIfEmpty()!!
}