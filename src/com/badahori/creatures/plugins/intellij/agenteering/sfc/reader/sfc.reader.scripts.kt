package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.PointerSfcMacro
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass
import com.badahori.creatures.plugins.intellij.agenteering.sfc.*
import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.Ptr.*
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcScript


internal fun SfcReader.readScript(): SfcScript {
    if (!variant.isOld) {
        throw OutOfVariantException(variant)
    }
    val family:Int
    val genus:Int
    val species:Int
    val eventNumber:Int
    if (variant == C1) {
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
    return SfcScript(
            classifier = classifier,
            eventNumber = eventNumber,
            script = script
    )
}

internal fun SfcReader.readMacro() : PointerSfcMacro {
    skip(12)
    val script = sfcString
    if (variant == C1) {
        skip  (128)
    } else
        skip(488)
    val owner = readClass(SfcType.OBJECT) as SfcObjectPtr
    val from = readClass(SfcType.OBJECT) as SfcObjectPtr
    val targ = readClass(SfcType.OBJECT) as? SfcObjectPtr
    variantSkip(18, 34)
    return PointerSfcMacro(
            script = script,
            ownr = owner,
            from = from,
            targ = targ
    )
}