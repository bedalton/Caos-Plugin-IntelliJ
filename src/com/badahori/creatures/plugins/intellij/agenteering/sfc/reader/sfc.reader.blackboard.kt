package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcBlackboard
import com.badahori.creatures.plugins.intellij.agenteering.sfc.Vector2
import com.badahori.creatures.plugins.intellij.agenteering.utils.cString

internal fun SfcReader.readBlackBoard() : SfcBlackboard {
    val baseObject = readCompoundObject()
    val readColor:()->Int  = {
        if (variant == C1)
            uInt8
        else
            uInt32
    }
    val backgroundColor:Int = readColor()
    val chalkColor:Int = readColor()
    val aliasColor:Int = readColor()
    val textPosition = Vector2(uInt8, uInt8)
    val words = (0 until (if (variant == C1 ) 12 else 48)).map {
        val value = uInt32
        val string = byteBuffer.cString(11)
        value to string
    }.toMap()
    return SfcBlackboard(
            baseObject = baseObject,
            textPosition = textPosition,
            backgroundColor = backgroundColor,
            chalkColor = chalkColor,
            aliasColor = aliasColor,
            strings = words
    )
}