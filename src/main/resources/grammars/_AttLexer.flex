package com.badahori.creatures.plugins.intellij.agenteering.att.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.badahori.creatures.plugins.intellij.agenteering.att.lexer.AttTypes.*;

%%

%{
  public _AttLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _AttLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

INT_LITERAL=[0-9]+
ERROR_VALUE_LITERAL=[^0-9 \r\n]+
NEWLINE_LITERAL=[\r]?[\n]
SPACE_LITERAL=[ ]+
ERROR_SPACE_LITERAL=[\t]

%%
<YYINITIAL> {
  {ERROR_SPACE_LITERAL}      { return ATT_ERROR_SPACE_LITERAL; }
  {ERROR_VALUE_LITERAL}      { return ATT_ERROR_VALUE_LITERAL; }
  {INT_LITERAL}              { return ATT_INT_LITERAL; }
  {NEWLINE_LITERAL}          { return ATT_NEWLINE_LITERAL; }
  {SPACE_LITERAL}            { return ATT_SPACE_LITERAL; }
}

[^] { return ATT_ERROR_VALUE_LITERAL; }
