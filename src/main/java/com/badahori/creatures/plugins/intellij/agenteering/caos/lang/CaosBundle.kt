package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.badahori.creatures.plugins.intellij.agenteering.common.IMessageBundle
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.bedalton.log.Log
import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

object CaosBundle : IMessageBundle {

    private const val BUNDLE = "com.badahori.creatures.plugins.intellij.caos-bundle"
    private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE) }

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return AbstractBundle.messageOrDefault(bundle, key, "", *params).nullIfEmpty()
            ?: throw NullPointerException("Bundle property $key is empty or does not exist")
    }


    override fun getMessage(key: String, vararg params: Any): String? {
        return AbstractBundle.messageOrDefault(bundle, key, "", *params).nullIfEmpty()
            ?: return null.also {
                Log.e("Failed to get message for key: $key in bundle ${BUNDLE}")
            }
    }
}


object AgentMessages : IMessageBundle {

    private const val BUNDLE = "com.badahori.creatures.plugins.intellij.agent-bundle"
    private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE) }

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return AbstractBundle.messageOrDefault(bundle, key, "", *params).nullIfEmpty()
            ?: throw NullPointerException("Bundle property $key is empty or does not exist")
    }


    override fun getMessage(key: String, vararg params: Any): String? {
        return AbstractBundle.messageOrDefault(bundle, key, "", *params).nullIfEmpty()
            ?: return null.also {
                Log.e("Failed to get message for key: $key in bundle ${BUNDLE}")
            }
    }
}



object ActionsBundle : IMessageBundle {

    private const val BUNDLE = "com.badahori.creatures.plugins.intellij.actions-bundle"
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