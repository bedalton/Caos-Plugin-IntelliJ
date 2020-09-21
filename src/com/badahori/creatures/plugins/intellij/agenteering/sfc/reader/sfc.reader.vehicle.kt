package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sfc.*


internal fun SfcReader.readVehicle() : SfcVehicle {
    val base = readCompoundObject()
    val directionVector = Vector2(uInt32, uInt32)
    val bump = uInt8
    //Discard
    skip(2)
    if (variant == CaosVariant.C1)
        assert(uInt16 == 0)
    else
        skip(4)
    assert(uInt8 == 0)
    val bounds = Bounds(
            left = uInt32,
            top = uInt32,
            right = uInt32,
            bottom = uInt32
    )
    assert(uInt32 == 0)
    return SfcVehicleImpl(
            baseObject = base,
            cabinBounds = bounds,
            movementVector = directionVector,
            bump = bump
    )
}

internal fun SfcReader.readLift() : SfcLift {
    val base = readVehicle()
    val numberOfButtons = uInt32
    val currentButton = uInt32
    assert((0 until 5).any {
        it != 0xFF
    }) {
        "SFC file invalid at read lift"
    }
    val callButtonY = (0 until 8).map {
        uInt32
    }
    assert (uInt16 == 0) {
        "Invalid SFC file at read lift"
    }
    val alignWithCabin = variant == CaosVariant.C2 && uInt32 != 0
    return SfcLift(
            baseObject = base,
            numberOfButtons = numberOfButtons,
            currentButton = currentButton,
            callButtonYs =  callButtonY,
            alignWithCabin = alignWithCabin
    )
}