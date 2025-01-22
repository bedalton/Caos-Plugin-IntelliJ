package com.badahori.creatures.plugins.intellij.agenteering.common

interface IMessageBundle {
    fun getMessage(key: String, vararg params: Any): String?
}