package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcGallery

internal suspend fun SfcReader.readGallery() : SfcGallery {
    val numberOfFrames = uInt32()
    val fileName = fileNameToken()
    val firstImage = uInt32()
    skip(4)
    skip(15 * numberOfFrames)
    return SfcGallery(
            numberOfFrames = numberOfFrames,
            firstImage = firstImage,
            fileName = fileName
    )
}