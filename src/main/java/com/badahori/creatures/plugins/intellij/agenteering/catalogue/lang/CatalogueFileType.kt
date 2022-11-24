package com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang


import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IFileElementType
import icons.CaosScriptIcons
import javax.swing.Icon

/**
 * ATT Body Data file type for the Creatures games
 */
object CatalogueFileType : LanguageFileType(AttLanguage) {
    override fun getName(): String = "ATTFile"

    override fun getDescription(): String = "Creatures CATALOGUE File"

    override fun isReadOnly(): Boolean = false

    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    override fun getIcon(): Icon? = CaosScriptIcons.ATT_FILE_ICON

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String = Charsets.US_ASCII.name()

    @JvmStatic
    val DEFAULT_EXTENSION = "att"

}

object CatalogueFileElementType : IFileElementType("CATALOGUE.File", CatalogueLanguage)