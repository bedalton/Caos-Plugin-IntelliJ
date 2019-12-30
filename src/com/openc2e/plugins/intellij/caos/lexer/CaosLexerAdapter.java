package com.openc2e.plugins.intellij.caos.lexer;

import com.intellij.lexer.FlexAdapter;
import com.openc2e.plugins.intellij.caos.lexer._CaosLexer;

public class CaosLexerAdapter extends FlexAdapter {
    public CaosLexerAdapter() {
            super(new _CaosLexer());
        }
}
