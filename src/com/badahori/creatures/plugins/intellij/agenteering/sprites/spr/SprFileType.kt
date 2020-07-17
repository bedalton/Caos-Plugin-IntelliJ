package com.badahori.creatures.plugins.intellij.agenteering.sprites.spr


import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon


class SprFileType private constructor() : FileType {
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

    override fun getIcon(): Icon? {
        return null
    }

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String? {
        return Charsets.UTF_8.name()
    }

    companion object {
        @JvmStatic
        val INSTANCE = SprFileType()
        @JvmStatic
        val DEFAULT_EXTENSION = "spr"
    }
}