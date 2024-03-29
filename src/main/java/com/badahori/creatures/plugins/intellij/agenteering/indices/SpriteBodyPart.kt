package com.badahori.creatures.plugins.intellij.agenteering.indices

import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileData
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileParser
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteFileHolder
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking


data class SpriteBodyPart(
    val sprite: SpriteFileHolder,
    val bodyData: AttFileData
)

data class BodyPartFiles(
    val spriteFile:VirtualFile,
    val bodyDataFile:VirtualFile
) {

    val baseName:String by lazy {
        spriteFile.nameWithoutExtension.lowercase()
    }

    val key by lazy {
        BreedPartKey.fromFileName(
            spriteFile.nameWithoutExtension
        )
    }

    val part: Char? get() = key?.part

    val genus: Int? get() = key?.genus

    val age: Int? get() = key?.ageGroup

    val gender: Int? get() = key?.gender

    fun data(project:Project): SpriteBodyPart {
        val sprite = runBlocking { SpriteParser.parse(spriteFile, bodyPart = true) }
        return SpriteBodyPart(
            sprite = sprite,
            bodyData = runReadAction { AttFileParser.parse(project, bodyDataFile) }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BodyPartFiles)
            return false
        return spriteFile.path == other.spriteFile.path && bodyDataFile.path == other.bodyDataFile.path
    }

    override fun hashCode(): Int {
        var result = spriteFile.path.hashCode()
        result = 31 * result + bodyDataFile.path.hashCode()
        return result
    }

}