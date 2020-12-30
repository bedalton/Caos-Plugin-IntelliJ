// This is a generated file. Not intended for manual editing.
package com.badahori.creatures.plugins.intellij.agenteering.att.psi.api;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;

public class AttVisitor extends PsiElementVisitor {

  public void visitErrorValue(@NotNull AttErrorValue o) {
    visitCompositeElement(o);
  }

  public void visitInt(@NotNull AttInt o) {
    visitCompositeElement(o);
  }

  public void visitItem(@NotNull AttItem o) {
    visitCompositeElement(o);
  }

  public void visitLine(@NotNull AttLine o) {
    visitCompositeElement(o);
  }

  public void visitNewline(@NotNull AttNewline o) {
    visitCompositeElement(o);
  }

  public void visitSpace(@NotNull AttSpace o) {
    visitCompositeElement(o);
  }

  public void visitCompositeElement(@NotNull AttCompositeElement o) {
    visitElement(o);
  }

}
