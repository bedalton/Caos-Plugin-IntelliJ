package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types;

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class CaosScriptElementType extends IElementType {
    public CaosScriptElementType(
            @NotNull
                    String debugName) {
        super(debugName, CaosScriptLanguage.INSTANCE);
    }
}
