package com.openc2e.plugins.intellij.caos.lang

import com.intellij.lang.Language

class CaosScriptLanguage private constructor() : Language(NAME) {
    companion object {
        private const val NAME = "Caos"
        @JvmStatic
        val instance = CaosScriptLanguage()
    }
}
