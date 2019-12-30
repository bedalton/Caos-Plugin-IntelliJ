package com.openc2e.plugins.intellij.caos.def.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.openc2e.plugins.intellij.caos.def.lexer.CaosDefTypes.*;

%%

%{
	public _CaosDefLexer() {
	this((java.io.Reader)null);
	}

	private boolean yytextContainLineBreaks() {
		return CharArrayUtil.containLineBreaks(zzBuffer, zzStartRead, zzMarkedPos);
	}
%}

%public
%class _CaosDefLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

WHITE_SPACE=\s+
TEXT=([^ *]|[*][^/])+
DOC_COMMENT_OPEN="/"[*]+
DOC_COMMENT_CLOSE=[*]+"/"
LINE_COMMENT="//"[^\n]*
WORD=[a-zA-Z_][a-zA-Z0-9#!$_:]{3}
TYPE_LINK=[@]\{[^}]\}
ID=[_a-zA-Z][_a-zA-Z0-9]*
SPACE=[ \t\n\x0B\f\r]+
CODE_BLOCK_LITERAL=#\{[^}]*\}
WORD_LINK=\[[^\]]*\]
AT_RVALUE=[@][rR][vV][aA][lL][uU][eE]
AT_LVALUE=[@][lL][vV][aA][lL][uU][eE]
AT_PARAM=[@][pP][aA][rR][aA][mM]
AT_RETURNS=[@][rR][eE][tT][uU][rR][nN][sS]?
AT_ID=[@][a-zA-Z_][a-zA-Z_0-9]
TYPE_DEF_KEY=[^=]+
TYPE_DEF_VALUE = [^-\n,]+
DEF_TEXT=[^\n]+
INT=[-+]?[0-9]+
TO='..'|[Tt][Oo]
UNTIL=[uU][nN][tT][iI][lL]

%state IN_COMMENT COMMENT_START IN_PARAM_COMMENT IN_TYPEDEF IN_COMMENT_AFTER_VAR

%%

<IN_COMMENT, COMMENT_START, IN_PARAM_COMMENT, IN_COMMENT_AFTER_VAR> {
	{WHITE_SPACE}				 {
		if (yytextContainLineBreaks()) {
			yybegin(COMMENT_START);
		}
		return WHITE_SPACE;
  	}
	"*/"						{ yybegin(YYINITIAL); return CaosDef_DOC_COMMENT_CLOSE; }
}

<COMMENT_START> {
	{AT_RVALUE}                	{ yybegin(IN_COMMENT); return CaosDef_AT_RVALUE; }
	{AT_LVALUE}                	{ yybegin(IN_COMMENT); return CaosDef_AT_LVALUE; }
	{AT_PARAM}                 	{ yybegin(IN_PARAM_COMMENT); return CaosDef_AT_PARAM; }
	{AT_RETURNS}               	{ return CaosDef_AT_RETURNS; }
	"*"	                     	{ return CaosDef_LEADING_ASTRISK; }
	[^]						 	{ yybegin(IN_COMMENT); yypushback(1);}
}

<IN_PARAM_COMMENT> {
    "{"							{ return CaosDef_OPEN_BRACE;  }
    "}"							{ yybegin(IN_COMMENT_AFTER_VAR); return CaosDef_CLOSE_BRACE; }
    {TO}						{ return CaosDef_TO;}
    {UNTIL}						{ return CaosDef_UNTIL; }
    {INT}						{ return CaosDef_INT; }
    {ID}						{ return CaosDef_ID; }
	[^]						 	{ yybegin(IN_COMMENT); yypushback(1);}
}

<IN_COMMENT_AFTER_VAR> {
    {ID}						{ yybegin(IN_COMMENT); return CaosDef_ID; }
    {AT_ID}						{ return CaosDef_AT_ID; }
    "("							{ return CaosDef_OPEN_PAREN; }
	")"						 	{ yybegin(YYINITIAL); return CaosDef_CLOSE_PAREN; }
	[^]						 	{ yybegin(IN_COMMENT); yypushback(1);}
}

<IN_COMMENT, IN_PARAM_COMMENT> {
	{AT_ID}					 	{ return CaosDef_AT_ID; }
}

<IN_COMMENT> {
	{TYPE_LINK}				 	{ return CaosDef_TYPE_LINK; }
	{WORD_LINK}				 	{ return CaosDef_WORD_LINK; }
	{CODE_BLOCK_LITERAL}       	{ return CaosDef_CODE_BLOCK_LITERAL; }
	{TEXT}						{ return CaosDef_TEXT; }
}

<IN_TYPEDEF> {
	"="                        	{ return CaosDef_EQ; }
	"-"                        	{ return CaosDef_DASH; }
	"}"						 	{ return CaosDef_CLOSE_BRACE; }
	"{"						 	{ return CaosDef_OPEN_BRACE; }
	","                        	{ return CaosDef_COMMA; }
	{TYPE_DEF_KEY}			 	{ return CaosDef_TYPE_DEF_KEY; }
	{TYPE_DEF_VALUE}			{ return CaosDef_TYPE_DEF_VALUE; }
	{DEF_TEXT}				 	{ return CaosDef_TEXT; }
}

<YYINITIAL> {
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
	"/*"						{ yybegin(IN_COMMENT); return CaosDef_DOC_COMMENT_OPEN; }
	";"                        	{ return CaosDef_SEMI; }
	":"                        	{ return CaosDef_COLON; }
	"("                        	{ return CaosDef_OPEN_PAREN; }
	")"                        	{ return CaosDef_CLOSE_PAREN; }
 	"["							{ return CaosDef_OPEN_BRACKET; }
 	"]"							{ return CaosDef_CLOSE_BRACKET; }

	{DOC_COMMENT_OPEN}         	{ return CaosDef_DOC_COMMENT_OPEN; }
	{DOC_COMMENT_CLOSE}        	{ return CaosDef_DOC_COMMENT_CLOSE; }
	{LINE_COMMENT}             	{ return CaosDef_LINE_COMMENT; }
	{WORD}                     	{ return CaosDef_WORD; }
	{ID}                       	{ return CaosDef_ID; }
	{AT_ID}                    	{ yybegin(IN_TYPEDEF); return CaosDef_AT_ID; }

}

[^] { return BAD_CHARACTER; }
