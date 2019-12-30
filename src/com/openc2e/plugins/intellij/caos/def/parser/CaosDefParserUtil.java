package com.openc2e.plugins.intellij.caos.def.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.util.Key;
import com.intellij.psi.TokenType;
import com.openc2e.plugins.intellij.caos.def.lexer.CaosDefTypes;
import gnu.trove.TObjectLongHashMap;

public class CaosDefParserUtil extends GeneratedParserUtilBase {

    static final String LOOP = "loop";
    static final String INTERFACE = "interface";

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
        return builder_.lookAhead(index) == CaosDefTypes.CaosDef_DOC_COMMENT_OPEN ||
                builder_.lookAhead(index) == CaosDefTypes.CaosDef_SEMI ||
                eof(builder_, level);
    }
}
