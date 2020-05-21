package com.openc2e.plugins.intellij.caos.def.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import icons.CaosScriptIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CaosDefFileType extends LanguageFileType {

    @NonNls
    public static final CaosDefFileType INSTANCE = new CaosDefFileType();
    @NonNls
    public static final String DEFAULT_EXTENSION = "caosdef";
    @NonNls
    public static final String DOT_DEFAULT_EXTENSION = "."+DEFAULT_EXTENSION;

    private CaosDefFileType() {
        super(CaosDefLanguage.Companion.getInstance());
    }

    @NotNull
    @Override
    public String getName() {
        return "Caos Definitions File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "A Caos definitions file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return CaosScriptIcons.CAOS_DEF_FILE_ICON;
    }
}
