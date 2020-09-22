package com.badahori.creatures.plugins.intellij.agenteering.sfc.lang


import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import javax.swing.Icon


object SfcFileType : FileType {

    override fun getName(): String = "SFCFile"

    override fun getDescription(): String = "Creatures SFC File"

    override fun isBinary(): Boolean = true

    override fun isReadOnly(): Boolean = true

    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    override fun getIcon(): Icon? = CaosScriptIcons.SFC_FILE_ICON

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String? = Charsets.US_ASCII.name()

    @JvmStatic
    val DEFAULT_EXTENSION = "sfc"
}

val IS_SFC_USER_DATA_KEY:Key<Boolean> = object:KeyWithDefaultValue<Boolean>("com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.IS_SFC_VIRTUAL_FILE") {
    override fun getDefaultValue(): Boolean = false
}