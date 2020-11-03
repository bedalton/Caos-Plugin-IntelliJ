package com.badahori.creatures.plugins.intellij.agenteering.caos.parser

import com.badahori.creatures.plugins.intellij.agenteering.caos.parser.CaosTokenConsumption.TokenConsumer
import com.badahori.creatures.plugins.intellij.agenteering.caos.parser.CaosTokenConsumption.TokenConsumption
import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosFileUtil

object CaosScriptConsumerLoader {

    fun getConsumer(variant:String) : TokenConsumption? {
        val directory = CaosFileUtil.PLUGIN_HOME_DIRECTORY
        val child = directory?.findChild(variant.toLowerCase()+"-consumer.caoscdef")
                ?: return null
        val bytes = child.contentsToByteArray()
        return TokenConsumption.parseFrom(bytes)

    }

}

fun TokenConsumption.getLValue(command:String) : TokenConsumer? {
    return lvaluesList.firstOrNull { it.command == command }
}

fun TokenConsumption.getRValue(command:String) : TokenConsumer? {
    return rvaluesList.firstOrNull { it.command == command }
}

fun TokenConsumption.getCommand(command:String) : TokenConsumer? {
    return commandsList.firstOrNull { it.command == command }
}