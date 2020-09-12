package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.lang


import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon


object CobFileType : FileType {
    override fun getName(): String = "COBFile"

    override fun getDescription(): String = "Creatures COB File"

    override fun isBinary(): Boolean = true

    override fun isReadOnly(): Boolean = true

    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    override fun getIcon(): Icon? = null

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String? = Charsets.UTF_8.name()

    @JvmStatic
    val DEFAULT_EXTENSION = "cob"
}