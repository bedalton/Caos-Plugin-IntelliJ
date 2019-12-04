package com.openc2e.plugins.intellij.caos.def.psi.types;

import com.intellij.psi.tree.IElementType;
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage;
import org.jetbrains.annotations.NotNull;

public class CaosDefElementType extends IElementType {
    public CaosDefElementType(
            @NotNull
                    String debugName) {
        super(debugName, CaosDefLanguage.getInstance());
    }
}
