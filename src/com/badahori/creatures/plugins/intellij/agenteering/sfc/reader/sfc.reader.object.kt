package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass
import com.badahori.creatures.plugins.intellij.agenteering.sfc.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.cString

internal fun SfcReader.readObject(): SfcObject {
    val family: Int
    val genus: Int
    val species: Int
    val unId: Int?
    if (variant == C1) {
        skip(1)
        species = uInt8
        genus = uInt8
        family = uInt8
        skip(1)
        unId = null
    } else {
        genus = uInt8
        family = uInt8
        assert(uInt16 == 0)
        species = uInt8
        unId = uInt32
        skip(1)
    }
    val classifier = AgentClass(family, genus, species)

    val attr = if (variant == C1) uInt8 else uInt16
    // Read unknown coordinates

    val bounds = Bounds(
            left = uInt32,
            top = uInt32,
            right = uInt32,
            bottom = uInt32
    )
    skip(2)
    val actv = uInt8
    val sprite = slurp(TYPE_CGALLERY) as SfcGallery
    val tickReset = uInt32
    val tickState = uInt32
    assert(tickReset >= tickState)
    assert(uInt16 == 0)
    val currentSound = byteBuffer.cString(4)
    val variables = (0 until (if (variant == C1) 3 else 100)).map {
        uInt32
    }
    if (variant == C1) {
        return SfcObjectImpl(
                classifier = classifier,
                attr = attr,
                actv = actv,
                bounds = bounds,
                currentSound = currentSound,
                sprite = sprite,
                tickReset = tickReset,
                tickState = tickState,
                variables = variables,
                scripts = readScripts()
        )
    }
    val size = uInt8
    val range = uInt8
    val gravityData = uInt32
    val accg = uInt32
    val velocity = Vector2(uInt32, uInt32)
    val restitution = uInt32
    val aero = uInt32
    skip(6)
    val threat = uInt8
    val flags = uInt8
    val frozen = flags and 0x02 != 0
    return SfcObjectImpl(
            classifier = classifier,
            unId = unId,
            attr = attr,
            bounds = bounds,
            actv = actv,
            currentSound = currentSound,
            sprite = sprite,
            tickReset = tickReset,
            tickState = tickState,
            variables = variables,
            size = size,
            threat = threat,
            range = range,
            accelerationG = accg,
            velocity = velocity,
            restitution = restitution,
            aero = aero,
            gravityData = gravityData,
            frozen = frozen,
            scripts = readScripts()
    )
}

internal fun SfcReader.readScripts(): List<SfcScript> {
    return (0 until uInt32).map {
        readScript()
    }
}