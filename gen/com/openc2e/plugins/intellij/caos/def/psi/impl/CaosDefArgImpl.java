// This is a generated file. Not intended for manual editing.
package com.openc2e.plugins.intellij.caos.def.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.openc2e.plugins.intellij.caos.def.lexer.CaosDefTypes.*;
import com.openc2e.plugins.intellij.caos.def.psi.api.*;
import com.openc2e.plugins.intellij.caos.def.parser.CaosDefParserUtil;

public class CaosDefArgImpl extends CaosDefCompositeElementImpl implements CaosDefArg {

  public CaosDefArgImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CaosDefVisitor visitor) {
    visitor.visitArg(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CaosDefVisitor) accept((CaosDefVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public CaosDefParameterName getParameterName() {
    return findNotNullChildByClass(CaosDefParameterName.class);
  }

  @Override
  @Nullable
  public CaosDefString getString() {
    return findChildByClass(CaosDefString.class);
  }

  @Override
  @Nullable
  public CaosDefTypeName getTypeName() {
    return findChildByClass(CaosDefTypeName.class);
  }

}
