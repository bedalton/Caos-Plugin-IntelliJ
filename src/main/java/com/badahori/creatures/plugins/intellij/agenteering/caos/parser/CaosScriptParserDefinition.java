package com.badahori.creatures.plugins.intellij.agenteering.caos.parser;

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile;
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptLexerAdapter;
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes;
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets;
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubTypes;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

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
    public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        if (WHITE_SPACES.contains(astNode.getElementType()))
            return SpaceRequirements.MAY;
        return SpaceRequirements.MUST_NOT;
    }
}
