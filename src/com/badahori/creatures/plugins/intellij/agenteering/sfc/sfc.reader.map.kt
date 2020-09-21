package com.badahori.creatures.plugins.intellij.agenteering.sfc

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant.C1

internal fun SfcReader.readMapData(): SfcMapData {
    variant = when (val version = uInt32) {
        0 -> C1
        1 -> CaosVariant.C2
        else -> throw OutOfVariantException(CaosVariant.UNKNOWN, "MapData variant value $version is invalid. Value must be 0 or 1")
    }

    skip(if (variant == C1) 4 else 12)
    val background = slurp(TYPE_CGALLERY) as SfcGallery
    val numberOfRooms = uInt32
    val rooms = readRooms(numberOfRooms)
    val groundLevels = readGroundLevels()
    return SfcMapData(
            gallery = background,
            rooms = rooms,
            groundLevels = groundLevels
    )
}
private fun SfcReader.readRooms(numberOfRoomsIn: Int): List<SfcRoom> {
    var numberOfRooms = numberOfRoomsIn

    // Read C1 rooms in
    if (variant == C1)
        return (0 until numberOfRooms).map { id ->
            readC1Room(id)
        }

    // Read C2 rooms in through slurping
    var i = 0
    val rooms = mutableListOf<SfcRoom.SfcC2Room>()
    while (i < numberOfRooms) {
        val room = slurp(TYPE_CROOM) as? SfcRoom.SfcC2Room
        if (room != null)
            rooms.add(room)
        else
            numberOfRooms++
        i++
    }
    return rooms
}