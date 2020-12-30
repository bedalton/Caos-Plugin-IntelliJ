package com.badahori.creatures.plugins.intellij.agenteering.caos.lexer;

import com.intellij.psi.tree.IElementType;

import com.intellij.lexer.FlexLexer;
import java.util.List;
import java.lang.Exception;
import java.util.ArrayList;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.*;
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosScriptArrayUtils;
%%

%{

  	public _CaosScriptLexer(boolean plusPlus) {
		this((java.io.Reader)null);
		this.plusPlus = plusPlus;
	}
	//private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("CaosScriptLexer");
	private boolean plusPlus;
	private int braceDepth;
	private static final List<Character> BYTE_STRING_CHARS = CaosScriptArrayUtils.toList("0123456789 R".toCharArray());
	protected int blockDepth = 0;
	private List<Integer> blockDepths = new ArrayList();
	private int loopDepth = 0;
	private int enumDepth = 0;
	private int escnDepth = 0;
	private int repsDepth = 0;
	private int doifDepth = 0;
	private int subrDepth = 0;

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

	private boolean nextIsValid() {
	    if (enumDepth > 0)
	        return true;
	    int i = -1;
	    char previousChar = yycharat(i);
	    while(previousChar == ' ' || previousChar == '\t' || previousChar == '\n') {
	        try {
	        	previousChar = yycharat(--i);
	        } catch(Exception e) {
	            return false;
	        }
	    }
	    try {
	        char[] chars = new char[] { yycharat(i-3), yycharat(i-2), yycharat(i-1), yycharat(i) };
	        String token = new String(chars).toUpperCase();
	        return token.equals("HIST") || token.equals("PRAY");
	    } catch (Exception e) {
	        return false;
	    }
	}

	private int currentBlockDepth() {
	    return blockDepths.size() > 0 ? blockDepths.get(0) : 0;
	}

%}

%public
%class _CaosScriptLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

