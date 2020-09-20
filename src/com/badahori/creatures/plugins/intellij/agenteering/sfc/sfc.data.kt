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
        val mapData:SfcData.SfcMapData,
        val variant: CaosVariant,
        val objects:List<SfcData.SfcObject>,
        val scenery:List<SfcData.SfcObject.SfcScenery>,
        val scripts:List<SfcData.SfcScript>,
        val macros:List<SfcData.SfcMacro>,
        val scrollPosition:Vector2,
        val favoritePlaceName:String,
        val favoritePlacePosition:Vector2,
        val speechHistory:List<String>
)


/**
 * SFC data objects
 */
sealed class SfcData {

    /**
     * An SFC data class for agent objects in C1/C2
     */
    open class SfcObject(
            open val classifier: AgentClass,
            open val unId: Int? = null,
            open val attr: Int,
            open val bounds: Bounds,
            open val actv: Int,
            open val currentSound: String,
            open val sprite: SfcGallery,
            open val tickReset: Int,
            open val tickState: Int,
            open val variables: List<Int>,
            open val size: Int? = null,
            open val threat: Int? = null,
            open val range: Int? = null,
            open val accelerationG: Int? = null,
            open val velocity: Vector2? = null,
            open val restitution: Int? = null,
            open val aero: Int? = null,
            open val gravityData: Int? = null,
            open val frozen: Boolean? = null,
            open val scripts: List<SfcScript>
    ) : SfcData() {

        /**
         * An SFC data class for Compound objects in C1/C2
         */
        open class SfcCompoundObject(
                classifier: AgentClass,
                unId: Int?,
                attr: Int,
                bounds: Bounds,
                actv: Int,
                currentSound: String,
                sprite: SfcGallery,
                tickReset: Int,
                tickState: Int,
                variables: List<Int>,
                size: Int?,
                threat: Int?,
                range: Int?,
                accelerationG: Int?,
                velocity: Vector2?,
                restitution: Int?,
                aero: Int?,
                gravityData: Int?,
                frozen: Boolean?,
                scripts: List<SfcScript>,
                val parts: List<SfcEntity?>,
                val hotspots: List<SfcHotspot>
        ) : SfcObject(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite,
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
        ) {

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
        class SfcBlackboard(
                classifier: AgentClass,
                unId: Int?,
                attr: Int,
                bounds: Bounds,
                actv: Int,
                currentSound: String,
                sprite: SfcGallery,
                tickReset: Int,
                tickState: Int,
                variables: List<Int>,
                size: Int?,
                threat: Int?,
                range: Int?,
                accelerationG: Int?,
                velocity: Vector2?,
                restitution: Int?,
                aero: Int?,
                gravityData: Int?,
                frozen: Boolean?,
                scripts: List<SfcScript>,
                parts: List<SfcEntity?>,
                hotspots: List<SfcHotspot>,
                val textPosition: Vector2,
                val backgroundColor: Int,
                val chalkColor: Int,
                val aliasColor: Int,
                val strings: Map<Int, String>,
        ) : SfcCompoundObject(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite,
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
                parts,
                hotspots
        ) {

            /**
             * Helper constructor to initialize with a parent base read in
             */
            constructor(
                    baseObject: SfcCompoundObject,
                    textPosition: Vector2,
                    backgroundColor: Int,
                    chalkColor: Int,
                    aliasColor: Int,
                    strings: Map<Int, String>,
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
                    strings = strings,
            )

            companion object
        } // End SfcBlackboard

        /**
         * An SFC data class for Vehicles in C1/C2
         */
        open class SfcVehicle(
                classifier: AgentClass,
                unId: Int?,
                attr: Int,
                bounds: Bounds,
                actv: Int,
                currentSound: String,
                sprite: SfcGallery,
                tickReset: Int,
                tickState: Int,
                variables: List<Int>,
                size: Int?,
                threat: Int?,
                range: Int?,
                accelerationG: Int?,
                velocity: Vector2?,
                restitution: Int?,
                aero: Int?,
                gravityData: Int?,
                frozen: Boolean?,
                scripts: List<SfcScript>,
                parts: List<SfcEntity?>,
                hotspots: List<SfcHotspot>,
                val cabinBounds: Bounds,
                val movementVector: Vector2,
                val bump: Int
        ) : SfcCompoundObject(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite,
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
                parts,
                hotspots
        ) {

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
        class SfcLift(
                classifier: AgentClass,
                unId: Int?,
                attr: Int,
                bounds: Bounds,
                actv: Int,
                currentSound: String,
                sprite: SfcGallery,
                tickReset: Int,
                tickState: Int,
                variables: List<Int>,
                size: Int?,
                threat: Int?,
                range: Int?,
                accelerationG: Int?,
                velocity: Vector2?,
                restitution: Int?,
                aero: Int?,
                gravityData: Int?,
                frozen: Boolean?,
                scripts: List<SfcScript>,
                parts: List<SfcEntity?>,
                hotspots: List<SfcHotspot>,
                cabinBounds: Bounds,
                movementVector: Vector2,
                bump: Int,
                val numberOfButtons: Int,
                val currentButton: Int,
                val callButtonYs: List<Int>,
                val alignWithCabin: Boolean
        ) : SfcVehicle(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite,
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
                parts,
                hotspots,
                cabinBounds,
                movementVector,
                bump
        ) {
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
        open class SfcSimpleObject(
                classifier: AgentClass,
                unId: Int?,
                attr: Int,
                bounds: Bounds,
                actv: Int,
                currentSound: String,
                sprite: SfcGallery,
                tickReset: Int,
                tickState: Int,
                variables: List<Int>,
                size: Int?,
                threat: Int?,
                range: Int?,
                accelerationG: Int?,
                velocity: Vector2?,
                restitution: Int?,
                aero: Int?,
                gravityData: Int?,
                frozen: Boolean?,
                scripts: List<SfcScript>,
                val entity: SfcEntity
        ) : SfcObject(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite,
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
        ) {

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
                    entity
            )
            companion object
        } // End SfcSimpleObject


        /**
         * An SFC data class for the pointer agent in C1/C2
         */
        class SfcPointer(
                classifier: AgentClass,
                unId: Int?,
                attr: Int,
                bounds: Bounds,
                actv: Int,
                currentSound: String,
                sprite: SfcGallery,
                tickReset: Int,
                tickState: Int,
                variables: List<Int>,
                size: Int?,
                threat: Int?,
                range: Int?,
                accelerationG: Int?,
                velocity: Vector2?,
                restitution: Int?,
                aero: Int?,
                gravityData: Int?,
                frozen: Boolean?,
                scripts: List<SfcScript>,
                entity: SfcEntity
        ) : SfcSimpleObject(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite,
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
                entity
        ) {

            /**
             * Helper constructor to initialize with a parent base read in
             */
            constructor(baseObject: SfcSimpleObject) : this(
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
        class SfcCallButton(
                classifier: AgentClass,
                unId: Int?,
                attr: Int,
                bounds: Bounds,
                actv: Int,
                currentSound: String,
                sprite: SfcGallery,
                tickReset: Int,
                tickState: Int,
                variables: List<Int>,
                size: Int?,
                threat: Int?,
                range: Int?,
                accelerationG: Int?,
                velocity: Vector2?,
                restitution: Int?,
                aero: Int?,
                gravityData: Int?,
                frozen: Boolean?,
                scripts: List<SfcScript>,
                entity: SfcEntity,
                val ourLift: SfcLift,
                val liftId: Int
        ) : SfcSimpleObject(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite,
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
                entity
        ) {

            /**
             * Helper constructor to initialize with a parent base read in
             */
            constructor(
                    baseObject: SfcSimpleObject,
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
                classifier: AgentClass,
                unId: Int?,
                attr: Int,
                bounds: Bounds,
                actv: Int,
                currentSound: String,
                sprite: SfcGallery,
                tickReset: Int,
                tickState: Int,
                variables: List<Int>,
                size: Int?,
                threat: Int?,
                range: Int?,
                accelerationG: Int?,
                velocity: Vector2?,
                restitution: Int?,
                aero: Int?,
                gravityData: Int?,
                frozen: Boolean?,
                scripts: List<SfcScript>,
                entity: SfcEntity
        ) : SfcSimpleObject(
                classifier,
                unId,
                attr,
                bounds,
                actv,
                currentSound,
                sprite,
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
                entity
        ) {

            /**
             * Helper constructor to initialize with a parent base read in
             */
            constructor(baseObject: SfcSimpleObject) : this(
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
            )

            companion object
        } // End SfcScenery

        companion object
    } // End SfcObject

    /**
     * An SFC data object for Scripts in C1/C2
     */
    data class SfcScript(
            val classifier: AgentClass,
            val eventNumber: Int,
            val script: String
    ) : SfcData() {
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
    ) : SfcData() {
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
    ) : SfcData() {
        companion object
    }


    /**
     * SFC Door info object
     */
    data class SfcDoor(
            val openness: Int,
            val otherRoom: Int
    ) : SfcData() {
        companion object
    }

    /**
     * an SFC data class for rooms in C1/C2
     */
    open class SfcRoom(
            open val id: Int,
            open val bounds: Bounds,
            open val roomType: Int
    ) : SfcData() {
        /**
         * An C2 specific SFC room data object
         */
        data class SfcC2Room(
                override val id: Int,
                override val bounds: Bounds,
                override val roomType: Int,
                val doors: Map<Int,List<SfcDoor>>,
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
                val windVector:Vector2,
                val floorPoints: List<Vector2>,
                val music: String,
                val dropStatus: Int
        ) : SfcRoom(
                id = id,
                bounds = bounds,
                roomType = roomType
        ) {
            companion object
        }
    }

    /**
     * SFC map data object
     */
    data class SfcMapData(
            val gallery: SfcGallery,
            val rooms: List<SfcRoom>,
            val groundLevels: List<Int>? = null // Should be 261 points
    ) : SfcData() {
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
    ) : SfcData() {
        companion object
    }
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
    val size:Size by lazy {
        val width = max(left, right) - min(left, right)
        val height = max(top, bottom) - min(top, bottom)
        Size(width = width, height = height)
    }
}

val Bounds.creaturesOrigin get() = Vector2(left, max(bottom, top))

val Bounds.javaOrigin:Vector2 get() = Vector2(left, min(bottom, top))


data class Rect(
    val origin:Vector2,
    val size:Size
)

data class Size(
        val width:Int,
        val height:Int
)

data class Vector2(val x: Int, val y: Int) {
    companion object {
        val zero by lazy {
            Vector2(0,0)
        }
    }
}

data class SfcHotspot(
        val bounds: Bounds,
        val function: Int,
        val message: Int? = null,
        val mask: Int? = null
)