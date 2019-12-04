// This is a generated file. Not intended for manual editing.
package com.openc2e.plugins.intellij.caos.def.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface CaosDefCommandDefElement extends CaosDefCompositeElement {

  @Nullable
  CaosDefArgs getArgs();

  @NotNull
  CaosDefCommandName getCommandName();

  @Nullable
  CaosDefInlineDocComment getInlineDocComment();

  @Nullable
  CaosDefNamespace getNamespace();

  @Nullable
  CaosDefReturnType getReturnType();

  @Nullable
  CaosDefString getString();

  @Nullable
  PsiElement getArgsKeyword();

  @NotNull
  PsiElement getCommandKeyword();

  @Nullable
  PsiElement getNewline();

}
