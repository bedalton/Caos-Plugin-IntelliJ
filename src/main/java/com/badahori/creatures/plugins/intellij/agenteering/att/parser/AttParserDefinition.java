package com.badahori.creatures.plugins.intellij.agenteering.att.parser;

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFile;
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFileElementType;
import com.badahori.creatures.plugins.intellij.agenteering.att.lexer.AttLexerAdapter;
import com.badahori.creatures.plugins.intellij.agenteering.att.lexer.AttTypes;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class AttParserDefinition implements ParserDefinition {

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new AttLexerAdapter();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new AttParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return AttFileElementType.INSTANCE;
    }

    @NotNull
    public TokenSet getWhitespaceTokens() {
        return TokenSet.EMPTY;
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
        return AttTypes.Factory.createElement(astNode);
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new AttFile(fileViewProvider);
    }

    @SuppressWarnings("deprecation")
    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        return SpaceRequirements.MUST_NOT;
    }
}
