package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.bedalton.io.bytes.*
import com.bedalton.creatures.sprite.parsers.parseS16FrameAtCurrentPosition
import com.bedalton.creatures.sprite.util.ColorEncoding
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScriptType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.transparentBlack
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import korlibs.image.awt.toAwt
import java.util.*


internal suspend fun ByteStreamReaderEx.readC2CobBlock() : CobBlock? {
    val type = try {
        string(4)
    } catch (e: Exception) {
        return null
    }
    val size = uInt32()
    return when (type) {
        "agnt" -> readC2AgentBlock()
        "auth" -> readC2AuthorBlock()
        "file" -> readC2FileBlock()
        else -> throw Exception("Encountered BAD C2 COB Block. Type: $type; Size: $size")
    }
}

private suspend fun ByteStreamReaderEx.readC2AgentBlock() : CobBlock.AgentBlock {
    val quantityAvailable = uInt16().let { if (it == 0xffff) -1 else it }
    val lastUsageDate = uInt32()
    val reuseInterval = uInt32()
    val expiryDay = uInt8()
    val expiryMonth = uInt8()
    val expiryYear = uInt16()
    val expiry = Calendar.getInstance().apply {
        set (expiryYear, expiryMonth, expiryDay, 0, 0, 0)
    }
    skip(12) // Reserved?
    val agentName = cString()
    val description = cString()
    val installScript = AgentScript.InstallScript(cString())
    val removalScript = cString().nullIfEmpty()?.let { AgentScript.RemovalScript(it) }
    val eventScripts = (0 until uInt16()).map {index ->
        val script = cString()
        AgentScript(script, "Script $index", AgentScriptType.EVENT)
    }
    val dependencies = (0 until uInt16()).map {
        val type = if (uInt16() == 0) CobFileBlockType.SPRITE else CobFileBlockType.SOUND
        val name = cString()
        CobDependency(type, name)
    }

    val thumbWidth = uInt16()
    val thumbHeight = uInt16()
    val image = if (thumbWidth > 0 && thumbHeight > 0)
        parseS16FrameAtCurrentPosition(this, thumbWidth, thumbHeight, ColorEncoding.X_565, transparentBlack).toAwt()
    else
        null
    skip(thumbWidth * thumbHeight * 2)
    return CobBlock.AgentBlock(
            format = CobFormat.C2,
            name = agentName,
            description = description,
            image = image,
            quantityAvailable = quantityAvailable,
            quantityUsed = null,
            expiry = expiry,
            lastUsageDate = lastUsageDate.toInt(),
            useInterval = reuseInterval.toInt(),
            eventScripts = eventScripts,
            installScripts = listOf(installScript),
            removalScript = removalScript,
            dependencies = dependencies
    )
}

private suspend fun ByteStreamReaderEx.readC2AuthorBlock() : CobBlock.AuthorBlock {
    val creationDay = uInt8()
    val creationMonth = uInt8()
    val creationYear = uInt16()
    val creationDate = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(creationYear, creationMonth, creationDay, 0, 0, 0)
    }
    val version = uInt8()
    val revision = uInt8()
    val authorName = cString()
    val authorEmail = cString()
    val authorUrl = cString()
    val authorComment = cString()
    return CobBlock.AuthorBlock(
            creationDate = creationDate,
            version = version,
            revision = revision,
            authorName = authorName,
            authorEmail = authorEmail,
            authorUrl = authorUrl,
            authorComments = authorComment
    )
}

private suspend fun ByteStreamReaderEx.readC2FileBlock() : CobBlock.FileBlock {
    val type = when (val typeInt = uInt16()) {
        0 -> CobFileBlockType.SPRITE
        1 -> CobFileBlockType.SOUND
        else -> throw Exception("Invalid file block type '$typeInt'")
    }
    val reserved = uInt32().toInt()
    val size = uInt32()
    val fileName = cString()
    val contents = bytes(size)
    return if (type == CobFileBlockType.SPRITE)
        CobBlock.FileBlock.SpriteBlock(
                fileName = fileName,
                reserved = reserved,
                contents = contents
        )
    else
        CobBlock.FileBlock.SoundBlock(
                fileName = fileName,
                reserved = reserved,
                contents = contents
        )
}
