package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage;
import org.jetbrains.annotations.Nullable;


public class CaosScriptCodeStyleSettings extends CustomCodeStyleSettings {
    static final String INDENT_SETTINGS_GROUP_NAME = "Indent Settings";
    public boolean INDENT_BLOCKS = true;
    public boolean INDENT_SCRP = true;
    public boolean INDENT_RSCR = true;
    public boolean INDENT_ISCR = true;

    public boolean indentAny() {
        return INDENT_BLOCKS || INDENT_ISCR || INDENT_RSCR || INDENT_SCRP;
    }

    public boolean indentNone() {
        return !(INDENT_BLOCKS || INDENT_ISCR || INDENT_RSCR || INDENT_SCRP);
    }

    public CaosScriptCodeStyleSettings(@Nullable CodeStyleSettings container) {
        super(CaosScriptLanguage.INSTANCE.getID(), container);
    }
}
