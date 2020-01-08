package com.openc2e.plugins.intellij.caos.stubs.types;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefStubIndexService;
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage;
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement;
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement;
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptCommandStub;
import org.jetbrains.annotations.NotNull;

public abstract class CaosScriptStubElementType<StubT extends StubElement<PsiT>, PsiT extends CaosScriptCompositeElement> extends IStubElementType<StubT, PsiT> {

    public CaosScriptStubElementType(
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


    @Override
    public void indexStub(@NotNull StubT stub, @NotNull IndexSink indexSink) {
        // ignore
    }
}
