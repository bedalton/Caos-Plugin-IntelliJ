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

public class CaosDefTypeDefElementImpl extends CaosDefCompositeElementImpl implements CaosDefTypeDefElement {

  public CaosDefTypeDefElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CaosDefVisitor visitor) {
    visitor.visitTypeDefElement(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CaosDefVisitor) accept((CaosDefVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public CaosDefString getString() {
    return findNotNullChildByClass(CaosDefString.class);
  }

  @Override
  @NotNull
  public CaosDefTypeName getTypeName() {
    return findNotNullChildByClass(CaosDefTypeName.class);
  }

  @Override
  @Nullable
  public PsiElement getNewline() {
    return findChildByType(CaosDef_NEWLINE);
  }

  @Override
  @NotNull
  public PsiElement getTypeKeyword() {
    return findNotNullChildByType(CaosDef_TYPE_KEYWORD);
  }

}
