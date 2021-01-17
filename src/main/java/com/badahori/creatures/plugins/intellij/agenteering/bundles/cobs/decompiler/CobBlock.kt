package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.AgentBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16SpriteFile
import java.awt.image.BufferedImage
import java.util.*

sealed class CobFileData {
    data class C2CobData(val blocks:List<CobBlock>) : CobFileData() {
        val agentBlocks by lazy { blocks.filterIsInstance(AgentBlock::class.java) }
        val authorBlocks by lazy { blocks.filterIsInstance(CobBlock.AuthorBlock::class.java) }
        private val fileBlocks by lazy { blocks.filterIsInstance<CobBlock.FileBlock>() }
        val spriteFileBlocks = fileBlocks.filterIsInstance<CobBlock.FileBlock.SpriteBlock>()
        val soundFileBlocks = fileBlocks.filterIsInstance<CobBlock.FileBlock.SoundBlock>()
        override val variant: CaosVariant get() = CaosVariant.C2
    }
    data class C1CobData(val cobBlock: AgentBlock) : CobFileData() {
        override val variant: CaosVariant get() = CaosVariant.C1
    }
    data class InvalidCobData(val message:String) : CobFileData() {
        override val variant: CaosVariant get() = CaosVariant.UNKNOWN
    }
    abstract val variant: CaosVariant
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
            val installScript: AgentScript.InstallScript? = null,
            val removalScript: AgentScript.RemovalScript? = null,
            val eventScripts: List<AgentScript>,
            val image: BufferedImage?
    ) : CobBlock() {
        val expiresYear by lazy {
            expiry.get(Calendar.YEAR)
        }
        val expiresMonth by lazy {
            expiry.get(Calendar.MONTH) + 1
        }
        val expiresDay by lazy {
            expiry.get(Calendar.DAY_OF_MONTH) + 1
        }
    }

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
            val sprite: S16SpriteFile by lazy {
                S16SpriteFile(contents)
            }
        }

        class SoundBlock(
                fileName: String,
                reserved: Int,
                contents: ByteArray
        ) : FileBlock(CobFileBlockType.SOUND, fileName, reserved, contents)
    }

    data class UnknownCobBlock(val type:String, val contents: ByteArray) : CobBlock() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UnknownCobBlock) return false

            if (type != other.type) return false
            if (!contents.contentEquals(other.contents)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + contents.contentHashCode()
            return result
        }
    }

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