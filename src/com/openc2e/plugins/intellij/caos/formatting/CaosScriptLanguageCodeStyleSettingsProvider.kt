package com.openc2e.plugins.intellij.caos.formatting

import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage
import com.openc2e.plugins.intellij.caos.lang.CaosVariant
import com.openc2e.plugins.intellij.caos.settings.CaosScriptProjectSettings

class CaosScriptLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language {
        return CaosScriptLanguage.instance
    }

    override fun getCodeSample(p0: SettingsType): String? {
        return if (CaosScriptProjectSettings.isVariant(CaosVariant.C1)) {
            """
            doif 10 lt 11
            setv var1 norn
            enum 4 1 0
            touc var1 targ
            dde: puts [DRV! == ] dde: putv DRV!
            next
            endi
        """.trimIndent()
        } else {
            """
            doif 10 lt 11
            targ norn
            etch 4 1 0
            dde: puts [DRV! == ] dde: putv DRV!
            next
            endi
            """.trimIndent()
        }
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        if (settingsType == SettingsType.INDENT_SETTINGS) {
            consumer.renameStandardOption("INDENT_BLOCKS", "indent code within blocks. (ie. Doif..ENDI, Enum..Next)")
        }
    }
}