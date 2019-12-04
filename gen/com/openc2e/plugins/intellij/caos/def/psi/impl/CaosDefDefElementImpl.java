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

public class CaosDefDefElementImpl extends CaosDefCompositeElementImpl implements CaosDefDefElement {

  public CaosDefDefElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CaosDefVisitor visitor) {
    visitor.visitDefElement(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CaosDefVisitor) accept((CaosDefVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CaosDefCommandDefElement getCommandDefElement() {
    return findChildByClass(CaosDefCommandDefElement.class);
  }

  @Override
  @Nullable
  public CaosDefComment getComment() {
    return findChildByClass(CaosDefComment.class);
  }

  @Override
  @Nullable
  public CaosDefTypeDefElement getTypeDefElement() {
    return findChildByClass(CaosDefTypeDefElement.class);
  }

  @Override
  @Nullable
  public CaosDefVarDefElement getVarDefElement() {
    return findChildByClass(CaosDefVarDefElement.class);
  }

  @Override
  @Nullable
  public PsiElement getNewline() {
    return findChildByType(CaosDef_NEWLINE);
  }

}
