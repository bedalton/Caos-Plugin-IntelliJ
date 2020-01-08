package com.openc2e.plugins.intellij.caos.def.stubs.types;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefStubIndexService;
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
    protected CaosDefStubIndexService getService() {
        return ServiceManager.getService(CaosDefStubIndexService.class);
    }

    @NotNull
    @Override
    public String getExternalId() {
        return "caos."+super.toString();
    }
}
