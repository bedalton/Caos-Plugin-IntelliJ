package com.openc2e.plugins.intellij.caos.deducer



sealed class CaosNumber {
    data class CaosIntNumber(val value:Int) : CaosNumber()
    data class CaosFloatNumber(val value:Float) : CaosNumber()
    object Undefined: CaosNumber()
}
