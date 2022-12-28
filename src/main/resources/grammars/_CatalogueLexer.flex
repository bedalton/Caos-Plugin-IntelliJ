package grammars;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer.CatalogueTypes.*;

%%

%{
  public _CatalogueLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _CatalogueLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R

INT_STRING_LITERAL=\"[+-]?([0-9]|[0-9]*\.[0-9]+)+\"
STRING_LITERAL=\"([^\"\\\n]|\\.)*\"
INVALID_STRING_LITERAL=\"([^\"\n\\])*|'([^'\n\\]|\\\')*'?
NEWLINE_LITERAL=\r?\n
COMMENT=#[^\n]*
TAG=[Tt][Aa][Gg]
ARRAY=[Aa][Rr]{2}[Aa][Yy]
OVERRIDE=[Oo][Vv][Ee][Rr][Rr][Ii][Dd][Ee]
INT=[0-9]+
ERROR_CHAR=[^ \t\r\n]
SPACE=[ \t]
ID=\S+
%state LINE_START
%%
<YYINITIAL> {

  {INT_STRING_LITERAL}          { return CATALOGUE_INT_STRING_LITERAL; }
  {STRING_LITERAL}              { return CATALOGUE_STRING_LITERAL; }
  {INVALID_STRING_LITERAL}      { return CATALOGUE_INVALID_STRING_LITERAL; }
  {NEWLINE_LITERAL}             { return CATALOGUE_NEWLINE_LITERAL; }
  {COMMENT}                     { return CATALOGUE_COMMENT_LITERAL; }
  {TAG}                         { return CATALOGUE_TAG_KW; }
  {ARRAY}                       { return CATALOGUE_ARRAY_KW; }
  {OVERRIDE}                    { return CATALOGUE_OVERRIDE_KW; }
  {INT}                         { return CATALOGUE_INT; }
  {ID}                          { return CATALOGUE_WORD; }
  {ERROR_CHAR}                  { return CATALOGUE_ERROR_CHAR; }
  {SPACE}                       { return WHITE_SPACE; }
}

[^] { return BAD_CHARACTER; }
