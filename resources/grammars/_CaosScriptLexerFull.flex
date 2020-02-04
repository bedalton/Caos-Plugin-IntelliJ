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
  [sS][tT][iI][mM]       { return CAOS_SCRIPT_K_STIM; }
  [wW][rR][iI][tT]       { return CAOS_SCRIPT_K_WRIT; }
  [sS][hH][oO][uU]       { return CAOS_SCRIPT_K_SHOU; }
  [sS][iI][gG][nN]       { return CAOS_SCRIPT_K_SIGN; }
  [tT][aA][cC][tT]       { return CAOS_SCRIPT_K_TACT; }
  [nN][eE][wW][:]        { return CAOS_SCRIPT_K_NEW_COL; }
  [bB][kK][bB][dD]       { return CAOS_SCRIPT_K_BKBD; }
  [rR][oO][oO][mM]       { return CAOS_SCRIPT_K_ROOM; }
  [sS][iI][mM][pP]       { return CAOS_SCRIPT_K_SIMP; }
  [pP][aA][rR][tT]       { return CAOS_SCRIPT_K_PART; }
  [sS][pP][oO][tT]       { return CAOS_SCRIPT_K_SPOT; }
  [cC][bB][tT][nN]       { return CAOS_SCRIPT_K_CBTN; }
  [cC][oO][mM][pP]       { return CAOS_SCRIPT_K_COMP; }
  [sS][cC][eE][nN]       { return CAOS_SCRIPT_K_SCEN; }
  [cC][aA][bB][nN]       { return CAOS_SCRIPT_K_CABN; }
  [sS][cC][rR][xX]       { return CAOS_SCRIPT_K_SCRX; }
  [sS][yY][sS][:]        { return CAOS_SCRIPT_K_SYS_COL; }
  [eE][dD][iI][tT]       { return CAOS_SCRIPT_K_EDIT; }
  [tT][oO][oO][lL]       { return CAOS_SCRIPT_K_TOOL; }
  [wW][pP][oO][sS]       { return CAOS_SCRIPT_K_WPOS; }
  [gG][eE][nN][eE]       { return CAOS_SCRIPT_K_GENE; }
  [lL][iI][fF][tT]       { return CAOS_SCRIPT_K_LIFT; }
  [vV][hH][cC][lL]       { return CAOS_SCRIPT_K_VHCL; }
  [rR][nN][dD][vV]       { return CAOS_SCRIPT_K_RNDV; }
  [bB][bB][dD][:]        { return CAOS_SCRIPT_K_BBD_COL; }
  [wW][oO][rR][dD]       { return CAOS_SCRIPT_K_WORD; }
  [dD][dD][eE][:]        { return CAOS_SCRIPT_K_DDE_COL; }
  [cC][eE][lL][lL]       { return CAOS_SCRIPT_K_CELL; }
  [dD][oO][iI][fF]       { return CAOS_SCRIPT_K_DOIF; }
  [eE][nN][uU][mM]       { return CAOS_SCRIPT_K_ENUM; }
  [fF][iI][rR][eE]       { return CAOS_SCRIPT_K_FIRE; }
  [lL][tT][cC][yY]       { return CAOS_SCRIPT_K_LTCY; }
  [cC][rR][eE][aA]       { return CAOS_SCRIPT_K_CREA; }
  [rR][tT][aA][rR]       { return CAOS_SCRIPT_K_RTAR; }
  [tT][rR][iI][gG]       { return CAOS_SCRIPT_K_TRIG; }
  [uU][nN][tT][lL]       { return CAOS_SCRIPT_K_UNTL; }
  [aA][dD][dD][vV]       { return CAOS_SCRIPT_K_ADDV; }
  [aA][nN][dD][vV]       { return CAOS_SCRIPT_K_ANDV; }
  [dD][iI][vV][vV]       { return CAOS_SCRIPT_K_DIVV; }
  [mM][oO][dD][vV]       { return CAOS_SCRIPT_K_MODV; }
  [mM][uU][lL][vV]       { return CAOS_SCRIPT_K_MULV; }
  [oO][rR][rR][vV]       { return CAOS_SCRIPT_K_ORRV; }
  [sS][eE][tT][vV]       { return CAOS_SCRIPT_K_SETV; }
  [sS][uU][bB][vV]       { return CAOS_SCRIPT_K_SUBV; }
  [bB][hH][vV][rR]       { return CAOS_SCRIPT_K_BHVR; }
  [cC][hH][eE][mM]       { return CAOS_SCRIPT_K_CHEM; }
  [pP][uU][tT][bB]       { return CAOS_SCRIPT_K_PUTB; }
  [kK][nN][oO][bB]       { return CAOS_SCRIPT_K_KNOB; }
  [mM][cC][rR][tT]       { return CAOS_SCRIPT_K_MCRT; }
  [mM][eE][sS][gG]       { return CAOS_SCRIPT_K_MESG; }
  [mM][vV][tT][oO]       { return CAOS_SCRIPT_K_MVTO; }
  [sS][pP][aA][sS]       { return CAOS_SCRIPT_K_SPAS; }
  [sS][tT][mM][#]        { return CAOS_SCRIPT_K_STM_NUM; }
  [cC][mM][rR][aA]       { return CAOS_SCRIPT_K_CMRA; }
  [gG][rR][nN][dD]       { return CAOS_SCRIPT_K_GRND; }
  [tT][eE][lL][eE]       { return CAOS_SCRIPT_K_TELE; }
  [sS][nN][dD][qQ]       { return CAOS_SCRIPT_K_SNDQ; }
  [aA][bB][rR][tT]       { return CAOS_SCRIPT_K_ABRT; }
  [aA][pP][pP][rR]       { return CAOS_SCRIPT_K_APPR; }
  [cC][mM][nN][dD]       { return CAOS_SCRIPT_K_CMND; }
  [dD][iI][eE][dD]       { return CAOS_SCRIPT_K_DIED; }
  [hH][aA][tT][cC]       { return CAOS_SCRIPT_K_HATC; }
  [lL][iI][vV][eE]       { return CAOS_SCRIPT_K_LIVE; }
  [lL][oO][bB][eE]       { return CAOS_SCRIPT_K_LOBE; }
  [nN][eE][gG][gG]       { return CAOS_SCRIPT_K_NEGG; }
  [pP][aA][nN][cC]       { return CAOS_SCRIPT_K_PANC; }
  [pP][iI][cC][tT]       { return CAOS_SCRIPT_K_PICT; }
  [sS][cC][rR][pP]       { return CAOS_SCRIPT_K_SCRP; }
  [dD][oO][nN][eE]       { return CAOS_SCRIPT_K_DONE; }
  [dD][pP][aA][sS]       { return CAOS_SCRIPT_K_DPAS; }
  [dD][rR][oO][pP]       { return CAOS_SCRIPT_K_DROP; }
  [eE][lL][sS][eE]       { return CAOS_SCRIPT_K_ELSE; }
  [eE][nN][dD][iI]       { return CAOS_SCRIPT_K_ENDI; }
  [eE][nN][dD][mM]       { return CAOS_SCRIPT_K_ENDM; }
  [gG][pP][aA][sS]       { return CAOS_SCRIPT_K_GPAS; }
  [iI][nN][sS][tT]       { return CAOS_SCRIPT_K_INST; }
  [kK][iI][lL][lL]       { return CAOS_SCRIPT_K_KILL; }
  [lL][oO][cC][kK]       { return CAOS_SCRIPT_K_LOCK; }
  [lL][oO][oO][pP]       { return CAOS_SCRIPT_K_LOOP; }
  [mM][aA][tT][eE]       { return CAOS_SCRIPT_K_MATE; }
  [mM][vV][bB][yY]       { return CAOS_SCRIPT_K_MVBY; }
  [nN][eE][xX][tT]       { return CAOS_SCRIPT_K_NEXT; }
  [oO][vV][eE][rR]       { return CAOS_SCRIPT_K_OVER; }
  [pP][oO][iI][nN]       { return CAOS_SCRIPT_K_POIN; }
  [qQ][uU][iI][tT]       { return CAOS_SCRIPT_K_QUIT; }
  [rR][eE][pP][eE]       { return CAOS_SCRIPT_K_REPE; }
  [rR][eE][tT][nN]       { return CAOS_SCRIPT_K_RETN; }
  [sS][aA][yY][nN]       { return CAOS_SCRIPT_K_SAYN; }
  [sS][lL][iI][mM]       { return CAOS_SCRIPT_K_SLIM; }
  [sS][lL][oO][wW]       { return CAOS_SCRIPT_K_SLOW; }
  [sS][nN][eE][zZ]       { return CAOS_SCRIPT_K_SNEZ; }
  [sS][tT][oO][pP]       { return CAOS_SCRIPT_K_STOP; }
  [sS][tT][pP][cC]       { return CAOS_SCRIPT_K_STPC; }
  [cC][aA][mM][tT]       { return CAOS_SCRIPT_K_CAMT; }
  [wW][tT][oO][pP]       { return CAOS_SCRIPT_K_WTOP; }
  [tT][oO][uU][cC]       { return CAOS_SCRIPT_K_TOUC; }
  [uU][nN][lL][kK]       { return CAOS_SCRIPT_K_UNLK; }
  [wW][aA][lL][kK]       { return CAOS_SCRIPT_K_WALK; }
  [nN][eE][gG][vV]       { return CAOS_SCRIPT_K_NEGV; }
  [gG][sS][uU][bB]       { return CAOS_SCRIPT_K_GSUB; }
  [pP][lL][dD][sS]       { return CAOS_SCRIPT_K_PLDS; }
  [sS][nN][dD][cC]       { return CAOS_SCRIPT_K_SNDC; }
  [sS][nN][dD][eE]       { return CAOS_SCRIPT_K_SNDE; }
  [sS][nN][dD][fF]       { return CAOS_SCRIPT_K_SNDF; }
  [sS][nN][dD][lL]       { return CAOS_SCRIPT_K_SNDL; }
  [sS][uU][bB][rR]       { return CAOS_SCRIPT_K_SUBR; }
  [aA][iI][mM][:]        { return CAOS_SCRIPT_K_AIM_COL; }
  [aA][nN][iI][mM]       { return CAOS_SCRIPT_K_ANIM; }
  [aA][sS][lL][pP]       { return CAOS_SCRIPT_K_ASLP; }
  [bB][aA][sS][eE]       { return CAOS_SCRIPT_K_BASE; }
  [eE][mM][iI][tT]       { return CAOS_SCRIPT_K_EMIT; }
  [sS][hH][oO][wW]       { return CAOS_SCRIPT_K_SHOW; }
  [bB][dD][vV][gG]       { return CAOS_SCRIPT_K_BDVG; }
  [dD][bB][gG][mM]       { return CAOS_SCRIPT_K_DBGM; }
  [dD][bB][uU][gG]       { return CAOS_SCRIPT_K_DBUG; }
  [gG][eE][tT][bB]       { return CAOS_SCRIPT_K_GETB; }
  [pP][uU][tT][sS]       { return CAOS_SCRIPT_K_PUTS; }
  [pP][uU][tT][vV]       { return CAOS_SCRIPT_K_PUTV; }
  [dD][rR][eE][aA]       { return CAOS_SCRIPT_K_DREA; }
  [eE][vV][nN][tT]       { return CAOS_SCRIPT_K_EVNT; }
  [iI][mM][pP][tT]       { return CAOS_SCRIPT_K_IMPT; }
  [pP][oO][sS][eE]       { return CAOS_SCRIPT_K_POSE; }
  [pP][rR][lL][dD]       { return CAOS_SCRIPT_K_PRLD; }
  [rR][eE][pP][sS]       { return CAOS_SCRIPT_K_REPS; }
  [rR][mM][eE][vV]       { return CAOS_SCRIPT_K_RMEV; }
  [sS][aA][yY][$]        { return CAOS_SCRIPT_K_SAY_DOL; }
  [sS][nN][dD][vV]       { return CAOS_SCRIPT_K_SNDV; }
  [tT][aA][rR][gG]       { return CAOS_SCRIPT_K_TARG; }
  [tT][iI][cC][kK]       { return CAOS_SCRIPT_K_TICK; }
  [vV][rR][sS][nN]       { return CAOS_SCRIPT_K_VRSN; }
  [wW][aA][iI][tT]       { return CAOS_SCRIPT_K_WAIT; }
  [wW][rR][lL][dD]       { return CAOS_SCRIPT_K_WRLD; }
  [tT][oO][tT][lL]       { return CAOS_SCRIPT_K_TOTL; }
  [oO][bB][vV][xX]       { return CAOS_SCRIPT_K_OBVX; }
  [vV][aA][rR][xX]       { return CAOS_SCRIPT_K_VARX; }
  [_][iI][tT][_]         { return CAOS_SCRIPT_K__IT_; }
  [aA][cC][tT][vV]       { return CAOS_SCRIPT_K_ACTV; }
  [aA][tT][tT][nN]       { return CAOS_SCRIPT_K_ATTN; }
  [aA][tT][tT][rR]       { return CAOS_SCRIPT_K_ATTR; }
  [bB][aA][bB][yY]       { return CAOS_SCRIPT_K_BABY; }
  [bB][uU][mM][pP]       { return CAOS_SCRIPT_K_BUMP; }
  [cC][aA][gG][eE]       { return CAOS_SCRIPT_K_CAGE; }
  [cC][aA][mM][nN]       { return CAOS_SCRIPT_K_CAMN; }
  [cC][aA][rR][rR]       { return CAOS_SCRIPT_K_CARR; }
  [cC][lL][aA][sS]       { return CAOS_SCRIPT_K_CLAS; }
  [dD][eE][aA][dD]       { return CAOS_SCRIPT_K_DEAD; }
  [dD][rR][vV][!]        { return CAOS_SCRIPT_K_DRV_EXL; }
  [eE][xX][eE][cC]       { return CAOS_SCRIPT_K_EXEC; }
  [fF][mM][lL][yY]       { return CAOS_SCRIPT_K_FMLY; }
  [fF][rR][oO][mM]       { return CAOS_SCRIPT_K_FROM; }
  [gG][nN][dD][#]        { return CAOS_SCRIPT_K_GND_NUM; }
  [gG][nN][dD][wW]       { return CAOS_SCRIPT_K_GNDW; }
  [gG][nN][uU][sS]       { return CAOS_SCRIPT_K_GNUS; }
  [hH][gG][hH][tT]       { return CAOS_SCRIPT_K_HGHT; }
  [hH][oO][uU][rR]       { return CAOS_SCRIPT_K_HOUR; }
  [lL][iI][mM][bB]       { return CAOS_SCRIPT_K_LIMB; }
  [lL][iI][mM][lL]       { return CAOS_SCRIPT_K_LIML; }
  [lL][iI][mM][rR]       { return CAOS_SCRIPT_K_LIMR; }
  [lL][iI][mM][tT]       { return CAOS_SCRIPT_K_LIMT; }
  [mM][iI][nN][sS]       { return CAOS_SCRIPT_K_MINS; }
  [mM][oO][vV][sS]       { return CAOS_SCRIPT_K_MOVS; }
  [nN][eE][iI][dD]       { return CAOS_SCRIPT_K_NEID; }
  [nN][oO][rR][nN]       { return CAOS_SCRIPT_K_NORN; }
  [oO][bB][jJ][pP]       { return CAOS_SCRIPT_K_OBJP; }
  [oO][wW][nN][rR]       { return CAOS_SCRIPT_K_OWNR; }
  [pP][nN][tT][rR]       { return CAOS_SCRIPT_K_PNTR; }
  [pP][oO][sS][bB]       { return CAOS_SCRIPT_K_POSB; }
  [pP][oO][sS][lL]       { return CAOS_SCRIPT_K_POSL; }
  [pP][oO][sS][rR]       { return CAOS_SCRIPT_K_POSR; }
  [pP][oO][sS][tT]       { return CAOS_SCRIPT_K_POST; }
  [rR][mM][sS][#]        { return CAOS_SCRIPT_K_RMS_NUM; }
  [sS][cC][oO][rR]       { return CAOS_SCRIPT_K_SCOR; }
  [sS][nN][dD][sS]       { return CAOS_SCRIPT_K_SNDS; }
  [sS][pP][cC][sS]       { return CAOS_SCRIPT_K_SPCS; }
  [tT][cC][aA][rR]       { return CAOS_SCRIPT_K_TCAR; }
  [tT][eE][mM][pP]       { return CAOS_SCRIPT_K_TEMP; }
  [wW][dD][tT][hH]       { return CAOS_SCRIPT_K_WDTH; }
  [wW][iI][nN][dD]       { return CAOS_SCRIPT_K_WIND; }
  [wW][iI][nN][hH]       { return CAOS_SCRIPT_K_WINH; }
  [wW][iI][nN][wW]       { return CAOS_SCRIPT_K_WINW; }
  [xX][vV][eE][cC]       { return CAOS_SCRIPT_K_XVEC; }
  [yY][vV][eE][cC]       { return CAOS_SCRIPT_K_YVEC; }
  [dD][rR][iI][vV]       { return CAOS_SCRIPT_K_DRIV; }
  [tT][oO][kK][nN]       { return CAOS_SCRIPT_K_TOKN; }
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
