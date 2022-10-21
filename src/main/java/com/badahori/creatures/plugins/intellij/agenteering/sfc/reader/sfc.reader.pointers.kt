@file:Suppress("unused", "UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")
package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.Ptr.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass
import com.badahori.creatures.plugins.intellij.agenteering.sfc.*
import bedalton.creatures.util.className
import com.google.gson.Gson

sealed class Ptr<DClassT>(
        open val type: Int,
        open var pointed: DClassT?
) {
    data class SfcBlackboardPtr(override val type: Int, override var pointed: PointerSfcBlackboard? = null) : SfcObjectPtr<PointerSfcBlackboard>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcBiochemistryPtr(override val type: Int, override var pointed: SfcBiochemistry? = null) : Ptr<SfcBiochemistry>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcCallButtonPtr(override val type: Int, override var pointed: PointerSfcCallButton? = null) : SfcObjectPtr<PointerSfcCallButton>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcCompoundObjectPtr(override val type: Int, override var pointed: PointerSfcCompoundObject<*>? = null) : SfcObjectPtr<PointerSfcCompoundObject<*>>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcDoorPtr(override val type: Int, override var pointed: SfcDoor? = null) : Ptr<SfcDoor>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcEntityPtr(override val type: Int, override var pointed: PointerSfcEntity? = null) : Ptr<PointerSfcEntity>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcGalleryPtr(override val type: Int, override var pointed: SfcGallery? = null) : Ptr<SfcGallery>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcLiftPtr(override val type: Int, override var pointed: PointerSfcLift? = null) : SfcObjectPtr<PointerSfcLift>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcMacroPtr(override val type: Int, override var pointed: PointerSfcMacro? = null) : Ptr<PointerSfcMacro>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcCreaturePtr(override val type:Int, override var pointed: PointerSfcCreature? = null) : Ptr<PointerSfcCreature>(type, pointed)

    data class SfcMapDataPtr(override val type: Int, override var pointed: PointerSfcMapData? = null) : Ptr<PointerSfcMapData>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    abstract class SfcObjectPtr<ObjT : PointerSfcObject<*>>(override val type: Int, override var pointed: ObjT? = null) : Ptr<ObjT>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcObjectImplPtr(override val type: Int, override var pointed: PointerSfcObject<*>? = null) : Ptr<PointerSfcObject<*>>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcPointerToolPtr(override val type: Int, override var pointed: PointerSfcPointerTool? = null) : SfcObjectPtr<PointerSfcPointerTool>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcRoomPtr(override val type: Int, override var pointed: PointerSfcRoom<*>? = null) : Ptr<PointerSfcRoom<*>>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcSimpleObjectPtr(override val type: Int, override var pointed: PointerSfcSimpleObject<*>? = null) : SfcObjectPtr<PointerSfcSimpleObject<*>>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcSceneryPtr(override val type: Int, override var pointed: PointerSfcScenery? = null) : SfcObjectPtr<PointerSfcScenery>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    data class SfcVehiclePtr(override val type: Int, override var pointed: PointerSfcVehicle<*>? = null) : SfcObjectPtr<PointerSfcVehicle<*>>(type, pointed) {
        override fun toString(): String = toString(this)
    }

    override fun toString(): String = toString(this)

    companion object {
        fun toString(ptr: Ptr<*>): String = ptr.pointed?.toString() ?: "$className.NULL"
    }
}


interface PointerSfcData<SfcT : SfcData> {
    fun point(): SfcT
}

interface PointerSfcRoom<SfcT:SfcRoom> : PointerSfcData<SfcT>

/**
 * Interface for PointerSfcObject to allow for data classes
 */
interface PointerSfcObject<SfcT : SfcObject> : PointerSfcData<SfcT> {
    val classifier: AgentClass
    val unId: Int?
    val attr: Int
    val bounds: Bounds
    val actv: Int
    val currentSound: String?
    val sprite: SfcGalleryPtr
    val tickReset: Int
    val tickState: Int
    val variables: List<Int>
    val size: Int?
    val threat: Int?
    val range: Int?
    val accelerationG: Int?
    val velocity: Vector2?
    val restitution: Int?
    val aero: Int?
    val gravityData: Int?
    val frozen: Boolean?
    val scripts: List<SfcScript>
}