NEWLINE=\n
VARx=[Vv][Aa][Rr][0-9]
VAxx=[Vv][Aa][0-9][0-9]
OBVx=[Oo][Bb][Vv][0-9]
OVxx=[Oo][Vv][0-9][0-9]
MVxx=[Mm][Vv][0-9][0-9]
COMMENT_TEXT=[^ \n]+
FLOAT=[-]?[0-9]*\.[0-9]+
INT=[-]?[0-9]+
TEXT=[^\]]+
ID=[_a-zA-Z][_a-zA-Z0-9]*
SPACE=[ \t]
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
CONST_EQ = [=]
N_CONST = [#][a-zA-Z_0-9]+
N_VAR = [$][a-zA-Z_0-9]+
ESCAPE_CHAR=("\\\\"|"\\\""|"\\"[^\"])
QUOTE_STRING_CHAR=[^\"\\\n]
QUOTE_CHARS=({ESCAPE_CHAR}|{QUOTE_STRING_CHAR})+
WORD_CHAR = [a-zA-Z0-9_$#:!+*]
ERROR_WORD={WORD_CHAR}{5,100}
WORD={WORD_CHAR}{4}
INCOMPLETE_WORD={WORD_CHAR}{1,3}
CHAR_ESCAPE_CHAR=("\\\\"|"\\\'"|"\\"[^\'])
CHAR_CHAR=[^\'\\]
CHAR_CHARS=({CHAR_ESCAPE_CHAR}|{CHAR_CHAR})+
SWIFT_ESCAPE=\\\([^)]*\)

%state START_OF_LINE IN_LINE IN_BYTE_STRING IN_TEXT IN_CONST IN_COMMENT COMMENT_START IN_CONST IN_VAR IN_PICT IN_STRING IN_CHAR IN_SUBROUTINE_NAME
%%

<START_OF_LINE> {
	\n						{ return CaosScript_NEWLINE; }
	[\s\t]+					{ return WHITE_SPACE; }
    "*"						{ yybegin(COMMENT_START); return CaosScript_COMMENT_START; }
    [^]					 	{ yybegin(IN_LINE); yypushback(yylength());}
}

<IN_BYTE_STRING> {
	{INT}				 	{ return CaosScript_INT; }
	{SPACE}				 	{ return CaosScript_SPACE_; }
    "R"					 	{ return CaosScript_ANIM_R; }
    [^]					 	{ yybegin(IN_LINE); yypushback(yylength());}
}

<COMMENT_START> {
	//{N_CONST}				{ yybegin(IN_CONST); return CaosScript_N_CONST; }
  	//{N_VAR}				{ yybegin(IN_VAR); return CaosScript_N_VAR; }
    " "						{ return WHITE_SPACE; }
    [^]						{ yybegin(IN_COMMENT); yypushback(yylength()); }
}

<IN_COMMENT> {
	{COMMENT_TEXT}			{ return CaosScript_COMMENT_TEXT; }
    " "						{ return WHITE_SPACE; }
    \n						{ yybegin(START_OF_LINE); return CaosScript_NEWLINE; }
    [^]					 	{ yybegin(IN_LINE); yypushback(yylength());}
}

<IN_TEXT> {
	{TEXT}					{ return CaosScript_TEXT_LITERAL; }
    [^]					 	{ yybegin(IN_LINE); yypushback(yylength());}
}

<IN_BYTE_STRING, IN_TEXT> {
    ']'					 	{ yybegin(IN_LINE); return CaosScript_CLOSE_BRACKET; }
    [^]					 	{ yybegin(IN_LINE); yypushback(yylength());}
}

<IN_CONST> {
	{CONST_EQ}			 	{ return CaosScript_CONST_EQ; }
	{NEWLINE}			 	{ yybegin(START_OF_LINE); return CaosScript_NEWLINE; }
    {FLOAT}					{ return CaosScript_FLOAT; }
	{INT}				 	{ return CaosScript_INT; }
	" "+				 	{ return WHITE_SPACE; }
	[^]					 	{ yybegin(IN_LINE); yypushback(yylength()); }
}

<IN_VAR> {
	{CONST_EQ}			 	{ return CaosScript_CONST_EQ; }
	{NEWLINE}			 	{ yybegin(START_OF_LINE); return CaosScript_NEWLINE; }
	{OVxx}				 	{ return CaosScript_OV_XX; }
	{OBVx}				 	{ return CaosScript_OBV_X; }
	{MVxx}				 	{ return CaosScript_MV_XX; }
	{VARx}				 	{ return CaosScript_VAR_X; }
	{VAxx}				 	{ return CaosScript_VA_XX; }
	" "+				 	{ return WHITE_SPACE; }
	[^]					 	{ yybegin(IN_LINE); yypushback(yylength()); }
}

<IN_PICT> {
	[^\s ,\n][^\s ,\n][^\s ,\n] { yybegin(IN_LINE); return CaosScript_PICT_DIMENSION; }
	\s+				 	 	{ return CaosScript_SPACE_; }
    [^]						{ yybegin(IN_LINE); yypushback(yylength()); }
}

<IN_STRING> {
	\"						{ yybegin(IN_LINE); return CaosScript_DOUBLE_QUOTE;}
	{QUOTE_CHARS}     		{ return CaosScript_STRING_CHAR; }
  	\n+						{ yybegin(IN_LINE); return BAD_CHARACTER; }
	[ \t]+					{ return WHITE_SPACE; }
    [^]						{ yybegin(IN_LINE); yypushback(yylength());}
}

<IN_CHAR> {
	{CHAR_CHARS}			{ return CaosScript_CHAR_CHAR; }
 	"'"						{ yybegin(IN_LINE); return CaosScript_SINGLE_QUOTE; }
    [^]						{ yybegin(IN_LINE); yypushback(yylength());}
}

<IN_SUBROUTINE_NAME> {
	{ID}					{ yybegin(IN_LINE); return CaosScript_ID; }
    [^]						{ yybegin(IN_LINE); yypushback(yylength());}
}

<IN_LINE> {
	\"         				{ yybegin(IN_STRING); return CaosScript_DOUBLE_QUOTE; }
	":"                    	{ return CaosScript_COLON; }
	"+"                    	{ return CaosScript_PLUS; }
	"["                    	{ braceDepth++; yybegin(isByteString() ? IN_BYTE_STRING : IN_TEXT); return CaosScript_OPEN_BRACKET; }
	"]"                    	{ braceDepth--; return CaosScript_CLOSE_BRACKET; }
	","                    	{ return CaosScript_COMMA; }
    "'"						{ yybegin(IN_CHAR); return CaosScript_SINGLE_QUOTE; }
 	{SWIFT_ESCAPE}			{ return CaosScript_SWIFT_ESCAPE; }
	{EQ_C1}				 	{ return CaosScript_EQ_OP_OLD_; }
	{EQ_NEW}			 	{ return CaosScript_EQ_OP_NEW_; }
	{N_CONST}				{ return CaosScript_N_CONST; }
	{N_VAR}				 	{ return CaosScript_N_VAR; }
	{NEWLINE}              	{ yybegin(START_OF_LINE); if(yycharat(-1) == ',') return WHITE_SPACE; return CaosScript_NEWLINE; }
	{OVxx}				 	{ return CaosScript_OV_XX; }
	{OBVx}				 	{ return CaosScript_OBV_X; }
	{MVxx}				 	{ return CaosScript_MV_XX; }
	{VARx}				 	{ return CaosScript_VAR_X; }
	{VAxx}				 	{ return CaosScript_VA_XX; }
 	[%][01]+				{ return CaosScript_BINARY; }
	{FLOAT}              	{ return CaosScript_FLOAT; }
	{INT}                  	{ return CaosScript_INT; }
	[Nn][Ee][Ww][:]        	{ return CaosScript_K_NEW_COL; }
	[Ss][Cc][Ee][Nn]       	{ return CaosScript_K_SCEN; }
	[Ss][Ii][Mm][Pp]       	{ return CaosScript_K_SIMP; }
	[Cc][Bb][Tt][Nn]       	{ return CaosScript_K_CBTN; }
	[Cc][Oo][Mm][Pp]       	{ return CaosScript_K_COMP; }
	[Pp][Aa][Rr][Tt]       	{ return CaosScript_K_PART; }
	[Vv][Hh][Cc][Ll]       	{ return CaosScript_K_VHCL; }
	[Ll][Ii][Ff][Tt]       	{ return CaosScript_K_LIFT; }
	[Bb][Kk][Bb][Dd]       	{ return CaosScript_K_BKBD; }
	[Tt][Oo][Kk][Nn]       	{ return CaosScript_K_TOKN; }
	[Cc][Rr][Ee][Aa]       	{ return CaosScript_K_CREA; }
	[Gg][Ee][Nn][Ee]       	{ return CaosScript_K_GENE; }
	[Cc][Bb][Uu][Bb]       	{ return CaosScript_K_CBUB; }
	[Bb][Bb][Tt][Xx]       	{ return CaosScript_K_BBTX; }
	[Tt][Aa][Rr][Gg]       	{ return CaosScript_K_TARG; }
	[Ff][Rr][Oo][Mm]       	{ return CaosScript_K_FROM; }
	[Nn][Oo][Rr][Nn]       	{ return CaosScript_K_NORN; }
	[Pp][Nn][Tt][Rr]       	{ return CaosScript_K_PNTR; }
	[_][Ii][Tt][_]         	{ return CaosScript_K__IT_; }
	[Cc][Aa][Rr][Rr]       	{ return CaosScript_K_CARR; }
	[Ee][Dd][Ii][Tt]       	{ return CaosScript_K_EDIT; }
	[Tt][Cc][Aa][Rr]       	{ return CaosScript_K_TCAR; }
	[Oo][Bb][Jj][Pp]       	{ return CaosScript_K_OBJP; }
	[Pp][Uu][Pp][Tt]       	{ return CaosScript_K_PUPT; }
	[Pp][Uu][Hh][Ll]       	{ return CaosScript_K_PUHL; }
	[Aa][Cc][Cc][Gg]       	{ return CaosScript_K_ACCG; }
	[Aa][Ee][Rr][Oo]       	{ return CaosScript_K_AERO; }
	[Rr][Ee][Ss][Tt]       	{ return CaosScript_K_REST; }
	[Ss][Ii][Zz][Ee]       	{ return CaosScript_K_SIZE; }
	[Rr][Nn][Gg][Ee]       	{ return CaosScript_K_RNGE; }
	[Aa][Tt][Tt][Rr]       	{ return CaosScript_K_ATTR; }
	[Bb][Hh][Vv][Rr]       	{ return CaosScript_K_BHVR; }
	[Ww][Dd][Tt][Hh]       	{ return CaosScript_K_WDTH; }
	[Hh][Gg][Hh][Tt]       	{ return CaosScript_K_HGHT; }
	[_][Pp][1][_]          	{ return CaosScript_K__P1_; }
	[_][Pp][2][_]          	{ return CaosScript_K__P2_; }
	[Cc][Ll][Ss][2]        	{ return CaosScript_K_CLS2; }
	[Uu][Nn][Ii][Dd]       	{ return CaosScript_K_UNID; }
	[Gg][Rr][Aa][Vv]       	{ return CaosScript_K_GRAV; }
	[Ww][Aa][Ll][Ll]       	{ return CaosScript_K_WALL; }
	[Kk][Ii][Ll][Ll]       	{ return CaosScript_K_KILL; }
	[Rr][Ee][Ll][Xx]       	{ return CaosScript_K_RELX; }
	[Rr][Ee][Ll][Yy]       	{ return CaosScript_K_RELY; }
	[Tt][Ii][Cc][Kk]       	{ return CaosScript_K_TICK; }
	[Ff][Rr][Zz][Nn]       	{ return CaosScript_K_FRZN; }
	[Pp][Oo][Ss][Xx]       	{ return CaosScript_K_POSX; }
	[Pp][Oo][Ss][Yy]       	{ return CaosScript_K_POSY; }
	[Pp][Oo][Ss][Ll]       	{ return CaosScript_K_POSL; }
	[Pp][Oo][Ss][Rr]       	{ return CaosScript_K_POSR; }
	[Pp][Oo][Ss][Bb]       	{ return CaosScript_K_POSB; }
	[Pp][Oo][Ss][Tt]       	{ return CaosScript_K_POST; }
	[Ll][Ii][Mm][Ll]       	{ return CaosScript_K_LIML; }
	[Ll][Ii][Mm][Rr]       	{ return CaosScript_K_LIMR; }
	[Ll][Ii][Mm][Tt]       	{ return CaosScript_K_LIMT; }
	[Ll][Ii][Mm][Bb]       	{ return CaosScript_K_LIMB; }
	[Ff][Mm][Ll][Yy]       	{ return CaosScript_K_FMLY; }
	[Gg][Nn][Uu][Ss]       	{ return CaosScript_K_GNUS; }
	[Ss][Pp][Cc][Ss]       	{ return CaosScript_K_SPCS; }
	[Mm][Oo][Vv][Ss]       	{ return CaosScript_K_MOVS; }
	[Aa][Cc][Tt][Vv]       	{ return CaosScript_K_ACTV; }
	[Nn][Ee][Ii][Dd]       	{ return CaosScript_K_NEID; }
	[Tt][Oo][Tt][Ll]       	{ return CaosScript_K_TOTL; }
	[Tt][Oo][Uu][Cc]       	{ return CaosScript_K_TOUC; }
	[Ss][Ll][Ii][Mm]       	{ return CaosScript_K_SLIM; }
	[Aa][Dd][Dd][Vv]       	{ return CaosScript_K_ADDV; }
	[Ss][Uu][Bb][Vv]       	{ return CaosScript_K_SUBV; }
	[Mm][Uu][Ll][Vv]       	{ return CaosScript_K_MULV; }
	[Dd][Ii][Vv][Vv]       	{ return CaosScript_K_DIVV; }
	[Mm][Oo][Dd][Vv]       	{ return CaosScript_K_MODV; }
	[Nn][Ee][Gg][Vv]       	{ return CaosScript_K_NEGV; }
	[Aa][Nn][Dd][Vv]       	{ return CaosScript_K_ANDV; }
	[Rr][Nn][Dd][Vv]       	{ return CaosScript_K_RNDV; }
	[Ss][Ee][Tt][Vv]       	{ return CaosScript_K_SETV; }
	[Bb][Bb][Ll][Ee]       	{ return CaosScript_K_BBLE; }
	[Ss][Tt][Oo][Pp]       	{ return CaosScript_K_STOP; }
	[Ee][Nn][Dd][Mm]       	{ return CaosScript_K_ENDM; }
	[Ss][Uu][Bb][Rr]       	{ subrDepth++; yybegin(IN_SUBROUTINE_NAME); if (blockDepths.size() > 0) blockDepths.add(0, blockDepth); else blockDepths.add(blockDepth); return CaosScript_K_SUBR; }
	[Gg][Ss][Uu][Bb]       	{ yybegin(IN_SUBROUTINE_NAME); return CaosScript_K_GSUB; }
	[Rr][Ee][Tt][Nn]       	{
          if (blockDepths.size() < 1) {
              return CaosScript_K_BAD_LOOP_TERMINATOR;
		  }
          if (blockDepth > currentBlockDepth())
              return CaosScript_K_CRETN;
          if (blockDepths.size() > 0)
          	blockDepths.remove(0);
          subrDepth--;
          return CaosScript_K_RETN;
      }
	[Rr][Ee][Pp][Ss]       	{ repsDepth++; blockDepth++; return CaosScript_K_REPS; }
	[Rr][Ee][Pp][Ee]       	{ if (repsDepth < 1) return CaosScript_K_BAD_LOOP_TERMINATOR; else repsDepth--; if (blockDepth > 0) blockDepth--; return CaosScript_K_REPE; }
	[Ll][Oo][Oo][Pp]       	{ loopDepth++; blockDepth++; return CaosScript_K_LOOP; }
	[Uu][Nn][Tt][Ll]       	{ if (loopDepth < 1) return CaosScript_K_BAD_LOOP_TERMINATOR; else loopDepth--; if (blockDepth > 0) blockDepth--; return CaosScript_K_UNTL; }
	[Ee][Nn][Uu][Mm]       	{ enumDepth++; blockDepth++; return CaosScript_K_ENUM; }
	[Ee][Ss][Ee][Ee]       	{ enumDepth++; blockDepth++; return CaosScript_K_ESEE; }
	[Ee][Tt][Cc][Hh]       	{ enumDepth++; blockDepth++; return CaosScript_K_ETCH; }
	[Nn][Ee][Xx][Tt]       	{ if (enumDepth < 1) { if (nextIsValid()) return CaosScript_K_NEXT; else return CaosScript_K_BAD_LOOP_TERMINATOR; } else enumDepth--; if (blockDepth > 0) blockDepth--; return CaosScript_K_NEXT; }
	[Ee][Ss][Cc][Nn]       	{ escnDepth++; blockDepth++; return CaosScript_K_ESCN; }
	[Nn][Ss][Cc][Nn]       	{ if (escnDepth < 1) return CaosScript_K_BAD_LOOP_TERMINATOR; else escnDepth--; if (blockDepth > 0) blockDepth--; return CaosScript_K_NSCN; }
	[Rr][Tt][Aa][Rr]       	{ return CaosScript_K_RTAR; }
	[Ss][Tt][Aa][Rr]       	{ return CaosScript_K_STAR; }
	[Ii][Nn][Ss][Tt]       	{ return CaosScript_K_INST; }
	[Ss][Ll][Oo][Ww]       	{ return CaosScript_K_SLOW; }
	[Ee][Vv][Ee][Rr]       	{ if (loopDepth < 1) return CaosScript_K_BAD_LOOP_TERMINATOR; else loopDepth--; if (blockDepth > 0) blockDepth--; return CaosScript_K_EVER; }
	[Dd][Oo][Ii][Ff]       	{ doifDepth++; blockDepth++; return CaosScript_K_DOIF; }
	[Ee][Ll][Ss][Ee]       	{ if (doifDepth < 1) return CaosScript_K_BAD_LOOP_TERMINATOR; return CaosScript_K_ELSE; }
	[Ee][Nn][Dd][Ii]       	{ if (doifDepth < 1) return CaosScript_K_BAD_LOOP_TERMINATOR; else doifDepth--; if (blockDepth > 0) blockDepth--; return CaosScript_K_ENDI; }
	[Ww][Aa][Ii][Tt]       	{ return CaosScript_K_WAIT; }
	[Aa][Nn][Ii][Mm]       	{ return CaosScript_K_ANIM; }
	[Oo][Vv][Ee][Rr]       	{ return CaosScript_K_OVER; }
	[Pp][Oo][Ss][Ee]       	{ return CaosScript_K_POSE; }
	[Pp][Rr][Ll][Dd]       	{ return CaosScript_K_PRLD; }
	[Bb][Aa][Ss][Ee]       	{ return CaosScript_K_BASE; }
	[Vv][Ee][Ll][Xx]       	{ return CaosScript_K_VELX; }
	[Vv][Ee][Ll][Yy]       	{ return CaosScript_K_VELY; }
	[Mm][Vv][Tt][Oo]       	{ return CaosScript_K_MVTO; }
	[Mm][Cc][Rr][Tt]       	{ return CaosScript_K_MCRT; }
	[Mm][Vv][Bb][Yy]       	{ return CaosScript_K_MVBY; }
	[Mm][Ee][Ss][Gg]       	{ return CaosScript_K_MESG; }
	[Ss][Hh][Oo][Uu]       	{ return CaosScript_K_SHOU; }
	[Ss][Ii][Gg][Nn]       	{ return CaosScript_K_SIGN; }
	[Tt][Aa][Cc][Tt]       	{ return CaosScript_K_TACT; }
	[Ww][Rr][Tt][+]        	{ return CaosScript_K_WRT_PLUS; }
	[Ss][Tt][Mm][#]        	{ return CaosScript_K_STM_NUM; }
	[Ww][Rr][Ii][Tt]       	{ return CaosScript_K_WRIT; }
	[Ss][Tt][Ii][Mm]       	{ return CaosScript_K_STIM; }
	[Tt][Ee][Mm][Pp]       	{ return CaosScript_K_TEMP; }
	[Ll][Ii][Tt][Ee]       	{ return CaosScript_K_LITE; }
	[Rr][Aa][Dd][Nn]       	{ return CaosScript_K_RADN; }
	[Oo][Nn][Tt][Rr]       	{ return CaosScript_K_ONTR; }
	[Ii][Nn][Tt][Rr]       	{ return CaosScript_K_INTR; }
	[Pp][Rr][Ee][Ss]       	{ return CaosScript_K_PRES; }
	[Ww][Nn][Dd][Xx]       	{ return CaosScript_K_WNDX; }
	[Ww][Nn][Dd][Yy]       	{ return CaosScript_K_WNDY; }
	[Hh][Ss][Rr][Cc]       	{ return CaosScript_K_HSRC; }
	[Pp][Ss][Rr][Cc]       	{ return CaosScript_K_PSRC; }
	[Ll][Ss][Rr][Cc]       	{ return CaosScript_K_LSRC; }
	[Rr][Ss][Rr][Cc]       	{ return CaosScript_K_RSRC; }
	[Rr][Mm][Nn][Oo]       	{ return CaosScript_K_RMNO; }
	[Rr][Tt][Yy][Pp]       	{ return CaosScript_K_RTYP; }
	[Rr][Mm][Nn][#]        	{ return CaosScript_K_RMN_NUM; }
	[Rr][Mm][Nn][Dd]       	{ return CaosScript_K_RMND; }
	[Rr][Mm][Nn][Rr]       	{ return CaosScript_K_RMNR; }
	[Rr][Oo][Oo][Mm]       	{ return CaosScript_K_ROOM; }
	[Dd][Ee][Ll][Rr]       	{ return CaosScript_K_DELR; }
	[Dd][Ee][Ll][Nn]       	{ return CaosScript_K_DELN; }
	[Dd][Oo][Oo][Rr]       	{ return CaosScript_K_DOOR; }
	[Ww][Ll][Dd][Ww]       	{ return CaosScript_K_WLDW; }
	[Ww][Ll][Dd][Hh]       	{ return CaosScript_K_WLDH; }
	[Tt][Ee][Cc][Oo]       	{ return CaosScript_K_TECO; }
	[Oo][Bb][Ss][Tt]       	{ return CaosScript_K_OBST; }
	[Oo][Bb][Dd][Tt]       	{ return CaosScript_K_OBDT; }
	[Oo][Bb][Ss][Vv]       	{ return CaosScript_K_OBSV; }
	[Ff][Ll][Oo][Rr]       	{ return CaosScript_K_FLOR; }
	[Rr][Mm][Ss][#]        	{ return CaosScript_K_RMS_NUM; }
	[Gg][Rr][Nn][Dd]       	{ return CaosScript_K_GRND; }
	[Ii][Ss][Aa][Rr]       	{ return CaosScript_K_ISAR; }
	[Ss][Ee][Aa][Nn]       	{ return CaosScript_K_SEAN; }
	[Ss][Ee][Aa][Vv]       	{ return CaosScript_K_SEAV; }
	[Aa][Ss][Ee][Aa]       	{ return CaosScript_K_ASEA; }
	[Tt][Mm][Oo][Dd]       	{ return CaosScript_K_TMOD; }
	[Yy][Ee][Aa][Rr]       	{ return CaosScript_K_YEAR; }
	[Ee][Gg][Gg][Ll]       	{ return CaosScript_K_EGGL; }
	[Hh][Aa][Tt][Ll]       	{ return CaosScript_K_HATL; }
	[Ss][Pp][Oo][Tt]       	{ return CaosScript_K_SPOT; }
	[Kk][Nn][Oo][Bb]       	{ return CaosScript_K_KNOB; }
	[Kk][Mm][Ss][Gg]       	{ return CaosScript_K_KMSG; }
	[Cc][Aa][Bb][Nn]       	{ return CaosScript_K_CABN; }
	[Dd][Pp][Ss][2]        	{ return CaosScript_K_DPS2; }
	[Dd][Pp][Aa][Ss]       	{ return CaosScript_K_DPAS; }
	[Gg][Pp][Aa][Ss]       	{ return CaosScript_K_GPAS; }
	[Ss][Pp][Aa][Ss]       	{ return CaosScript_K_SPAS; }
	[Ll][Aa][Cc][Bb]       	{ return CaosScript_K_LACB; }
	[Xx][Vv][Ee][Cc]       	{ return CaosScript_K_XVEC; }
	[Yy][Vv][Ee][Cc]       	{ return CaosScript_K_YVEC; }
	[Bb][Uu][Mm][Pp]       	{ return CaosScript_K_BUMP; }
	[Tt][Ee][Ll][Ee]       	{ return CaosScript_K_TELE; }
	[Bb][Bb][Dd][:]        	{ return CaosScript_K_BBD_COL; }
	[Vv][Oo][Cc][Bb]       	{ return CaosScript_K_VOCB; }
	[Vv][Cc][Bb][1]        	{ return CaosScript_K_VCB1; }
	[Ww][Oo][Rr][Dd]       	{ return CaosScript_K_WORD; }
	[Ss][Hh][Oo][Ww]       	{ return CaosScript_K_SHOW; }
	[Ee][Mm][Ii][Tt]       	{ return CaosScript_K_EMIT; }
	[Bb][Bb][Tt][2]        	{ return CaosScript_K_BBT2; }
	[Bb][Bb][Ff][Dd]       	{ return CaosScript_K_BBFD; }
	[Cc][Bb][Rr][Gg]       	{ return CaosScript_K_CBRG; }
	[Cc][Bb][Rr][Xx]       	{ return CaosScript_K_CBRX; }
	[Rr][Aa][Ii][Nn]       	{ return CaosScript_K_RAIN; }
	[Ss][Yy][Ss][:]        	{ return CaosScript_K_SYS_COL; }
	[Cc][Mm][Rr][Pp]       	{ return CaosScript_K_CMRP; }
	[Cc][Mm][Rr][Aa]       	{ return CaosScript_K_CMRA; }
	[Cc][Mm][Rr][Xx]       	{ return CaosScript_K_CMRX; }
	[Cc][Mm][Rr][Yy]       	{ return CaosScript_K_CMRY; }
	[Tt][Hh][Rr][Tt]       	{ return CaosScript_K_THRT; }
	[Rr][Mm][Ss][Cc]       	{ return CaosScript_K_RMSC; }
	[Rr][Cc][Ll][Rr]       	{ return CaosScript_K_RCLR; }
	[Mm][Uu][Ss][Cc]       	{ return CaosScript_K_MUSC; }
	[Ss][Nn][Dd][Ee]       	{ return CaosScript_K_SNDE; }
	[Ss][Nn][Dd][Qq]       	{ return CaosScript_K_SNDQ; }
	[Ss][Nn][Dd][Cc]       	{ return CaosScript_K_SNDC; }
	[Ss][Nn][Dd][Ll]       	{ return CaosScript_K_SNDL; }
	[Pp][Ll][Dd][Ss]       	{ return CaosScript_K_PLDS; }
	[Ss][Tt][Pp][Cc]       	{ return CaosScript_K_STPC; }
	[Ff][Aa][Dd][Ee]       	{ return CaosScript_K_FADE; }
	[Dd][Rr][Ii][Vv]       	{ return CaosScript_K_DRIV; }
	[Dd][Rr][Vv][!]        	{ return CaosScript_K_DRV_EXC; }
	[Cc][Hh][Ee][Mm]       	{ return CaosScript_K_CHEM; }
	[Bb][Aa][Bb][Yy]       	{ return CaosScript_K_BABY; }
	[Aa][Ss][Ll][Pp]       	{ return CaosScript_K_ASLP; }
	[Uu][Nn][Cc][Ss]       	{ return CaosScript_K_UNCS; }
	[Ii][Nn][Ss][#]        	{ return CaosScript_K_INS_NUM; }
	[Dd][Ii][Rr][Nn]       	{ return CaosScript_K_DIRN; }
	[Mm][Oo][Nn][Kk]       	{ return CaosScript_K_MONK; }
	[Oo][Rr][Gg][Nn]       	{ return CaosScript_K_ORGN; }
	[Ii][Nn][Jj][Rr]       	{ return CaosScript_K_INJR; }
	[Ff][Ii][Rr][Ee]       	{ return CaosScript_K_FIRE; }
	[Tt][Rr][Ii][Gg]       	{ return CaosScript_K_TRIG; }
	[Aa][Pp][Pp][Rr]       	{ return CaosScript_K_APPR; }
	[Ww][Aa][Ll][Kk]       	{ return CaosScript_K_WALK; }
	[Pp][Oo][Ii][Nn]       	{ return CaosScript_K_POIN; }
	[Aa][Ii][Mm][:]        	{ return CaosScript_K_AIM_COL; }
	[Ss][Aa][Yy][#]        	{ return CaosScript_K_SAY_NUM; }
	[Ss][Aa][Yy][$]        	{ return CaosScript_K_SAY_DOL; }
	[Ss][Aa][Yy][Nn]       	{ return CaosScript_K_SAYN; }
	[Ii][Mm][Pp][Tt]       	{ return CaosScript_K_IMPT; }
	[Dd][Oo][Nn][Ee]       	{ return CaosScript_K_DONE; }
	[Ll][Tt][Cc][Yy]       	{ return CaosScript_K_LTCY; }
	[Dd][Rr][Ee][Aa]       	{ return CaosScript_K_DREA; }
	[Dd][Rr][Oo][Pp]       	{ return CaosScript_K_DROP; }
	[Mm][Aa][Tt][Ee]       	{ return CaosScript_K_MATE; }
	[Ss][Nn][Ee][Zz]       	{ return CaosScript_K_SNEZ; }
	[Cc][Aa][Mm][Nn]       	{ return CaosScript_K_CAMN; }
	[Cc][Aa][Gg][Ee]       	{ return CaosScript_K_CAGE; }
	[Dd][Dd][Ee][:]        	{ return CaosScript_K_DDE_COL; }
	[Gg][Ii][Dd][Ss]       	{ return CaosScript_K_GIDS; }
	[Rr][Oo][Oo][Tt]       	{ return CaosScript_K_ROOT; }
	[Gg][Ee][Tt][Bb]       	{ return CaosScript_K_GETB; }
	[Aa][Ll][Ll][Rr]       	{ return CaosScript_K_ALLR; }
	[Rr][Pp][Tt][Yy]       	{ return CaosScript_K_RPTY; }
	[Rr][Rr][Cc][Tt]       	{ return CaosScript_K_RRCT; }
	[Nn][Ee][Ww][Vv]       	{ return CaosScript_K_NEWV; }
	[Ll][Vv][Oo][Bb]       	{ return CaosScript_K_LVOB; }
	[Bb][Ii][Oo][Cc]       	{ return CaosScript_K_BIOC; }
	[Ee][Mm][Tt][Rr]       	{ return CaosScript_K_EMTR; }
	[Rr][Cc][Tt][Nn]       	{ return CaosScript_K_RCTN; }
	[Pp][Ii][Cc][2]        	{ return CaosScript_K_PIC2; }
	[Pp][Uu][Tt][Vv]       	{ return CaosScript_K_PUTV; }
	[Nn][Aa][Cc][Tt]       	{ return CaosScript_K_NACT; }
	[Ll][Nn][Ee][Uu]       	{ return CaosScript_K_LNEU; }
	[Ll][Cc][Uu][Ss]       	{ return CaosScript_K_LCUS; }
	[Pp][Uu][Tt][Ss]       	{ return CaosScript_K_PUTS; }
	[Dd][Aa][Tt][Aa]       	{ return CaosScript_K_DATA; }
	[Cc][Nn][Aa][Mm]       	{ return CaosScript_K_CNAM; }
	[Cc][Tt][Ii][Mm]       	{ return CaosScript_K_CTIM; }
	[Oo][Vv][Vv][Dd]       	{ return CaosScript_K_OVVD; }
	[Nn][Ee][Gg][Gg]       	{ return CaosScript_K_NEGG; }
	[Pp][Aa][Nn][Cc]       	{ return CaosScript_K_PANC; }
	[Ll][Oo][Bb][Ee]       	{ return CaosScript_K_LOBE; }
	[Cc][Ee][Ll][Ll]       	{ return CaosScript_K_CELL; }
	[Dd][Ii][Ee][Dd]       	{ return CaosScript_K_DIED; }
	[Ll][Ii][Vv][Ee]       	{ return CaosScript_K_LIVE; }
	[Hh][Aa][Tt][Cc]       	{ return CaosScript_K_HATC; }
	[Dd][Bb][Uu][Gg]       	{ return CaosScript_K_DBUG; }
	[Dd][Bb][Gg][Vv]       	{ return CaosScript_K_DBGV; }
	[Dd][Bb][Gg][Mm]       	{ return CaosScript_K_DBGM; }
	[Ss][Cc][Rr][Pp]       	{ return CaosScript_K_SCRP; }
	[Ss][Cc][Rr][Xx]       	{ return CaosScript_K_SCRX; }
	[Pp][Aa][Uu][Ss]       	{ return CaosScript_K_PAUS; }
	[Ll][Oo][Cc][Kk]       	{ return CaosScript_K_LOCK; }
	[Uu][Nn][Ll][Kk]       	{ return CaosScript_K_UNLK; }
	[Ee][Vv][Nn][Tt]       	{ return CaosScript_K_EVNT; }
	[Rr][Mm][Ee][Vv]       	{ return CaosScript_K_RMEV; }
	[Hh][Oo][Uu][Rr]       	{ return CaosScript_K_HOUR; }
	[Mm][Ii][Nn][Ss]       	{ return CaosScript_K_MINS; }
	[Dd][Mm][Aa][Pp]       	{ return CaosScript_K_DMAP; }
	[Ww][Tt][Oo][Pp]       	{ return CaosScript_K_WTOP; }
	[Qq][Uu][Ii][Tt]       	{ return CaosScript_K_QUIT; }
	[Aa][Bb][Rr][Tt]       	{ return CaosScript_K_ABRT; }
	[Ww][Rr][Ll][Dd]       	{ return CaosScript_K_WRLD; }
	[Cc][Oo][Nn][Vv]       	{ return CaosScript_K_CONV; }
	[Vv][Rr][Ss][Nn]       	{ return CaosScript_K_VRSN; }
	[Ii][Ss][Cc][Rr]       	{ return CaosScript_K_ISCR; }
	[Rr][Ss][Cc][Rr]       	{ return CaosScript_K_RSCR; }
	[Ll][Aa][Nn][Gg]       	{ return CaosScript_K_LANG; }
	[Oo][Ww][Nn][Rr]       	{ return CaosScript_K_OWNR; }
	[Aa][Tt][Tt][Nn]       	{ return CaosScript_K_ATTN; }
	[Ee][Xx][Ee][Cc]       	{ return CaosScript_K_EXEC; }
	[Ss][Nn][Dd][Ss]       	{ return CaosScript_K_SNDS; }
	[Ww][Ii][Nn][Ww]       	{ return CaosScript_K_WINW; }
	[Ww][Ii][Nn][Hh]       	{ return CaosScript_K_WINH; }
	[Cc][Ll][Aa][Ss]       	{ return CaosScript_K_CLAS; }
	[Ss][Cc][Oo][Rr]       	{ return CaosScript_K_SCOR; }
	[Dd][Ee][Aa][Dd]       	{ return CaosScript_K_DEAD; }
	[Ww][Ii][Nn][Dd]       	{ return CaosScript_K_WIND; }
	[Gg][Nn][Dd][#]        	{ return CaosScript_K_GND_NUM; }
	[Gg][Nn][Dd][Ww]       	{ return CaosScript_K_GNDW; }
	[Pp][Uu][Tt][Bb]       	{ return CaosScript_K_PUTB; }
	[Pp][Ii][Cc][Tt]       	{ yybegin(IN_PICT); return CaosScript_K_PICT; }
	[Cc][Mm][Nn][Dd]       	{ return CaosScript_K_CMND; }
	[Ww][Pp][Oo][Ss]       	{ return CaosScript_K_WPOS; }
	[Cc][Aa][Mm][Tt]       	{ return CaosScript_K_CAMT; }
	[Oo][Rr][Rr][Vv]       	{ return CaosScript_K_ORRV; }
	[Tt][Oo][Oo][Ll]       	{ return CaosScript_K_TOOL; }
	[Ss][Nn][Dd][Ff]       	{ return CaosScript_K_SNDF; }
	[Ss][Nn][Dd][Vv]       	{ return CaosScript_K_SNDV; }
	[Aa][Ll][Pp][Hh]       	{ return CaosScript_K_ALPH; }
	[Aa][Nn][Mm][Ss]       	{ return CaosScript_K_ANMS; }
	[Bb][Mm][Pp][Ss]       	{ return CaosScript_K_BMPS; }
	[Cc][Aa][Tt][Ii]       	{ return CaosScript_K_CATI; }
	[Cc][Aa][Tt][Xx]       	{ return CaosScript_K_CATX; }
	[Cc][Ll][Aa][Cc]       	{ return CaosScript_K_CLAC; }
	[Cc][Ll][Ii][Kk]       	{ return CaosScript_K_CLIK; }
	[Dd][Ii][Ss][Qq]       	{ return CaosScript_K_DISQ; }
	[Ff][Ll][Tt][Xx]       	{ return CaosScript_K_FLTX; }
	[Ff][Ll][Tt][Yy]       	{ return CaosScript_K_FLTY; }
	[Ff][Rr][Aa][Tt]       	{ return CaosScript_K_FRAT; }
	[Gg][Aa][Ii][Tt]       	{ return CaosScript_K_GAIT; }
	[Gg][Aa][Ll][Ll]       	{ return CaosScript_K_GALL; }
	[Hh][Aa][Nn][Dd]       	{ return CaosScript_K_HAND; }
	[Hh][Ee][Dd][Xx]       	{ return CaosScript_K_HEDX; }
	[Hh][Ee][Dd][Yy]       	{ return CaosScript_K_HEDY; }
	[Hh][Ee][Ll][Dd]       	{ return CaosScript_K_HELD; }
	[Ii][Ii][Tt][Tt]       	{ return CaosScript_K_IITT; }
	[Ii][Mm][Gg][Ee]       	{ return CaosScript_K_IMGE; }
	[Ii][Mm][Ss][Kk]       	{ return CaosScript_K_IMSK; }
	[Mm][Ii][Rr][Aa]       	{ return CaosScript_K_MIRA; }
	[Mm][Oo][Ww][Ss]       	{ return CaosScript_K_MOWS; }
	[Mm][Tt][Hh][Xx]       	{ return CaosScript_K_MTHX; }
	[Mm][Tt][Hh][Yy]       	{ return CaosScript_K_MTHY; }
	[Nn][Cc][Ll][Ss]       	{ return CaosScript_K_NCLS; }
	[Nn][Oo][Hh][Hh]       	{ return CaosScript_K_NOHH; }
	[Nn][Uu][Ll][Ll]       	{ return CaosScript_K_NULL; }
	[Oo][Nn][Tt][Vv]       	{ return CaosScript_K_ONTV; }
	[Pp][Cc][Ll][Ss]       	{ return CaosScript_K_PCLS; }
	[Pp][Ll][Nn][Ee]       	{ return CaosScript_K_PLNE; }
	[Ss][Cc][Ll][Ee]       	{ return CaosScript_K_SCLE; }
	[Ss][Ee][Ee][Ee]       	{ return CaosScript_K_SEEE; }
	[Tt][Ii][Nn][Tt]       	{ return CaosScript_K_TINT; }
	[Tt][Rr][Aa][Nn]       	{ return CaosScript_K_TRAN; }
	[Tt][Tt][Aa][Rr]       	{ return CaosScript_K_TTAR; }
	[Tt][Ww][Ii][Nn]       	{ return CaosScript_K_TWIN; }
	[Vv][Ii][Ss][Ii]       	{ return CaosScript_K_VISI; }
	[Ww][Ii][Ll][Dd]       	{ return CaosScript_K_WILD; }
	[Bb][Kk][Gg][Dd]       	{ return CaosScript_K_BKGD; }
	[Bb][Rr][Mm][Ii]       	{ return CaosScript_K_BRMI; }
	[Cc][Mm][Rr][Tt]       	{ return CaosScript_K_CMRT; }
	[Ff][Rr][Ss][Hh]       	{ return CaosScript_K_FRSH; }
	[Ll][Ii][Nn][Ee]       	{ return CaosScript_K_LINE; }
	[Ll][Oo][Ff][Tt]       	{ return CaosScript_K_LOFT; }
	[Mm][Ee][Tt][Aa]       	{ return CaosScript_K_META; }
	[Mm][Ii][Rr][Rr]       	{ return CaosScript_K_MIRR; }
	[Pp][Rr][Nn][Tt]       	{ return CaosScript_K_PRNT; }
	[Ss][Cc][Aa][Mm]       	{ return CaosScript_K_SCAM; }
	[Ss][Cc][Rr][Ll]       	{ return CaosScript_K_SCRL; }
	[Ss][Nn][Aa][Pp]       	{ return CaosScript_K_SNAP; }
	[Ss][Nn][Aa][Xx]       	{ return CaosScript_K_SNAX; }
	[Tt][Nn][Tt][Oo]       	{ return CaosScript_K_TNTO; }
	[Tt][Rr][Cc][Kk]       	{ return CaosScript_K_TRCK; }
	[Ww][Dd][Oo][Ww]       	{ return CaosScript_K_WDOW; }
	[Ww][Nn][Dd][Bb]       	{ return CaosScript_K_WNDB; }
	[Ww][Nn][Dd][Hh]       	{ return CaosScript_K_WNDH; }
	[Ww][Nn][Dd][Ll]       	{ return CaosScript_K_WNDL; }
	[Ww][Nn][Dd][Rr]       	{ return CaosScript_K_WNDR; }
	[Ww][Nn][Dd][Tt]       	{ return CaosScript_K_WNDT; }
	[Ww][Nn][Dd][Ww]       	{ return CaosScript_K_WNDW; }
	[Zz][Oo][Oo][Mm]       	{ return CaosScript_K_ZOOM; }
	[Cc][Hh][Aa][Rr]       	{ return CaosScript_K_CHAR; }
	[Ff][Cc][Uu][Ss]       	{ return CaosScript_K_FCUS; }
	[Ff][Rr][Mm][Tt]       	{ return CaosScript_K_FRMT; }
	[Gg][Rr][Pp][Ll]       	{ return CaosScript_K_GRPL; }
	[Gg][Rr][Pp][Vv]       	{ return CaosScript_K_GRPV; }
	[Nn][Pp][Gg][Ss]       	{ return CaosScript_K_NPGS; }
	[Pp][Aa][Gg][Ee]       	{ return CaosScript_K_PAGE; }
	[Pp][Tt][Xx][Tt]       	{ return CaosScript_K_PTXT; }
	[Aa][Gg][Ee][Ss]       	{ return CaosScript_K_AGES; }
	[Bb][Oo][Dd][Yy]       	{ return CaosScript_K_BODY; }
	[Bb][Oo][Rr][Nn]       	{ return CaosScript_K_BORN; }
	[Bb][Rr][Ee][Dd]       	{ return CaosScript_K_BRED; }
	[Bb][Vv][Aa][Rr]       	{ return CaosScript_K_BVAR; }
	[Bb][Yy][Ii][Tt]       	{ return CaosScript_K_BYIT; }
	[Dd][Ee][Cc][Nn]       	{ return CaosScript_K_DECN; }
	[Dd][Ff][Tt][Xx]       	{ return CaosScript_K_DFTX; }
	[Dd][Ff][Tt][Yy]       	{ return CaosScript_K_DFTY; }
	[Dd][Yy][Ee][Dd]       	{ return CaosScript_K_DYED; }
	[Ee][Xx][Pp][Rr]       	{ return CaosScript_K_EXPR; }
	[Ff][Aa][Cc][Ee]       	{ return CaosScript_K_FACE; }
	[Ff][Oo][Rr][Ff]       	{ return CaosScript_K_FORF; }
	[Hh][Aa][Ii][Rr]       	{ return CaosScript_K_HAIR; }
	[Hh][Hh][Ll][Dd]       	{ return CaosScript_K_HHLD; }
	[Ll][Ii][Kk][Ee]       	{ return CaosScript_K_LIKE; }
	[Ll][Oo][Cc][Ii]       	{ return CaosScript_K_LOCI; }
	[Mm][Vv][Ff][Tt]       	{ return CaosScript_K_MVFT; }
	[Nn][Ee][Ww][Cc]       	{ return CaosScript_K_NEWC; }
	[Nn][Uu][Dd][Ee]       	{ return CaosScript_K_NUDE; }
	[Oo][Rr][Gg][Ff]       	{ return CaosScript_K_ORGF; }
	[Oo][Rr][Gg][Ii]       	{ return CaosScript_K_ORGI; }
	[Rr][Ss][Ee][Tt]       	{ return CaosScript_K_RSET; }
	[Ss][Pp][Nn][Ll]       	{ return CaosScript_K_SPNL; }
	[Ss][Tt][Rr][Ee]       	{ return CaosScript_K_STRE; }
	[Ss][Ww][Aa][Pp]       	{ return CaosScript_K_SWAP; }
	[Tt][Aa][Gg][Ee]       	{ return CaosScript_K_TAGE; }
	[Tt][Nn][Tt][Cc]       	{ return CaosScript_K_TNTC; }
	[Uu][Ff][Tt][Xx]       	{ return CaosScript_K_UFTX; }
	[Uu][Ff][Tt][Yy]       	{ return CaosScript_K_UFTY; }
	[Ww][Ee][Aa][Rr]       	{ return CaosScript_K_WEAR; }
	[Zz][Oo][Mm][Bb]       	{ return CaosScript_K_ZOMB; }
	[Aa][Gg][Nn][Tt]       	{ return CaosScript_K_AGNT; }
	[Aa][Pp][Rr][Oo]       	{ return CaosScript_K_APRO; }
	[Cc][Oo][Dd][Ee]       	{ return CaosScript_K_CODE; }
	[Cc][Oo][Dd][Ff]       	{ return CaosScript_K_CODF; }
	[Cc][Oo][Dd][Gg]       	{ return CaosScript_K_CODG; }
	[Cc][Oo][Dd][Pp]       	{ return CaosScript_K_CODP; }
	[Cc][Oo][Dd][Ss]       	{ return CaosScript_K_CODS; }
	[Dd][Bb][Gg][#]        	{ return CaosScript_K_DBG_NUM; }
	[Dd][Bb][Gg][Aa]       	{ return CaosScript_K_DBGA; }
	[Hh][Ee][Aa][Pp]       	{ return CaosScript_K_HEAP; }
	[Hh][Ee][Ll][Pp]       	{ return CaosScript_K_HELP; }
	[Mm][Aa][Nn][Nn]       	{ return CaosScript_K_MANN; }
	[Mm][Ee][Mm][Xx]       	{ return CaosScript_K_MEMX; }
	[Pp][Aa][Ww][Ss]       	{ return CaosScript_K_PAWS; }
	[Tt][Aa][Cc][Kk]       	{ return CaosScript_K_TACK; }
	[Ff][Vv][Ww][Mm]       	{ return CaosScript_K_FVWM; }
	[Ii][Nn][Nn][Ff]       	{ return CaosScript_K_INNF; }
	[Ii][Nn][Nn][Ii]       	{ return CaosScript_K_INNI; }
	[Ii][Nn][Nn][Ll]       	{ return CaosScript_K_INNL; }
	[Ii][Nn][Oo][Kk]       	{ return CaosScript_K_INOK; }
	[Oo][Uu][Tt][Ss]       	{ return CaosScript_K_OUTS; }
	[Oo][Uu][Tt][Vv]       	{ return CaosScript_K_OUTV; }
	[Oo][Uu][Tt][Xx]       	{ return CaosScript_K_OUTX; }
	[Ee][Ll][Ii][Ff]       	{ if (doifDepth < 1) return CaosScript_K_BAD_LOOP_TERMINATOR; return CaosScript_K_ELIF; }
	[Gg][Oo][Tt][Oo]       	{ return CaosScript_K_GOTO; }
	[Gg][Tt][Oo][Ss]       	{ return CaosScript_K_GTOS; }
	[Mm][Tt][Oo][Aa]       	{ return CaosScript_K_MTOA; }
	[Mm][Tt][Oo][Cc]       	{ return CaosScript_K_MTOC; }
	[Oo][Oo][Ww][Ww]       	{ return CaosScript_K_OOWW; }
	[Hh][Oo][Tt][Ss]       	{ return CaosScript_K_HOTS; }
	[Kk][Ee][Yy][Dd]       	{ return CaosScript_K_KEYD; }
	[Mm][Oo][Pp][Xx]       	{ return CaosScript_K_MOPX; }
	[Mm][Oo][Pp][Yy]       	{ return CaosScript_K_MOPY; }
	[Mm][Oo][Uu][Ss]       	{ return CaosScript_K_MOUS; }
	[Mm][Oo][Vv][Xx]       	{ return CaosScript_K_MOVX; }
	[Mm][Oo][Vv][Yy]       	{ return CaosScript_K_MOVY; }
	[Pp][Uu][Rr][Ee]       	{ return CaosScript_K_PURE; }
	[Aa][Dd][Dd][Bb]       	{ return CaosScript_K_ADDB; }
	[Aa][Dd][Dd][Mm]       	{ return CaosScript_K_ADDM; }
	[Aa][Dd][Dd][Rr]       	{ return CaosScript_K_ADDR; }
	[Aa][Ll][Tt][Rr]       	{ return CaosScript_K_ALTR; }
	[Bb][Kk][Dd][Ss]       	{ return CaosScript_K_BKDS; }
	[Cc][Aa][Cc][Ll]       	{ return CaosScript_K_CACL; }
	[Dd][Ee][Ll][Mm]       	{ return CaosScript_K_DELM; }
	[Dd][Oo][Cc][Aa]       	{ return CaosScript_K_DOCA; }
	[Dd][Oo][Ww][Nn]       	{ return CaosScript_K_DOWN; }
	[Ee][Mm][Ii][Dd]       	{ return CaosScript_K_EMID; }
	[Ee][Rr][Ii][Dd]       	{ return CaosScript_K_ERID; }
	[Gg][Mm][Aa][Pp]       	{ return CaosScript_K_GMAP; }
	[Gg][Rr][Aa][Pp]       	{ return CaosScript_K_GRAP; }
	[Gg][Rr][Ii][Dd]       	{ return CaosScript_K_GRID; }
	[Hh][Ii][Rr][Pp]       	{ return CaosScript_K_HIRP; }
	[Ll][Ee][Ff][Tt]       	{ return CaosScript_K_LEFT; }
	[Ll][Ii][Nn][Kk]       	{ return CaosScript_K_LINK; }
	[Ll][Oo][Rr][Pp]       	{ return CaosScript_K_LORP; }
	[Mm][Aa][Pp][Dd]       	{ return CaosScript_K_MAPD; }
	[Mm][Aa][Pp][Hh]       	{ return CaosScript_K_MAPH; }
	[Mm][Aa][Pp][Kk]       	{ return CaosScript_K_MAPK; }
	[Mm][Aa][Pp][Ww]       	{ return CaosScript_K_MAPW; }
	[Mm][Ll][Oo][Cc]       	{ return CaosScript_K_MLOC; }
	[Pp][Ee][Rr][Mm]       	{ return CaosScript_K_PERM; }
	[Pp][Rr][Oo][Pp]       	{ return CaosScript_K_PROP; }
	[Rr][Aa][Tt][Ee]       	{ return CaosScript_K_RATE; }
	[Rr][Gg][Hh][Tt]       	{ return CaosScript_K_RGHT; }
	[Rr][Ll][Oo][Cc]       	{ return CaosScript_K_RLOC; }
	[Tt][Oo][Rr][Xx]       	{ return CaosScript_K_TORX; }
	[Tt][Oo][Rr][Yy]       	{ return CaosScript_K_TORY; }
	[_][Uu][Pp][_]         	{ return CaosScript_K__UP_; }
	[Ee][Ll][Aa][Ss]       	{ return CaosScript_K_ELAS; }
	[Ff][Aa][Ll][Ll]       	{ return CaosScript_K_FALL; }
	[Ff][Ll][Tt][Oo]       	{ return CaosScript_K_FLTO; }
	[Ff][Rr][Ee][Ll]       	{ return CaosScript_K_FREL; }
	[Ff][Rr][Ii][Cc]       	{ return CaosScript_K_FRIC; }
	[Mm][Vv][Ss][Ff]       	{ return CaosScript_K_MVSF; }
	[Tt][Mm][Vv][Bb]       	{ return CaosScript_K_TMVB; }
	[Tt][Mm][Vv][Ff]       	{ return CaosScript_K_TMVF; }
	[Tt][Mm][Vv][Tt]       	{ return CaosScript_K_TMVT; }
	[Vv][Ee][Ll][Oo]       	{ return CaosScript_K_VELO; }
	[Ee][Cc][Oo][Nn]       	{ enumDepth++; return CaosScript_K_ECON; }
	[Cc][Aa][Oo][Ss]       	{ return CaosScript_K_CAOS; }
	[Ss][Oo][Rr][Cc]       	{ return CaosScript_K_SORC; }
	[Ss][Oo][Rr][Qq]       	{ return CaosScript_K_SORQ; }
	[Ss][Tt][Pp][Tt]       	{ return CaosScript_K_STPT; }
	[Mm][Cc][Ll][Rr]       	{ return CaosScript_K_MCLR; }
	[Mm][Ii][Dd][Ii]       	{ return CaosScript_K_MIDI; }
	[Mm][Mm][Ss][Cc]       	{ return CaosScript_K_MMSC; }
	[Mm][Uu][Tt][Ee]       	{ return CaosScript_K_MUTE; }
	[Ss][Ee][Zz][Zz]       	{ return CaosScript_K_SEZZ; }
	[Ss][Tt][Rr][Kk]       	{ return CaosScript_K_STRK; }
	[Vv][Oo][Ii][Cc]       	{ return CaosScript_K_VOIC; }
	[Vv][Oo][Ii][Ss]       	{ return CaosScript_K_VOIS; }
	[Vv][Oo][Ll][Mm]       	{ return CaosScript_K_VOLM; }
	[Dd][Aa][Tt][Ee]       	{ return CaosScript_K_DATE; }
	[Dd][Aa][Yy][Tt]       	{ return CaosScript_K_DAYT; }
	[Ee][Tt][Ii][Kk]       	{ return CaosScript_K_ETIK; }
	[Mm][Oo][Nn][Tt]       	{ return CaosScript_K_MONT; }
	[Mm][Ss][Ee][Cc]       	{ return CaosScript_K_MSEC; }
	[Pp][Aa][Cc][Ee]       	{ return CaosScript_K_PACE; }
	[Rr][Aa][Cc][Ee]       	{ return CaosScript_K_RACE; }
	[Rr][Tt][Ii][Ff]       	{ return CaosScript_K_RTIF; }
	[Rr][Tt][Ii][Mm]       	{ return CaosScript_K_RTIM; }
	[Ss][Cc][Oo][Ll]       	{ return CaosScript_K_SCOL; }
	[Tt][Ii][Mm][Ee]       	{ return CaosScript_K_TIME; }
	[Ww][Oo][Ll][Ff]       	{ return CaosScript_K_WOLF; }
	[Ww][Pp][Aa][Uu]       	{ return CaosScript_K_WPAU; }
	[Ww][Tt][Ii][Kk]       	{ return CaosScript_K_WTIK; }
	[Aa][Bb][Ss][Vv]       	{ return CaosScript_K_ABSV; }
	[Aa][Cc][Oo][Ss]       	{ return CaosScript_K_ACOS; }
	[Aa][Dd][Dd][Ss]       	{ return CaosScript_K_ADDS; }
	[Aa][Ss][Ii][Nn]       	{ return CaosScript_K_ASIN; }
	[Aa][Tt][Aa][Nn]       	{ return CaosScript_K_ATAN; }
	[Aa][Vv][Aa][Rr]       	{ return CaosScript_K_AVAR; }
	[Cc][Oo][Ss][_]        	{ return CaosScript_K_COS_; }
	[Dd][Ee][Ll][Gg]       	{ return CaosScript_K_DELG; }
	[Ff][Tt][Oo][Ii]       	{ return CaosScript_K_FTOI; }
	[Gg][Aa][Mm][Ee]       	{ return CaosScript_K_GAME; }
	[Gg][Aa][Mm][Nn]       	{ return CaosScript_K_GAMN; }
	[Gg][Nn][Aa][Mm]       	{ return CaosScript_K_GNAM; }
	[Ii][Tt][Oo][Ff]       	{ return CaosScript_K_ITOF; }
	[Rr][Aa][Nn][Dd]       	{ return CaosScript_K_RAND; }
	[Rr][Ee][Aa][Dd]       	{ return CaosScript_K_READ; }
	[Rr][Ee][Aa][Ff]       	{ return CaosScript_K_REAF; }
	[Rr][Ee][Aa][Nn]       	{ return CaosScript_K_REAN; }
	[Rr][Ee][Aa][Qq]       	{ return CaosScript_K_REAQ; }
	[Ss][Ee][Tt][Aa]       	{ return CaosScript_K_SETA; }
	[Ss][Ee][Tt][Ss]       	{ return CaosScript_K_SETS; }
	[Ss][Ii][Nn][_]        	{ return CaosScript_K_SIN_; }
	[Ss][Qq][Rr][Tt]       	{ return CaosScript_K_SQRT; }
	[Ss][Tt][Oo][Ff]       	{ return CaosScript_K_STOF; }
	[Ss][Tt][Oo][Ii]       	{ return CaosScript_K_STOI; }
	[Ss][Tt][Rr][Ll]       	{ return CaosScript_K_STRL; }
	[Ss][Uu][Bb][Ss]       	{ return CaosScript_K_SUBS; }
	[Tt][Aa][Nn][_]        	{ return CaosScript_K_TAN_; }
	[Tt][Yy][Pp][Ee]       	{ return CaosScript_K_TYPE; }
	[Vv][Mm][Jj][Rr]       	{ return CaosScript_K_VMJR; }
	[Vv][Mm][Nn][Rr]       	{ return CaosScript_K_VMNR; }
	[Vv][Tt][Oo][Ss]       	{ return CaosScript_K_VTOS; }
	[Cc][Aa][Bb][Bb]       	{ return CaosScript_K_CABB; }
	[Cc][Aa][Bb][Ll]       	{ return CaosScript_K_CABL; }
	[Cc][Aa][Bb][Pp]       	{ return CaosScript_K_CABP; }
	[Cc][Aa][Bb][Rr]       	{ return CaosScript_K_CABR; }
	[Cc][Aa][Bb][Tt]       	{ return CaosScript_K_CABT; }
	[Cc][Aa][Bb][Vv]       	{ return CaosScript_K_CABV; }
	[Cc][Aa][Bb][Ww]       	{ return CaosScript_K_CABW; }
	[Ee][Pp][Aa][Ss]       	{ enumDepth++; return CaosScript_K_EPAS; }
	[Rr][Pp][Aa][Ss]       	{ return CaosScript_K_RPAS; }
	[Dd][Ee][Ll][Ww]       	{ return CaosScript_K_DELW; }
	[Ll][Oo][Aa][Dd]       	{ return CaosScript_K_LOAD; }
	[Nn][Ww][Ll][Dd]       	{ return CaosScript_K_NWLD; }
	[Pp][Ss][Ww][Dd]       	{ return CaosScript_K_PSWD; }
	[Rr][Gg][Aa][Mm]       	{ return CaosScript_K_RGAM; }
	[Ss][Aa][Vv][Ee]       	{ return CaosScript_K_SAVE; }
	[Tt][Nn][Tt][Ww]       	{ return CaosScript_K_TNTW; }
	[Ww][Nn][Aa][Mm]       	{ return CaosScript_K_WNAM; }
	[Ww][Nn][Tt][Ii]       	{ return CaosScript_K_WNTI; }
	[Ww][Tt][Nn][Tt]       	{ return CaosScript_K_WTNT; }
	[Ww][Uu][Ii][Dd]       	{ return CaosScript_K_WUID; }
	[Aa][Bb][Bb][Aa]       	{ return CaosScript_K_ABBA; }
	[Cc][Aa][Ll][Ll]       	{ return CaosScript_K_CALL; }
	[Cc][Aa][Tt][Aa]       	{ return CaosScript_K_CATA; }
	[Cc][Aa][Tt][Oo]       	{ return CaosScript_K_CATO; }
	[Cc][Oo][Rr][Ee]       	{ return CaosScript_K_CORE; }
	[Dd][Cc][Oo][Rr]       	{ return CaosScript_K_DCOR; }
	[Dd][Ss][Ee][Ee]       	{ return CaosScript_K_DSEE; }
	[Tt][Cc][Oo][Rr]       	{ return CaosScript_K_TCOR; }
	[Tt][Ii][Nn][Oo]       	{ return CaosScript_K_TINO; }
	[Uu][Cc][Ll][Nn]       	{ return CaosScript_K_UCLN; }
	[Aa][Dd][Ii][Nn]       	{ return CaosScript_K_ADIN; }
	[Bb][Rr][Nn][:]        	{ return CaosScript_K_BRN_COL; }
	[Dd][Mm][Pp][Bb]       	{ return CaosScript_K_DMPB; }
	[Dd][Mm][Pp][Dd]       	{ return CaosScript_K_DMPD; }
	[Dd][Mm][Pp][Ll]       	{ return CaosScript_K_DMPL; }
	[Dd][Mm][Pp][Nn]       	{ return CaosScript_K_DMPN; }
	[Dd][Mm][Pp][Tt]       	{ return CaosScript_K_DMPT; }
	[Ss][Ee][Tt][Dd]       	{ return CaosScript_K_SETD; }
	[Ss][Ee][Tt][Ll]       	{ return CaosScript_K_SETL; }
	[Ss][Ee][Tt][Nn]       	{ return CaosScript_K_SETN; }
	[Ss][Ee][Tt][Tt]       	{ return CaosScript_K_SETT; }
	[Dd][Oo][Ii][Nn]       	{ return CaosScript_K_DOIN; }
	[_][Cc][Dd][_]         	{ return CaosScript_K__CD_; }
	[Ee][Jj][Cc][Tt]       	{ return CaosScript_K_EJCT; }
	[Ff][Rr][Qq][Hh]       	{ return CaosScript_K_FRQH; }
	[Ff][Rr][Qq][Ll]       	{ return CaosScript_K_FRQL; }
	[Ff][Rr][Qq][Mm]       	{ return CaosScript_K_FRQM; }
	[Ii][Nn][Ii][Tt]       	{ return CaosScript_K_INIT; }
	[Pp][Ll][Aa][Yy]       	{ return CaosScript_K_PLAY; }
	[Ss][Hh][Uu][Tt]       	{ return CaosScript_K_SHUT; }
	[Pp][Aa][Tt][:]        	{ return CaosScript_K_PAT_COL; }
	[Bb][Uu][Tt][Tt]       	{ return CaosScript_K_BUTT; }
	[Dd][Uu][Ll][Ll]       	{ return CaosScript_K_DULL; }
	[Ff][Ii][Xx][Dd]       	{ return CaosScript_K_FIXD; }
	[Gg][Rr][Pp][Hh]       	{ return CaosScript_K_GRPH; }
	[Mm][Oo][Vv][Ee]       	{ return CaosScript_K_MOVE; }
	[Tt][Ee][Xx][Tt]       	{ return CaosScript_K_TEXT; }
	[Pp][Nn][Xx][Tt]       	{ return CaosScript_K_PNXT; }
	[Bb][Oo][Oo][Tt]       	{ return CaosScript_K_BOOT; }
	[Cc][Aa][Ll][Gg]       	{ return CaosScript_K_CALG; }
	[Mm][Ii][Nn][Dd]       	{ return CaosScript_K_MIND; }
	[Mm][Oo][Tt][Rr]       	{ return CaosScript_K_MOTR; }
	[Cc][Rr][Aa][Gg]       	{ return CaosScript_K_CRAG; }
	[Oo][Rr][Dd][Rr]       	{ return CaosScript_K_ORDR; }
	[Pp][Ll][Mm][Dd]       	{ return CaosScript_K_PLMD; }
	[Pp][Ll][Mm][Uu]       	{ return CaosScript_K_PLMU; }
	[Ss][Ee][Ee][Nn]       	{ return CaosScript_K_SEEN; }
	[Ss][Oo][Uu][Ll]       	{ return CaosScript_K_SOUL; }
	[Ss][Tt][Ee][Pp]       	{ return CaosScript_K_STEP; }
	[Ss][Ww][Aa][Yy]       	{ return CaosScript_K_SWAY; }
	[Uu][Rr][Gg][Ee]       	{ return CaosScript_K_URGE; }
	[Bb][Aa][Nn][Gg]       	{ return CaosScript_K_BANG; }
	[Dd][Bb][Gg][:]        	{ return CaosScript_K_DBG_COL; }
	[Aa][Ss][Rr][Tt]       	{ return CaosScript_K_ASRT; }
	[Cc][Pp][Rr][Oo]       	{ return CaosScript_K_CPRO; }
	[Ff][Ll][Ss][Hh]       	{ return CaosScript_K_FLSH; }
	[Hh][Tt][Mm][Ll]       	{ return CaosScript_K_HTML; }
	[Pp][Oo][Ll][Ll]       	{ return CaosScript_K_POLL; }
	[Pp][Rr][Oo][Ff]       	{ return CaosScript_K_PROF; }
	[Tt][Oo][Cc][Kk]       	{ return CaosScript_K_TOCK; }
	[Ff][Ii][Ll][Ee]       	{ return CaosScript_K_FILE; }
	[Gg][Ll][Oo][Bb]       	{ return CaosScript_K_GLOB; }
	[Ii][Cc][Ll][Oo]       	{ return CaosScript_K_ICLO; }
	[Ii][Oo][Pp][Ee]       	{ return CaosScript_K_IOPE; }
	[Jj][Dd][Ee][Ll]       	{ return CaosScript_K_JDEL; }
	[Oo][Cc][Ll][Oo]       	{ return CaosScript_K_OCLO; }
	[Oo][Ff][Ll][Uu]       	{ return CaosScript_K_OFLU; }
	[Oo][Oo][Pp][Ee]       	{ return CaosScript_K_OOPE; }
	[Ww][Ee][Bb][Bb]       	{ return CaosScript_K_WEBB; }
	[Cc][Ll][Oo][Nn]       	{ return CaosScript_K_CLON; }
	[Cc][Rr][Oo][Ss]       	{ return CaosScript_K_CROS; }
	[Hh][Ii][Ss][Tt]       	{ return CaosScript_K_HIST; }
	[Cc][Oo][Uu][Nn]       	{ return CaosScript_K_COUN; }
	[Ff][Ii][Nn][Dd]       	{ return CaosScript_K_FIND; }
	[Ff][Ii][Nn][Rr]       	{ return CaosScript_K_FINR; }
	[Ff][Oo][Tt][Oo]       	{ return CaosScript_K_FOTO; }
	[Gg][Ee][Nn][Dd]       	{ return CaosScript_K_GEND; }
	[Nn][Aa][Mm][Ee]       	{ return CaosScript_K_NAME; }
	[Nn][Ee][Tt][Uu]       	{ return CaosScript_K_NETU; }
	[Pp][Rr][Ee][Vv]       	{ return CaosScript_K_PREV; }
	[Uu][Tt][Xx][Tt]       	{ return CaosScript_K_UTXT; }
	[Vv][Aa][Rr][Ii]       	{ return CaosScript_K_VARI; }
	[Ww][Ii][Pp][Ee]       	{ return CaosScript_K_WIPE; }
	[Ww][Vv][Ee][Tt]       	{ return CaosScript_K_WVET; }
	[Hh][Oo][Tt][Pp]       	{ return CaosScript_K_HOTP; }
	[Cc][Aa][Ll][Cc]       	{ return CaosScript_K_CALC; }
	[Aa][Dd][Mm][Pp]       	{ return CaosScript_K_ADMP; }
	[Aa][Nn][Gg][Ll]       	{ return CaosScript_K_ANGL; }
	[Aa][Vv][Ee][Ll]       	{ return CaosScript_K_AVEL; }
	[Ff][Dd][Mm][Pp]       	{ return CaosScript_K_FDMP; }
	[Ff][Vv][Ee][Ll]       	{ return CaosScript_K_FVEL; }
	[Rr][Oo][Tt][Nn]       	{ return CaosScript_K_ROTN; }
	[Ss][Dd][Mm][Pp]       	{ return CaosScript_K_SDMP; }
	[Ss][Pp][Ii][Nn]       	{ return CaosScript_K_SPIN; }
	[Ss][Vv][Ee][Ll]       	{ return CaosScript_K_SVEL; }
	[Vv][Aa][Rr][Cc]       	{ return CaosScript_K_VARC; }
	[Vv][Ee][Cc][Xx]       	{ return CaosScript_K_VECX; }
	[Vv][Ee][Cc][Yy]       	{ return CaosScript_K_VECY; }
	[Nn][Ee][Tt][:]        	{ return CaosScript_K_NET_COL; }
	[Ee][Rr][Rr][Aa]       	{ return CaosScript_K_ERRA; }
	[Ee][Xx][Pp][Oo]       	{ return CaosScript_K_EXPO; }
	[Hh][Ee][Aa][Dd]       	{ return CaosScript_K_HEAD; }
	[Hh][Ee][Aa][Rr]       	{ return CaosScript_K_HEAR; }
	[Hh][Oo][Ss][Tt]       	{ return CaosScript_K_HOST; }
	[Pp][Aa][Ss][Ss]       	{ return CaosScript_K_PASS; }
	[Rr][Aa][Ww][Ee]       	{ return CaosScript_K_RAWE; }
	[Rr][Uu][Ss][Oo]       	{ return CaosScript_K_RUSO; }
	[Ss][Tt][Aa][Tt]       	{ return CaosScript_K_STAT; }
	[Uu][Ll][Ii][Nn]       	{ return CaosScript_K_ULIN; }
	[Uu][Nn][Ii][Kk]       	{ return CaosScript_K_UNIK; }
	[Uu][Ss][Ee][Rr]       	{ return CaosScript_K_USER; }
	[Ww][Hh][Aa][Tt]       	{ return CaosScript_K_WHAT; }
	[Ww][Hh][Oo][Dd]       	{ return CaosScript_K_WHOD; }
	[Ww][Hh][Oo][Ff]       	{ return CaosScript_K_WHOF; }
	[Ww][Hh][Oo][Nn]       	{ return CaosScript_K_WHON; }
	[Ww][Hh][Oo][Zz]       	{ return CaosScript_K_WHOZ; }
	[Pp][Rr][Tt][:]        	{ return CaosScript_K_PRT_COL; }
	[Ff][Rr][Mm][Aa]       	{ return CaosScript_K_FRMA; }
	[Ii][Nn][Ee][Ww]       	{ return CaosScript_K_INEW; }
	[Ii][Tt][Oo][Tt]       	{ return CaosScript_K_ITOT; }
	[Ii][Zz][Aa][Pp]       	{ return CaosScript_K_IZAP; }
	[Jj][Oo][Ii][Nn]       	{ return CaosScript_K_JOIN; }
	[Kk][Rr][Aa][Kk]       	{ return CaosScript_K_KRAK; }
	[Oo][Nn][Ee][Ww]       	{ return CaosScript_K_ONEW; }
	[Oo][Tt][Oo][Tt]       	{ return CaosScript_K_OTOT; }
	[Oo][Zz][Aa][Pp]       	{ return CaosScript_K_OZAP; }
	[Ss][Ee][Nn][Dd]       	{ return CaosScript_K_SEND; }
	[Mm][Aa][Kk][Ee]       	{ return CaosScript_K_MAKE; }
	[Pp][Rr][Aa][Yy]       	{ return CaosScript_K_PRAY; }
	[Aa][Gg][Tt][Ii]       	{ return CaosScript_K_AGTI; }
	[Aa][Gg][Tt][Ss]       	{ return CaosScript_K_AGTS; }
	[Bb][Aa][Cc][Kk]       	{ return CaosScript_K_BACK; }
	[Dd][Ee][Pp][Ss]       	{ return CaosScript_K_DEPS; }
	[Ff][Oo][Rr][Ee]       	{ return CaosScript_K_FORE; }
	[Gg][Aa][Rr][Bb]       	{ return CaosScript_K_GARB; }
	[Ii][Mm][Pp][Oo]       	{ return CaosScript_K_IMPO; }
	[Ii][Nn][Jj][Tt]       	{ return CaosScript_K_INJT; }
	[Rr][Ee][Ff][Rr]       	{ return CaosScript_K_REFR; }
	[Tt][Ee][Ss][Tt]       	{ return CaosScript_K_TEST; }
	[Jj][Ee][Cc][Tt]       	{ return CaosScript_K_JECT; }
	[Bb][Uu][Zz][Zz]       	{ return CaosScript_K_BUZZ; }
	[Dd][Ee][Ll][Ee]       	{ return CaosScript_K_DELE; }
	[Ee][Aa][Mm][Ee]       	{ return CaosScript_K_EAME; }
	[Ee][Aa][Mm][Nn]       	{ return CaosScript_K_EAMN; }
	[Ll][Oo][Ww][Aa]       	{ return CaosScript_K_LOWA; }
	[Mm][Aa][Mm][Ee]       	{ return CaosScript_K_MAME; }
	[Mm][Oo][Dd][Uu]       	{ return CaosScript_K_MODU; }
	[Nn][Aa][Mm][Nn]       	{ return CaosScript_K_NAMN; }
	[Nn][Oo][Tt][Vv]       	{ return CaosScript_K_NOTV; }
	[Ss][Ii][Nn][Ss]       	{ return CaosScript_K_SINS; }
	[Uu][Ff][Oo][Ss]       	{ return CaosScript_K_UFOS; }
	[Uu][Pp][Pp][Aa]       	{ return CaosScript_K_UPPA; }
	[Mm][Oo][Nn][1]        	{ return CaosScript_K_MON1; }
	[Mm][Oo][Nn][2]        	{ return CaosScript_K_MON2; }
	[Rr][Nn][Dd][Rr]       	{ return CaosScript_K_RNDR; }
    [Ss][Ss][Ff][Cc]		{ return CaosScript_K_SSFC; }
	[Ll][Nn][Gg][+]        	{ return CaosScript_K_LNG_PLUS; }
    [Xx][Ii][Ss][Tt]       	{ return CaosScript_K_XIST; }
    [Dd][Ee][Nn][Dd]		{ return CaosScript_K_DEND; }
    [Rr][Cc][Pp][Rr]		{ return CaosScript_K_RCPR; }
    [Aa][Pp][Pp][:]			{ return CaosScript_K_APP_COL; }
    [Ff][*][*][Kk]			{ return CaosScript_K_F__K; }
    [Bb][Ll][Cc][Kk]		{ return CaosScript_K_BLCK; }
    [Ff][Ll][Ii][Pp] 		{ return CaosScript_K_FLIP; }
	[Rr][Oo][Tt][Aa]		{ return CaosScript_K_ROTA; }
    [Oo][Uu][Tt][Ll]		{ return CaosScript_K_OUTL; }
    [Ss][Hh][Aa][Dd]		{ return CaosScript_K_SHAD; }
    [Ss][Tt][Rr][Cc]		{ return CaosScript_K_STRC; }


	[Aa][Nn][Dd]           	{ return CaosScript_K_AND; }
	[Oo][Rr]               	{ return CaosScript_K_OR; }
    "____X____DEF__"		{ return CaosScript_K_XX_DEF; }
    "____X____EXPR__"		{ return CaosScript_K_XX_EXPR; }
	{EQ_C1}                	{ return CaosScript_EQ_OP_OLD_; }
	{EQ_NEW}               	{ return CaosScript_EQ_OP_NEW_; }
  	{ERROR_WORD}			{ return CaosScript_ERROR_WORD; }
	{WORD}                 	{ return CaosScript_WORD; }
	{INCOMPLETE_WORD}      	{ return CaosScript_ERROR_WORD; }
	{SPACE}                	{ return CaosScript_SPACE_; }
    [^]						{ return BAD_CHARACTER; }
}

<YYINITIAL> 	{
	[^]					 	{ yybegin(START_OF_LINE); yypushback(yylength());}
}
[^] { return BAD_CHARACTER; }
