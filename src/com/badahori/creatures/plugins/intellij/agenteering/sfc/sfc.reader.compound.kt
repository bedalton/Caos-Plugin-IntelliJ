package com.badahori.creatures.plugins.intellij.agenteering.sfc

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcData.SfcEntity
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcData.SfcObject.SfcCompoundObject


internal fun SfcReader.readCompoundObject() : SfcCompoundObject {

    val base = readObject()
    val numberOfParts = uInt32
    val parts = (0 until numberOfParts).map {i ->
        val entity = slurp(TYPE_ENTITY) as? SfcEntity
        if (entity == null) {
            assert(i != 0)
            skip(8)
        }
        if (i == 0) {
            assert(entity?.relativePosition == Vector2.zero)
        }
        entity
    }
    val hotspots = readHotspots()
    return SfcCompoundObject(
            baseObject = base,
            parts = parts,
            hotspots = hotspots
    )

}

internal fun SfcReader.readHotspots() : List<SfcHotspot> {

    val indices = 0 until 6

    val bounds = indices.map {
        Bounds(
                left = uInt32,
                top = uInt32,
                right = uInt32,
                bottom = uInt32
        )
    }

    val functions = indices.map{
        uInt32
    }
    if (variant == C1) {
        return indices.map {i ->
            SfcHotspot(
                bounds = bounds[i],
                function = functions[i]
            )
        }
    }
    val messages = (0 until 6).map {
        uInt16
    }
    val masks = (0 until 6).map {
        uInt8
    }
    return indices.map {i ->
        SfcHotspot(
                bounds = bounds[i],
                function = functions[i],
                message = messages[i],
                mask = masks[i]
        )
    }

}