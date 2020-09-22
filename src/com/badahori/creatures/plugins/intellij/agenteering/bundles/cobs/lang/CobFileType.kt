package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.lang


import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon


object CobFileType : FileType {

    override fun getName(): String = "COBFile"

    override fun getDescription(): String = "Creatures COB File"

    override fun isBinary(): Boolean = true

    override fun isReadOnly(): Boolean = true

    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    override fun getIcon(): Icon? = null

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String? = Charsets.US_ASCII.name()

    @JvmStatic
    val DEFAULT_EXTENSION = "cob"
}

val IS_COB_USER_DATA_KEY:Key<Boolean> = object:KeyWithDefaultValue<Boolean>("com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.IS_COB_VIRTUAL_FILE") {
    override fun getDefaultValue(): Boolean = false
}