package com.openc2e.plugins.intellij.caos.lexer;

import com.intellij.psi.tree.IElementType;
import com.openc2e.plugins.intellij.caos.lang.CaosLanguage;
import org.jetbrains.annotations.NotNull;

public class CaosTokenType extends IElementType {

    public CaosTokenType(
            @NotNull
                    String debug) {
        super(debug, CaosLanguage.getInstance());
    }
}
