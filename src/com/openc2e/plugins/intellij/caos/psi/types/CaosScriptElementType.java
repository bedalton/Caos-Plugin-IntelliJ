package com.openc2e.plugins.intellij.caos.psi.types;

import com.intellij.psi.tree.IElementType;
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage;
import org.jetbrains.annotations.NotNull;

public class CaosScriptElementType extends IElementType {
    public CaosScriptElementType(
            @NotNull
                    String debugName) {
        super(debugName, CaosScriptLanguage.getInstance());
    }
}
