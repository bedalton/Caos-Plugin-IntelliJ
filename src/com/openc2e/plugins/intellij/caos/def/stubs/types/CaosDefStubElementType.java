package com.openc2e.plugins.intellij.caos.def.stubs.types;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.stubs.*;
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage;
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement;
import org.jetbrains.annotations.NotNull;

public abstract class CaosDefStubElementType<StubT extends StubElement<PsiT>, PsiT extends CaosDefCompositeElement> extends IStubElementType<StubT, PsiT> {

    public CaosDefStubElementType(
            @NotNull
                    String debugName) {
        super(debugName, CaosDefLanguage.getInstance());
    }

    @NotNull
    protected StubIndexService getService() {
        return ServiceManager.getService(StubIndexService.class);
    }

    @NotNull
    @Override
    public String getExternalId() {
        return "brs."+super.toString();
    }
}
