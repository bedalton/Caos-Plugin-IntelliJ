package com.openc2e.plugins.intellij.caos.psi.types;

import com.intellij.psi.tree.IElementType;
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage;
import org.jetbrains.annotations.NotNull;

public class CaosElementType extends IElementType {
    public CaosElementType(
            @NotNull
                    String debugName) {
        super(debugName, CaosDefLanguage.getInstance());
    }
}
