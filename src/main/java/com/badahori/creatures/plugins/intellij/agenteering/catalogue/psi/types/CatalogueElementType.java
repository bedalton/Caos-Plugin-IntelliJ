package com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.types;

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class CatalogueElementType extends IElementType {
    public CatalogueElementType(
            @NotNull
                    String debugName) {
        super(debugName, CatalogueLanguage.INSTANCE);
    }
}
