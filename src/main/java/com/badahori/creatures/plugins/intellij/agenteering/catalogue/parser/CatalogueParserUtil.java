package com.badahori.creatures.plugins.intellij.agenteering.catalogue.parser;

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer.CatalogueTypes;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.util.Key;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import gnu.trove.TObjectLongHashMap;

public class CatalogueParserUtil extends GeneratedParserUtilBase {

    private static final Key<TObjectLongHashMap<String>> MODES_KEY = Key.create("MODES_KEY");

    private static TObjectLongHashMap<String> getParsingModes(PsiBuilder builder_) {
        TObjectLongHashMap<String> flags = builder_.getUserData(MODES_KEY);
        if (flags == null) {
            builder_.putUserData(MODES_KEY, flags = new TObjectLongHashMap<>());
        }
        return flags;
    }

    public static boolean enterMode(PsiBuilder builder_,
                                    @SuppressWarnings("UnusedParameters")
                                            int level, String mode) {
        TObjectLongHashMap<String> flags = getParsingModes(builder_);
        if (! flags.increment(mode)) {
            flags.put(mode, 1);
        }
        return true;
    }

    public static boolean exitMode(PsiBuilder builder_,
                                   @SuppressWarnings("UnusedParameters")
                                           int level, String mode) {
        TObjectLongHashMap<String> flags = getParsingModes(builder_);
        long count = flags.get(mode);
        if (count == 1) {
            flags.remove(mode);
        } else if (count > 1) {
            flags.put(mode, count - 1);
        } else {
            builder_.error("Could not exit inactive '" + mode + "' mode at offset " + builder_.getCurrentOffset());
        }
        return true;
    }

    public static boolean inMode(PsiBuilder builder_,
                                 @SuppressWarnings("UnusedParameters")
                                         int level, String mode) {
        return getParsingModes(builder_).get(mode) > 0;
    }

    public static boolean notInMode(PsiBuilder builder_,
                                    @SuppressWarnings("UnusedParameters")
                                            int level, String mode) {
        return getParsingModes(builder_).get(mode) == 0;
    }

    public static boolean eol(PsiBuilder builder_, int level) {
        final String text = builder_.getTokenText();
        return (text != null && text.contains("\n")) || eof(builder_, level);
    }

    public static boolean eos(PsiBuilder builder_, int level)
    {
        int index = 1;
        while (builder_.lookAhead(index) == TokenType.WHITE_SPACE)
            index++;
        IElementType next = builder_.lookAhead(index);
        return next == CatalogueTypes.CATALOGUE_ARRAY ||
                next == CatalogueTypes.CATALOGUE_TAG ||
                next == CatalogueTypes.CATALOGUE_NEWLINE;
    }
}
