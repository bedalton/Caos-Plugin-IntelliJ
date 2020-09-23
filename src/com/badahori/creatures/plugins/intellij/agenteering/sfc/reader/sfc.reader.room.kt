package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.PointerSfcC2Room
import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.PointerSfcRoomImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sfc.*
import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.Ptr.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER


internal fun SfcReader.readC1Room(id: Int): PointerSfcRoomImpl {
    val bounds = bounds
    val roomType = uInt32
    assert(roomType < 3) { "Invalid C1 room type '$roomType' should be between 0 and 3" }
    return PointerSfcRoomImpl(
            id = id,
            bounds = bounds,
            roomType = roomType
    )
}

internal fun SfcReader.readGroundLevels(): List<Int>? {
    if (variant == CaosVariant.C2)
        return null

    return (0 until 261).map {
        uInt32
    }
}

internal fun SfcReader.readDoor(): SfcDoor {
    val openness = uInt8
    val otherRoom = uInt16
    assert(uInt16 == 0)
    return SfcDoor(
            openness = openness,
            otherRoom = otherRoom
    )
}

internal fun SfcReader.readC2Room() : PointerSfcC2Room {
    val id = uInt32
    assert(uInt16 == 2)
    val bounds = bounds
    val doors = (0 until 4).map {i ->
        i to (0 until uInt16).mapNotNull {
           readClass(SfcType.DOOR) as? SfcDoorPtr
        }
    }.toMap()
    val roomType = uInt32
    assert(roomType < 4) { "Room type is invalid. Expected value between 0 and 4 found: '$roomType'"}
    val floorValue = uInt8

    // Nutrients
    val inorganicNutrients = uInt8
    val organicNutrients = uInt8

    // Temperature
    val temperature = uInt8
    val heatSource = uInt32

    // Pressure
    val pressure = uInt8
    val pressureSource = uInt32

    // Wind
    val wind = vector2

    // Light
    val lightLevel = uInt8
    val lightSource = uInt32

    // Radiation
    val radiation = uInt8
    val radiationSource = uInt32
    // Discard unknown bytes
    skip(800)
    val floorPoints = (0 until uInt16).map {
        vector2
    }
    uInt32.apply {
        assert(this == 0) { "Expected uInt32 in C2Room reader to be 0, but was actually $this"}
    }
    val music = sfcString
    val dropStatus = uInt32
    assert(dropStatus < 3) { "Drop status should be less than 3. Found: '$dropStatus'"}
    return PointerSfcC2Room(
            id = id,
            bounds = bounds,
            roomType = roomType,
            floorPoints = floorPoints,
            floorValue = floorValue,
            inorganicNutrients = inorganicNutrients,
            organicNutrients = organicNutrients,
            temperature = temperature,
            heatSource = heatSource,
            pressure = pressure,
            pressureSource = pressureSource,
            lightLevel = lightLevel,
            lightSource = lightSource,
            radiation = radiation,
            radiationSource = radiationSource,
            windVector = wind,
            music = music,
            dropStatus = dropStatus,
            doors = doors
    )
}