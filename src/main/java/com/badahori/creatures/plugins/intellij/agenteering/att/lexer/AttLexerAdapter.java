package com.badahori.creatures.plugins.intellij.agenteering.att.lexer;

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer._CaosScriptLexer;
import com.intellij.lexer.FlexAdapter;

public class AttLexerAdapter extends FlexAdapter {
    public AttLexerAdapter() {
            super(new _CaosScriptLexer(false));
        }
}
