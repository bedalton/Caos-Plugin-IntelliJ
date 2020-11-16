package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.Ptr.SfcGalleryPtr
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.Ptr.SfcRoomPtr
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C1

internal fun SfcReader.readMapData(): PointerSfcMapData {
    variant = when (val version = uInt32) {
        0 -> C1
        1 -> CaosVariant.C2
        else -> throw OutOfVariantException(CaosVariant.UNKNOWN, "MapData variant value $version is invalid. Value must be 0 or 1")
    }

    skip(if (variant == C1) 4 else 12)
    val background = readClass(SfcType.GALLERY) as SfcGalleryPtr
    val numberOfRooms = uInt32
    val rooms = readRooms(numberOfRooms)
    val groundLevels = readGroundLevels()
    if (variant == C1)
        skip(800)
    return PointerSfcMapData(
            gallery = background,
            rooms = rooms,
            groundLevels = groundLevels
    )
}
private fun SfcReader.readRooms(numberOfRoomsIn: Int): List<SfcRoomPtr> {
    var numberOfRooms = numberOfRoomsIn

    // Read C1 rooms in
    if (variant == C1) {
        return (0 until numberOfRooms).map { id ->
            SfcRoomPtr(type = SfcType.ROOM.value, pointed = readC1Room(id))
        }
    }

    // Read C2 rooms through class reader
    var i = 0
    val rooms = mutableListOf<SfcRoomPtr>()
    while (i < numberOfRooms) {
        val room = (readClass(SfcType.ROOM) as? SfcRoomPtr)
        if (room != null)
            rooms.add(room)
        else
            numberOfRooms++
        i++
    }
    return rooms
}