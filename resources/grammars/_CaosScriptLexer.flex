package com.openc2e.plugins.intellij.caos.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes.*;

%%

%{
  public _CaosScriptLexer() {
    this((java.io.Reader)null);
  }

  private int braceDepth;

%}

%public
%class _CaosScriptLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

NEWLINE=\n
ENDM=[eE][nN][dD][mM]
SUBR=[sS][uU][bB][rR]
REPS=[rR][eE][pP][sS]
REPE=[rR][eE][pP][eE]
LOOP=[lL][oO][oO][pP]
UNTL=[uU][nN][tT][lL]
EVER=[eE][vV][eE][rR]
ENUM=[eE][nN][uU][mM]
NEXT=[nN][eE][xX][tT]
DOIF=[dD][oO][iI][fF]
ELIF=[Ee][Ll][iI][fF]
ELSE=[eE][lL][sS][eE]
ENDI=[eE][nN][dD][iI]
SCRP=[sS][cC][rR][pP]
VAxx=[Vv][Aa][0-9][0-9]
OVxx=[Oo][Vv][0-9][0-9]
MVxx=[Mm][Vv][0-9][0-9]
VAxx=[Mm][Vv][0-9][0-9]
OBVx=[Oo][Bb][Vv][0-9]
VARx=[Vv][Aa][Rr][0-9]
COMMENT_LITERAL=\*[^\n]*
DECIMAL=[0-9]+\.[0-9]+
INT=[0-9]+
TEXT=\[[^\]]*\]
QUOTE_STRING=\"[^\n|\"]*\"
WORD=[_a-zA-Z][_a-zA-Z0-9!#]{3}
ID=[_a-zA-Z][_a-zA-Z0-9!#]*
SPACE=[ ]
TAB=[\t]
%state START_OF_LINE IN_LINE
%%

<START_OF_LINE> {
	'\s+'				 { return WHITE_SPACE; }
    [^]					 { yybegin(IN_LINE); yypushback(1);}
}

<IN_LINE> {
  ":"                    { return CaosScript_COLON; }
  "+"                    { return CaosScript_PLUS; }
  "["                    { braceDepth++; return CaosScript_OPEN_BRACKET; }
  "]"                    { braceDepth--; return CaosScript_CLOSE_BRACKET; }
  ","                    { return CaosScript_COMMA; }
  "R"                    {
          if (braceDepth < 1) {
              yypushback(1);
              return null;
		  }
          return CaosScript_ANIM_R;
  }

  {NEWLINE}              { yybegin(START_OF_LINE); return CaosScript_NEWLINE; }
  {ENDM}                 { return CaosScript_ENDM; }
  {SUBR}                 { return CaosScript_SUBR; }
  {REPS}                 { return CaosScript_REPS; }
  {REPE}                 { return CaosScript_REPE; }
  {LOOP}                 { return CaosScript_LOOP; }
  {UNTL}                 { return CaosScript_UNTL; }
  {EVER}                 { return CaosScript_EVER; }
  {ENUM}                 { return CaosScript_ENUM; }
  {NEXT}                 { return CaosScript_NEXT; }
  {DOIF}                 { return CaosScript_DOIF; }
  {ELIF}                 { return CaosScript_ELIF; }
  {ELSE}                 { return CaosScript_ELSE; }
  {ENDI}                 { return CaosScript_ENDI; }
  {SCRP}                 { return CaosScript_SCRP; }
  {OVxx}				 { return CaosScript_OV_XX; }
  {OBVx}				 { return CaosScript_OBV_X; }
  {MVxx}				 { return CaosScript_MV_XX; }
  {VARx}				 { return CaosScript_VAR_X; }
  {OBVx}				 { return CaosScript_OBV_X; }
  {COMMENT_LITERAL}      { return CaosScript_COMMENT_LITERAL; }
  {DECIMAL}              { return CaosScript_DECIMAL; }
  {INT}                  { return CaosScript_INT; }
  {TEXT}                 { return CaosScript_TEXT_LITERAL; }
  {QUOTE_STRING}         { return CaosScript_QUOTE_STRING; }
  {ID}                   { return CaosScript_ID; }
  {SPACE}                { return CaosScript_SPACE_; }
}

<YYINITIAL> {
	[^]					 {yybegin(START_OF_LINE); yypushback(1);}
}

[^] { return BAD_CHARACTER; }
