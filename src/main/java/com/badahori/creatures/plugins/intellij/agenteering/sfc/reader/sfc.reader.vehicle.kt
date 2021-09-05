package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import bedalton.creatures.bytes.bytes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C2
import com.badahori.creatures.plugins.intellij.agenteering.sfc.Vector2


internal fun SfcReader.readVehicle() : PointerSfcVehicle<*> {
    val base = readCompoundObject()
    val directionVector = Vector2(uInt32, uInt32)
    val bump = uInt8
    //Discard
    skip(2)
    if (variant == C2) {
        assert(uInt16 == 0)
        skip(2)
    } else
        skip(4)
    assert(uInt8 == 0)
    val bounds = bounds
    assert(uInt32 == 0)
    return PointerSfcVehicleImpl(
            baseObject = base,
            cabinBounds = bounds,
            movementVector = directionVector,
            bump = bump
    )
}

private val LIFT_CHECK_BYTE_SEQUENCE = byteArrayOf(0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0x00)

internal fun SfcReader.readLift() : PointerSfcLift {
    val base = readVehicle()
    val numberOfButtons = uInt32
    val currentButton = uInt32
    assert(byteBuffer.bytes(5).contentEquals(LIFT_CHECK_BYTE_SEQUENCE)) { "Invalid lift check sequence sequence" }
    val callButtonY = (0 until 8).map {
        uInt32.apply { assert(uInt16 == 0) } // Ignores internal check for bytes, returning original read
    }
    val alignWithCabin = if (variant == C2)
        uInt32 != 0
    else
        false
    return PointerSfcLift(
            baseObject = base,
            numberOfButtons = numberOfButtons,
            currentButton = currentButton,
            callButtonYs =  callButtonY,
            alignWithCabin = alignWithCabin
    )
}