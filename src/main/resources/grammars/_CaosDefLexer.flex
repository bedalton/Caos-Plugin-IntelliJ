package com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;

import java.lang.Exception;
import java.util.regex.Pattern;
import java.util.List;
import java.util.logging.Logger;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes.*;
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosScriptArrayUtils;
import static com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptTreeUtilKt.getLOGGER;

%%

%{
	private static final String WORD = "([a-zA-Z_0-9]{3}[a-zA-Z0-9#!$_:+]|[Ff][*]{2}[Kk])";
	private static final String WORD_CHECK_REGEX = WORD + "([ ]"+WORD+")*\\]";
  	private int paren_depth = 0;
	private boolean needs_type = false;
	private boolean canDoName = false;
	private static final Logger LOGGER = Logger.getLogger("#_CaosDefLexer");
	private int inLink;

	public _CaosDefLexer() {
		this((java.io.Reader)null);
	}

	private boolean yytextContainLineBreaks() {
		return CharArrayUtil.containLineBreaks(zzBuffer, zzStartRead, zzMarkedPos);
	}

	private boolean atIsNext() {
	    return yycharat(yylength()) == '@';
	}

	/**
	 * Checks whether the text following '[' is a link
	 * @return <b>true</b> if this link is valid
	 */
	private boolean isValidLink() {
		try {
			char[] chars = new char[5];
			for(int i=0; i<5;i++) {
				chars[i] = yycharat(i+1); // offset by one to account for position not yet consuming '['
			}
			final String word = new String(chars);
			return word.matches(WORD_CHECK_REGEX);
		} catch (Exception e) {
			return false;
		}
	}

%}

%public
%class _CaosDefLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

