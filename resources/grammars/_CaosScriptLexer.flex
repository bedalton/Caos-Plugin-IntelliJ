package com.openc2e.plugins.intellij.caos.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

import java.util.List;
import java.util.Arrays;import java.util.logging.Logger;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes.*;
import com.openc2e.plugins.intellij.caos.utils.CaosScriptArrayUtils;


%%

%{
	public _CaosScriptLexer() {
		this((java.io.Reader)null);
	}

	private int braceDepth;
	private static final List<Character> BYTE_STRING_CHARS = CaosScriptArrayUtils.toList("0123456789 R".toCharArray());


	protected boolean isByteString() {
		int index = 0;
		try {
			char nextChar = yycharat(++index);
			while (nextChar != ']') {
				if (!BYTE_STRING_CHARS.contains(nextChar)) {
	  				return false;
				}
				nextChar = yycharat(++index);
			}
			return true;
		} catch (Exception e) {
			return true;
		}
	}

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
RETN=[Rr][Ee][Tt][Nn]
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
VARx=[Vv][Aa][Rr][0-9]
VAxx=[Vv][Aa][0-9][0-9]
OBVx=[Oo][Bb][Vv][0-9]
OVxx=[Oo][Vv][0-9][0-9]
MVxx=[Mm][Vv][0-9][0-9]
COMMENT_LITERAL=\*[^\n]*
DECIMAL=[0-9]+\.[0-9]+
INT=[0-9]+
TEXT=[^\]]+
QUOTE_STRING=\"[^\n|\"]*\"
WORD=[_a-zA-Z][_a-zA-Z0-9]{2}[_a-zA-Z0-9!#:]
ID=[_a-zA-Z][_a-zA-Z0-9!#]*
SPACE=[ ]
EQ=[Ee][Qq]
NE=[Nn][Ee]
LT=[Ll][Tt]
GT=[Gg][Tt]
LE=[Ll][Ee]
GE=[Gg][Ee]
BT=[Bb][Tt]
BF=[Bb][Ff]
EQ_C1={EQ}|{NE}|{GT}|{LT}|{LE}|{GE}|{BT}|{BF}
EQ_NEW="="|"<>"|">"|">="|"<"|"<="

%state START_OF_LINE IN_LINE IN_BYTE_STRING IN_TEXT
%%

<START_OF_LINE> {
	\s+				 	 { return WHITE_SPACE; }
    [^]					 { yybegin(IN_LINE); yypushback(yylength());}
}

<IN_BYTE_STRING> {
	{INT}				 { return CaosScript_INT; }
	{SPACE}				 { return CaosScript_SPACE_; }
    "R"					 { return CaosScript_ANIM_R; }
}

<IN_TEXT> {
	{TEXT}				{ return CaosScript_TEXT_LITERAL; }
}

<IN_BYTE_STRING, IN_TEXT> {
    ']'					 { yybegin(IN_LINE); return CaosScript_CLOSE_BRACKET; }
    [^]					 { yybegin(IN_LINE); yypushback(yylength());}
}

<IN_LINE> {
  ":"                    { return CaosScript_COLON; }
  "+"                    { return CaosScript_PLUS; }
  "["                    { braceDepth++; yybegin(isByteString() ? IN_BYTE_STRING : IN_TEXT); return CaosScript_OPEN_BRACKET; }
  "]"                    { braceDepth--; return CaosScript_CLOSE_BRACKET; }
  ","                    { return CaosScript_COMMA; }
  {EQ_C1}				 { return CaosScript_EQ_OP_OLD_; }
  {EQ_NEW}			 { return CaosScript_EQ_OP_NEW_; }

  {NEWLINE}              { yybegin(START_OF_LINE); if(yycharat(-1) == ',') return WHITE_SPACE; return CaosScript_NEWLINE; }
  {ENDM}                 { return CaosScript_ENDM; }
  {SUBR}                 { return CaosScript_SUBR; }
  {RETN}				 { return CaosScript_RETN; }
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
  {VAxx}				 { return CaosScript_VA_XX; }
  {COMMENT_LITERAL}      { yybegin(START_OF_LINE); return CaosScript_COMMENT_LITERAL; }
  {DECIMAL}              { return CaosScript_DECIMAL; }
  {INT}                  { return CaosScript_INT; }
  {QUOTE_STRING}         { return CaosScript_QUOTE_STRING; }
  {WORD}				 { return CaosScript_WORD; }
  {ID}                   { return CaosScript_ID; }
  {SPACE}                { return CaosScript_SPACE_; }
}

<YYINITIAL> {
	[^]					 {yybegin(START_OF_LINE); yypushback(yylength());}
}

[^] { return BAD_CHARACTER; }
