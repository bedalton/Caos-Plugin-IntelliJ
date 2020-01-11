package com.openc2e.plugins.intellij.caos.lang;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public interface CaosScriptIcons {
    Icon SDK_ICON = IconLoader.getIcon("/icons/SdkIcon.png");
    Icon CAOS_FILE_ICON = IconLoader.getIcon("/icons/caosScriptFileIcon.png");
    Icon CAOS_DEF_FILE_ICON = IconLoader.getIcon("/icons/caosDefFileIcon");
    Icon COMMAND = IconLoader.getIcon("/icons/command.svg");
    Icon VARIABLE = IconLoader.getIcon("/icons/variable.svg");
}
