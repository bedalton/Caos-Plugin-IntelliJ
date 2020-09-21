package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcData

private const val SFC_BINARY_CONST = 0x8000

internal fun SfcReader.readClass(requiredType: Int): SfcData? {
    val pid = uInt16
    LOGGER.info("Reading PID: '$pid'")
    return when {
        pid == 0 -> null
        pid == 0xFFFF -> readNewMfc(requiredType)
        (pid and SFC_BINARY_CONST) != SFC_BINARY_CONST -> returnExistingObject(requiredType, pid)
        else -> createNewFromExisting(requiredType, pid)
    }
}

private fun SfcReader.readNewMfc(requiredType: Int) : SfcData? {
    skip(2) // Schema ID -> Seems to be ignored in C2e
    val className = string(uInt16).trim()
    val pid = storage.size
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
    // Push null object in place as reference to this type
    // Checked for later when creating instances of type
    storage.add(null)
    LOGGER.info("Reading: ($pidType)->$className. PID: $pid")
    types[pid] = pidType
    return read(requiredType, pid, pidType)
}

private fun SfcReader.returnExistingObject(requiredType:Int, pidIn:Int) : SfcData {
    val pid = pidIn - 1
    assert(pid < storage.size) { "PID '$pid' out of bounds for read class. Valid range: (0 to ${storage.size})" }
    val pidType = types[pid]
            ?: throw SfcReadException("Type for PID($pid) is null")
    assert(validSfcType(pidType, requiredType)) {
        val pidTypeString = typeToString(pidType)
        val requiredTypeString = typeToString(requiredType)
        "Failed to assert that pid type: $pidTypeString is compatible with required type: $requiredTypeString"
    }
    assert(storage[pid] != null) {
        "Object in storage at PID '$pid' is null. Expected $pidType. Previous: ${storage[pid - 1]?.let {it::class.java.name}}. Next: ${storage[pid + 1]?.let {it::class.java.name}}"
    }
    return storage[pid]!!
}

private fun SfcReader.createNewFromExisting(requiredType: Int, pidIn:Int) : SfcData {
    val pid = (pidIn xor SFC_BINARY_CONST) - 1
    assert(pid < storage.size) { "Pid should be less than size"}
    assert(storage[pid] == null) { "Storage for pid '$pid' should be null" }
    val pidType = types[pid]
            ?: throw SfcReadException("PidType is null for pid($pid)->original($pidIn)")
    return read(requiredType, pid, pidType)
}

private fun SfcReader.read(requiredType: Int, pid:Int, pidType:Int) : SfcData {
    types[storage.size] = pidType
    if (validSfcType(pidType, TYPE_COMPOUNDOBJECT))
        readingCompoundObject = true
    else if (pidType == TYPE_SCENERY)
        readingScenery = true
    
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
    storage.add(data)
    if (validSfcType(pidType, TYPE_COMPOUNDOBJECT))
        readingCompoundObject = false
    else if (pidType == TYPE_SCENERY)
        readingScenery = false
    LOGGER.info("Read($pid)->$data")
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