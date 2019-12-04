package com.openc2e.plugins.intellij.caos.def.lexer;

import brightscript.intellij.lexer._BrightScriptLexer;
import com.intellij.lexer.FlexAdapter;

import java.io.Reader;

public class BrsLexerAdapter extends FlexAdapter {
    public BrsLexerAdapter() {
            super(new _CaosDefLexer((Reader) null));
        }
}
