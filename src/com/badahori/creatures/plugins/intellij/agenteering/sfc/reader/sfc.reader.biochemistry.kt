package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcBiochemistry

internal fun SfcReader.readBiochemistry() : SfcBiochemistry {
    // Only C1 variant has been checked at all
    assert (variant == CaosVariant.C1)
    skip(14)
    val chemicals = (0 until 255).map {
        uInt8.apply { skip(1) }
    }
    return SfcBiochemistry(chemicals = chemicals)
}