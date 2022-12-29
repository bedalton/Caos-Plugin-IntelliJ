package com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.types

import com.intellij.psi.tree.TokenSet
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer.CatalogueTypes.*
import com.intellij.psi.TokenType

object CatalogueTokenSets {
    @JvmStatic
    val COMMENTS = TokenSet.create(
        CATALOGUE_COMMENT_LITERAL
    )

    @JvmStatic
    val WHITE_SPACE = TokenSet.create(
        TokenType.WHITE_SPACE
    )

    @JvmStatic
    val KEYWORDS = TokenSet.create(
        CATALOGUE_TAG_KW,
        CATALOGUE_ARRAY_KW
    )

    @JvmStatic
    val MODIFIERS = TokenSet.create(
        CATALOGUE_OVERRIDE_KW
    )

    @JvmStatic
    val STRINGS = TokenSet.create(
        CATALOGUE_STRING_LITERAL,
        CATALOGUE_INVALID_STRING_LITERAL,
        CATALOGUE_INT_STRING_LITERAL
    )
}