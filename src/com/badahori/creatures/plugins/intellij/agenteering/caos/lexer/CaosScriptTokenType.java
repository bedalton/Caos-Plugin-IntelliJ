package com.badahori.creatures.plugins.intellij.agenteering.caos.lexer;

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage;
import com.intellij.psi.tree.IElementType;
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage;
import org.jetbrains.annotations.NotNull;

public class CaosScriptTokenType extends IElementType {

    public CaosScriptTokenType(
            @NotNull
                    String debug) {
        super(debug, CaosScriptLanguage.INSTANCE);
    }
}
