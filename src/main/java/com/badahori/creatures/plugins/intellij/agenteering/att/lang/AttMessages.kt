package com.badahori.creatures.plugins.intellij.agenteering.att.lang

import com.badahori.creatures.plugins.intellij.agenteering.common.IMessageBundle
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.bedalton.log.Log
import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

object AttMessages: IMessageBundle {

    private const val BUNDLE = "com.badahori.creatures.plugins.intellij.att-bundle"
    private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE) }

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return AbstractBundle.messageOrDefault(bundle, key, "", *params).nullIfEmpty()
            ?: throw NullPointerException("Bundle property $key is empty or does not exist")
    }

    override fun getMessage(key: String, vararg params: Any): String? {
        return AbstractBundle.messageOrDefault(bundle, key, "", *params).nullIfEmpty()
            ?: return null.also {
                Log.e("Failed to get message for key: $key in bundle $BUNDLE")
            }
    }
}