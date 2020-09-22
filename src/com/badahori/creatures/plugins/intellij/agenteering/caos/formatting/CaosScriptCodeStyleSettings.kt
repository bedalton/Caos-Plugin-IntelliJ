package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage


class CaosScriptCodeStyleSettings internal constructor(container: CodeStyleSettings?) : CustomCodeStyleSettings(CaosScriptLanguage.id, container) {
    var INDENT_BLOCKS = true
}
