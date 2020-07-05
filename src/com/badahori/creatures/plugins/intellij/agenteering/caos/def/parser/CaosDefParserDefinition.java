package com.badahori.creatures.plugins.intellij.agenteering.caos.def.parser;

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile;
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefLexerAdapter;
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
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile;
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefLexerAdapter;
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes;
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.types.CaosDefTokenSets;
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubTypes;
import org.jetbrains.annotations.NotNull;

public class CaosDefParserDefinition implements ParserDefinition {

    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE, CaosDefTypes.CaosDef_LEADING_ASTRISK);
    private static final TokenSet COMMENTS = CaosDefTokenSets.getCOMMENTS();
    private static final TokenSet STRINGS = TokenSet.create();

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
        return SpaceRequirements.MAY;
    }
}