/**
 * Interfaces for compound objects
 */
interface PointerSfcCompoundObject<SfcT : SfcCompoundObject> : PointerSfcObject<SfcCompoundObject> {
    val parts: List<SfcEntityPtr?>
    val hotspots: List<SfcHotspot>
}

/**
 * Interface for PointerSfc Vehicle agents
 */
interface PointerSfcVehicle<SfcT : SfcVehicle> : PointerSfcCompoundObject<SfcT> {
    val cabinBounds: Bounds
    val movementVector: Vector2
    val bump: Int
}

/**
 * Interface for PointerSfc Simple agent objects
 */
interface PointerSfcSimpleObject<SfcT : SfcSimpleObject> : PointerSfcObject<SfcT> {
    val entity: SfcEntityPtr
}

/**
 * An PointerSfc data class for agent objects in C1/C2
 */
open class PointerSfcObjectImpl(
        override val classifier: AgentClass,
        override val unId: Int? = null,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String?,
        override val sprite: SfcGalleryPtr,
        override val tickReset: Int,
        override val tickState: Int,
        override val variables: List<Int>,
        override val size: Int? = null,
        override val threat: Int? = null,
        override val range: Int? = null,
        override val accelerationG: Int? = null,
        override val velocity: Vector2? = null,
        override val restitution: Int? = null,
        override val aero: Int? = null,
        override val gravityData: Int? = null,
        override val frozen: Boolean? = null,
        override val scripts: List<SfcScript>
) : PointerSfcObject<SfcObject> {
    override fun toString(): String {
        return Gson().toJson(this)
    }

    override fun point(): SfcObject {
        return SfcObjectImpl(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite.pointed!!,
                tickReset,
                tickState,
                variables,
                size,
                threat,
                range,
                accelerationG,
                velocity,
                restitution,
                aero,
                gravityData,
                frozen,
                scripts
        )
    }

    companion object
}

/**
 * An PointerSfc data class for Compound objects in C1/C2
 */
