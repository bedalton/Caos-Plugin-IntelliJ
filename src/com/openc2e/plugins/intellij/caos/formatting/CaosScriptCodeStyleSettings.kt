package com.openc2e.plugins.intellij.caos.formatting

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage


class CaosScriptCodeStyleSettings internal constructor(container: CodeStyleSettings?) : CustomCodeStyleSettings(CaosScriptLanguage.instance.id, container) {
    var INDENT_BLOCKS = true
}
