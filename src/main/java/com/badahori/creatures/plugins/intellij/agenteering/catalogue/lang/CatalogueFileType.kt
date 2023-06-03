package com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang


import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import javax.swing.Icon

/**
 * Catalogue file type for the Creatures games
 */
object CatalogueFileType : LanguageFileType(CatalogueLanguage) {
    override fun getName(): String = "Catalogue"

    override fun getDescription(): String = "Creatures CATALOGUE File"

    override fun isReadOnly(): Boolean = false

    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    override fun getIcon(): Icon? = CaosScriptIcons.CATALOGUE_FILE_ICON

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String = "windows-1252"

    @JvmStatic
    val DEFAULT_EXTENSION = "catalogue"

}
