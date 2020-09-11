package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScriptType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16SpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.ColorEncoding
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import java.nio.ByteBuffer
import java.util.*


internal fun ByteBuffer.readC2CobBlock() : CobBlock? {
    val type = try {
        cString(4)
    } catch (e: Exception) {
        return null
    }
    val size = uInt16
    return when (type) {
        "agnt" -> readC2AgentBlock()
        "auth" -> readC2AuthorBlock()
        "file" -> readC2FileBlock()
        else -> CobBlock.UnknownCobBlock(type, bytes(size))
    }
}

private fun ByteBuffer.readC2AgentBlock() : CobBlock.AgentBlock {
    val quantityAvailable = uInt8.let { if (it == 0xffff) -1 else it }
    val lastUsageDate = uInt16
    val reuseInterval = uInt16
    val expiryDay = byte
    val expiryMonth = byte
    val expiryYear = uInt8
    val expiry = Calendar.getInstance().apply {
        set (expiryYear, expiryMonth, expiryDay, 0, 0, 0)
    }
    skip(12)
    val agentName = cString
    val description = cString
    val installScript = AgentScript.InstallScript(cString)
    val removalScript = AgentScript.RemovalScript(cString)
    val eventScripts = (0 until uInt8).map {index ->
        AgentScript(cString, "Script $index", AgentScriptType.EVENT)
    }
    val dependencies = (0 until uInt8).map {
        val type = if (uInt8 == 0) CobFileBlockType.SPRITE else CobFileBlockType.SOUND
        val name = cString
        CobDependency(type, name)
    }

    val thumbWidth = uInt8
    val thumbHeight = uInt8
    val image = S16SpriteFrame(this, position().toLong(), thumbWidth, thumbHeight, ColorEncoding.x565).image
    skip((thumbWidth * thumbHeight * 0.5).toInt())
    return CobBlock.AgentBlock(
            format = CobFormat.C2,
            name = agentName,
            description = description,
            image = image,
            quantityAvailable = quantityAvailable,
            quantityUsed = null,
            expiry = expiry,
            lastUsageDate = lastUsageDate,
            useInterval = reuseInterval,
            eventScripts = eventScripts,
            installScript = installScript,
            removalScript = removalScript,
            dependencies = dependencies
    )
}

private fun ByteBuffer.readC2AuthorBlock() : CobBlock.AuthorBlock {
    val creationDay = byte
    val creationMonth = byte
    val creationYear = uInt8
    val creationDate = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(creationYear, creationMonth, creationDay, 0, 0, 0)
    }
    val version = byte
    val revision = byte
    val authorName = cString
    val authorEmail = cString
    val authorUrl = cString
    val authorComment = cString
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

private fun ByteBuffer.readC2FileBlock() : CobBlock.FileBlock {
    val type = if (uInt8 == 0) CobFileBlockType.SPRITE else CobFileBlockType.SOUND
    val reserved = uInt16
    val size = uInt16
    val fileName = cString
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
