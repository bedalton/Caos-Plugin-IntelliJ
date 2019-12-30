package com.openc2e.plugins.intellij.caos.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.openc2e.plugins.intellij.caos.lexer.CaosTypes.*;

%%

%{
  public _CaosLexer() {
    this((java.io.Reader)null);
  }

  private int braceDepth;

%}

%public
%class _CaosLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

NEWLINE=\n
ENDM=[eE][nN][dD][mM]
SUBR=[sS][uU][bB][rR]
GSUB=[gG][sS][uU][bB]
REPS=[rR][eE][pP][sS]
REPE=[rR][eE][pP][eE]
LOOP=[lL][oO][oO][pP]
UNTL=[uU][nN][tT][lL]
EVER=[eE][vV][eE][rR]
ENUM=[eE][nN][uU][mM]
NEXT=[nN][eE][xX][tT]
DOIF=[dD][oO][iI][fF]
ELSE=[eE][lL][sS][eE]
ENDI=[eE][nN][dD][iI]
SCRP=[sS][cC][rR][pP]
COMMENT_LITERAL=\*[^\n]*
DECIMAL=[0-9]+\.[0-9]+
INT=[0-9]+
TEXT=\[[^\]]*\]
QUOTE_STRING=\"[^\n|\"]*\"
ID=[_a-zA-Z][_a-zA-Z0-9!#]*
SPACE=[ ]
TAB=[\t]

%%



<YYINITIAL> {
  ":"                    { return Caos_COLON; }
  "+"                    { return Caos_PLUS; }
  "["                    { braceDepth++; return Caos_OPEN_BRACKET; }
  "]"                    { braceDepth--; return Caos_CLOSE_BRACKET; }
  ","                    { return Caos_COMMA; }
  "R"                    {
          if (braceDepth < 1) {
              yypushback(1);
              return null;
		  }
          return Caos_ANIM_R;
  }

  {NEWLINE}              { return Caos_NEWLINE; }
  {ENDM}                 { return Caos_ENDM; }
  {SUBR}                 { return Caos_SUBR; }
  {GSUB}                 { return Caos_GSUB; }
  {REPS}                 { return Caos_REPS; }
  {REPE}                 { return Caos_REPE; }
  {LOOP}                 { return Caos_LOOP; }
  {UNTL}                 { return Caos_UNTL; }
  {EVER}                 { return Caos_EVER; }
  {ENUM}                 { return Caos_ENUM; }
  {NEXT}                 { return Caos_NEXT; }
  {DOIF}                 { return Caos_DOIF; }
  {ELSE}                 { return Caos_ELSE; }
  {ENDI}                 { return Caos_ENDI; }
  {SCRP}                 { return Caos_SCRP; }
  {COMMENT_LITERAL}      { return Caos_COMMENT_LITERAL; }
  {DECIMAL}              { return Caos_DECIMAL; }
  {INT}                  { return Caos_INT; }
  {TEXT}                 { return Caos_TEXT_LITERAL; }
  {QUOTE_STRING}         { return Caos_QUOTE_STRING; }
  {ID}                   { return Caos_ID; }
  {SPACE}                { return Caos_SPACE; }
  {TAB}                  { return Caos_TAB; }

}

[^] { return BAD_CHARACTER; }
