package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScriptType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16SpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.ColorEncoding
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.vfs.VfsUtil
import java.io.File
import java.nio.ByteBuffer
import java.util.*


internal fun ByteBuffer.readC2CobBlock() : CobBlock? {
    val type = try {
        cString(4)
    } catch (e: Exception) {
        return null
    }
    val size = uInt32
    return when (type) {
        "agnt" -> readC2AgentBlock()
        "auth" -> readC2AuthorBlock()
        "file" -> readC2FileBlock()
        else -> {
            LOGGER.severe("Encountered BAD C2 COB Block. Type: $type; Size: $size")
            CobBlock.UnknownCobBlock(type, bytes(size))
        }
    }
}

private fun ByteBuffer.readC2AgentBlock() : CobBlock.AgentBlock {
    val quantityAvailable = uInt16.let { if (it == 0xffff) -1 else it }
    LOGGER.info("QuantityAvailable: $quantityAvailable")
    val lastUsageDate = uInt32
    val reuseInterval = uInt32
    LOGGER.info("LastUsage: $lastUsageDate; ReuseInterval: $reuseInterval")
    val expiryDay = uInt8
    val expiryMonth = uInt8
    val expiryYear = uInt16
    LOGGER.info("Expiry: $expiryMonth/$expiryDay/$expiryYear")
    val expiry = Calendar.getInstance().apply {
        set (expiryYear, expiryMonth, expiryDay, 0, 0, 0)
    }
    skip(12) // Reserved?
    val agentName = cString
    LOGGER.info("AgentName: $agentName")
    val description = cString
    LOGGER.info("Description: $description")
    val installScript = AgentScript.InstallScript(cString)
    LOGGER.info("InstallScript: ${installScript.code}")
    val removalScript = AgentScript.RemovalScript(cString)
    LOGGER.info("InstallScript: ${removalScript.code}")
    val eventScripts = (0 until uInt16).map {index ->
        val script = cString
        LOGGER.info("EventScript: $index: $script")
        AgentScript(script, "Script $index", AgentScriptType.EVENT)
    }
    val dependencies = (0 until uInt16).map {
        val type = if (uInt16 == 0) CobFileBlockType.SPRITE else CobFileBlockType.SOUND
        val name = cString
        LOGGER.info("Dependencies: $type, $name")
        CobDependency(type, name)
    }

    val thumbWidth = uInt16
    val thumbHeight = uInt16
    LOGGER.info("ThumbSize: ${thumbWidth}x$thumbHeight")
    val image = S16SpriteFrame(this, position().toLong(), thumbWidth, thumbHeight, ColorEncoding.X_565).image
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
            installScript = installScript,
            removalScript = removalScript,
            dependencies = dependencies
    )
}

private fun ByteBuffer.readC2AuthorBlock() : CobBlock.AuthorBlock {
    val creationDay = uInt8
    val creationMonth = uInt8
    val creationYear = uInt16
    val creationDate = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(creationYear, creationMonth, creationDay, 0, 0, 0)
    }
    val version = uInt8
    val revision = uInt8
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
    val type = if (uInt16 == 0) CobFileBlockType.SPRITE else CobFileBlockType.SOUND
    val reserved = uInt32.toInt()
    val size = uInt32
    val fileName = cString
    LOGGER.info("File: ${type.name}; FileName: $fileName; Size: $size")
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
