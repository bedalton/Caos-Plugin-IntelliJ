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

public class CaosDefVarDefElementImpl extends CaosDefCompositeElementImpl implements CaosDefVarDefElement {

  public CaosDefVarDefElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CaosDefVisitor visitor) {
    visitor.visitVarDefElement(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CaosDefVisitor) accept((CaosDefVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CaosDefInlineDocComment getInlineDocComment() {
    return findChildByClass(CaosDefInlineDocComment.class);
  }

  @Override
  @Nullable
  public CaosDefNamespace getNamespace() {
    return findChildByClass(CaosDefNamespace.class);
  }

  @Override
  @Nullable
  public CaosDefParameterName getParameterName() {
    return findChildByClass(CaosDefParameterName.class);
  }

  @Override
  @Nullable
  public CaosDefTypeName getTypeName() {
    return findChildByClass(CaosDefTypeName.class);
  }

  @Override
  @Nullable
  public PsiElement getNewline() {
    return findChildByType(CaosDef_NEWLINE);
  }

  @Override
  @NotNull
  public PsiElement getVarKeyword() {
    return findNotNullChildByType(CaosDef_VAR_KEYWORD);
  }

}
