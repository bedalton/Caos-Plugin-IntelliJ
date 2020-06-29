package com.openc2e.plugins.intellij.agenteering.sprites.spr.def


import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon


class CobDefFileType private constructor() : LanguageFileType(CobDefLanguage) {
    override fun getName(): String {
        return "C1 Cob Definition"
    }

    override fun getDescription(): String {
        return "Creatures 1 COB Definitions File"
    }

    override fun getDefaultExtension(): String {
        return DEFAULT_EXTENSION
    }

    override fun getIcon(): Icon? {
        return null
    }

    companion object {
        @JvmStatic
        val INSTANCE = CobDefFileType()
        @JvmStatic
        val DEFAULT_EXTENSION = "cobdef"
    }
}