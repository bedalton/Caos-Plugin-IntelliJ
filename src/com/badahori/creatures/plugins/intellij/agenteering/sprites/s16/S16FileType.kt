package com.badahori.creatures.plugins.intellij.agenteering.sprites.s16


import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon


class S16FileType private constructor() : FileType {
    override fun getName(): String {
        return "S16 Sprite File"
    }

    override fun getDescription(): String {
        return "Creatures S16 Sprite File"
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
        val INSTANCE = S16FileType()
        @JvmStatic
        val DEFAULT_EXTENSION = "s16"
    }
}