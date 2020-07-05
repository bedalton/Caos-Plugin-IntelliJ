package com.openc2e.plugins.intellij.agenteering.caos.stubs.types;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage;
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement;
import org.jetbrains.annotations.NotNull;

public abstract class CaosScriptStubElementType<StubT extends StubElement<PsiT>, PsiT extends CaosScriptCompositeElement> extends IStubElementType<StubT, PsiT> {

    public CaosScriptStubElementType(
            @NotNull
                    String debugName) {
        super(debugName, CaosScriptLanguage.getInstance());
    }

    /*@NotNull
    protected CaosScriptStubIndexService getService() {
        return ServiceManager.getService(CaosScriptStubIndexService.class);
    }*/

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
