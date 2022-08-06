package com.badahori.creatures.plugins.intellij.agenteering.sprites.photoalbum


import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import javax.swing.Icon

/**
 * Sprite file type for CV+
 * Is compressed version of S16 files
 */
object PhotoAlbumFileType : FileType {

    override fun getName(): String = "C1 Photo Album File"

    override fun getDescription(): String = "Creatures 1 Photo album file"

    override fun isBinary() = true

    override fun isReadOnly(): Boolean = true

    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    override fun getIcon(): Icon? = CaosScriptIcons.PHOTO_ALBUM_FILE_ICON

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String? = Charsets.UTF_8.name()

    @JvmStatic
    val DEFAULT_EXTENSION = "Photo Album"
}