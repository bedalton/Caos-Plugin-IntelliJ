package com.badahori.creatures.plugins.intellij.agenteering.att.lang


import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IFileElementType
import javax.swing.Icon

/**
 * ATT Body Data file type for the Creatures games
 */
object AttFileType : FileType {
    override fun getName(): String = "ATTFile"

    override fun getDescription(): String = "Creatures ATT File"

    override fun isBinary() = false

    override fun isReadOnly(): Boolean = false

    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    override fun getIcon(): Icon? = null

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String = Charsets.US_ASCII.name()

    @JvmStatic
    val DEFAULT_EXTENSION = "att"
}

object AttFileElementType : IFileElementType("ATT.File", AttLanguage)