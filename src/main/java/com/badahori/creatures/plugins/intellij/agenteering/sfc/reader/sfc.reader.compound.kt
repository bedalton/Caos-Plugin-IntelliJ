package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.sfc.Bounds
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcHotspot
import com.badahori.creatures.plugins.intellij.agenteering.sfc.Vector2
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.Ptr.SfcEntityPtr


internal fun SfcReader.readCompoundObject() : PointerSfcCompoundObject<*> {

    val base = readObject()
    val numberOfParts = uInt32()
    val parts = (0 until numberOfParts).map {i ->
        val entity = readClass(SfcType.ENTITY) as? SfcEntityPtr
        if (entity == null) {
            assert(i != 0)
            skip(8)
        }
        if (i == 0) {
            assert(entity?.pointed?.relativePosition == Vector2.zero) { "Failed to assert that relative position of entity 1 is (0,0)"}
        }
        entity
    }
    val hotspots = readHotspots()
    return PointerSfcCompoundObjectImpl(
            baseObject = base,
            parts = parts,
            hotspots = hotspots
    )

}

internal fun SfcReader.readHotspots() : List<SfcHotspot> {

    val indices = 0 until 6

    val bounds = indices.map {
        Bounds(
                left = uInt32(),
                top = uInt32(),
                right = uInt32(),
                bottom = uInt32()
        )
    }

    val functions = indices.map{
        uInt32()
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
        uInt16().apply { skip(2) }
    }
    val masks = (0 until 6).map {
        uInt8()
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