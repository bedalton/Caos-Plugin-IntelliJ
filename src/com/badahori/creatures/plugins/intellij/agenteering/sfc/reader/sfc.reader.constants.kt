package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.Ptr
import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.Ptr.*

enum class SfcType(val value:Int, val type:String, val pointer:(pid:Int)->Ptr<*>) {
    ANY(0, "Any", { pid -> throw SfcReadException("Cannot create sfc data object of type '(0)->ANY'")} ),
    MAP_DATA(1, "MapData", { pid -> SfcMapDataPtr(pid) }),
    GALLERY(2, "Gallery", {pid -> SfcGalleryPtr(pid) }),
    DOOR(3, "Door", { pid -> SfcDoorPtr(pid) }),
    ROOM(4, "Room", { pid -> SfcRoomPtr(pid) }),
    ENTITY(5, "Entity", {pid -> SfcEntityPtr(pid)}),
    COMPOUNDOBJECT(6, "CompoundObject", { pid -> SfcCompoundObjectPtr(pid) }),
    BLACKBOARD(7, "Blackboard", { pid -> SfcBlackboardPtr(pid) }),
    VEHICLE(8, "Vehicle", { pid -> SfcVehiclePtr(pid) }),
    LIFT(9, "Lift", { pid -> SfcLiftPtr(pid) }),
    SIMPLEOBJECT(10, "SimpleObject", { pid -> SfcSimpleObjectPtr(pid) }),
    POINTERTOOL(11, "PointerTool", { pid -> SfcPointerToolPtr(pid) }),
    CALLBUTTON(12, "CallButton", { pid -> SfcCallButtonPtr(pid) }),
    SCENERY(13, "Scenery", { pid -> SfcSceneryPtr(pid) }),
    MACRO(14, "Macro", { pid -> SfcMacroPtr(pid) }),
    OBJECT(100, "Object", { pid -> SfcObjectImplPtr(pid) }),
}