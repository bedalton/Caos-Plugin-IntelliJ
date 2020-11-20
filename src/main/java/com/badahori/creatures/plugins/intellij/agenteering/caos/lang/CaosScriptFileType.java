package com.badahori.creatures.plugins.intellij.agenteering.caos.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import icons.CaosScriptIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CaosScriptFileType extends LanguageFileType {

    @NonNls
    public static final CaosScriptFileType INSTANCE = new CaosScriptFileType();

    @NonNls
    public static final String DEFAULT_EXTENSION = "cos";

    private CaosScriptFileType() {
        super(CaosScriptLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Caos Script";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "A creatures caos script";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return CaosScriptIcons.CAOS_FILE_ICON;
    }

    @Override
    public String toString() {
        return super.toString();
    }


}
