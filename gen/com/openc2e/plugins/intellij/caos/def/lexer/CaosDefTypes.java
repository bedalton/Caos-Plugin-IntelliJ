// This is a generated file. Not intended for manual editing.
package com.openc2e.plugins.intellij.caos.def.lexer;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.openc2e.plugins.intellij.caos.def.psi.types.CaosDefElementType;
import com.openc2e.plugins.intellij.caos.def.psi.CaosDefElementTypeFactory;
import com.openc2e.plugins.intellij.caos.def.psi.impl.*;

public interface CaosDefTypes {

  IElementType CaosDef_ARG = new CaosDefElementType("CaosDef_ARG");
  IElementType CaosDef_ARGS = new CaosDefElementType("CaosDef_ARGS");
  IElementType CaosDef_COMMAND_DEF_ELEMENT = new CaosDefElementType("CaosDef_COMMAND_DEF_ELEMENT");
  IElementType CaosDef_COMMAND_NAME = new CaosDefElementType("CaosDef_COMMAND_NAME");
  IElementType CaosDef_COMMENT = CaosDefElementTypeFactory.factory("CaosDef_COMMENT");
  IElementType CaosDef_DEF_ELEMENT = new CaosDefElementType("CaosDef_DEF_ELEMENT");
  IElementType CaosDef_INLINE_DOC_COMMENT = new CaosDefElementType("CaosDef_INLINE_DOC_COMMENT");
  IElementType CaosDef_NAMESPACE = new CaosDefElementType("CaosDef_NAMESPACE");
  IElementType CaosDef_NAMESPACE_NAME = new CaosDefElementType("CaosDef_NAMESPACE_NAME");
  IElementType CaosDef_PARAMETER_NAME = new CaosDefElementType("CaosDef_PARAMETER_NAME");
  IElementType CaosDef_RETURN_TYPE = new CaosDefElementType("CaosDef_RETURN_TYPE");
  IElementType CaosDef_STRING = new CaosDefElementType("CaosDef_STRING");
  IElementType CaosDef_STRING_BODY = new CaosDefElementType("CaosDef_STRING_BODY");
  IElementType CaosDef_TYPE_DEF_ELEMENT = new CaosDefElementType("CaosDef_TYPE_DEF_ELEMENT");
  IElementType CaosDef_TYPE_NAME = new CaosDefElementType("CaosDef_TYPE_NAME");
  IElementType CaosDef_VAR_DEF_ELEMENT = new CaosDefElementType("CaosDef_VAR_DEF_ELEMENT");

  IElementType CaosDef_ARGS_KEYWORD = new CaosDefTokenType("ARGS_KEYWORD");
  IElementType CaosDef_CLOSE_BRACKET = new CaosDefTokenType("]");
  IElementType CaosDef_CLOSE_LINK = new CaosDefTokenType(">>");
  IElementType CaosDef_CLOSE_PAREN = new CaosDefTokenType(")");
  IElementType CaosDef_COLON = new CaosDefTokenType(":");
  IElementType CaosDef_COMMA = new CaosDefTokenType(",");
  IElementType CaosDef_COMMAND_KEYWORD = new CaosDefTokenType("COMMAND_KEYWORD");
  IElementType CaosDef_DOC_COMMENT = new CaosDefTokenType("DOC_COMMENT");
  IElementType CaosDef_DOUBLE_QUO = new CaosDefTokenType("\"");
  IElementType CaosDef_DOUBLE_QUO_STRING = new CaosDefTokenType("DOUBLE_QUO_STRING");
  IElementType CaosDef_EQ = new CaosDefTokenType("=");
  IElementType CaosDef_ID = new CaosDefTokenType("ID");
  IElementType CaosDef_LINE_COMMENT = new CaosDefTokenType("LINE_COMMENT");
  IElementType CaosDef_LINK = new CaosDefTokenType("LINK");
  IElementType CaosDef_NEWLINE = new CaosDefTokenType("NEWLINE");
  IElementType CaosDef_OPEN_BRACKET = new CaosDefTokenType("[");
  IElementType CaosDef_OPEN_LINK = new CaosDefTokenType("<<");
  IElementType CaosDef_OPEN_PAREN = new CaosDefTokenType("(");
  IElementType CaosDef_SEMI = new CaosDefTokenType(";");
  IElementType CaosDef_SINGLE_QUO = new CaosDefTokenType("'");
  IElementType CaosDef_SINGLE_QUO_STRING = new CaosDefTokenType("SINGLE_QUO_STRING");
  IElementType CaosDef_TEXT = new CaosDefTokenType("__TEXT__IN__DOUBLE__QUO__");
  IElementType CaosDef_TYPE_KEYWORD = new CaosDefTokenType("TYPE_KEYWORD");
  IElementType CaosDef_VAR_KEYWORD = new CaosDefTokenType("VAR_KEYWORD");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == CaosDef_ARG) {
        return new CaosDefArgImpl(node);
      }
      else if (type == CaosDef_ARGS) {
        return new CaosDefArgsImpl(node);
      }
      else if (type == CaosDef_COMMAND_DEF_ELEMENT) {
        return new CaosDefCommandDefElementImpl(node);
      }
      else if (type == CaosDef_COMMAND_NAME) {
        return new CaosDefCommandNameImpl(node);
      }
      else if (type == CaosDef_COMMENT) {
        return new CaosDefCommentImpl(node);
      }
      else if (type == CaosDef_DEF_ELEMENT) {
        return new CaosDefDefElementImpl(node);
      }
      else if (type == CaosDef_INLINE_DOC_COMMENT) {
        return new CaosDefInlineDocCommentImpl(node);
      }
      else if (type == CaosDef_NAMESPACE) {
        return new CaosDefNamespaceImpl(node);
      }
      else if (type == CaosDef_NAMESPACE_NAME) {
        return new CaosDefNamespaceNameImpl(node);
      }
      else if (type == CaosDef_PARAMETER_NAME) {
        return new CaosDefParameterNameImpl(node);
      }
      else if (type == CaosDef_RETURN_TYPE) {
        return new CaosDefReturnTypeImpl(node);
      }
      else if (type == CaosDef_STRING) {
        return new CaosDefStringImpl(node);
      }
      else if (type == CaosDef_STRING_BODY) {
        return new CaosDefStringBodyImpl(node);
      }
      else if (type == CaosDef_TYPE_DEF_ELEMENT) {
        return new CaosDefTypeDefElementImpl(node);
      }
      else if (type == CaosDef_TYPE_NAME) {
        return new CaosDefTypeNameImpl(node);
      }
      else if (type == CaosDef_VAR_DEF_ELEMENT) {
        return new CaosDefVarDefElementImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
