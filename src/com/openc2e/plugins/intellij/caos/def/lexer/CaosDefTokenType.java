package com.openc2e.plugins.intellij.caos.def.lexer;

import com.intellij.psi.tree.IElementType;
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage;
import org.jetbrains.annotations.NotNull;

public class CaosDefTokenType extends IElementType {

    public CaosDefTokenType(
            @NotNull
                    String debug) {
        super(debug, CaosDefLanguage.getInstance());
    }
}
