@file:Suppress("unused", "UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")

package com.badahori.creatures.plugins.intellij.agenteering.sfc

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass
import kotlin.math.max
import kotlin.math.min

/**
 * SFC data entity representing a parsed SFC world file
 */
data class SfcFile(
        val mapData: SfcMapData,
        val variant: CaosVariant,
        val objects: List<SfcObject>,
        val scenery: List<SfcScenery>,
        val scripts: List<SfcScript>,
        val macros: List<SfcMacro>,
        val scrollPosition: Vector2,
        val favoritePlaceName: String,
        val favoritePlacePosition: Vector2,
        val speechHistory: List<String>
)


interface SfcData

/**
 * Interface for SfcObject to allow for data classes
 */
interface SfcObject : SfcData {
    val classifier: AgentClass
    val unId: Int?
    val attr: Int
    val bounds: Bounds
    val actv: Int
    val currentSound: String
    val sprite: SfcGallery
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
interface SfcCompoundObject : SfcObject {
    val parts: List<SfcEntity?>
    val hotspots: List<SfcHotspot>
}

/**
 * Interface for SFC Vehicle agents
 */
interface SfcVehicle : SfcCompoundObject {
    val cabinBounds: Bounds
    val movementVector: Vector2
    val bump: Int
}

/**
 * Interface for SFC Simple agent objects
 */
interface SfcSimpleObject : SfcObject {
    val entity: SfcEntity
}

/**
 * An SFC data class for agent objects in C1/C2
 */
open class SfcObjectImpl(
        override val classifier: AgentClass,
        override val unId: Int? = null,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String,
        override val sprite: SfcGallery,
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
) : SfcObject {
    companion object
}

/**
 * An SFC data class for Compound objects in C1/C2
 */
data class SfcCompoundObjectImpl(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String,
        override val sprite: SfcGallery,
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
        override val parts: List<SfcEntity?>,
        override val hotspots: List<SfcHotspot>
) : SfcCompoundObject {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(
            baseObject: SfcObject,
            parts: List<SfcEntity?>,
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

    companion object
} // End SfcCompound Object

/**
 * An SFC data class for Blackboards in C1/C2
 */
data class SfcBlackboard(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String,
        override val sprite: SfcGallery,
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
        override val parts: List<SfcEntity?>,
        override val hotspots: List<SfcHotspot>,
        val textPosition: Vector2,
        val backgroundColor: Int,
        val chalkColor: Int,
        val aliasColor: Int,
        val strings: Map<Int, String>
) : SfcCompoundObject {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(
            baseObject: SfcCompoundObject,
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

    companion object
} // End SfcBlackboard

/**
 * An SFC data class for Vehicles in C1/C2
 */
data class SfcVehicleImpl(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String,
        override val sprite: SfcGallery,
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
        override val parts: List<SfcEntity?>,
        override val hotspots: List<SfcHotspot>,
        override val cabinBounds: Bounds,
        override val movementVector: Vector2,
        override val bump: Int
) : SfcVehicle {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(
            baseObject: SfcCompoundObject,
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

    companion object
} // End SfcVehicle


/**
 * An SFC data class for Lift objects in C1/C2
 */
data class SfcLift(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String,
        override val sprite: SfcGallery,
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
        override val parts: List<SfcEntity?>,
        override val hotspots: List<SfcHotspot>,
        override val cabinBounds: Bounds,
        override val movementVector: Vector2,
        override val bump: Int,
        val numberOfButtons: Int,
        val currentButton: Int,
        val callButtonYs: List<Int>,
        val alignWithCabin: Boolean
) : SfcVehicle {
    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(
            baseObject: SfcVehicle,
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

    companion object
} // End SfcLift

/**
 * An SFC data class for simple agent objects
 */
data class SfcSimpleObjectImpl(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String,
        override val sprite: SfcGallery,
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
        override val entity: SfcEntity
) : SfcSimpleObject {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(baseObject: SfcObject, entity: SfcEntity) : this(
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

    companion object
} // End SfcSimpleObject


/**
 * An SFC data class for the pointer agent in C1/C2
 */
data class SfcPointer(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String,
        override val sprite: SfcGallery,
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
        override val entity: SfcEntity
) : SfcSimpleObject {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(baseObject: SfcSimpleObjectImpl) : this(
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

    companion object
} // End SfcPointer

/**
 * An Sfc data class for call buttons in C1/C2
 */
data class SfcCallButton(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String,
        override val sprite: SfcGallery,
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
        override val entity: SfcEntity,
        val ourLift: SfcLift,
        val liftId: Int
) : SfcSimpleObject {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(
            baseObject: SfcSimpleObjectImpl,
            ourLift: SfcLift,
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

    companion object
} // End SfcCallButton

/**
 * An SFC data class for scenery objects in C1/C2
 */
class SfcScenery(
        override val classifier: AgentClass,
        override val unId: Int?,
        override val attr: Int,
        override val bounds: Bounds,
        override val actv: Int,
        override val currentSound: String,
        override val sprite: SfcGallery,
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
        override val entity: SfcEntity
) : SfcSimpleObject {

    /**
     * Helper constructor to initialize with a parent base read in
     */
    constructor(baseObject: SfcSimpleObjectImpl) : this(
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

    companion object
} // End SfcScenery

/**
 * An SFC data object for Scripts in C1/C2
 */
data class SfcScript(
        val classifier: AgentClass,
        val eventNumber: Int,
        val script: String
) : SfcData {
    companion object
}

/**
 * An SFC data object for scripts in C1/C2
 */
data class SfcMacro(
        val ownr: SfcObject,
        val from: SfcObject,
        val targ: SfcObject?,
        val script: String
) : SfcData {
    companion object
}

/**
 * An SFC data class for agent sprite data
 * Note: does not hold actual sprites, just agent information about them
 */
data class SfcGallery(
        val numberOfFrames: Int,
        val firstImage: Int,
        val fileName: String
) : SfcData {
    companion object
}


/**
 * SFC Door info object
 */
data class SfcDoor(
        val openness: Int,
        val otherRoom: Int
) : SfcData {
    companion object
}

interface SfcRoom: SfcData {
    val id: Int
    val bounds: Bounds
    val roomType: Int
}

/**
 * an SFC data class for rooms in C1/C2
 */
data class SfcRoomImpl(
        override val id: Int,
        override val bounds: Bounds,
        override val roomType: Int
) : SfcRoom {
   companion object
}

/**
 * An C2 specific SFC room data object
 */
data class SfcC2Room(
        override val id: Int,
        override val bounds: Bounds,
        override val roomType: Int,
        val doors: Map<Int, List<SfcDoor>>,
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
) : SfcRoom {
    companion object
}

/**
 * SFC map data object
 */
data class SfcMapData(
        val gallery: SfcGallery,
        val rooms: List<SfcRoom>,
        val groundLevels: List<Int>? = null // Should be 261 points
) : SfcData {
    companion object
}

/**
 * An SFC data class for an entity/agent part
 */
data class SfcEntity(
        val gallery: SfcGallery,
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
) : SfcData {
    companion object
}


/**
 * A data class to hold information about the edges of an object or room
 */
data class Bounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
) {
    val size: Size by lazy {
        val width = max(left, right) - min(left, right)
        val height = max(top, bottom) - min(top, bottom)
        Size(width = width, height = height)
    }
}
/**
 * Gets the origin for the bounds object from the Creatures runtime basis of 0,0 being bottom left
 */
val Bounds.creaturesOrigin get() = Vector2(left, max(bottom, top))

/**
 * Gets the origin for the bounds object from the java basis of 0,0 being top left
 */
val Bounds.javaOrigin: Vector2 get() = Vector2(left, min(bottom, top))


/**
 * Rect data class
 */
data class Rect(
        val origin: Vector2,
        val size: Size
)

/**
 * Size data class
 */
data class Size(
        val width: Int,
        val height: Int
)

/**
 * Vector 2 data class
 */
data class Vector2(val x: Int, val y: Int) {
    companion object {
        val zero by lazy {
            Vector2(0, 0)
        }
    }
}

/**
 * Hotspot data class
 */
data class SfcHotspot(
        val bounds: Bounds,
        val function: Int,
        val message: Int? = null,
        val mask: Int? = null
)