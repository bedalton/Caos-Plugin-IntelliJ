package com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer;

import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;

import com.intellij.lexer.FlexLexer;
import java.util.logging.Logger;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes.*;

%%

%{
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
%}

%public
%class _CaosDefLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

WHITE_SPACE=\s+
TEXT=([^*\n \[]|"*"[^/])+
DOC_COMMENT_OPEN="/"[*]+
DOC_COMMENT_CLOSE=[*]+"/"
LINE_COMMENT="//"[^\n]*
WORD=[a-zA-Z_0-9]{3}[a-zA-Z0-9#!$_:+]
TYPE_LINK=[@]\{[^}]\}
ID=[_a-zA-Z][_a-zA-Z0-9]*[%]?
CODE_BLOCK_LITERAL=[#][{][^}]*[}]
AT_RVALUE=[@][rR][vV][aA][lL][uU][eE]
AT_LVALUE=[@][lL][vV][aA][lL][uU][eE]
AT_PARAM=[@][pP][aA][rR][aA][mM]
AT_RETURN=[@][rR][eE][tT][uU][rR][nN][sS]?
AT_ID=[@][a-zA-Z_][a-zA-Z_0-9]*
AT_FILE=[@][Ff][Ii][Ll][Ee][.][a-zA-Z_][a-zA-Z_0-9/]*
TYPE_DEF_KEY=([!>][ ]?)?[a-zA-Z0-9_#-]+([ ]+ [a-zA-Z0-9_#-]+)*
TYPE_DEF_VALUE=([^-\n]|-[^ ])+
DEF_TEXT=[^\n\[]+
INT=[-+]?[0-9]+
TO=([.]{2,3}|[Tt][Oo])
UNTIL=[uU][nN][tT][iI][lL]
EXCLUSIVE=[!][Ee][Xx][Cc][Ll][Uu][Ss][Ii][Vv][Ee]
REGION=[#][^\n]+
HASH_TAG=[#][A-Za-z][_A-Za-z0-9]*
VARIABLE_LINK=[{]{ID}[}]
AT_VARIANTS=[@][Vv][Aa][Rr][Ii][Aa][Nn][Tt][Ss]?
VARIANT_ID=[A-Za-z][A-Za-z0-9]
VARIANT_NAME=[A-Za-z0-9 _]+
%state IN_TYPEDEF IN_TYPEDEF_VALUE IN_TYPEDEF_TEXT IN_LINK IN_COMMENT COMMENT_START IN_PARAM_COMMENT IN_COMMENT_AFTER_VAR IN_HEADER IN_BODY IN_VARIANT IN_HASHTAG_LINE AFTER_TYPEDEF_NAME

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

<COMMENT_START> {
	{AT_RVALUE}                	{ yybegin(IN_COMMENT); return CaosDef_AT_RVALUE; }
	{AT_LVALUE}                	{ yybegin(IN_COMMENT); return CaosDef_AT_LVALUE; }
	{AT_PARAM}                 	{ yybegin(IN_PARAM_COMMENT); return CaosDef_AT_PARAM; }
	{AT_RETURN}               	{ needs_type = true; yybegin(IN_COMMENT_AFTER_VAR); return CaosDef_AT_RETURN; }
  	{AT_VARIANTS}				{ return CaosDef_AT_VARIANT; }
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
    {INT}						{ return CaosDef_INT_LITERAL; }
	{WHITE_SPACE}          		{ return WHITE_SPACE; }
	[^]						 	{ yybegin(IN_COMMENT); yypushback(1); }
}

<IN_COMMENT_AFTER_VAR> {
    {TO}						{ return CaosDef_TO;}
    {UNTIL}						{ return CaosDef_UNTIL; }
    {AT_FILE}					{ return CaosDef_AT_FILE; }
    {AT_ID}						{ return CaosDef_AT_ID; }
    {INT}						{ return CaosDef_INT_LITERAL; }
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

<IN_COMMENT> {
	{CODE_BLOCK_LITERAL}       	{ return CaosDef_CODE_BLOCK_LITERAL; }
    {VARIABLE_LINK}				{ return CaosDef_VARIABLE_LINK_LITERAL; }
	{TYPE_LINK}				 	{ return CaosDef_TYPE_LINK_LITERAL; }
  	//"["	/{WORD}					{ return CaosDef_COMMENT_TEXT_LITERAL; }
  	"[" 						{ inLink = IN_COMMENT; yybegin(IN_LINK); return CaosDef_OPEN_BRACKET; }
	{TEXT}						{ return CaosDef_COMMENT_TEXT_LITERAL; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
}

<IN_TYPEDEF> {
	"="                        	{ yybegin(IN_TYPEDEF_VALUE); return CaosDef_EQ; }
	"}"						 	{ yybegin(IN_BODY); return CaosDef_CLOSE_BRACE; }
	"{"						 	{ return CaosDef_OPEN_BRACE; }
	","                        	{ return CaosDef_COMMA; }
    ":"							{ return CaosDef_COLON; }
    {EXCLUSIVE}					{ return CaosDef_EXCLUSIVE ;}
    {REGION}					{ return CaosDef_REGION_HEADING_LITERAL; }
	{TYPE_DEF_KEY}			 	{ return CaosDef_TYPE_DEF_KEY; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
}

<AFTER_TYPEDEF_NAME> {
	"-"                        	{ yybegin(IN_TYPEDEF_TEXT); return CaosDef_DASH; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
    [^]							{ yybegin(IN_TYPEDEF); yypushback(yylength()); }
}

<IN_TYPEDEF_VALUE> {
	{TYPE_DEF_VALUE}			{ yybegin(AFTER_TYPEDEF_NAME); return CaosDef_TYPE_DEF_VALUE; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
    [^]							{ yybegin(IN_TYPEDEF); yypushback(yylength()); }
}

<IN_TYPEDEF_TEXT> {
	\n							{ yybegin(IN_TYPEDEF); return WHITE_SPACE; }
  	"["							{ inLink = IN_TYPEDEF_TEXT; yybegin(IN_LINK); return CaosDef_OPEN_BRACKET; }
	{DEF_TEXT}					{ return CaosDef_TEXT_LITERAL; }
    [^]							{ yybegin(IN_TYPEDEF); yypushback(yylength()); }
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
	{AT_ID}                    	{ yybegin(IN_TYPEDEF); return CaosDef_AT_ID; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
}

<IN_VARIANT> {
	")"                        	{ yybegin(IN_BODY); return CaosDef_CLOSE_PAREN; }
    ","							{ canDoName = false; return CaosDef_COMMA; }
    "="							{ canDoName = true; return CaosDef_EQ; }
  	{VARIANT_ID}				{ return CaosDef_ID; }
    {VARIANT_NAME}				{ if (canDoName) { canDoName = false; return CaosDef_VARIANT_NAME_LITERAL; } else return CaosDef_ID;}
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
	[^]							{ yybegin(IN_BODY); yypushback(1); }
}

<YYINITIAL> {
	"("                        	{ yybegin(IN_VARIANT); return CaosDef_OPEN_PAREN; }
	{AT_VARIANTS}				{ return CaosDef_AT_VARIANT; }
	{WHITE_SPACE}              	{ return WHITE_SPACE; }
	[^]							{ yybegin(IN_BODY); yypushback(1); }
}

{LINE_COMMENT}					{ return CaosDef_LINE_COMMENT; }
[^] { return BAD_CHARACTER; }
