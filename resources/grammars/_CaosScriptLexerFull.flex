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
  [sS][tT][iI][mM]       { return CaosScript_K_STIM; }
  [wW][rR][iI][tT]       { return CaosScript_K_WRIT; }
  [sS][hH][oO][uU]       { return CaosScript_K_SHOU; }
  [sS][iI][gG][nN]       { return CaosScript_K_SIGN; }
  [tT][aA][cC][tT]       { return CaosScript_K_TACT; }
  [nN][eE][wW][:]        { return CaosScript_K_NEW_COL; }
  [bB][kK][bB][dD]       { return CaosScript_K_BKBD; }
  [rR][oO][oO][mM]       { return CaosScript_K_ROOM; }
  [sS][iI][mM][pP]       { return CaosScript_K_SIMP; }
  [pP][aA][rR][tT]       { return CaosScript_K_PART; }
  [sS][pP][oO][tT]       { return CaosScript_K_SPOT; }
  [cC][bB][tT][nN]       { return CaosScript_K_CBTN; }
  [cC][oO][mM][pP]       { return CaosScript_K_COMP; }
  [sS][cC][eE][nN]       { return CaosScript_K_SCEN; }
  [cC][aA][bB][nN]       { return CaosScript_K_CABN; }
  [sS][cC][rR][xX]       { return CaosScript_K_SCRX; }
  [sS][yY][sS][:]        { return CaosScript_K_SYS_COL; }
  [eE][dD][iI][tT]       { return CaosScript_K_EDIT; }
  [tT][oO][oO][lL]       { return CaosScript_K_TOOL; }
  [wW][pP][oO][sS]       { return CaosScript_K_WPOS; }
  [gG][eE][nN][eE]       { return CaosScript_K_GENE; }
  [lL][iI][fF][tT]       { return CaosScript_K_LIFT; }
  [vV][hH][cC][lL]       { return CaosScript_K_VHCL; }
  [rR][nN][dD][vV]       { return CaosScript_K_RNDV; }
  [bB][bB][dD][:]        { return CaosScript_K_BBD_COL; }
  [wW][oO][rR][dD]       { return CaosScript_K_WORD; }
  [dD][dD][eE][:]        { return CaosScript_K_DDE_COL; }
  [cC][eE][lL][lL]       { return CaosScript_K_CELL; }
  [dD][oO][iI][fF]       { return CaosScript_K_DOIF; }
  [eE][nN][uU][mM]       { return CaosScript_K_ENUM; }
  [fF][iI][rR][eE]       { return CaosScript_K_FIRE; }
  [lL][tT][cC][yY]       { return CaosScript_K_LTCY; }
  [cC][rR][eE][aA]       { return CaosScript_K_CREA; }
  [rR][tT][aA][rR]       { return CaosScript_K_RTAR; }
  [tT][rR][iI][gG]       { return CaosScript_K_TRIG; }
  [uU][nN][tT][lL]       { return CaosScript_K_UNTL; }
  [aA][dD][dD][vV]       { return CaosScript_K_ADDV; }
  [aA][nN][dD][vV]       { return CaosScript_K_ANDV; }
  [dD][iI][vV][vV]       { return CaosScript_K_DIVV; }
  [mM][oO][dD][vV]       { return CaosScript_K_MODV; }
  [mM][uU][lL][vV]       { return CaosScript_K_MULV; }
  [oO][rR][rR][vV]       { return CaosScript_K_ORRV; }
  [sS][eE][tT][vV]       { return CaosScript_K_SETV; }
  [sS][uU][bB][vV]       { return CaosScript_K_SUBV; }
  [bB][hH][vV][rR]       { return CaosScript_K_BHVR; }
  [cC][hH][eE][mM]       { return CaosScript_K_CHEM; }
  [pP][uU][tT][bB]       { return CaosScript_K_PUTB; }
  [kK][nN][oO][bB]       { return CaosScript_K_KNOB; }
  [mM][cC][rR][tT]       { return CaosScript_K_MCRT; }
  [mM][eE][sS][gG]       { return CaosScript_K_MESG; }
  [mM][vV][tT][oO]       { return CaosScript_K_MVTO; }
  [sS][pP][aA][sS]       { return CaosScript_K_SPAS; }
  [sS][tT][mM][#]        { return CaosScript_K_STM_NUM; }
  [cC][mM][rR][aA]       { return CaosScript_K_CMRA; }
  [gG][rR][nN][dD]       { return CaosScript_K_GRND; }
  [tT][eE][lL][eE]       { return CaosScript_K_TELE; }
  [sS][nN][dD][qQ]       { return CaosScript_K_SNDQ; }
  [aA][bB][rR][tT]       { return CaosScript_K_ABRT; }
  [aA][pP][pP][rR]       { return CaosScript_K_APPR; }
  [cC][mM][nN][dD]       { return CaosScript_K_CMND; }
  [dD][iI][eE][dD]       { return CaosScript_K_DIED; }
  [hH][aA][tT][cC]       { return CaosScript_K_HATC; }
  [lL][iI][vV][eE]       { return CaosScript_K_LIVE; }
  [lL][oO][bB][eE]       { return CaosScript_K_LOBE; }
  [nN][eE][gG][gG]       { return CaosScript_K_NEGG; }
  [pP][aA][nN][cC]       { return CaosScript_K_PANC; }
  [pP][iI][cC][tT]       { return CaosScript_K_PICT; }
  [sS][cC][rR][pP]       { return CaosScript_K_SCRP; }
  [dD][oO][nN][eE]       { return CaosScript_K_DONE; }
  [dD][pP][aA][sS]       { return CaosScript_K_DPAS; }
  [dD][rR][oO][pP]       { return CaosScript_K_DROP; }
  [eE][lL][sS][eE]       { return CaosScript_K_ELSE; }
  [eE][nN][dD][iI]       { return CaosScript_K_ENDI; }
  [eE][nN][dD][mM]       { return CaosScript_K_ENDM; }
  [gG][pP][aA][sS]       { return CaosScript_K_GPAS; }
  [iI][nN][sS][tT]       { return CaosScript_K_INST; }
  [kK][iI][lL][lL]       { return CaosScript_K_KILL; }
  [lL][oO][cC][kK]       { return CaosScript_K_LOCK; }
  [lL][oO][oO][pP]       { return CaosScript_K_LOOP; }
  [mM][aA][tT][eE]       { return CaosScript_K_MATE; }
  [mM][vV][bB][yY]       { return CaosScript_K_MVBY; }
  [nN][eE][xX][tT]       { return CaosScript_K_NEXT; }
  [oO][vV][eE][rR]       { return CaosScript_K_OVER; }
  [pP][oO][iI][nN]       { return CaosScript_K_POIN; }
  [qQ][uU][iI][tT]       { return CaosScript_K_QUIT; }
  [rR][eE][pP][eE]       { return CaosScript_K_REPE; }
  [rR][eE][tT][nN]       { return CaosScript_K_RETN; }
  [sS][aA][yY][nN]       { return CaosScript_K_SAYN; }
  [sS][lL][iI][mM]       { return CaosScript_K_SLIM; }
  [sS][lL][oO][wW]       { return CaosScript_K_SLOW; }
  [sS][nN][eE][zZ]       { return CaosScript_K_SNEZ; }
  [sS][tT][oO][pP]       { return CaosScript_K_STOP; }
  [sS][tT][pP][cC]       { return CaosScript_K_STPC; }
  [cC][aA][mM][tT]       { return CaosScript_K_CAMT; }
  [wW][tT][oO][pP]       { return CaosScript_K_WTOP; }
  [tT][oO][uU][cC]       { return CaosScript_K_TOUC; }
  [uU][nN][lL][kK]       { return CaosScript_K_UNLK; }
  [wW][aA][lL][kK]       { return CaosScript_K_WALK; }
  [nN][eE][gG][vV]       { return CaosScript_K_NEGV; }
  [gG][sS][uU][bB]       { return CaosScript_K_GSUB; }
  [pP][lL][dD][sS]       { return CaosScript_K_PLDS; }
  [sS][nN][dD][cC]       { return CaosScript_K_SNDC; }
  [sS][nN][dD][eE]       { return CaosScript_K_SNDE; }
  [sS][nN][dD][fF]       { return CaosScript_K_SNDF; }
  [sS][nN][dD][lL]       { return CaosScript_K_SNDL; }
  [sS][uU][bB][rR]       { return CaosScript_K_SUBR; }
  [aA][iI][mM][:]        { return CaosScript_K_AIM_COL; }
  [aA][nN][iI][mM]       { return CaosScript_K_ANIM; }
  [aA][sS][lL][pP]       { return CaosScript_K_ASLP; }
  [bB][aA][sS][eE]       { return CaosScript_K_BASE; }
  [eE][mM][iI][tT]       { return CaosScript_K_EMIT; }
  [sS][hH][oO][wW]       { return CaosScript_K_SHOW; }
  [bB][dD][vV][gG]       { return CaosScript_K_BDVG; }
  [dD][bB][gG][mM]       { return CaosScript_K_DBGM; }
  [dD][bB][uU][gG]       { return CaosScript_K_DBUG; }
  [gG][eE][tT][bB]       { return CaosScript_K_GETB; }
  [pP][uU][tT][sS]       { return CaosScript_K_PUTS; }
  [pP][uU][tT][vV]       { return CaosScript_K_PUTV; }
  [dD][rR][eE][aA]       { return CaosScript_K_DREA; }
  [eE][vV][nN][tT]       { return CaosScript_K_EVNT; }
  [iI][mM][pP][tT]       { return CaosScript_K_IMPT; }
  [pP][oO][sS][eE]       { return CaosScript_K_POSE; }
  [pP][rR][lL][dD]       { return CaosScript_K_PRLD; }
  [rR][eE][pP][sS]       { return CaosScript_K_REPS; }
  [rR][mM][eE][vV]       { return CaosScript_K_RMEV; }
  [sS][aA][yY][$]        { return CaosScript_K_SAY_DOL; }
  [sS][nN][dD][vV]       { return CaosScript_K_SNDV; }
  [tT][aA][rR][gG]       { return CaosScript_K_TARG; }
  [tT][iI][cC][kK]       { return CaosScript_K_TICK; }
  [vV][rR][sS][nN]       { return CaosScript_K_VRSN; }
  [wW][aA][iI][tT]       { return CaosScript_K_WAIT; }
  [wW][rR][lL][dD]       { return CaosScript_K_WRLD; }
  [tT][oO][tT][lL]       { return CaosScript_K_TOTL; }
  [oO][bB][vV][xX]       { return CaosScript_K_OBVX; }
  [vV][aA][rR][xX]       { return CaosScript_K_VARX; }
  [_][iI][tT][_]         { return CaosScript_K__IT_; }
  [aA][cC][tT][vV]       { return CaosScript_K_ACTV; }
  [aA][tT][tT][nN]       { return CaosScript_K_ATTN; }
  [aA][tT][tT][rR]       { return CaosScript_K_ATTR; }
  [bB][aA][bB][yY]       { return CaosScript_K_BABY; }
  [bB][uU][mM][pP]       { return CaosScript_K_BUMP; }
  [cC][aA][gG][eE]       { return CaosScript_K_CAGE; }
  [cC][aA][mM][nN]       { return CaosScript_K_CAMN; }
  [cC][aA][rR][rR]       { return CaosScript_K_CARR; }
  [cC][lL][aA][sS]       { return CaosScript_K_CLAS; }
  [dD][eE][aA][dD]       { return CaosScript_K_DEAD; }
  [dD][rR][vV][!]        { return CaosScript_K_DRV_EXL; }
  [eE][xX][eE][cC]       { return CaosScript_K_EXEC; }
  [fF][mM][lL][yY]       { return CaosScript_K_FMLY; }
  [fF][rR][oO][mM]       { return CaosScript_K_FROM; }
  [gG][nN][dD][#]        { return CaosScript_K_GND_NUM; }
  [gG][nN][dD][wW]       { return CaosScript_K_GNDW; }
  [gG][nN][uU][sS]       { return CaosScript_K_GNUS; }
  [hH][gG][hH][tT]       { return CaosScript_K_HGHT; }
  [hH][oO][uU][rR]       { return CaosScript_K_HOUR; }
  [lL][iI][mM][bB]       { return CaosScript_K_LIMB; }
  [lL][iI][mM][lL]       { return CaosScript_K_LIML; }
  [lL][iI][mM][rR]       { return CaosScript_K_LIMR; }
  [lL][iI][mM][tT]       { return CaosScript_K_LIMT; }
  [mM][iI][nN][sS]       { return CaosScript_K_MINS; }
  [mM][oO][vV][sS]       { return CaosScript_K_MOVS; }
  [nN][eE][iI][dD]       { return CaosScript_K_NEID; }
  [nN][oO][rR][nN]       { return CaosScript_K_NORN; }
  [oO][bB][jJ][pP]       { return CaosScript_K_OBJP; }
  [oO][wW][nN][rR]       { return CaosScript_K_OWNR; }
  [pP][nN][tT][rR]       { return CaosScript_K_PNTR; }
  [pP][oO][sS][bB]       { return CaosScript_K_POSB; }
  [pP][oO][sS][lL]       { return CaosScript_K_POSL; }
  [pP][oO][sS][rR]       { return CaosScript_K_POSR; }
  [pP][oO][sS][tT]       { return CaosScript_K_POST; }
  [rR][mM][sS][#]        { return CaosScript_K_RMS_NUM; }
  [sS][cC][oO][rR]       { return CaosScript_K_SCOR; }
  [sS][nN][dD][sS]       { return CaosScript_K_SNDS; }
  [sS][pP][cC][sS]       { return CaosScript_K_SPCS; }
  [tT][cC][aA][rR]       { return CaosScript_K_TCAR; }
  [tT][eE][mM][pP]       { return CaosScript_K_TEMP; }
  [wW][dD][tT][hH]       { return CaosScript_K_WDTH; }
  [wW][iI][nN][dD]       { return CaosScript_K_WIND; }
  [wW][iI][nN][hH]       { return CaosScript_K_WINH; }
  [wW][iI][nN][wW]       { return CaosScript_K_WINW; }
  [xX][vV][eE][cC]       { return CaosScript_K_XVEC; }
  [yY][vV][eE][cC]       { return CaosScript_K_YVEC; }
  [dD][rR][iI][vV]       { return CaosScript_K_DRIV; }
  [tT][oO][kK][nN]       { return CaosScript_K_TOKN; }
  {EQ_C1}				 { return CaosScript_EQ_OP_OLD_; }
  {EQ_NEW}			 	 { return CaosScript_EQ_OP_NEW_; }
  {WORD}				 { return CaosScript_WORD; }
  {ID}                   { return CaosScript_ID; }
  {SPACE}                { return CaosScript_SPACE_; }
}

<YYINITIAL> {
	[^]					 {yybegin(START_OF_LINE); yypushback(yylength());}
}

[^] { return BAD_CHARACTER; }
