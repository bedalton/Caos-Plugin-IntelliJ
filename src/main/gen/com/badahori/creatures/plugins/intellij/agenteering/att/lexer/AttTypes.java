// This is a generated file. Not intended for manual editing.
package com.badahori.creatures.plugins.intellij.agenteering.att.lexer;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.badahori.creatures.plugins.intellij.agenteering.att.psi.types.AttElementType;
import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.*;

public interface AttTypes {

  IElementType ATT_ERROR_VALUE = new AttElementType("ATT_ERROR_VALUE");
  IElementType ATT_INT = new AttElementType("ATT_INT");
  IElementType ATT_ITEM = new AttElementType("ATT_ITEM");
  IElementType ATT_LINE = new AttElementType("ATT_LINE");
  IElementType ATT_NEWLINE = new AttElementType("ATT_NEWLINE");
  IElementType ATT_SPACE = new AttElementType("ATT_SPACE");

  IElementType ATT_ERROR_SPACE_LITERAL = new AttTokenType("ERROR_SPACE_LITERAL");
  IElementType ATT_ERROR_VALUE_LITERAL = new AttTokenType("ERROR_VALUE_LITERAL");
  IElementType ATT_INT_LITERAL = new AttTokenType("INT_LITERAL");
  IElementType ATT_NEWLINE_LITERAL = new AttTokenType("NEWLINE_LITERAL");
  IElementType ATT_SPACE_LITERAL = new AttTokenType("SPACE_LITERAL");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ATT_ERROR_VALUE) {
        return new AttErrorValueImpl(node);
      }
      else if (type == ATT_INT) {
        return new AttIntImpl(node);
      }
      else if (type == ATT_ITEM) {
        return new AttItemImpl(node);
      }
      else if (type == ATT_LINE) {
        return new AttLineImpl(node);
      }
      else if (type == ATT_NEWLINE) {
        return new AttNewlineImpl(node);
      }
      else if (type == ATT_SPACE) {
        return new AttSpaceImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
