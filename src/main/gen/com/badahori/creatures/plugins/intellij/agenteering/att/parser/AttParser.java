// This is a generated file. Not intended for manual editing.
package com.badahori.creatures.plugins.intellij.agenteering.att.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.badahori.creatures.plugins.intellij.agenteering.att.lexer.AttTypes.*;
import static com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class AttParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return file(b, l + 1);
  }

  /* ********************************************************** */
  // ERROR_VALUE_LITERAL
  public static boolean error_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "error_value")) return false;
    if (!nextTokenIs(b, ATT_ERROR_VALUE_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ATT_ERROR_VALUE_LITERAL);
    exit_section_(b, m, ATT_ERROR_VALUE, r);
    return r;
  }

  /* ********************************************************** */
  // (!<<eof>> line)*
  static boolean file(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "file")) return false;
    while (true) {
      int c = current_position_(b);
      if (!file_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "file", c)) break;
    }
    return true;
  }

  // !<<eof>> line
  private static boolean file_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "file_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = file_0_0(b, l + 1);
    r = r && line(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<eof>>
  private static boolean file_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "file_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !eof(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // INT_LITERAL
  public static boolean int_$(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "int_$")) return false;
    if (!nextTokenIs(b, ATT_INT_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ATT_INT_LITERAL);
    exit_section_(b, m, ATT_INT, r);
    return r;
  }

  /* ********************************************************** */
  // int
  // 	| 	error_value
  public static boolean item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item")) return false;
    if (!nextTokenIs(b, "<item>", ATT_ERROR_VALUE_LITERAL, ATT_INT_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ATT_ITEM, "<item>");
    r = int_$(b, l + 1);
    if (!r) r = error_value(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // line_items (newline|<<eof>>)//
  // 	|	newline
  public static boolean line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ATT_LINE, "<line>");
    r = line_0(b, l + 1);
    if (!r) r = newline(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // line_items (newline|<<eof>>)
  private static boolean line_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = line_items(b, l + 1);
    r = r && line_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // newline|<<eof>>
  private static boolean line_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = newline(b, l + 1);
    if (!r) r = eof(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // space? item (space item)*
  // 	|	space
  static boolean line_items(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_items")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = line_items_0(b, l + 1);
    if (!r) r = space(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // space? item (space item)*
  private static boolean line_items_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_items_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = line_items_0_0(b, l + 1);
    r = r && item(b, l + 1);
    r = r && line_items_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // space?
  private static boolean line_items_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_items_0_0")) return false;
    space(b, l + 1);
    return true;
  }

  // (space item)*
  private static boolean line_items_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_items_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!line_items_0_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "line_items_0_2", c)) break;
    }
    return true;
  }

  // space item
  private static boolean line_items_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_items_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = space(b, l + 1);
    r = r && item(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(NEWLINE_LITERAL)
  static boolean line_items_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_items_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, ATT_NEWLINE_LITERAL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // NEWLINE_LITERAL+
  public static boolean newline(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newline")) return false;
    if (!nextTokenIs(b, ATT_NEWLINE_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ATT_NEWLINE_LITERAL);
    while (r) {
      int c = current_position_(b);
      if (!consumeToken(b, ATT_NEWLINE_LITERAL)) break;
      if (!empty_element_parsed_guard_(b, "newline", c)) break;
    }
    exit_section_(b, m, ATT_NEWLINE, r);
    return r;
  }

  /* ********************************************************** */
  // SPACE_LITERAL
  // 	|	ERROR_SPACE_LITERAL
  public static boolean space(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "space")) return false;
    if (!nextTokenIs(b, "<space>", ATT_ERROR_SPACE_LITERAL, ATT_SPACE_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ATT_SPACE, "<space>");
    r = consumeToken(b, ATT_SPACE_LITERAL);
    if (!r) r = consumeToken(b, ATT_ERROR_SPACE_LITERAL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
