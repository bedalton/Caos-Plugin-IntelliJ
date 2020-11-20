package com.badahori.creatures.plugins.intellij.agenteering.sprites.spr


import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import javax.swing.Icon

/**
 * Creatures 1 sprite file
 */
object SprFileType : FileType {
    override fun getName(): String {
        return "SPR file"
    }

    override fun getDescription(): String {
        return "Creatures 1 Sprite File"
    }

    override fun isBinary(): Boolean {
        return true
    }

    override fun isReadOnly(): Boolean {
        return true
    }

    override fun getDefaultExtension(): String {
        return DEFAULT_EXTENSION
    }

    override fun getIcon(): Icon? = CaosScriptIcons.SPR_FILE_ICON

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String? {
        return Charsets.UTF_8.name()
    }

    @JvmStatic
    val DEFAULT_EXTENSION = "spr"
}