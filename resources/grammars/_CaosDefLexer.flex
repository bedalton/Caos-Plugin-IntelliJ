package grammars;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;import com.intellij.psi.tree.IElementType;

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

EOL=\R
WHITE_SPACE=\s+

LINE_COMMENT="//"[^\n]*
VAR_KEYWORD=[vV][aA][rR]
ARGS_KEYWORD=[aA][rR][gG][sS]
COMMAND_KEYWORD=[cC][oO][mM][mM][aA][nN][dD]
TYPE_KEYWORD=[tT][yY][pP][eE]
SPACE=[ ]
NEWLINE=\n
ID=[_a-zA-Z][_a-zA-Z0-9]*
TEXT = [^ \[]+
%state LINE_START LINE_CONT DOUBLE_QUOTE SINGLE_QUOTE
%%

<DOUBLE_QUOTE> {
	"\""				   { yypop(); return DOUBLE_QUOTE;}
}

<SINGLE_QUOTE> {
	"\""				   { yypop(); return DOUBLE_QUOTE;}
}

<SINGLE_QUOTE,DOUBLE_QUOTE> {
	{ID}					{ return CaosDef_ID; }
    "["						{ return CaosDef_OPEN_BRACKET;}
    "]"						{ return CaosDef_CLOSE_BRACKET;}
	{TEXT}					{ return CaosDef_TEXT; }
    [ ]					    { return WHITE_SPACE; }
}

<YYINITIAL> {
  {WHITE_SPACE}            { return WHITE_SPACE; }

  ":"                      { return CaosDef_COLON; }
  "("                      { return CaosDef_OPEN_PAREN; }
  ")"                      { return CaosDef_CLOSE_PAREN; }
  "["                      { return CaosDef_OPEN_BRACKET; }
  "]"                      { return CaosDef_CLOSE_BRACKET; }
  "="                      { return CaosDef_EQ; }
  ","                      { return CaosDef_COMMA; }
  ";"                      { return CaosDef_SEMI; }
  "'"					   { yybegin(SINGLE_QUOTE); return CaosDef_SINGLE_QUO;}
  "\""					   { yybegin(DOUBLE_QUOTE); return CaosDef_DOUBLE_QUO;}

  {LINE_COMMENT}           { return CaosDef_LINE_COMMENT; }
  {VAR_KEYWORD}            { return CaosDef_VAR_KEYWORD; }
  {ARGS_KEYWORD}           { return CaosDef_ARGS_KEYWORD; }
  {COMMAND_KEYWORD}        { return CaosDef_COMMAND_KEYWORD; }
  {TYPE_KEYWORD}           { return CaosDef_TYPE_KEYWORD; }
  {SPACE}                  { return CaosDef_SPACE; }
  {NEWLINE}                { return CaosDef_NEWLINE; }
  {ID}                     { return CaosDef_ID; }

}

[^] { return BAD_CHARACTER; }
