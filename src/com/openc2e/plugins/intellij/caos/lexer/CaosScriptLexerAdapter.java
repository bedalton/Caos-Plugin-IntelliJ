package com.openc2e.plugins.intellij.caos.lexer;

import com.intellij.lexer.FlexAdapter;

public class CaosScriptLexerAdapter extends FlexAdapter {
    public CaosScriptLexerAdapter() {
            super(new _CaosScriptLexer());
        }
}
