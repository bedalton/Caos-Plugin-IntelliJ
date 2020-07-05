package com.openc2e.plugins.intellij.agenteering.caos.def.lang

import com.intellij.lang.Language

class CaosDefLanguage private constructor() : Language(NAME) {
    companion object {
        private const val NAME = "CaosDef"
        @JvmStatic
        val instance = CaosDefLanguage()
    }
}
