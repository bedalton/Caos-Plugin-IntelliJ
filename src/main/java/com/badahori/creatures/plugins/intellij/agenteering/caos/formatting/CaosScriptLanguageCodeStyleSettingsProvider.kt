package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

class CaosScriptLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language {
        return CaosScriptLanguage
    }

    override fun getCodeSample(p0: SettingsType): String {
        return """
iscr
    new: simp 2 3 1801 "meander_plant" 4 0 rand 4000 5200
    attr 16
    bhvr 0
endm

* Timer Script for meander seed
scrp 2 3 1801 9
    doif 10 lt 11
        seta va01 norn
        enum 4 1 0
            setv va00 attr
            andv va00 48
            outs "DRV! == " outv DRV!
        next
    endi
endm

* Simple removal script
rscr
    scrx 2 3 1801 9
    scrx 2 3 1801 10
endm
        """.trimIndent()
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        if (settingsType == SettingsType.SPACING_SETTINGS) {
            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "INDENT_BLOCKS",
                "Indent blocks (ie. DOIF..ENDI, Enum..Next)",
                CaosScriptCodeStyleSettings.INDENT_SETTINGS_GROUP_NAME
            )
            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "INDENT_SCRP",
                "Indent SCRP Body",
                CaosScriptCodeStyleSettings.INDENT_SETTINGS_GROUP_NAME
            )
            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "INDENT_RSCR",
                "Indent RSCR body",
                CaosScriptCodeStyleSettings.INDENT_SETTINGS_GROUP_NAME
            )
            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "INDENT_ISCR",
                "Indent ISCR body",
                CaosScriptCodeStyleSettings.INDENT_SETTINGS_GROUP_NAME
            )
        }
    }
}