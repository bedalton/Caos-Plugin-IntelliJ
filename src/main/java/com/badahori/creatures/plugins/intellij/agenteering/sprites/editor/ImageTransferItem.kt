package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.badahori.creatures.plugins.intellij.agenteering.sprites.toPngByteArray
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files

interface HasImage {
    val image:BufferedImage
}

internal data class ImageTransferItem(internal val fileName:String, override val image: BufferedImage) : HasImage {
    internal val file: File by lazy {
        val tempDirectory = Files.createTempDirectory(null).toFile()
        File(tempDirectory.path + File.separator + fileName).apply {
            if (!exists()) {writeBytes(image.toPngByteArray())
                createNewFile()
            }
        }
    }
}
