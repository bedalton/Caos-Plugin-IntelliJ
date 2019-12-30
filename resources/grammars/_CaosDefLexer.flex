package grammars;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.openc2e.plugins.intellij.caos.def.lexer.CaosDefTypes.*;

%%

%{
  public _CaosDefLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _CaosDefLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

WHITE_SPACE=\s+
TEXT=[^ ]+
DOC_COMMENT_OPEN="/"[*]+
DOC_COMMENT_CLOSE=[*]+"/"
LINE_COMMENT="//"[^\n]*
WORD=[a-zA-Z_][a-zA-Z0-9#!$_]{3}
TYPE_LINK=[@]{[^}]}
ID=[_a-zA-Z][_a-zA-Z0-9]*
SPACE=[ \t\n\x0B\f\r]+
CODE_BLOCK_LITERAL=#\{[^}]*\}
WORD_LINK=\[[^\]]*\]
AT_RVALUE=[@][rR][vV][aA][lL][uU][eE]
AT_LVALUE=[@][lL][vV][aA][lL][uU][eE]
AT_PARAM=[@][pP][aA][rR][aA][mM]
AT_RETURNS=[@][rR][eE][tT][uU][rR][nN][sS]?
AT_ID=[@][a-zA-Z_][a-zA-Z_0-9]
INT=[0-9]+
TYPE_DEF_KEY=[^=]+
TYPE_DEF_VALUE = [^-\n,]+
DEF_TEXT=[^\n]+
STRING_LITERAL_TYPE=\[[^\]]+\]

%state IN_COMMENT IN_PARAM_COMMENT IN_TYPEDEF

%%


<IN_COMMENT, IN_PARAM_COMMENT> {

  "*"	                     { return CaosDef_LEADING_ASTRISK; }
  "*/"						 { return CaosDef_DOC_COMMENT_CLOSE; }
  {WORD_LINK}				 { return CaosDef_LINK; }
  {STRING_LITERAL_TYPE}		 { return CaosDef_STRING_LITERAL_TYPE; }
  {TYPE_LINK}				 { return CaosDef_TYPE_LINK; }
  {TEXT}					 { return CaosDef_TEXT; }
  {CODE_BLOCK_LITERAL}       { return CaosDef_CODE_BLOCK_LITERAL; }
  {AT_RVALUE}                { return CaosDef_AT_RVALUE; }
  {AT_LVALUE}                { return CaosDef_AT_LVALUE; }
  {AT_PARAM}                 { return CaosDef_AT_PARAM; }
  {AT_RETURNS}               { return CaosDef_AT_RETURNS; }
  {AT_ID}					 { return CaosDef_AT_ID; }
  {ID}						 { return CaosDef_ID; }
}

<IN_TYPEDEF> {
  "="                        { return CaosDef_EQ; }
  "-"                        { return CaosDef_DASH; }
  "}"						 { return CaosDef_CLOSE_BRACE; }
  "{"						 { return CaosDef_OPEN_BRACE; }
  ","                        { return CaosDef_COMMA; }
  ")"						 { yybegin(YYINITIAL); return CaosDef_CLOSE_PAREN; }
  {TYPE_DEF_KEY}			 { return CaosDef_TYPE_DEF_KEY; }
  {TYPE_DEF_VALUE}			 { return CaosDef_TYPE_DEF_VALUE; }
  {DEF_TEXT}				 { return CaosDef_TEXT; }
}

<YYINITIAL> {
  {WHITE_SPACE}              { return WHITE_SPACE; }
  "/*"						 { yybegin(IN_COMMENT); return CaosDef_DOC_COMMENT_OPEN; }
  ";"                        { return CaosDef_SEMI; }
  ":"                        { return CaosDef_COLON; }
  "("                        { return CaosDef_OPEN_PAREN; }
  ")"                        { return CaosDef_CLOSE_PAREN; }

  {DOC_COMMENT_OPEN}         { return CaosDef_DOC_COMMENT_OPEN; }
  {DOC_COMMENT_CLOSE}        { return CaosDef_DOC_COMMENT_CLOSE; }
  {LINE_COMMENT}             { return CaosDef_LINE_COMMENT; }
  {WORD}                     { return CaosDef_WORD; }
  {ID}                       { return CaosDef_ID; }
  {SPACE}                    { return CaosDef_SPACE; }
  {AT_ID}                    { yybegin(IN_TYPEDEF); return CaosDef_AT_ID; }

}

[^] { return BAD_CHARACTER; }
