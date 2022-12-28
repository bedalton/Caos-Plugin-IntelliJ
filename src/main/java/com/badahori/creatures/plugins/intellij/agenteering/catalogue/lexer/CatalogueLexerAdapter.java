package com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer;

import com.intellij.lexer.FlexAdapter;
import grammars._CatalogueLexer;

public class CatalogueLexerAdapter extends FlexAdapter {
    public CatalogueLexerAdapter() {
            super(new _CatalogueLexer());
        }
}
