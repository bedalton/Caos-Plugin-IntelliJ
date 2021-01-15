package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

interface Caos2Cob {
    val targetFile:String
    val agentName:String
    fun compile():ByteArray
}