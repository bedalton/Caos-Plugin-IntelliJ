@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage.Companion.instance
import com.intellij.openapi.fileTypes.LanguageFileType
import icons.CaosScriptIcons
import org.jetbrains.annotations.NonNls
import javax.swing.Icon


object CaosDefFileType : LanguageFileType(instance) {
    override fun getName(): String {
        return "Caos Definitions File"
    }

    override fun getDescription(): String {
        return "A Caos definitions file"
    }

    override fun getDefaultExtension(): String {
        return DEFAULT_EXTENSION
    }

    override fun getIcon(): Icon? {
        return CaosScriptIcons.CAOS_DEF_FILE_ICON
    }
        @NonNls
        val DEFAULT_EXTENSION = "caosdef"

        @NonNls
        val DOT_DEFAULT_EXTENSION = ".$DEFAULT_EXTENSION"
}