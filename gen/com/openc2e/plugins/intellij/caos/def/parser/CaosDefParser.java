// This is a generated file. Not intended for manual editing.
package com.openc2e.plugins.intellij.caos.def.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.openc2e.plugins.intellij.caos.def.lexer.CaosDefTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class CaosDefParser implements PsiParser, LightPsiParser {

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
    return def(b, l + 1);
  }

  /* ********************************************************** */
  // parameter_name ':' type_name ('=' string)?
  public static boolean arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arg")) return false;
    if (!nextTokenIs(b, CaosDef_ID)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CaosDef_ARG, null);
    r = parameter_name(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, CaosDef_COLON));
    r = p && report_error_(b, type_name(b, l + 1)) && r;
    r = p && arg_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ('=' string)?
  private static boolean arg_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arg_3")) return false;
    arg_3_0(b, l + 1);
    return true;
  }

  // '=' string
  private static boolean arg_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arg_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_EQ);
    r = r && string(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // arg (',' arg)*
  public static boolean args(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "args")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CaosDef_ARGS, "<args>");
    r = arg(b, l + 1);
    p = r; // pin = 1
    r = r && args_1(b, l + 1);
    exit_section_(b, l, m, r, p, args_recover_parser_);
    return r || p;
  }

  // (',' arg)*
  private static boolean args_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "args_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!args_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "args_1", c)) break;
    }
    return true;
  }

  // ',' arg
  private static boolean args_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "args_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_COMMA);
    r = r && arg(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(NEWLINE|']')
  static boolean args_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "args_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !args_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // NEWLINE|']'
  private static boolean args_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "args_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_NEWLINE);
    if (!r) r = consumeToken(b, CaosDef_CLOSE_BRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // namespace? command_name '(' return_type ('=' string)? ')'
  static boolean command(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command")) return false;
    if (!nextTokenIs(b, "", CaosDef_CMND, CaosDef_ID)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = command_0(b, l + 1);
    r = r && command_name(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, consumeToken(b, CaosDef_OPEN_PAREN));
    r = p && report_error_(b, return_type(b, l + 1)) && r;
    r = p && report_error_(b, command_4(b, l + 1)) && r;
    r = p && consumeToken(b, CaosDef_CLOSE_PAREN) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // namespace?
  private static boolean command_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_0")) return false;
    namespace(b, l + 1);
    return true;
  }

  // ('=' string)?
  private static boolean command_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_4")) return false;
    command_4_0(b, l + 1);
    return true;
  }

  // '=' string
  private static boolean command_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_EQ);
    r = r && string(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // COMMAND_KEYWORD command ARGS_KEYWORD '=' '[' args ']' ('=' inline_doc_comment)? line_terminator
  public static boolean command_def_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_def_element")) return false;
    if (!nextTokenIs(b, CaosDef_COMMAND_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CaosDef_COMMAND_DEF_ELEMENT, null);
    r = consumeToken(b, CaosDef_COMMAND_KEYWORD);
    r = r && command(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, consumeTokens(b, -1, CaosDef_ARGS_KEYWORD, CaosDef_EQ, CaosDef_OPEN_BRACKET));
    r = p && report_error_(b, args(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, CaosDef_CLOSE_BRACKET)) && r;
    r = p && report_error_(b, command_def_element_7(b, l + 1)) && r;
    r = p && line_terminator(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ('=' inline_doc_comment)?
  private static boolean command_def_element_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_def_element_7")) return false;
    command_def_element_7_0(b, l + 1);
    return true;
  }

  // '=' inline_doc_comment
  private static boolean command_def_element_7_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_def_element_7_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_EQ);
    r = r && inline_doc_comment(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // CMND
  public static boolean command_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "command_name")) return false;
    if (!nextTokenIs(b, CaosDef_CMND)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_CMND);
    exit_section_(b, m, CaosDef_COMMAND_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // DOC_COMMENT
  // 	| 	LINE_COMMENT
  public static boolean comment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comment")) return false;
    if (!nextTokenIs(b, "<comment>", CaosDef_DOC_COMMENT, CaosDef_LINE_COMMENT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CaosDef_COMMENT, "<comment>");
    r = consumeToken(b, CaosDef_DOC_COMMENT);
    if (!r) r = consumeToken(b, CaosDef_LINE_COMMENT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (!<<eof>> def_element)*
  static boolean def(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "def")) return false;
    while (true) {
      int c = current_position_(b);
      if (!def_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "def", c)) break;
    }
    return true;
  }

  // !<<eof>> def_element
  private static boolean def_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "def_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = def_0_0(b, l + 1);
    r = r && def_element(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !<<eof>>
  private static boolean def_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "def_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !eof(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // var_def_element
  // 	| command_def_element
  // 	| type_def_element
  // 	| comment
  // 	| NEWLINE
  // 	| ';'
  public static boolean def_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "def_element")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CaosDef_DEF_ELEMENT, "<def element>");
    r = var_def_element(b, l + 1);
    if (!r) r = command_def_element(b, l + 1);
    if (!r) r = type_def_element(b, l + 1);
    if (!r) r = comment(b, l + 1);
    if (!r) r = consumeToken(b, CaosDef_NEWLINE);
    if (!r) r = consumeToken(b, CaosDef_SEMI);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // string
  public static boolean inline_doc_comment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_doc_comment")) return false;
    if (!nextTokenIs(b, "<inline doc comment>", CaosDef_DOUBLE_QUO, CaosDef_SINGLE_QUO)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CaosDef_INLINE_DOC_COMMENT, "<inline doc comment>");
    r = string(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // NEWLINE ';'?
  // 	|	';'?<<eof>>
  static boolean line_terminator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_terminator")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = line_terminator_0(b, l + 1);
    if (!r) r = line_terminator_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // NEWLINE ';'?
  private static boolean line_terminator_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_terminator_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_NEWLINE);
    r = r && line_terminator_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean line_terminator_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_terminator_0_1")) return false;
    consumeToken(b, CaosDef_SEMI);
    return true;
  }

  // ';'?<<eof>>
  private static boolean line_terminator_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_terminator_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = line_terminator_1_0(b, l + 1);
    r = r && eof(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean line_terminator_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_terminator_1_0")) return false;
    consumeToken(b, CaosDef_SEMI);
    return true;
  }

  /* ********************************************************** */
  // LINK_TOKEN
  static boolean link(PsiBuilder b, int l) {
    return consumeToken(b, CaosDef_LINK_TOKEN);
  }

  /* ********************************************************** */
  // namespace_name ':'
  public static boolean namespace(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace")) return false;
    if (!nextTokenIs(b, CaosDef_ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = namespace_name(b, l + 1);
    r = r && consumeToken(b, CaosDef_COLON);
    exit_section_(b, m, CaosDef_NAMESPACE, r);
    return r;
  }

  /* ********************************************************** */
  // ID
  public static boolean namespace_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_name")) return false;
    if (!nextTokenIs(b, CaosDef_ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_ID);
    exit_section_(b, m, CaosDef_NAMESPACE_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // ID
  public static boolean parameter_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_name")) return false;
    if (!nextTokenIs(b, CaosDef_ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_ID);
    exit_section_(b, m, CaosDef_PARAMETER_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // ID
  public static boolean return_type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "return_type")) return false;
    if (!nextTokenIs(b, CaosDef_ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_ID);
    exit_section_(b, m, CaosDef_RETURN_TYPE, r);
    return r;
  }

  /* ********************************************************** */
  // SINGLE_QUO <<enterMode "single_qou">>string_body SINGLE_QUO <<exitMode "single_qou">>
  // 	| 	DOUBLE_QUO <<enterMode "double_qou">>string_body <<enterMode "double_qou">>DOUBLE_QUO
  public static boolean string(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string")) return false;
    if (!nextTokenIs(b, "<string>", CaosDef_DOUBLE_QUO, CaosDef_SINGLE_QUO)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CaosDef_STRING, "<string>");
    r = string_0(b, l + 1);
    if (!r) r = string_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // SINGLE_QUO <<enterMode "single_qou">>string_body SINGLE_QUO <<exitMode "single_qou">>
  private static boolean string_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_SINGLE_QUO);
    r = r && enterMode(b, l + 1, "single_qou");
    r = r && string_body(b, l + 1);
    r = r && consumeToken(b, CaosDef_SINGLE_QUO);
    r = r && exitMode(b, l + 1, "single_qou");
    exit_section_(b, m, null, r);
    return r;
  }

  // DOUBLE_QUO <<enterMode "double_qou">>string_body <<enterMode "double_qou">>DOUBLE_QUO
  private static boolean string_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_DOUBLE_QUO);
    r = r && enterMode(b, l + 1, "double_qou");
    r = r && string_body(b, l + 1);
    r = r && enterMode(b, l + 1, "double_qou");
    r = r && consumeToken(b, CaosDef_DOUBLE_QUO);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // string_body_parts+
  public static boolean string_body(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_body")) return false;
    if (!nextTokenIs(b, "<string body>", CaosDef_LINK_TOKEN, CaosDef_TEXT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CaosDef_STRING_BODY, "<string body>");
    r = string_body_parts(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!string_body_parts(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "string_body", c)) break;
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // TEXT | link
  static boolean string_body_parts(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_body_parts")) return false;
    if (!nextTokenIs(b, "", CaosDef_LINK_TOKEN, CaosDef_TEXT)) return false;
    boolean r;
    r = consumeToken(b, CaosDef_TEXT);
    if (!r) r = link(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // !(<<inMode "single_qou">>SINGLE_QUO)
  // 	|	!(<<inMode "double_qou">>DOUBLE_QUO)
  static boolean string_body_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_body_recover")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = string_body_recover_0(b, l + 1);
    if (!r) r = string_body_recover_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // !(<<inMode "single_qou">>SINGLE_QUO)
  private static boolean string_body_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_body_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !string_body_recover_0_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<inMode "single_qou">>SINGLE_QUO
  private static boolean string_body_recover_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_body_recover_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = inMode(b, l + 1, "single_qou");
    r = r && consumeToken(b, CaosDef_SINGLE_QUO);
    exit_section_(b, m, null, r);
    return r;
  }

  // !(<<inMode "double_qou">>DOUBLE_QUO)
  private static boolean string_body_recover_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_body_recover_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !string_body_recover_1_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<inMode "double_qou">>DOUBLE_QUO
  private static boolean string_body_recover_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_body_recover_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = inMode(b, l + 1, "double_qou");
    r = r && consumeToken(b, CaosDef_DOUBLE_QUO);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // TYPE_KEYWORD type_name '=' string line_terminator?
  public static boolean type_def_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_def_element")) return false;
    if (!nextTokenIs(b, CaosDef_TYPE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_TYPE_KEYWORD);
    r = r && type_name(b, l + 1);
    r = r && consumeToken(b, CaosDef_EQ);
    r = r && string(b, l + 1);
    r = r && type_def_element_4(b, l + 1);
    exit_section_(b, m, CaosDef_TYPE_DEF_ELEMENT, r);
    return r;
  }

  // line_terminator?
  private static boolean type_def_element_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_def_element_4")) return false;
    line_terminator(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // ID
  public static boolean type_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_name")) return false;
    if (!nextTokenIs(b, CaosDef_ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_ID);
    exit_section_(b, m, CaosDef_TYPE_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // VAR_KEYWORD namespace? parameter_name '(' type_name  ')' ('=' inline_doc_comment)? line_terminator
  public static boolean var_def_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_def_element")) return false;
    if (!nextTokenIs(b, CaosDef_VAR_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CaosDef_VAR_DEF_ELEMENT, null);
    r = consumeToken(b, CaosDef_VAR_KEYWORD);
    r = r && var_def_element_1(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, parameter_name(b, l + 1));
    r = p && report_error_(b, consumeToken(b, CaosDef_OPEN_PAREN)) && r;
    r = p && report_error_(b, type_name(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, CaosDef_CLOSE_PAREN)) && r;
    r = p && report_error_(b, var_def_element_6(b, l + 1)) && r;
    r = p && line_terminator(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // namespace?
  private static boolean var_def_element_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_def_element_1")) return false;
    namespace(b, l + 1);
    return true;
  }

  // ('=' inline_doc_comment)?
  private static boolean var_def_element_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_def_element_6")) return false;
    var_def_element_6_0(b, l + 1);
    return true;
  }

  // '=' inline_doc_comment
  private static boolean var_def_element_6_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_def_element_6_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CaosDef_EQ);
    r = r && inline_doc_comment(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  static final Parser args_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return args_recover(b, l + 1);
    }
  };
}
