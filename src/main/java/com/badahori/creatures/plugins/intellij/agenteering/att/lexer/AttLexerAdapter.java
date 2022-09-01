package com.badahori.creatures.plugins.intellij.agenteering.att.lexer;

import com.intellij.lexer.FlexAdapter;

public class AttLexerAdapter extends FlexAdapter {
    public AttLexerAdapter() {
            super(new _AttLexer());
        }
}
