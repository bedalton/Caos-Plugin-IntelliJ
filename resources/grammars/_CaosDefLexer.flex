package grammars;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.openc2e.plugins.intellij.caos.def.lexer.CaosDefTypes.*;

%%

%{

  private int braceSteps = 0;
  private boolean inLink = false;
    private boolean needsEquals = false;
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

LINE_COMMENT="//"[^\n]*
VAR_KEYWORD=[@][vV][aA][rR]
ARGS_KEYWORD=[aA][rR][gG][sS]
COMMAND_KEYWORD=[@][cC][oO][mM][mM][aA][nN][dD]
TYPE_KEYWORD=[@][tT][yY][pP][eE]
WHITE_SPACE=\s
NEWLINE=\n
ID=[_a-zA-Z][_a-zA-Z0-9]*
TEXT = [^ \[]+
%state IN_BLOCK IN_COMMENT
%%

<IN_BLOCK> {
	{ID}					{ return CaosDef_ID; }
    ':'                     { return CaosDef_COLON; }
    \s+                     { return WHITE_SPACE; }
    ']'                     { yybegin(YYINITIAL); return CaosDef_CLOSE_BRACKET; }
}

<IN_COMMENT> {
    "<<"                    { inLink = true; return CaosDef_OPEN_LINK; }
      ">>"                  {
          if (!inLink)
              return CaosDef_TEXT;
        inLink = false;
        return CaosDef_CLOSE_LINK;
      }
    "["						{ braceSteps++; return CaosDef_OPEN_BRACKET;}
    "]"						{
          if(--braceSteps == 0) {
              yybegin(YYINITIAL);
            return CaosDef_CLOSE_BRACKET;
          }
          return CaosDef_TEXT;
      }
      '='                   { return needsEquals ? CaosDef_EQ : CaosDef_TEXT; }
      ":"                   { if (inLink) return CaosDef_COLON; return CaosDef_TEXT; }
    {ID}                    { if (inLink) return CaosDef_ID; return CaosDef_TEXT;}
	{TEXT}					{ return CaosDef_TEXT; }
    [ ]					    { return WHITE_SPACE; }
}

<YYINITIAL> {
  {WHITE_SPACE}            { return WHITE_SPACE; }
  "("                      { return CaosDef_OPEN_PAREN; }
  ")"                      { return CaosDef_CLOSE_PAREN; }
  "["                      { yybegin(IN_BLOCK); return CaosDef_OPEN_BRACKET; }
  "]"                      { return CaosDef_CLOSE_BRACKET; }
  "="                      { return CaosDef_EQ; }
  ","                      { return CaosDef_COMMA; }
  ";"                      { return CaosDef_SEMI; }

  {LINE_COMMENT}           { yybefin(IN_COMMENT); return CaosDef_LINE_COMMENT; }
  {VAR_KEYWORD}            { return CaosDef_VAR_KEYWORD; }
  {ARGS_KEYWORD}           { return CaosDef_ARGS_KEYWORD; }
  {COMMAND_KEYWORD}        { return CaosDef_COMMAND_KEYWORD; }
  {TYPE_KEYWORD}           { return CaosDef_TYPE_KEYWORD; }
  {NEWLINE}                { return CaosDef_NEWLINE; }
  {ID}                     { return CaosDef_ID; }

}

[^] { return BAD_CHARACTER; }
