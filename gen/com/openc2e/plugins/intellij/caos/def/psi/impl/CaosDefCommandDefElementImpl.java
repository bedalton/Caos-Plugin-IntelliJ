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

public class CaosDefCommandDefElementImpl extends CaosDefCompositeElementImpl implements CaosDefCommandDefElement {

  public CaosDefCommandDefElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CaosDefVisitor visitor) {
    visitor.visitCommandDefElement(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CaosDefVisitor) accept((CaosDefVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CaosDefArgs getArgs() {
    return findChildByClass(CaosDefArgs.class);
  }

  @Override
  @NotNull
  public CaosDefCommandName getCommandName() {
    return findNotNullChildByClass(CaosDefCommandName.class);
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
  public CaosDefReturnType getReturnType() {
    return findChildByClass(CaosDefReturnType.class);
  }

  @Override
  @Nullable
  public CaosDefString getString() {
    return findChildByClass(CaosDefString.class);
  }

  @Override
  @Nullable
  public PsiElement getArgsKeyword() {
    return findChildByType(CaosDef_ARGS_KEYWORD);
  }

  @Override
  @NotNull
  public PsiElement getCommandKeyword() {
    return findNotNullChildByType(CaosDef_COMMAND_KEYWORD);
  }

  @Override
  @Nullable
  public PsiElement getNewline() {
    return findChildByType(CaosDef_NEWLINE);
  }

}
