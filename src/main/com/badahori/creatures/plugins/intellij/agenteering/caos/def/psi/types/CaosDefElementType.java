package com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.types;

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage;
import com.intellij.psi.tree.IElementType;
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage;
import org.jetbrains.annotations.NotNull;

public class CaosDefElementType extends IElementType {
    public CaosDefElementType(
            @NotNull
                    String debugName) {
        super(debugName, CaosDefLanguage.getInstance());
    }
}
