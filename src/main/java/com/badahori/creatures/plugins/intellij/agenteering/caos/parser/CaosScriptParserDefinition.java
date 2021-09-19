package com.badahori.creatures.plugins.intellij.agenteering.caos.parser;

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile;
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptLexerAdapter;
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes;
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets;
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubTypes;
import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosScriptTreeUtilKt;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import static com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.*;

public class CaosScriptParserDefinition implements ParserDefinition {

    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE, CaosScriptTypes.CaosScript_COMMA, CaosScriptTypes.CaosScript_NEWLINE);
    private static final TokenSet COMMENTS = TokenSet.create(CaosScriptTypes.CaosScript_COMMENT_START, CaosScriptTypes.CaosScript_COMMENT_BODY_LITERAL);//, CaosScriptTypes.CaosScript_AT_DIRECTIVE_COMMENT, CaosScriptTypes.CaosScript_AT_DIRECTIVE_COMMENT_LITERAL, CaosScriptTypes.CaosScript_AT_DIRECTIVE_COMMENT_START);
    private static final TokenSet STRINGS = CaosScriptTokenSets.getSTRING_LIKE();

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new CaosScriptLexerAdapter();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new CaosScriptParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return CaosScriptStubTypes.Companion.getFILE();
    }

    @NotNull
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return STRINGS;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode astNode) {
        return CaosScriptTypes.Factory.createElement(astNode);
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        assert (!fileViewProvider.getVirtualFile().getName().endsWith("cob")) : "CaosScriptParserDefinition called, but COB file type was not properly set";
        return new CaosScriptFile(fileViewProvider, fileViewProvider.getVirtualFile());
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return spaceExistenceTypeBetweenTokens(left, right);
    }

    @Override
    public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        if (WHITE_SPACES.contains(astNode.getElementType()))
            return SpaceRequirements.MAY;
        final IElementType t1 = astNode.getElementType();
        final IElementType t2 = astNode1.getElementType();
        if (astNode.getTextLength() == 1) {
            if (t1 == CaosScriptTypes.CaosScript_COMMENT_START)
                return SpaceRequirements.MAY;
            if (t1 == CaosScript_DOUBLE_QUOTE || t1 == CaosScript_SINGLE_QUOTE) {
                if (t2 == CaosScript_CHAR_CHAR || t2 == CaosScript_STRING_TEXT || t2 == CaosScript_STRING_CHAR || t2 == CaosScript_STRING_ESCAPE_CHAR) {
                    return SpaceRequirements.MAY;
                }
            }
        }
        if (astNode1.getTextLength() == 1) {
            if (t2 == CaosScript_DOUBLE_QUOTE || t2 == CaosScript_SINGLE_QUOTE) {
                if (t1 == CaosScript_CHAR_CHAR || t1 == CaosScript_STRING_TEXT || t1 == CaosScript_STRING_CHAR || t1 == CaosScript_STRING_ESCAPE_CHAR) {
                    return SpaceRequirements.MAY;
                }
            }
        }
        CaosScriptTreeUtilKt.getLOGGER().info("Add Space: " + astNode.getElementType() + "<>" + astNode1.getElementType() );
        return SpaceRequirements.MUST;
    }

    private boolean is(final ASTNode node) {
        if (node.getTextLength() != 1)
            return false;
        final char first = node.getText().charAt(0);
        return first == '\'' ||
                first == '"' ||
                first == '*';
    }
}
