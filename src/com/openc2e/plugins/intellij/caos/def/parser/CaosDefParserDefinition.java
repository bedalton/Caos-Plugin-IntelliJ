package com.openc2e.plugins.intellij.caos.def.parser;

import brightscript.intellij.lexer.BrsLexerAdapter;
import brightscript.intellij.lang.BrsFile;
import brightscript.intellij.psi.types.BrsTokenSets;
import brightscript.intellij.psi.types.BrsTypes;
import brightscript.intellij.stubs.types.BrsStubTypes;
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

public class CaosDefParserDefinition implements ParserDefinition {

    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    private static final TokenSet COMMENTS = BrsTokenSets.INSTANCE.getCOMMENTS();
    private static final TokenSet STRINGS = TokenSet.create(BrsTypes.BRS_STRING);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new BrsLexerAdapter();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new BrsParser(false);
    }

    @Override
    public IFileElementType getFileNodeType() {
        return BrsStubTypes.FILE;
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
        return BrsTypes.Factory.createElement(astNode);
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new BrsFile(fileViewProvider);
    }

    @SuppressWarnings("deprecation")
    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        return SpaceRequirements.MAY;
    }
}
