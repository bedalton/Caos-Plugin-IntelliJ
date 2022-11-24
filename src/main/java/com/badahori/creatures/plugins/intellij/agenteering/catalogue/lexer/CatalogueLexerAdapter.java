package com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer;

import com.intellij.lexer.FlexAdapter;

public class CatalogueLexerAdapter extends FlexAdapter {
    public CatalogueLexerAdapter() {
            super(new _CatalogueLexer());
        }
}
