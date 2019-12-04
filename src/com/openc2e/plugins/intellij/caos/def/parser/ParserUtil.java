package com.openc2e.plugins.intellij.caos.def.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import org.jetbrains.annotations.NotNull;

public class ParserUtil extends GeneratedParserUtilBase {

    public static boolean isDouble(@NotNull
                                           PsiBuilder builder) {
        final String tokenText = builder.getTokenText();
        if (tokenText == null) {
            return false;
        }
        final String onlyDigits = tokenText.replaceAll("[^\\d]", "");
        return onlyDigits.length() >= 10;
    }

}
