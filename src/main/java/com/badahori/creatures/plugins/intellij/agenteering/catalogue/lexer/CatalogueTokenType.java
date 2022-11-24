package com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer;

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class CatalogueTokenType extends IElementType {

    public CatalogueTokenType(@NotNull String debug) {
        super(debug, AttLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "CatalogueTokenType."+super.toString();
    }
}
