package com.openc2e.plugins.intellij.caos.sprites.spr.def


import com.intellij.openapi.fileTypes.LanguageFileType
import com.openc2e.plugins.intellij.caos.sprites.spr.SprLanguage
import javax.swing.Icon


class SprDefFileType private constructor() : LanguageFileType(SprDefLanguage) {
    override fun getName(): String {
        return "SPR Definitions file"
    }

    override fun getDescription(): String {
        return "Creatures 1 Sprite Definitions File"
    }

    override fun getDefaultExtension(): String {
        return DEFAULT_EXTENSION
    }

    override fun getIcon(): Icon? {
        return null
    }

    companion object {
        @JvmStatic
        val INSTANCE = SprDefFileType()
        @JvmStatic
        val DEFAULT_EXTENSION = "sprdef"
    }
}