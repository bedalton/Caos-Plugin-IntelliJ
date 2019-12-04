package com.openc2e.plugins.intellij.caos.indices;

import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;

public class IndexKeyUtil {

    public static <Key, PsiT extends PsiElement, IndexT extends StubIndexExtension<Key,PsiT>> StubIndexKey<Key, PsiT> create(Class<IndexT> clazz) {
        return StubIndexKey.createIndexKey(clazz.getCanonicalName());
    }
}
