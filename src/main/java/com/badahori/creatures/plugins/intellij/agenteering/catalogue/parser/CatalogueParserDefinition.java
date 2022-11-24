package com.badahori.creatures.plugins.intellij.agenteering.catalogue.parser;

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

public class CatalogueParserDefinition implements ParserDefinition {

    private static final TokenSet WHITESPACE = TokenSet.create(
            TokenType.WHITE_SPACE,
            CatalogueType.CATALOGUE_ERROR_SPACE_LITERAL,
            CatalogueType.CATALOGUE_SPACE_LITERAL
    );

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new CatalogueLexerAdapter();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new CatalogueParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return CatalogueFileElementType.INSTANCE;
    }

    @NotNull
    public TokenSet getWhitespaceTokens() {
        return WHITESPACE;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode astNode) {
        return CatalogueTypes.Factory.createElement(astNode);
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new CatalogueFile(fileViewProvider);
    }
}
