package com.badahori.creatures.plugins.intellij.agenteering.sprites.s16


import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * Sprite file type origination in Creatures 2
 * Also can be used in C2+
 */
object S16FileType : FileType {
    override fun getName(): String =  "S16 Sprite File"

    override fun getDescription(): String = "Creatures S16 Sprite File"

    override fun isBinary(): Boolean = true

    override fun isReadOnly(): Boolean = true

    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    override fun getIcon(): Icon? = null

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String? = Charsets.UTF_8.name()

    @JvmStatic
    val DEFAULT_EXTENSION = "s16"
}