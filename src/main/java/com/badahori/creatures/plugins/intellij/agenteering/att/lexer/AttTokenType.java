package com.badahori.creatures.plugins.intellij.agenteering.att.lexer;

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class AttTokenType extends IElementType {

    public AttTokenType(@NotNull String debug) {
        super(debug, AttLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "AttTokenType."+super.toString();
    }
}
