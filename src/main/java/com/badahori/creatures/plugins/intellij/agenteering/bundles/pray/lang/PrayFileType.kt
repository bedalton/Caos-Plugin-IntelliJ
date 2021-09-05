package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang


import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IFileElementType
import icons.CaosScriptIcons
import javax.swing.Icon

/**
 * PRAY Body Data file type for the Creatures games
 */
object PrayFileType : LanguageFileType(PrayLanguage) {

    override fun getName(): String = "PRAYFile"

    override fun getDescription(): String = "Creatures PRAY File"

    override fun isReadOnly(): Boolean = false

    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    override fun getIcon(): Icon? = CaosScriptIcons.PRAY_FILE_ICON

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String = Charsets.US_ASCII.name()

    @JvmStatic
    val DEFAULT_EXTENSION = "ps"
}

object PrayFileElementType : IFileElementType("PRAY.File", PrayLanguage)