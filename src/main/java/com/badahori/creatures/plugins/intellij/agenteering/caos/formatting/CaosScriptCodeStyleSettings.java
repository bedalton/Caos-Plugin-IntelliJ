package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting;

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import org.jetbrains.annotations.Nullable;


public class CaosScriptCodeStyleSettings extends CustomCodeStyleSettings {
    static final String SCRIPT_INDENT_SETTINGS_GROUP_NAME = "Script & Block Indents";

    // Indent
    public boolean INDENT_BLOCKS = true;
    public boolean INDENT_SCRP = true;
    public boolean INDENT_RSCR = true;
    public boolean INDENT_ISCR = true;
    public boolean INDENT_COMMENTS = true;


    // Spacing
    public boolean SPACE_BETWEEN_BYTE_STRING_AND_BRACKETS = false;

    // Blank Lines
    public boolean FORCE_BLANK_LINES_AFTER_COMMENT = false;
    public int MIN_BLANK_LINES_BETWEEN_COMMANDS = 0;
    public int MAX_BLANK_LINES_BETWEEN_COMMANDS = 0;
    public boolean ALLOW_MULTIPLE_COMMANDS_ON_SINGLE_LINE = true;



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
