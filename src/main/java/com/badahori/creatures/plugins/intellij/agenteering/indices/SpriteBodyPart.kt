package com.badahori.creatures.plugins.intellij.agenteering.indices

import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileData
import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileParser
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteFile
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem


data class SpriteBodyPart(
    val sprite: SpriteFile<*>,
    val bodyData: AttFileData
)

data class BodyPartFiles(
    val spriteFile:VirtualFile,
    val bodyDataFile:VirtualFile
) {

    val baseName:String by lazy {
        spriteFile.nameWithoutExtension.toLowerCase()
    }

    val key by lazy {
        BreedPartKey.fromFileName(
            bodyDataFile.nameWithoutExtension
        )
    }
    fun data(project:Project): SpriteBodyPart {
        return SpriteBodyPart(
            sprite = SpriteParser.parse(spriteFile),
            bodyData = AttFileParser.parse(project, bodyDataFile)
        )
    }
}