package com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.types;

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices.CatalogueStubIndexService;
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueLanguage;
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueCompositeElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;

public abstract class CatalogueStubElementType<StubT extends StubElement<PsiT>, PsiT extends CatalogueCompositeElement> extends IStubElementType<StubT, PsiT> {

    public CatalogueStubElementType(
            @NotNull
                    String debugName) {
        super(debugName, CatalogueLanguage.INSTANCE);
    }

    @NotNull
    protected CatalogueStubIndexService getService() {
        return CatalogueStubIndexService.INSTANCE;
    }

    @NotNull
    @Override
    public String getExternalId() {
        return "catalogue."+super.toString();
    }
}
