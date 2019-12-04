package com.openc2e.plugins.intellij.caos.lang

import com.intellij.lang.Language

class CaosLanguage private constructor() : Language(NAME) {
    companion object {
        private const val NAME = "Caos"
        val instance = CaosLanguage()
    }
}
