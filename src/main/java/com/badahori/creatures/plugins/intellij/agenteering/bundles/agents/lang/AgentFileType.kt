package com.badahori.creatures.plugins.intellij.agenteering.bundles.agents.lang


import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import javax.swing.Icon


object AgentFileType : FileType {

    override fun getName(): String = "AGENTFile"

    override fun getDescription(): String = "Creatures agent file"

    override fun isBinary(): Boolean = true

    override fun isReadOnly(): Boolean = true

    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    override fun getIcon(): Icon? = CaosScriptIcons.AGENT_FILE_ICON

    override fun getCharset(p0: VirtualFile, p1: ByteArray): String = "cp1252"

    @JvmStatic
    val DEFAULT_EXTENSION = "agents"
}

val IS_AGENT_USER_DATA_KEY:Key<Boolean> = object:KeyWithDefaultValue<Boolean>("com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.IS_AGENT_VIRTUAL_FILE") {
    override fun getDefaultValue(): Boolean = false
}