WHITE_SPACE=\s+
TEXT=([^*\n \[#]|[#][^{]|[#]\$|"*"[^/])+
DOC_COMMENT_OPEN="/"[*]+
DOC_COMMENT_CLOSE=[*]+"/"
LINE_COMMENT="//"[^\n]*
WORD_SIMPLE=[a-zA-Z_0-9]{3}[a-zA-Z0-9#!$_:+]
F__K=[Ff][*]{2}[Kk]
WORD={WORD_SIMPLE}|{F__K}
TYPE_LINK=[@]\{[^}]\}
ID=[_a-zA-Z][_a-zA-Z0-9]*[%]?
CODE_BLOCK_LITERAL=[#][{][^}]*[}]
AT_RVALUE=[@][rR][vV][aA][lL][uU][eE]
AT_LVALUE=[@][lL][vV][aA][lL][uU][eE]
AT_PARAM=[@][pP][aA][rR][aA][mM]
AT_RETURN=[@][rR][eE][tT][uU][rR][nN][sS]?
AT_OWNR=[@][Oo][Ww][Nn][Rr]
AT_ID=[@][a-zA-Z_][a-zA-Z_0-9]*
AT_FILE=[@][Ff][Ii][Ll][Ee][.][a-zA-Z_][a-zA-Z_0-9/]*
VALUES_LIST_VALUE_KEY=([!>][ ]?)?[a-zA-Z0-9_#-]+([ ]+ [a-zA-Z0-9_#-]+)*
VALUES_LIST_VALUE_NAME=([^-\n@]|-[^ ])+
DEF_TEXT=[^\n\[]+
TO=([.]{2,3}|[Tt][Oo])
UNTIL=[uU][nN][tT][iI][lL]
EXCLUSIVE=[!][Ee][Xx][Cc][Ll][Uu][Ss][Ii][Vv][Ee]
REGION=[#][^\n]+
HASH_TAG=[#][A-Za-z][_A-Za-z0-9]*
VARIABLE_LINK=[{]{ID}[}]
AT_VARIANTS=[@][Vv][Aa][Rr][Ii][Aa][Nn][Tt][Ss]?
VARIANT_ID=[A-Za-z][A-Za-z0-9]
VARIANT_NAME=[A-Za-z0-9 _]+
EQ=[Ee][Qq]|[Nn][Ee]|[Ll][Tt]|[Gg][Tt]|[Ll][Ee]|[Gg][Ee]|[Bb][Tt]|[Bb][Ff]|"="|"<>"|">"|">="|"<"|"<="
INT_SIGN=[-+]
INT = [0-9]+
NUMBER={INT_SIGN}?({INT}[.])?{INT}
ANIMATION=\[({INT}[ ]?)+R?\]
BRACKET_STRING=\[[^\]]\]
QUOTE_STRING=\"(\\\"|[^\"])*\"
STRING={QUOTE_STRING}|{BRACKET_STRING}
AND=[Aa][Nn][Dd]
OR=[Oo][Rr]
AND_OR={AND}|{OR}
COMMENT=[*][^\n\*]*
%state IN_VALUES_LIST IN_VALUES_LIST_VALUE IN_VALUES_LIST_TEXT IN_LINK IN_COMMENT COMMENT_START IN_PARAM_COMMENT IN_COMMENT_AFTER_VAR IN_HEADER IN_BODY IN_VARIANT IN_HASHTAG_LINE AFTER_VALUES_LIST_NAME IN_CODE_BLOCK_LITERAL IN_LVALUE

%%

<IN_COMMENT, COMMENT_START, IN_PARAM_COMMENT, IN_COMMENT_AFTER_VAR, IN_HASHTAG_LINE> {
	{WHITE_SPACE}				{
									if (yytextContainLineBreaks()) {
										yybegin(COMMENT_START);
									}
									return WHITE_SPACE;
								}
	"*/"						{ yybegin(IN_BODY); return CaosDef_DOC_COMMENT_CLOSE; }
  	'\n'						{ yybegin(COMMENT_START); return CaosDef_NEWLINE; }
}

<IN_HASHTAG_LINE> {
    {HASH_TAG}					{ return CaosDef_HASH_TAG; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
	[^]						 	{ yybegin(IN_COMMENT); yypushback(1); }
}

<IN_LVALUE> {
	"("							{ return CaosDef_OPEN_PAREN; }
	")"							{ return CaosDef_CLOSE_PAREN; }
  	.+ / [\)\n]					{ return CaosDef_ID; }
	[^]							{ yybegin(IN_COMMENT); yypushback(yylength());}
}

<COMMENT_START> {
	{AT_RVALUE}                	{ yybegin(IN_COMMENT); return CaosDef_AT_RVALUE; }
	{AT_LVALUE}                	{ yybegin(IN_LVALUE); return CaosDef_AT_LVALUE; }
	{AT_PARAM}                 	{ yybegin(IN_PARAM_COMMENT); return CaosDef_AT_PARAM; }
	{AT_RETURN}               	{ needs_type = true; yybegin(IN_COMMENT_AFTER_VAR); return CaosDef_AT_RETURN; }
  	{AT_VARIANTS}				{ return CaosDef_AT_VARIANT; }
  	{AT_OWNR}					{ return CaosDef_AT_OWNR; }
	{VARIABLE_LINK}				{ return CaosDef_VARIABLE_LINK_LITERAL; }
    {HASH_TAG}					{ return CaosDef_HASH_TAG; }
	{WHITE_SPACE}         		{ return WHITE_SPACE; }
	"*"	                     	{ return CaosDef_LEADING_ASTRISK; }
	[^]						 	{ yybegin(IN_COMMENT); yypushback(1); }
}

<IN_PARAM_COMMENT> {
    "{"							{ return CaosDef_OPEN_BRACE;  }
    "}"							{ needs_type = true; return CaosDef_CLOSE_BRACE; }
	{VARIABLE_LINK}				{ needs_type = true; yybegin(IN_COMMENT_AFTER_VAR); return CaosDef_VARIABLE_LINK_LITERAL; }
    {NUMBER}					{ return CaosDef_INT_LITERAL; }
	{WHITE_SPACE}          		{ return WHITE_SPACE; }
	[^]						 	{ yybegin(IN_COMMENT); yypushback(1); }
}

<IN_COMMENT_AFTER_VAR> {
    {TO}						{ return CaosDef_TO;}
    {UNTIL}						{ return CaosDef_UNTIL; }
    {AT_FILE}					{ return CaosDef_AT_FILE; }
    {AT_ID}						{ return CaosDef_AT_ID; }
    {NUMBER}						{ return CaosDef_INT_LITERAL; }
    {ID}						{ return CaosDef_ID; }
    "("							{ paren_depth++; return CaosDef_OPEN_PAREN; }
	")"						 	{ paren_depth--; yybegin(IN_COMMENT);return CaosDef_CLOSE_PAREN; }
  	"["							{ return needs_type ? CaosDef_OPEN_BRACKET : CaosDef_TEXT_LITERAL; }
  	"]"							{
          if (paren_depth < 1)
			yybegin(IN_COMMENT);
		if (needs_type) {
			needs_type = false;
			return CaosDef_CLOSE_BRACKET;
		}
		return CaosDef_TEXT_LITERAL;
      }
	":"							{ return CaosDef_COLON; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
	[^]						 	{ yybegin(IN_COMMENT); yypushback(1); }
}

<IN_COMMENT, IN_PARAM_COMMENT> {
	{AT_ID}					 	{ return CaosDef_AT_ID; }
}

<IN_LINK> {
	{WORD}						{ return CaosDef_WORD; }
    "]"							{ yybegin(inLink); return CaosDef_CLOSE_BRACKET; }
    " "							{ return WHITE_SPACE; }
  	[^]							{ yybegin(inLink); yypushback(yylength()); }
}

<IN_CODE_BLOCK_LITERAL> {
	{ANIMATION}					{ return CaosDef_CODE_BLOCK_ANIMATION; }
    {STRING}					{ return CaosDef_CODE_BLOCK_STRING; }
	{WORD}   					{ return CaosDef_WORD; }
 	{NUMBER}					{ return CaosDef_CODE_BLOCK_NUMBER; }
 	{EQ}						{ return CaosDef_CODE_BLOCK_EQ; }
    {AND_OR}					{ return CaosDef_CODE_BLOCK_AND_OR; }
  	{COMMENT}					{ return CaosDef_CODE_BLOCK_COMMENT;}
    \}#?						{ yybegin(IN_COMMENT); return CaosDef_CLOSE_BRACE; }
    \s+							{ return WHITE_SPACE; }
 	[^]							{ yybegin(IN_COMMENT); yypushback(yylength()); }
}

<IN_COMMENT> {
	[#][{]       				{ yybegin(IN_CODE_BLOCK_LITERAL); return CaosDef_CODE_BLOCK_OPEN_BRACE; }
    {VARIABLE_LINK}				{ return CaosDef_VARIABLE_LINK_LITERAL; }
	{TYPE_LINK}				 	{ return CaosDef_TYPE_LINK_LITERAL; }
  	//"["	/{WORD}					{ return CaosDef_COMMENT_TEXT_LITERAL; }
  	"[" 						{
          if (!isValidLink())
              return CaosDef_COMMENT_TEXT_LITERAL;
          inLink = IN_COMMENT;
          yybegin(IN_LINK);
          return CaosDef_OPEN_BRACKET;
  	}
	{TEXT}						{ return CaosDef_COMMENT_TEXT_LITERAL; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
}

<IN_VALUES_LIST> {
	"="                        	{ yybegin(IN_VALUES_LIST_VALUE); return CaosDef_EQ; }
	"}"						 	{ yybegin(IN_BODY); return CaosDef_CLOSE_BRACE; }
	"{"						 	{ return CaosDef_OPEN_BRACE; }
	","                        	{ return CaosDef_COMMA; }
    ":"							{ return CaosDef_COLON; }
    {EXCLUSIVE}					{ return CaosDef_EXCLUSIVE ;}
    {REGION}					{ return CaosDef_REGION_HEADING_LITERAL; }
	{VALUES_LIST_VALUE_KEY}		{ return CaosDef_VALUES_LIST_VALUE_KEY_LITERAL; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
}

<AFTER_VALUES_LIST_NAME> {
	"-"                        	{ yybegin(IN_VALUES_LIST_TEXT); return CaosDef_DASH; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
    [^]							{ yybegin(IN_VALUES_LIST); yypushback(yylength()); }
}

<IN_VALUES_LIST_VALUE> {
	{VALUES_LIST_VALUE_NAME}	{ if (!atIsNext()) yybegin(AFTER_VALUES_LIST_NAME); return CaosDef_VALUES_LIST_VALUE_NAME_LITERAL; }
  	{AT_ID}						{ yybegin(AFTER_VALUES_LIST_NAME); return CaosDef_AT_ID; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
    [^]							{ yybegin(IN_VALUES_LIST); yypushback(yylength()); }
}

<IN_VALUES_LIST_TEXT> {
	\n							{ yybegin(IN_VALUES_LIST); return WHITE_SPACE; }
  	"["							{
				if (!isValidLink())
					return CaosDef_TEXT_LITERAL;
				inLink = IN_VALUES_LIST_TEXT;
				yybegin(IN_LINK);
				return CaosDef_OPEN_BRACKET;
			}
	{DEF_TEXT}					{ return CaosDef_TEXT_LITERAL; }
    [^]							{ yybegin(IN_VALUES_LIST); yypushback(yylength()); }
}

<IN_BODY> {
	"/*"						{ yybegin(IN_COMMENT); return CaosDef_DOC_COMMENT_OPEN; }
	";"                        	{ return CaosDef_SEMI; }
	":"                        	{ return CaosDef_COLON; }
	"("                        	{ return CaosDef_OPEN_PAREN; }
	")"                        	{ return CaosDef_CLOSE_PAREN; }
 	"["							{ return CaosDef_OPEN_BRACKET; }
 	"]"							{ return CaosDef_CLOSE_BRACKET; }

	{DOC_COMMENT_OPEN}         	{ yybegin(IN_COMMENT); return CaosDef_DOC_COMMENT_OPEN; }
	{LINE_COMMENT}             	{ return CaosDef_LINE_COMMENT; }
	{WORD}                     	{ return CaosDef_WORD; }
	{ID}                       	{ return CaosDef_ID; }
	{AT_ID}                    	{ yybegin(IN_VALUES_LIST); return CaosDef_AT_ID; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
}

<IN_VARIANT> {
	")"                        	{ yybegin(IN_BODY); return CaosDef_CLOSE_PAREN; }
    ","							{ canDoName = false; return CaosDef_COMMA; }
    "="							{ canDoName = true; return CaosDef_EQ; }
  	{VARIANT_ID}				{ return CaosDef_ID; }
    {VARIANT_NAME}				{ if (canDoName) { canDoName = false; return CaosDef_VARIANT_NAME_LITERAL; } else return CaosDef_ID;}
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
	[^]							{ yybegin(IN_BODY); yypushback(yylength()); }
}

<YYINITIAL> {
	"("                        	{ yybegin(IN_VARIANT); return CaosDef_OPEN_PAREN; }
	{AT_VARIANTS}				{ return CaosDef_AT_VARIANT; }
  	{REGION}					{ return CaosDef_REGION_HEADING_LITERAL; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
	[^]							{ yybegin(IN_BODY); yypushback(yylength()); }
}

{LINE_COMMENT}					{ return CaosDef_LINE_COMMENT; }
[^] { return BAD_CHARACTER; }
