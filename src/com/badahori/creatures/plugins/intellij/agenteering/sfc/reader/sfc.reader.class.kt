package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sfc.Ptr
import com.badahori.creatures.plugins.intellij.agenteering.utils.className

private const val SFC_BINARY_CONST = 0x8000

internal fun SfcReader.readClass(requiredType: SfcType): Ptr<*>? {
    val pid = uInt16
    LOGGER.info("Reading PID: '$pid'")
    return when {
        pid == 0 -> null
        pid == 0xFFFF -> readNewMfc()
        (pid and SFC_BINARY_CONST) != SFC_BINARY_CONST -> returnExistingObject(requiredType, pid)
        else -> createNewFromExisting(requiredType, pid)
    }
}

private fun SfcReader.readNewMfc() : Ptr<*>? {
    skip(2) // Schema ID -> Seems to be ignored in C2e
    val className = string(uInt16).trim()
    val pid = storage.size
    val pidType = when(className) {
        "MapData" -> SfcType.MAP_DATA
        "CGallery" -> SfcType.GALLERY
        "CDoor" -> SfcType.DOOR
        "CRoom" -> SfcType.ROOM
        "Entity" -> SfcType.ENTITY
        "CompoundObject" -> SfcType.COMPOUNDOBJECT
        "Blackboard" -> SfcType.BLACKBOARD
        "Vehicle" -> SfcType.VEHICLE
        "Lift" -> SfcType.LIFT
        "SimpleObject" -> SfcType.SIMPLEOBJECT
        "PointerTool" -> SfcType.POINTERTOOL
        "CallButton" -> SfcType.CALLBUTTON
        "Scenery" -> SfcType.SCENERY
        "Macro" -> SfcType.MACRO
        else -> throw SfcReadException("Failed to understand MFC class name: '$className'")
    }
    // Push null object in place as reference to this type
    // Checked for later when creating instances of type
    storage.add(null)
    LOGGER.info("Reading: (${pidType.type})->$className. PID: $pid")
    types[pid] = pidType
    return read(pidType, pid)
}

private fun SfcReader.returnExistingObject(requiredType:SfcType, pidIn:Int) : Ptr<*> {
    val pid = pidIn - 1
    assert(pid < storage.size) { "PID '$pid' out of bounds for read class. Valid range: (0 to ${storage.size})" }
    val pidType = types[pid]
            ?: throw SfcReadException("Type for PID($pid) is null")
    assert(validSfcType(pidType, requiredType)) {
        "Failed to assert that pid type: ${pidType.type} is compatible with required type: ${requiredType.type}"
    }
    assert(storage[pid] != null) {
        "Object in storage at PID '$pid' is null. Expected ${pidType.type}. Previous: ${storage[pid - 1]?.pointed.className}. Next: ${storage[pid + 1]?.pointed.className}}"
    }
    return storage[pid]!!
}

private fun SfcReader.createNewFromExisting(requiredType: SfcType, pidIn:Int) : Ptr<*> {
    val pid = (pidIn xor SFC_BINARY_CONST) - 1
    assert(pid < storage.size) { "Pid should be less than size"}
    assert(storage[pid] == null) { "Storage for pid '$pid' should be null" }
    val pidType = types[pid]
            ?: throw SfcReadException("PidType is null for pid($pid)->original($pidIn)")
    assert(validSfcType(pidType, requiredType)) { "Failed to assert that required type ${requiredType.type} is compatible with actual type: ${pidType.type}"}
    return read(pidType, pid)
}

private fun SfcReader.read(pidType:SfcType, pid:Int) : Ptr<*> {
    types[storage.size] = pidType
    if (validSfcType(pidType, SfcType.COMPOUNDOBJECT))
        readingCompoundObject = true
    else if (pidType == SfcType.SCENERY)
        readingScenery = true

    val pointer = pidType.pointer(pid)
    storage.add(pointer)

    when(pidType) {
        SfcType.MAP_DATA -> (pointer as Ptr.SfcMapDataPtr).pointed = readMapData()
        SfcType.GALLERY -> (pointer as Ptr.SfcGalleryPtr).pointed = readGallery()
        SfcType.DOOR -> (pointer as Ptr.SfcDoorPtr).pointed = readDoor()
        SfcType.ROOM -> (pointer as Ptr.SfcRoomPtr).pointed = readC2Room()
        SfcType.ENTITY -> (pointer as Ptr.SfcEntityPtr).pointed = readEntity()
        SfcType.COMPOUNDOBJECT -> (pointer as Ptr.SfcCompoundObjectPtr).pointed = readCompoundObject()
        SfcType.BLACKBOARD -> (pointer as Ptr.SfcBlackboardPtr).pointed = readBlackBoard()
        SfcType.VEHICLE -> (pointer as Ptr.SfcVehiclePtr).pointed = readVehicle()
        SfcType.LIFT -> (pointer as Ptr.SfcLiftPtr).pointed = readLift()
        SfcType.SIMPLEOBJECT -> (pointer as Ptr.SfcSimpleObjectPtr).pointed = readSimpleObject()
        SfcType.POINTERTOOL -> (pointer as Ptr.SfcPointerToolPtr).pointed = readPointerTool()
        SfcType.CALLBUTTON -> (pointer as Ptr.SfcCallButtonPtr).pointed = readCallButton()
        SfcType.SCENERY -> (pointer as Ptr.SfcSceneryPtr).pointed = readScenery()
        SfcType.MACRO -> (pointer as Ptr.SfcMacroPtr).pointed = readMacro()
        else -> throw SfcReadException("Bad MFC type: '$pidType'")
    }

    if (validSfcType(pidType, SfcType.COMPOUNDOBJECT))
        readingCompoundObject = false
    else if (pidType == SfcType.SCENERY)
        readingScenery = false
    LOGGER.info("Read($pid)->$pointer")
    return pointer
}

private fun validSfcType(type:SfcType, requiredType:SfcType) : Boolean {
    return when {
        requiredType == SfcType.ANY -> true
        type == requiredType -> true
        requiredType == SfcType.OBJECT -> type.value >= SfcType.COMPOUNDOBJECT.value
        requiredType == SfcType.COMPOUNDOBJECT -> type.value in (SfcType.COMPOUNDOBJECT.value .. SfcType.LIFT.value)
        else -> false
    }
}