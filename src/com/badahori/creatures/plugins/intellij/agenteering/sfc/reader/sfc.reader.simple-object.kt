package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sfc.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.skip
import com.badahori.creatures.plugins.intellij.agenteering.utils.uInt8

/**
 * Reads a simple object from a byte buffer of an SFC File
 */
internal fun SfcReader.readSimpleObject() : SfcSimpleObjectImpl {
    val baseObject = readObject()
    val entity = readClass(TYPE_ENTITY) as SfcEntity
    return SfcSimpleObjectImpl(baseObject, entity)
}

internal fun SfcReader.readPointerTool(): SfcPointer {
    val simpleObject = readSimpleObject()
    // Discard Uknown bytes
    if (variant == CaosVariant.C1)
        byteBuffer.skip(35)
    else
        byteBuffer.skip(51)
    return SfcPointer(simpleObject)
}


internal fun SfcReader.readCallButton(): SfcCallButton {
    val simpleObject = readSimpleObject()
    val ourLift = readClass(TYPE_LIFT) as SfcLift
    val liftId = byteBuffer.uInt8
    return SfcCallButton(
            simpleObject,
            ourLift,
            liftId
    )
}

internal fun SfcReader.readScenery() : SfcScenery {
    return SfcScenery(readSimpleObject())
}