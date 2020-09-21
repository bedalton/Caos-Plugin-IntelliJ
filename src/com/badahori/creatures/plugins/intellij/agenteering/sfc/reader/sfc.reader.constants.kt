package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader


internal const val TYPE_MAPDATA = 1
internal const val TYPE_CGALLERY = 2
internal const val TYPE_CDOOR = 3
internal const val TYPE_CROOM = 4
internal const val TYPE_ENTITY = 5
internal const val TYPE_COMPOUNDOBJECT = 6
internal const val TYPE_BLACKBOARD = 7
internal const val TYPE_VEHICLE = 8
internal const val TYPE_LIFT = 9
internal const val TYPE_SIMPLEOBJECT = 10
internal const val TYPE_POINTERTOOL = 11
internal const val TYPE_CALLBUTTON = 12
internal const val TYPE_SCENERY = 13
internal const val TYPE_MACRO = 14
internal const val TYPE_OBJECT = 100

internal fun typeToString(type:Int) : String {
    return when(type) {
        TYPE_MAPDATA -> "MapData"
        TYPE_CGALLERY -> "Gallery"
        TYPE_CDOOR -> "Door"
        TYPE_CROOM -> "C2Room"
        TYPE_ENTITY -> "Entity"
        TYPE_COMPOUNDOBJECT -> "CompoundObject"
        TYPE_BLACKBOARD -> "BlackBoard"
        TYPE_VEHICLE -> "Vehicle"
        TYPE_LIFT -> "Lift"
        TYPE_SIMPLEOBJECT -> "SimpleObject"
        TYPE_POINTERTOOL -> "PointerTool"
        TYPE_CALLBUTTON -> "CallButton"
        TYPE_SCENERY -> "Scenery"
        TYPE_MACRO -> "Macro"
        else -> "UNKNOWN"
    }
}