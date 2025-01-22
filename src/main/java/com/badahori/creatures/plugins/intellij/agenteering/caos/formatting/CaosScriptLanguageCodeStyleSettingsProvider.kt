package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.*
import javax.swing.JCheckBox

class CaosScriptLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language {
        return CaosScriptLanguage
    }

    override fun getIndentOptionsEditor(): IndentOptionsEditor {
        return CaosScriptIndentOptionsEditor(this)
    }

    override fun getSupportedFields(type: SettingsType?): MutableSet<String> {
        return super.getSupportedFields(type)
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {

        val customizationOptions = CodeStyleSettingsCustomizableOptions.getInstance()

        if (settingsType == SettingsType.INDENT_SETTINGS) {
            consumer.showAllStandardOptions()

            consumer.showStandardOptions("INDENT_SETTINGS")

            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "INDENT_COMMENTS",
                "Indent comments",
                customizationOptions.BLANK_LINES
            )

//            consumer.showCustomOption(
//                CaosScriptCodeStyleSettings::class.java,
//                "CONTINUATION_INDENT",
//                "Continuation indent",
//                null
//            )

            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "INDENT_BLOCKS",
                "Indent blocks (ie. DOIF..ENDI, Enum..Next)",
                CaosScriptCodeStyleSettings.SCRIPT_INDENT_SETTINGS_GROUP_NAME
            )

            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "INDENT_SCRP",
                "Indent SCRP body",
                CaosScriptCodeStyleSettings.SCRIPT_INDENT_SETTINGS_GROUP_NAME
            )

            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "INDENT_RSCR",
                "Indent RSCR body",
                CaosScriptCodeStyleSettings.SCRIPT_INDENT_SETTINGS_GROUP_NAME
            )

            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "INDENT_ISCR",
                "Indent ISCR body",
                CaosScriptCodeStyleSettings.SCRIPT_INDENT_SETTINGS_GROUP_NAME
            )
        }

        if (settingsType == SettingsType.SPACING_SETTINGS) {
            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "SPACE_BETWEEN_BYTE_STRING_AND_BRACKETS",
                "Space between byte string and brackets",
                customizationOptions.SPACES_WITHIN
            )
        }

        if (settingsType == SettingsType.BLANK_LINES_SETTINGS) {
            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "MIN_BLANK_LINES_BETWEEN_COMMANDS",
                "Min blank lines between commands",
                customizationOptions.BLANK_LINES
            )

            consumer.showCustomOption(
                CaosScriptCodeStyleSettings::class.java,
                "MAX_BLANK_LINES_BETWEEN_COMMANDS",
                "Max blank lines between commands",
                customizationOptions.BLANK_LINES
            )
        }
    }

    private class CaosScriptIndentOptionsEditor(provider: LanguageCodeStyleSettingsProvider): SmartIndentOptionsEditor(provider) {

        private val indentScrpScripts by lazy {
            JCheckBox(CaosBundle.message("caos.settings.style.indent.scrp"));
        }

        private val indentIscrScripts by lazy {
            JCheckBox(CaosBundle.message("caos.settings.style.indent.iscr"));
        }

        private val indentRscrScripts by lazy {
            JCheckBox(CaosBundle.message("caos.settings.style.indent.rscr"));
        }

        private val indentBlocks by lazy {
            JCheckBox(CaosBundle.message("caos.settings.style.indent.block"));
        }


        @Override
        override fun addComponents() {
            super.addComponents()
            add(indentBlocks)
            add(indentScrpScripts)
            add(indentIscrScripts)
            add(indentRscrScripts)
        }

        override fun setEnabled(enabled: Boolean) {
            super.setEnabled(enabled)
            indentBlocks.setEnabled(enabled)
            indentScrpScripts.setEnabled(enabled)
            indentIscrScripts.setEnabled(enabled)
            indentRscrScripts.setEnabled(enabled)
        }


        override fun isModified(settings: CodeStyleSettings,  options: CommonCodeStyleSettings.IndentOptions): Boolean {
            var isModified = super.isModified(settings, options)
            val caosSettings: CaosScriptCodeStyleSettings  = settings.getCustomSettings(CaosScriptCodeStyleSettings::class.java)

            isModified = isModified || isFieldModified(indentBlocks, caosSettings.INDENT_BLOCKS);
            isModified = isModified || isFieldModified(indentScrpScripts, caosSettings.INDENT_BLOCKS);
            isModified = isModified || isFieldModified(indentIscrScripts, caosSettings.INDENT_BLOCKS);
            isModified = isModified || isFieldModified(indentRscrScripts, caosSettings.INDENT_BLOCKS);

            return isModified
        }

        override fun apply( settings: CodeStyleSettings, options:  CommonCodeStyleSettings.IndentOptions) {
            super.apply(settings, options)
            val caosSettings = settings.getCustomSettings(CaosScriptCodeStyleSettings::class.java)
            caosSettings.INDENT_BLOCKS = indentBlocks.isSelected
            caosSettings.INDENT_SCRP = indentScrpScripts.isSelected
            caosSettings.INDENT_ISCR = indentIscrScripts.isSelected
            caosSettings.INDENT_RSCR = indentRscrScripts.isSelected
        }

        override fun reset( settings: CodeStyleSettings, options: CommonCodeStyleSettings.IndentOptions) {
            super.reset(settings, options);
            val caosSettings = settings.getCustomSettings(CaosScriptCodeStyleSettings::class.java)
            indentBlocks.isSelected = caosSettings.INDENT_BLOCKS
            indentScrpScripts.isSelected = caosSettings.INDENT_SCRP
            indentIscrScripts.isSelected = caosSettings.INDENT_ISCR
            indentRscrScripts.isSelected = caosSettings.INDENT_RSCR
        }
    }


    override fun getCodeSample(p0: SettingsType): String {
        return """
iscr
    new: simp 2 3 1801 "meander_plant" 4 0 rand 4000 5200
    attr 16
    bhvr 0
    anim [0 1 2 1 0 255]
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

}