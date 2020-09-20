package com.badahori.creatures.plugins.intellij.agenteering.sfc

import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcData.SfcGallery

internal fun SfcReader.readGallery() : SfcGallery {
    val numberOfFrames = uInt32
    val fileName = fileNameToken
    val firstImage = uInt32
    skip(4)
    skip (15 * numberOfFrames) // Unknown(3) width(4) height(4) offset(4)
    return SfcGallery(
            numberOfFrames = numberOfFrames,
            firstImage = firstImage,
            fileName = fileName
    )
}