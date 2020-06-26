package com.openc2e.plugins.intellij.caos.sprites.spr


import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon


class SprFileType private constructor() : LanguageFileType(SprLanguage) {
    override fun getName(): String {
        return "SPR file"
    }

    override fun getDescription(): String {
        return "Creatures 1 Sprite File"
    }

    override fun getDefaultExtension(): String {
        return DEFAULT_EXTENSION
    }

    override fun getIcon(): Icon? {
        return null
    }

    companion object {
        @JvmStatic
        val INSTANCE = SprFileType()
        @JvmStatic
        val DEFAULT_EXTENSION = "spr"
    }
}