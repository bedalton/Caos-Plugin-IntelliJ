package com.openc2e.plugins.intellij.caos.lexer;

import com.intellij.psi.tree.IElementType;
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage;
import org.jetbrains.annotations.NotNull;

public class CaosScriptTokenType extends IElementType {

    public CaosScriptTokenType(
            @NotNull
                    String debug) {
        super(debug, CaosScriptLanguage.getInstance());
    }
}
