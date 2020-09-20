package com.badahori.creatures.plugins.intellij.agenteering.sfc

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass


internal fun SfcReader.readScript(): SfcData.SfcScript {
    if (!variant.isOld) {
        throw OutOfVariantException(variant)
    }
    val family:Int
    val genus:Int
    val species:Int
    val eventNumber:Int
    if (variant == CaosVariant.C1) {
        eventNumber = uInt8
        species = uInt8
        genus = uInt8
        family = uInt8
    } else {
        genus = uInt8
        family = uInt8
        eventNumber = uInt16
        species = uInt16
    }
    val classifier = AgentClass(family, genus, species)
    val script = sfcString
    return SfcData.SfcScript(
            classifier = classifier,
            eventNumber = eventNumber,
            script = script
    )
}