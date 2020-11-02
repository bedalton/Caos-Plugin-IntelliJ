package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer



sealed class CaosNumber {
    data class CaosIntNumber(val value:Int) : CaosNumber()
    data class CaosFloatNumber(val value:Float) : CaosNumber()
    object Undefined: CaosNumber()
}
