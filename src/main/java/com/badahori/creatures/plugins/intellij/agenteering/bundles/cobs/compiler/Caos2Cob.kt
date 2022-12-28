package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

interface Caos2Cob {
    val targetFile:String
    val agentName:String
    suspend fun compile():ByteArray
}