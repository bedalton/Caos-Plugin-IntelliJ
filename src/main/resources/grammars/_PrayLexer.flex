package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lexer.PrayTypes.*;

%%

%{
  public _PrayLexer() {
    this((java.io.Reader)null);
  }

  private boolean doLanguage = true;

%}

%public
%class _PrayLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

WHITE_SPACE=\s+

BLOCK_COMMENT=\(-([^-]|-[^)]?)*-\)
LINE_COMMENT=#[^\n\r]*
GROUP=[Gg][Rr][Oo][Uu][Pp]
INLINE=[Ii][Nn][Ll][Ii][Nn][Ee]
BLOCK_TAG_LITERAL=[a-zA-Z_][a-zA-Z0-9#!$_]{3}
INT=[-+]?[0-9]+
FLOAT=[-+]?[0-9]*\.[0-9]+
SINGLE_QUO_STRING='([^\n']|\\[^\n])*'
DOUBLE_QUO_STRING=\"([^\n\"]|\\[^\n])*\"
LANGUAGE=\"[a-zA-Z]{2}-[a-zA-Z]{2}\"

%%
<YYINITIAL> {
  {WHITE_SPACE}            { return WHITE_SPACE; }

  "@"                      { return Pray_AT; }

  {BLOCK_COMMENT}          { return Pray_BLOCK_COMMENT; }
  {LINE_COMMENT}           { return Pray_LINE_COMMENT; }
  {GROUP}                  { doLanguage = false; return Pray_GROUP; }
  {INLINE}                 { doLanguage = false; return Pray_INLINE; }
  {BLOCK_TAG_LITERAL}      { doLanguage = false; return Pray_BLOCK_TAG_LITERAL; }
  {INT}                    { doLanguage = false; return Pray_INT; }
  {FLOAT}                  { doLanguage = false; return Pray_FLOAT; }
  {LANGUAGE}               {
          if (doLanguage) {
		  	doLanguage = false;
          	return Pray_LANGUAGE_STRING;
          }
  }
  {SINGLE_QUO_STRING}      { doLanguage = false; return Pray_SINGLE_QUO_STRING; }
  {DOUBLE_QUO_STRING}      { doLanguage = false; return Pray_DOUBLE_QUO_STRING; }

}

[^] { return BAD_CHARACTER; }
