// This is a generated file. Not intended for manual editing.
package com.badahori.creatures.plugins.intellij.agenteering.att.psi.api;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface AttLine extends AttCompositeElement {

  @NotNull
  List<AttItem> getItemList();

  @Nullable
  AttNewline getNewline();

  @NotNull
  List<AttSpace> getSpaceList();

}
