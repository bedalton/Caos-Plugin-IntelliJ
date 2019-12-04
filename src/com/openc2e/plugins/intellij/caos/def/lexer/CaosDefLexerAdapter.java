package com.openc2e.plugins.intellij.caos.def.lexer;

import com.intellij.lexer.FlexAdapter;
import grammars._CaosDefLexer;

public class CaosDefLexerAdapter extends FlexAdapter {
    public CaosDefLexerAdapter() {
            super(new _CaosDefLexer(null));
        }
}
