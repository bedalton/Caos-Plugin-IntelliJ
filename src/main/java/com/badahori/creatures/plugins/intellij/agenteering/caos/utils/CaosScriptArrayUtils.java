package com.badahori.creatures.plugins.intellij.agenteering.caos.utils;

import java.util.ArrayList;
import java.util.List;

public class CaosScriptArrayUtils {

    public static List<Character> toList(final char[] chars) {
        final List<Character> list = new ArrayList<>();
        for(char c : chars) {
            list.add(c);
        }
        return list;
    }

}
