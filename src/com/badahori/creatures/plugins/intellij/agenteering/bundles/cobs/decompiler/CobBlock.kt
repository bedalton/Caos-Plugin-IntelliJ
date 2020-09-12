package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.AgentBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16SpriteFile
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.util.*

sealed class CobFileData {
    data class C2CobData(private val cobBlocks:List<CobBlock>) : CobFileData() {
        val agentBlocks by lazy { cobBlocks.filterIsInstance(CobBlock.AgentBlock::class.java) }
        val authorBlocks by lazy { cobBlocks.filterIsInstance(CobBlock.AuthorBlock::class.java) }
        private val fileBlocks by lazy { cobBlocks.filterIsInstance<CobBlock.FileBlock>() }
        val spriteFileBlocks = fileBlocks.filterIsInstance<CobBlock.FileBlock.SpriteBlock>()
        val soundFileBlocks = fileBlocks.filterIsInstance<CobBlock.FileBlock.SoundBlock>()
    }
    data class C1CobData(val cobBlock: AgentBlock) : CobFileData()
}

sealed class CobBlock {
    data class AgentBlock(
            val format: CobFormat,
            val name: String,
            val description: String,
            val lastUsageDate: Int? = null,
            val useInterval: Int? = null,
            val quantityAvailable: Int,
            val quantityUsed: Int? = null,
            val expiry: Calendar,
            val dependencies: List<CobDependency> = emptyList(),
            val installScript: AgentScript.InstallScript,
            val removalScript: AgentScript.RemovalScript? = null,
            val eventScripts: List<AgentScript>,
            val image: BufferedImage?
    ) : CobBlock()

    data class AuthorBlock(
            val creationDate:Calendar,
            val authorName: String,
            val authorEmail:String?,
            val version:Int,
            val revision:Int,
            val authorUrl:String?,
            val authorComments:String?
    ) : CobBlock()

    sealed class FileBlock(
            val fileType: CobFileBlockType,
            val fileName: String,
            val reserved: Int,
            val contents: ByteArray
    ) : CobBlock() {
        class SpriteBlock(
                fileName: String,
                reserved: Int,
                contents: ByteArray
        ) : FileBlock(CobFileBlockType.SPRITE, fileName, reserved, contents) {
            private val sprite: S16SpriteFile by lazy {
                S16SpriteFile(contents)
            }
        }

        class SoundBlock(
                fileName: String,
                reserved: Int,
                contents: ByteArray
        ) : FileBlock(CobFileBlockType.SOUND, fileName, reserved, contents)
    }

    data class UnknownCobBlock(val type:String, val contents: ByteArray) : CobBlock()

}

data class CobDependency(val type:CobFileBlockType, val fileName:String)

enum class CobFileBlockType {
    SOUND,
    SPRITE
}

enum class CobFormat(val variant: CaosVariant) {
    C1(CaosVariant.C1),
    C2(CaosVariant.C2)
}