package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.parser;

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile;
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lexer.PrayTypes;
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.lexer.PrayLexerAdapter;
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayFileStubType;
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

public class PrayParserDefinition implements ParserDefinition {

    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    private static final TokenSet COMMENTS = TokenSet.create(PrayTypes.Pray_BLOCK_COMMENT, PrayTypes.Pray_COMMENT, PrayTypes.Pray_LINE_COMMENT);
    private static final TokenSet STRINGS = TokenSet.create(PrayTypes.Pray_SINGLE_QUO_STRING, PrayTypes.Pray_DOUBLE_QUO_STRING, PrayTypes.Pray_STRING);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new PrayLexerAdapter();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new PrayParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return PrayFileStubType.INSTANCE;
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
        return PrayTypes.Factory.createElement(astNode);
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new PrayFile(fileViewProvider);
    }

    @Override
    public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        if (WHITE_SPACES.contains(astNode.getElementType()))
            return SpaceRequirements.MAY;
        return SpaceRequirements.MUST_NOT;
    }
}
