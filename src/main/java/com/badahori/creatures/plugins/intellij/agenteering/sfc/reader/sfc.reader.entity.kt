package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.Ptr.SfcGalleryPtr


internal fun SfcReader.readEntity(): PointerSfcEntity {

    val sprite = readClass(SfcType.GALLERY) as SfcGalleryPtr
    val currentFrame = uInt8()
    val imageOffset = uInt8()
    val zOrder = uInt32()
    val position = vector2()
    val animationByte = uInt8()
    var frame: Int? = null
    var animationString: String? = null
    if (animationByte > 0) {
        assert(animationByte == 1) { "Animation byte has value other than 0 or 1" }
        frame = uInt8()
        animationString = if (variant == C1)
            string(32)
        else
            string(99)
        animationString = animationString.split(0x00.toChar()).first()
    }
    if (readingScenery) {
        return PointerSfcEntity(
                gallery = sprite,
                currentFrame = currentFrame,
                imageOffset = imageOffset,
                zOrder = zOrder,
                position = position,
                animationFrame = frame,
                animationString = animationString
        )
    }

    if (readingCompoundObject) {
        return PointerSfcEntity(
                gallery = sprite,
                currentFrame = currentFrame,
                imageOffset = imageOffset,
                zOrder = zOrder,
                position = position,
                animationFrame = frame,
                animationString = animationString,
                relativePosition = vector2()
        )
    }

    val partZOrder = uInt32()
    val behaviorClicks = (0 until 3).map { uInt8() }
    val behaviorTouch = uInt8()
    if (variant == C1) {
        return PointerSfcEntity(
                gallery = sprite,
                currentFrame = currentFrame,
                imageOffset = imageOffset,
                zOrder = zOrder,
                position = position,
                animationFrame = frame,
                animationString = animationString,
                behaviorClicks = behaviorClicks,
                behaviorTouch = behaviorTouch,
                partZOrder = partZOrder
        )
    }

    val pickupHandles = (0 until uInt16()).map {
        vector2()
    }
    val pickupPoints = (0 until uInt16()).map {
        vector2()
    }


    return PointerSfcEntity(
            gallery = sprite,
            currentFrame = currentFrame,
            imageOffset = imageOffset,
            zOrder = zOrder,
            position = position,
            animationFrame = frame,
            animationString = animationString,
            behaviorClicks = behaviorClicks,
            behaviorTouch = behaviorTouch,
            pickupHandles = pickupHandles,
            pickupPoints = pickupPoints,
            partZOrder = partZOrder
    )
}