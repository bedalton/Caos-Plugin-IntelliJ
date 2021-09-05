package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import bedalton.creatures.bytes.skip
import bedalton.creatures.bytes.uInt8
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.Ptr.*

/**
 * Reads a simple object from a byte buffer of an SFC File
 */
internal fun SfcReader.readSimpleObject() : PointerSfcSimpleObjectImpl {
    val baseObject = readObject()
    val entity = readClass(SfcType.ENTITY) as SfcEntityPtr
    return PointerSfcSimpleObjectImpl(baseObject, entity)
}

internal fun SfcReader.readPointerTool(): PointerSfcPointerTool {
    val simpleObject = readSimpleObject()
    // Discard Uknown bytes
    if (variant == CaosVariant.C1)
        byteBuffer.skip(35)
    else
        byteBuffer.skip(51)
    return PointerSfcPointerTool(simpleObject)
}


internal fun SfcReader.readCallButton(): PointerSfcCallButton {
    val simpleObject = readSimpleObject()
    val ourLift = readClass(SfcType.LIFT) as SfcLiftPtr
    val liftId = byteBuffer.uInt8
    return PointerSfcCallButton(
            simpleObject,
            ourLift,
            liftId
    )
}

internal fun SfcReader.readScenery() : PointerSfcScenery {
    return PointerSfcScenery(readSimpleObject())
}