package com.badahori.creatures.plugins.intellij.agenteering.caos.lexer;

import com.intellij.lexer.FlexAdapter;

public class CaosScriptLexerAdapter extends FlexAdapter {
    public CaosScriptLexerAdapter() {
            super(new _CaosScriptLexer(false));
        }
}
