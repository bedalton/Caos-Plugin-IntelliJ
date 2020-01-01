package com.openc2e.plugins.intellij.caos.lexer;

import com.intellij.lexer.FlexAdapter;
import com.openc2e.plugins.intellij.caos.lexer._CaosLexer;

public class CaosScriptLexerAdapter extends FlexAdapter {
    public CaosScriptLexerAdapter() {
            super(new _CaosLexer());
        }
}
