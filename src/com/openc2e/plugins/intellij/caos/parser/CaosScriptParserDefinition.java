package com.openc2e.plugins.intellij.caos.parser;

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
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile;
import com.openc2e.plugins.intellij.caos.def.lexer.CaosDefLexerAdapter;
import com.openc2e.plugins.intellij.caos.def.lexer.CaosDefTypes;
import com.openc2e.plugins.intellij.caos.def.parser.CaosDefParser;
import com.openc2e.plugins.intellij.caos.def.psi.types.CaosDefTokenSets;
import com.openc2e.plugins.intellij.caos.def.stubs.types.CaosDefStubTypes;
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets;
import org.jetbrains.annotations.NotNull;

public class CaosScriptParserDefinition implements ParserDefinition {

    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    private static final TokenSet COMMENTS = CaosScriptTokenSets.getCOMMENTS();
    private static final TokenSet STRINGS = CaosScriptTokenSets.getSTRING_LIKE();

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new CaosDefLexerAdapter();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new CaosDefParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return CaosDefStubTypes.getFILE();
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
        return CaosDefTypes.Factory.createElement(astNode);
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new CaosDefFile(fileViewProvider);
    }

    @SuppressWarnings("deprecation")
    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        if (WHITE_SPACES.contains(astNode.getElementType()))
            return SpaceRequirements.MAY;
        return SpaceRequirements.MUST_NOT;
    }
}
