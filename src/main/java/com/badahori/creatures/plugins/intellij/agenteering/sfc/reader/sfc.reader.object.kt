package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C2
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass
import com.badahori.creatures.plugins.intellij.agenteering.sfc.*
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.Ptr.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty

internal fun SfcReader.readObject(): PointerSfcObject<*> {
    val family: Int
    val genus: Int
    val species: Int
    val unId: Int?
    if (variant == C1) {
        skip(1)
        species = uInt8
        genus = uInt8
        family = uInt8
        unId = null
    } else {
        genus = uInt8
        family = uInt8
        assert(uInt16 == 0)
        species = uInt16
        unId = uInt32
    }
    skip(1)
    val classifier = AgentClass(family, genus, species)
    val attr = if (variant == C1) uInt8 else uInt16
    // Read unknown coordinates
    if (variant == C2)
        assert (uInt16 == 0)
    val bounds = bounds
    skip(2)
    val actv = uInt8
    val sprite = readClass(SfcType.GALLERY) as SfcGalleryPtr
    val tickReset = uInt32
    val tickState = uInt32
    assert(tickReset >= tickState)
    assert (uInt16 == 0)
    val currentSound = string(4).nullIfEmpty()
    val numberOfObjectVariables = if (variant == C1) 3 else 100
    val variables = (0 until numberOfObjectVariables).map {
        uInt32
    }
    if (variant == C1) {
        return PointerSfcObjectImpl(
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
    val range = uInt32
    val gravityData = uInt32
    val accg = uInt32
    val velocity = Vector2(uInt32, uInt32)
    val restitution = uInt32
    val aero = uInt32
    skip(6)
    val threat = uInt8
    val flags = uInt8
    val frozen = flags and 0x02 != 0
    return PointerSfcObjectImpl(
            classifier = classifier,
            accelerationG = accg,
            actv = actv,
            aero = aero,
            attr = attr,
            bounds = bounds,
            currentSound = currentSound,
            frozen = frozen,
            gravityData = gravityData,
            range = range,
            restitution = restitution,
            scripts = readScripts(),
            size = size,
            sprite = sprite,
            threat = threat,
            tickReset = tickReset,
            tickState = tickState,
            unId = unId,
            variables = variables,
            velocity = velocity
    )
}

internal fun SfcReader.readScripts(): List<SfcScript> {
    return (0 until uInt32).map {
        readScript()
    }
}