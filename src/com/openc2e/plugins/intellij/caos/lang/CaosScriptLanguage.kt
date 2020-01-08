package com.openc2e.plugins.intellij.caos.lang

import com.intellij.lang.Language

class CaosScriptLanguage private constructor() : Language(NAME) {
    companion object {
        private const val NAME = "CaosScript"
        @JvmStatic
        val instance = CaosScriptLanguage()
    }
}
