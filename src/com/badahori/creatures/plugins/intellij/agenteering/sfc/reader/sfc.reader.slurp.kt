package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcData


internal fun SfcReader.slurp(requiredType: Int): SfcData? {
    val pid = uInt16
    return when {
        pid == 0 -> null
        pid == 0xFFFF -> readNewMfc(requiredType)
        pid and 0x8000 != 0x8000 -> returnExisting(requiredType, pid)
        else -> createFromExisting(requiredType, pid)
    }
}

private fun SfcReader.readNewMfc(requiredType: Int) : SfcData? {
    skip(2) // Schema ID -> Seems to be ignored in C2e
    val className = string(uInt16).trim()
    val pid = storage.size
    storage.add(null) // Push a null onto the stack. Not sure why
    val pidType = when(className) {
        "MapData" -> TYPE_MAPDATA
        "CGallery" -> TYPE_CGALLERY
        "CDoor" -> TYPE_CDOOR
        "CRoom" -> TYPE_CROOM
        "Entity" -> TYPE_ENTITY
        "CompoundObject" -> TYPE_COMPOUNDOBJECT
        "Blackboard" -> TYPE_BLACKBOARD
        "Vehicle" -> TYPE_VEHICLE
        "Lift" -> TYPE_LIFT
        "SimpleObject" -> TYPE_SIMPLEOBJECT
        "PointerTool" -> TYPE_POINTERTOOL
        "CallButton" -> TYPE_CALLBUTTON
        "Scenery" -> TYPE_SCENERY
        "Macro" -> TYPE_MACRO
        else -> throw SfcReadException("Failed to understand MFC class name: '$className'")
    }
    types[pid] = pidType
    return read(requiredType, pid, pidType)
}

private fun SfcReader.returnExisting(requiredType:Int, pidIn:Int) : SfcData {
    val pid = pidIn - 1
    val pidType = types[pid]
            ?: throw SfcReadException("Cannot return existing MFC data. Type for PID does not exist")
    assert(pid < storage.size) { "SFC reader tried slurping PID out of bounds" }
    assert(validSfcType(pidType, requiredType))
    return storage[pid]!!
}

private fun SfcReader.createFromExisting(requiredType: Int, pidIn:Int) : SfcData {
    val pid = (pidIn xor 8000) - 1
    val pidType = types[pid]!!
    return read(requiredType, pid, pidType)
}

private fun SfcReader.read(requiredType: Int, pid:Int, pidType:Int) : SfcData {
    if (validSfcType(pidType, TYPE_COMPOUNDOBJECT))
        readingCompoundObject = true
    else if (pidType == TYPE_SCENERY)
        readingScenery = true
    assert(validSfcType(pidType, requiredType))
    assert(storage[pid] == null) { "SFC MFC storage should not contain object for PID" }
    val data = when(pidType) {
        TYPE_MAPDATA -> readMapData()
        TYPE_CGALLERY -> readGallery()
        TYPE_CDOOR -> readDoor()
        TYPE_CROOM -> readC2Room()
        TYPE_ENTITY -> readEntity()
        TYPE_COMPOUNDOBJECT -> readCompoundObject()
        TYPE_BLACKBOARD -> readBlackBoard()
        TYPE_VEHICLE -> readVehicle()
        TYPE_LIFT -> readLift()
        TYPE_SIMPLEOBJECT -> readSimpleObject()
        TYPE_POINTERTOOL -> readPointerTool()
        TYPE_CALLBUTTON -> readCallButton()
        TYPE_SCENERY -> readScenery()
        TYPE_MACRO -> readMacro()
        else -> throw SfcReadException("Bad MFC type: '$pidType'")
    }
    types[storage.size] = pidType
    storage.add(data)
    if (validSfcType(pidType, TYPE_COMPOUNDOBJECT))
        readingCompoundObject = false
    else if (pidType == TYPE_SCENERY)
        readingScenery = false
    return data
}

private fun validSfcType(type:Int, requiredType:Int) : Boolean {
    return when {
        requiredType == 0 -> true
        type == requiredType -> true
        requiredType == TYPE_OBJECT -> type >= TYPE_COMPOUNDOBJECT
        requiredType == TYPE_COMPOUNDOBJECT -> type >= TYPE_COMPOUNDOBJECT
        else -> false
    }
}