data class PointerSfcCompoundObjectImpl(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String?,
        override val sprite: SfcGalleryPtr,
        override val tickReset: Int,
        override val tickState: Int,
        override val variables: List<Int>,
        override val size: Int?,
        override val threat: Int?,
        override val range: Int?,
        override val accelerationG: Int?,
        override val velocity: Vector2?,
        override val restitution: Int?,
        override val aero: Int?,
        override val gravityData: Int?,
        override val frozen: Boolean?,
        override val scripts: List<SfcScript>,
        override val parts: List<SfcEntityPtr?>,
        override val hotspots: List<SfcHotspot>
) : PointerSfcCompoundObject<SfcCompoundObject> {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(
            baseObject: PointerSfcObject<*>,
            parts: List<SfcEntityPtr?>,
            hotspots: List<SfcHotspot>
    ) : this(
            classifier = baseObject.classifier,
            unId = baseObject.unId,
            attr = baseObject.attr,
            bounds = baseObject.bounds,
            actv = baseObject.actv,
            currentSound = baseObject.currentSound,
            sprite = baseObject.sprite,
            tickReset = baseObject.tickReset,
            tickState = baseObject.tickState,
            variables = baseObject.variables,
            size = baseObject.size,
            threat = baseObject.threat,
            range = baseObject.range,
            accelerationG = baseObject.accelerationG,
            velocity = baseObject.velocity,
            restitution = baseObject.restitution,
            aero = baseObject.aero,
            gravityData = baseObject.gravityData,
            frozen = baseObject.frozen,
            scripts = baseObject.scripts,
            parts = parts,
            hotspots = hotspots
    )

    override fun point(): SfcCompoundObject {
        return SfcCompoundObjectImpl(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite.pointed!!,
                tickReset,
                tickState,
                variables,
                size,
                threat,
                range,
                accelerationG,
                velocity,
                restitution,
                aero,
                gravityData,
                frozen,
                scripts,
                parts.map { it?.pointed?.point() },
                hotspots
        )
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object
} // End PointerSfcCompound Object

/**
 * An PointerSfc data class for Blackboards in C1/C2
 */
data class PointerSfcBlackboard(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String?,
        override val sprite: SfcGalleryPtr,
        override val tickReset: Int,
        override val tickState: Int,
        override val variables: List<Int>,
        override val size: Int?,
        override val threat: Int?,
        override val range: Int?,
        override val accelerationG: Int?,
        override val velocity: Vector2?,
        override val restitution: Int?,
        override val aero: Int?,
        override val gravityData: Int?,
        override val frozen: Boolean?,
        override val scripts: List<SfcScript>,
        override val parts: List<SfcEntityPtr?>,
        override val hotspots: List<SfcHotspot>,
        val textPosition: Vector2,
        val backgroundColor: Int,
        val chalkColor: Int,
        val aliasColor: Int,
        val strings: Map<Int, String>
) : PointerSfcCompoundObject<SfcBlackboard> {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(
            baseObject: PointerSfcCompoundObject<*>,
            textPosition: Vector2,
            backgroundColor: Int,
            chalkColor: Int,
            aliasColor: Int,
            strings: Map<Int, String>
    ) : this(
            classifier = baseObject.classifier,
            unId = baseObject.unId,
            attr = baseObject.attr,
            bounds = baseObject.bounds,
            actv = baseObject.actv,
            currentSound = baseObject.currentSound,
            sprite = baseObject.sprite,
            tickReset = baseObject.tickReset,
            tickState = baseObject.tickState,
            variables = baseObject.variables,
            size = baseObject.size,
            threat = baseObject.threat,
            range = baseObject.range,
            accelerationG = baseObject.accelerationG,
            velocity = baseObject.velocity,
            restitution = baseObject.restitution,
            aero = baseObject.aero,
            gravityData = baseObject.gravityData,
            frozen = baseObject.frozen,
            scripts = baseObject.scripts,
            parts = baseObject.parts,
            hotspots = baseObject.hotspots,
            textPosition = textPosition,
            backgroundColor = backgroundColor,
            chalkColor = chalkColor,
            aliasColor = aliasColor,
            strings = strings
    )

    override fun point(): SfcBlackboard {
        return SfcBlackboard(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite.pointed!!,
                tickReset,
                tickState,
                variables,
                size,
                threat,
                range,
                accelerationG,
                velocity,
                restitution,
                aero,
                gravityData,
                frozen,
                scripts,
                parts.map { it?.pointed?.point() },
                hotspots,
                textPosition,
                backgroundColor,
                chalkColor,
                aliasColor,
                strings
        )
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object
} // End PointerSfcBlackboard

/**
 * An PointerSfc data class for Vehicles in C1/C2
 */
data class PointerSfcVehicleImpl(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String?,
        override val sprite: SfcGalleryPtr,
        override val tickReset: Int,
        override val tickState: Int,
        override val variables: List<Int>,
        override val size: Int?,
        override val threat: Int?,
        override val range: Int?,
        override val accelerationG: Int?,
        override val velocity: Vector2?,
        override val restitution: Int?,
        override val aero: Int?,
        override val gravityData: Int?,
        override val frozen: Boolean?,
        override val scripts: List<SfcScript>,
        override val parts: List<SfcEntityPtr?>,
        override val hotspots: List<SfcHotspot>,
        override val cabinBounds: Bounds,
        override val movementVector: Vector2,
        override val bump: Int
) : PointerSfcVehicle<SfcVehicle> {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(
            baseObject: PointerSfcCompoundObject<*>,
            cabinBounds: Bounds,
            movementVector: Vector2,
            bump: Int
    ) : this(
            classifier = baseObject.classifier,
            unId = baseObject.unId,
            attr = baseObject.attr,
            bounds = baseObject.bounds,
            actv = baseObject.actv,
            currentSound = baseObject.currentSound,
            sprite = baseObject.sprite,
            tickReset = baseObject.tickReset,
            tickState = baseObject.tickState,
            variables = baseObject.variables,
            size = baseObject.size,
            threat = baseObject.threat,
            range = baseObject.range,
            accelerationG = baseObject.accelerationG,
            velocity = baseObject.velocity,
            restitution = baseObject.restitution,
            aero = baseObject.aero,
            gravityData = baseObject.gravityData,
            frozen = baseObject.frozen,
            scripts = baseObject.scripts,
            parts = baseObject.parts,
            hotspots = baseObject.hotspots,
            cabinBounds = cabinBounds,
            movementVector = movementVector,
            bump = bump
    )

    override fun point(): SfcCompoundObject {
        return SfcVehicleImpl(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite.pointed!!,
                tickReset,
                tickState,
                variables,
                size,
                threat,
                range,
                accelerationG,
                velocity,
                restitution,
                aero,
                gravityData,
                frozen,
                scripts,
                parts.map { it?.pointed?.point() },
                hotspots,
                cabinBounds,
                movementVector,
                bump
        )
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object
} // End PointerSfcVehicle


/**
 * An PointerSfc data class for Lift objects in C1/C2
 */
data class PointerSfcLift(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String?,
        override val sprite: SfcGalleryPtr,
        override val tickReset: Int,
        override val tickState: Int,
        override val variables: List<Int>,
        override val size: Int?,
        override val threat: Int?,
        override val range: Int?,
        override val accelerationG: Int?,
        override val velocity: Vector2?,
        override val restitution: Int?,
        override val aero: Int?,
        override val gravityData: Int?,
        override val frozen: Boolean?,
        override val scripts: List<SfcScript>,
        override val parts: List<SfcEntityPtr?>,
        override val hotspots: List<SfcHotspot>,
        override val cabinBounds: Bounds,
        override val movementVector: Vector2,
        override val bump: Int,
        val numberOfButtons: Int,
        val currentButton: Int,
        val callButtonYs: List<Int>,
        val alignWithCabin: Boolean
) : PointerSfcVehicle<SfcLift> {
    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(
            baseObject: PointerSfcVehicle<*>,
            numberOfButtons: Int,
            currentButton: Int,
            callButtonYs: List<Int>,
            alignWithCabin: Boolean
    ) : this(
            classifier = baseObject.classifier,
            unId = baseObject.unId,
            attr = baseObject.attr,
            bounds = baseObject.bounds,
            actv = baseObject.actv,
            currentSound = baseObject.currentSound,
            sprite = baseObject.sprite,
            tickReset = baseObject.tickReset,
            tickState = baseObject.tickState,
            variables = baseObject.variables,
            size = baseObject.size,
            threat = baseObject.threat,
            range = baseObject.range,
            accelerationG = baseObject.accelerationG,
            velocity = baseObject.velocity,
            restitution = baseObject.restitution,
            aero = baseObject.aero,
            gravityData = baseObject.gravityData,
            frozen = baseObject.frozen,
            scripts = baseObject.scripts,
            parts = baseObject.parts,
            hotspots = baseObject.hotspots,
            cabinBounds = baseObject.cabinBounds,
            movementVector = baseObject.movementVector,
            bump = baseObject.bump,
            numberOfButtons = numberOfButtons,
            currentButton = currentButton,
            callButtonYs = callButtonYs,
            alignWithCabin = alignWithCabin
    )

    override fun point(): SfcLift {
        return SfcLift(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite.pointed!!,
                tickReset,
                tickState,
                variables,
                size,
                threat,
                range,
                accelerationG,
                velocity,
                restitution,
                aero,
                gravityData,
                frozen,
                scripts,
                parts.map { it?.pointed?.point() },
                hotspots,
                cabinBounds,
                movementVector,
                bump,
                numberOfButtons,
                currentButton,
                callButtonYs,
                alignWithCabin
        )
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object
} // End PointerSfcLift

/**
 * An PointerSfc data class for simple agent objects
 */
data class PointerSfcSimpleObjectImpl(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String?,
        override val sprite: SfcGalleryPtr,
        override val tickReset: Int,
        override val tickState: Int,
        override val variables: List<Int>,
        override val size: Int?,
        override val threat: Int?,
        override val range: Int?,
        override val accelerationG: Int?,
        override val velocity: Vector2?,
        override val restitution: Int?,
        override val aero: Int?,
        override val gravityData: Int?,
        override val frozen: Boolean?,
        override val scripts: List<SfcScript>,
        override val entity: SfcEntityPtr
) : PointerSfcSimpleObject<SfcSimpleObjectImpl> {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(baseObject: PointerSfcObject<*>, entity: SfcEntityPtr) : this(
            classifier = baseObject.classifier,
            unId = baseObject.unId,
            attr = baseObject.attr,
            bounds = baseObject.bounds,
            actv = baseObject.actv,
            currentSound = baseObject.currentSound,
            sprite = baseObject.sprite,
            tickReset = baseObject.tickReset,
            tickState = baseObject.tickState,
            variables = baseObject.variables,
            size = baseObject.size,
            threat = baseObject.threat,
            range = baseObject.range,
            accelerationG = baseObject.accelerationG,
            velocity = baseObject.velocity,
            restitution = baseObject.restitution,
            aero = baseObject.aero,
            gravityData = baseObject.gravityData,
            frozen = baseObject.frozen,
            scripts = baseObject.scripts,
            entity = entity
    )

    override fun point(): SfcSimpleObjectImpl {
        return SfcSimpleObjectImpl(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite.pointed!!,
                tickReset,
                tickState,
                variables,
                size,
                threat,
                range,
                accelerationG,
                velocity,
                restitution,
                aero,
                gravityData,
                frozen,
                scripts,
                entity.pointed!!.point()
        )
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object
} // End PointerSfcSimpleObject


/**
 * An PointerSfc data class for the pointer agent in C1/C2
 */
data class PointerSfcPointerTool(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String?,
        override val sprite: SfcGalleryPtr,
        override val tickReset: Int,
        override val tickState: Int,
        override val variables: List<Int>,
        override val size: Int?,
        override val threat: Int?,
        override val range: Int?,
        override val accelerationG: Int?,
        override val velocity: Vector2?,
        override val restitution: Int?,
        override val aero: Int?,
        override val gravityData: Int?,
        override val frozen: Boolean?,
        override val scripts: List<SfcScript>,
        override val entity: SfcEntityPtr
) : PointerSfcSimpleObject<SfcPointerTool> {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(baseObject: PointerSfcSimpleObjectImpl) : this(
            classifier = baseObject.classifier,
            unId = baseObject.unId,
            attr = baseObject.attr,
            bounds = baseObject.bounds,
            actv = baseObject.actv,
            currentSound = baseObject.currentSound,
            sprite = baseObject.sprite,
            tickReset = baseObject.tickReset,
            tickState = baseObject.tickState,
            variables = baseObject.variables,
            size = baseObject.size,
            threat = baseObject.threat,
            range = baseObject.range,
            accelerationG = baseObject.accelerationG,
            velocity = baseObject.velocity,
            restitution = baseObject.restitution,
            aero = baseObject.aero,
            gravityData = baseObject.gravityData,
            frozen = baseObject.frozen,
            scripts = baseObject.scripts,
            entity = baseObject.entity
    )

    override fun point(): SfcPointerTool {
        return SfcPointerTool(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite.pointed!!,
                tickReset,
                tickState,
                variables,
                size,
                threat,
                range,
                accelerationG,
                velocity,
                restitution,
                aero,
                gravityData,
                frozen,
                scripts,
                entity.pointed!!.point()
        )
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }


    companion object
} // End PointerSfcPointer

/**
 * An PointerSfc data class for call buttons in C1/C2
 */
data class PointerSfcCallButton(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String?,
        override val sprite: SfcGalleryPtr,
        override val tickReset: Int,
        override val tickState: Int,
        override val variables: List<Int>,
        override val size: Int?,
        override val threat: Int?,
        override val range: Int?,
        override val accelerationG: Int?,
        override val velocity: Vector2?,
        override val restitution: Int?,
        override val aero: Int?,
        override val gravityData: Int?,
        override val frozen: Boolean?,
        override val scripts: List<SfcScript>,
        override val entity: SfcEntityPtr,
        val ourLift: SfcLiftPtr,
        val liftId: Int
) : PointerSfcSimpleObject<SfcCallButton> {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(
            baseObject: PointerSfcSimpleObjectImpl,
            ourLift: SfcLiftPtr,
            liftId: Int
    ) : this(
            classifier = baseObject.classifier,
            unId = baseObject.unId,
            attr = baseObject.attr,
            bounds = baseObject.bounds,
            actv = baseObject.actv,
            currentSound = baseObject.currentSound,
            sprite = baseObject.sprite,
            tickReset = baseObject.tickReset,
            tickState = baseObject.tickState,
            variables = baseObject.variables,
            size = baseObject.size,
            threat = baseObject.threat,
            range = baseObject.range,
            accelerationG = baseObject.accelerationG,
            velocity = baseObject.velocity,
            restitution = baseObject.restitution,
            aero = baseObject.aero,
            gravityData = baseObject.gravityData,
            frozen = baseObject.frozen,
            scripts = baseObject.scripts,
            entity = baseObject.entity,
            ourLift = ourLift,
            liftId = liftId
    )

    override fun point(): SfcCallButton {
        return SfcCallButton(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite.pointed!!,
                tickReset,
                tickState,
                variables,
                size,
                threat,
                range,
                accelerationG,
                velocity,
                restitution,
                aero,
                gravityData,
                frozen,
                scripts,
                entity.pointed!!.point(),
                ourLift.pointed!!.point(),
                liftId
        )
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }


    companion object
} // End PointerSfcCallButton

/**
 * An PointerSfc data class for scenery objects in C1/C2
 */
data class PointerSfcScenery(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String?,
        override val sprite: SfcGalleryPtr,
        override val tickReset: Int,
        override val tickState: Int,
        override val variables: List<Int>,
        override val size: Int?,
        override val threat: Int?,
        override val range: Int?,
        override val accelerationG: Int?,
        override val velocity: Vector2?,
        override val restitution: Int?,
        override val aero: Int?,
        override val gravityData: Int?,
        override val frozen: Boolean?,
        override val scripts: List<SfcScript>,
        override val entity: SfcEntityPtr
) : PointerSfcSimpleObject<SfcScenery> {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(baseObject: PointerSfcSimpleObjectImpl) : this(
            classifier = baseObject.classifier,
            unId = baseObject.unId,
            attr = baseObject.attr,
            bounds = baseObject.bounds,
            actv = baseObject.actv,
            currentSound = baseObject.currentSound,
            sprite = baseObject.sprite,
            tickReset = baseObject.tickReset,
            tickState = baseObject.tickState,
            variables = baseObject.variables,
            size = baseObject.size,
            threat = baseObject.threat,
            range = baseObject.range,
            accelerationG = baseObject.accelerationG,
            velocity = baseObject.velocity,
            restitution = baseObject.restitution,
            aero = baseObject.aero,
            gravityData = baseObject.gravityData,
            frozen = baseObject.frozen,
            scripts = baseObject.scripts,
            entity = baseObject.entity
    )

    override fun point(): SfcScenery {
        return SfcScenery(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite.pointed!!,
                tickReset,
                tickState,
                variables,
                size,
                threat,
                range,
                accelerationG,
                velocity,
                restitution,
                aero,
                gravityData,
                frozen,
                scripts,
                entity.pointed!!.point()
        )
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object
} // End PointerSfcScenery

/**
 * An PointerSfc data object for scripts in C1/C2
 */
data class PointerSfcMacro(
        val ownr: SfcObjectPtr<*>,
        val from: SfcObjectPtr<*>,
        val targ: SfcObjectPtr<*>?,
        val script: String
) : PointerSfcData<SfcMacro> {
    override fun toString(): String {
        return Gson().toJson(this)
    }

    override fun point(): SfcMacro {
        return SfcMacro(
                ownr.pointed!!.point(),
                from.pointed!!.point(),
                targ?.pointed?.point(),
                script
        )
    }

    companion object
}


/**
 * an SFC data class for rooms in C1/C2
 */
data class PointerSfcRoomImpl(
        val id: Int,
        val bounds: Bounds,
        val roomType: Int
) : PointerSfcRoom<SfcRoomImpl> {

    override fun point(): SfcRoomImpl {
        return SfcRoomImpl(
                id,
                bounds,
                roomType
        )
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object
}

/**
 * An C2 specific PointerSfc room data object
 */
data class PointerSfcC2Room(
        val id: Int,
        val bounds: Bounds,
        val roomType: Int,
        val doors: Map<Int, List<SfcDoorPtr>>,
        val floorValue: Int,
        val inorganicNutrients: Int,
        val organicNutrients: Int,
        val temperature: Int,
        val pressure: Int,
        val lightLevel: Int,
        val radiation: Int,
        val heatSource: Int,
        val pressureSource: Int,
        val lightSource: Int,
        val radiationSource: Int,
        val windVector: Vector2,
        val floorPoints: List<Vector2>,
        val music: String,
        val dropStatus: Int
) : PointerSfcRoom<SfcC2Room> {
    override fun toString(): String {
        return Gson().toJson(this)
    }

    override fun point(): SfcC2Room {
        return SfcC2Room(
                id,
                bounds,
                roomType,
                doors.map { keySet -> keySet.key to keySet.value.map { door -> door.pointed!! } }.toMap(),
                floorValue,
                inorganicNutrients,
                organicNutrients,
                temperature,
                pressure,
                lightLevel,
                radiation,
                heatSource,
                pressureSource,
                lightSource,
                radiationSource,
                windVector,
                floorPoints,
                music,
                dropStatus
        )
    }

    companion object
}

/**
 * PointerSfc map data object
 */
data class PointerSfcMapData(
        val gallery: SfcGalleryPtr,
        val rooms: List<SfcRoomPtr>,
        val groundLevels: List<Int>? = null // Should be 261 points
) : PointerSfcData<SfcMapData> {
    override fun toString(): String {
        return Gson().toJson(this)
    }

    override fun point(): SfcMapData {
        return SfcMapData(
                gallery.pointed!!,
                rooms.map { it.pointed!!.point() },
                groundLevels
        )
    }

    companion object
}

/**
 * An PointerSfc data class for an entity/agent part
 */
data class PointerSfcEntity(
        val gallery: SfcGalleryPtr,
        val currentFrame: Int,
        val imageOffset: Int,
        val zOrder: Int,
        val position: Vector2,
        val animationFrame: Int?,
        val animationString: String?,
        val relativePosition: Vector2? = null,
        val partZOrder: Int? = null,
        val behaviorClicks: List<Int>? = null,
        val behaviorTouch: Int? = null,
        val pickupHandles: List<Vector2>? = null,
        val pickupPoints: List<Vector2>? = null
) : PointerSfcData<SfcEntity> {
    override fun toString(): String {
        return Gson().toJson(this)
    }

    override fun point(): SfcEntity {
        return SfcEntity(
                gallery.pointed!!,
                currentFrame,
                imageOffset,
                zOrder,
                position,
                animationFrame,
                animationString,
                relativePosition,
                partZOrder,
                behaviorClicks,
                behaviorTouch,
                pickupHandles,
                pickupPoints
        )
    }

    companion object
}

data class PointerSfcCreature(
        val moniker:String
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}

private fun List<SfcEntityPtr?>.point(): List<SfcEntity?> {
    return map { it?.pointed?.point() }
}