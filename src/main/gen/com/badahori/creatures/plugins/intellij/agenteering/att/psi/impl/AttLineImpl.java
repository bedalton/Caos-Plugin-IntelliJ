// This is a generated file. Not intended for manual editing.
package com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.badahori.creatures.plugins.intellij.agenteering.att.lexer.AttTypes.*;
import com.badahori.creatures.plugins.intellij.agenteering.att.psi.api.*;
import com.badahori.creatures.plugins.intellij.agenteering.att.psi.util.AttPsiImplUtil;

public class AttLineImpl extends AttCompositeElementImpl implements AttLine {

  public AttLineImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AttVisitor visitor) {
    visitor.visitLine(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AttVisitor) accept((AttVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<AttItem> getItemList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AttItem.class);
  }

  @Override
  @Nullable
  public AttNewline getNewline() {
    return findChildByClass(AttNewline.class);
  }

  @Override
  @NotNull
  public List<AttSpace> getSpaceList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AttSpace.class);
  }

}
