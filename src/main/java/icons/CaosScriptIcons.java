package icons;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface CaosScriptIcons {

    // IntelliJ Icons
    Icon SDK_ICON = getIconUsingClassLoader("/icons/sdk-icon.svg");
    Icon MODULE_ICON = getIconUsingClassLoader("/icons/sdk-icon.svg");

    // CAOS Icons
    Icon CAOS_FILE_ICON = getIconUsingClassLoader("/icons/caos-script-file-icon.svg");
    Icon CAOS_DEF_FILE_ICON = getIconUsingClassLoader("/icons/caos-def-file-icon.svg");

    // ATT Icons
    Icon ATT_FILE_ICON = getIconUsingClassLoader("/icons/att-file-icon.svg");
    Icon ATT_GROUP = getIconUsingClassLoader("/icons/att-group-icon.svg");

    // Misc Files
    Icon CATALOGUE_FILE_ICON = getIconUsingClassLoader("/icons/catalogue-file-icon.svg");
    Icon SFC_FILE_ICON = getIconUsingClassLoader("/icons/sfc-file-icon.svg");

    // Agent Icons
    Icon PRAY_FILE_ICON = getIconUsingClassLoader("/icons/pray-file-icon.svg");
    Icon PRAY_AGENT_ICON = getIconUsingClassLoader("/icons/agent-icon.svg");

    // Agent Compile
    Icon COMPILE = getIconUsingClassLoader("/icons/compile.svg");
    Icon BUILD = getIconUsingClassLoader("/icons/compile.svg");

    // COB
    Icon COB_FILE_ICON = getIconUsingClassLoader("/icons/c1-cob-file-icon.svg");
    Icon C1_COB_FILE_ICON = getIconUsingClassLoader("/icons/c1-cob-file-icon.svg");
    Icon C1_COB_AGENT_ICON = getIconUsingClassLoader("/icons/c1-cob-agent.svg");
    Icon C2_COB_FILE_ICON = getIconUsingClassLoader("/icons/c2-cob-file-icon.svg");
    Icon C2_COB_AGENT_ICON = getIconUsingClassLoader("/icons/c2-cob-agent.svg");
    Icon RCB_FILE_ICON = getIconUsingClassLoader("/icons/rcb-file-icon.svg");

    // Sprite Files
    Icon S16_FILE_ICON = getIconUsingClassLoader("/icons/s16-file-icon.svg");
    Icon C16_FILE_ICON = getIconUsingClassLoader("/icons/c16-file-icon.svg");
    Icon SPR_FILE_ICON = getIconUsingClassLoader("/icons/spr-file-icon.svg");
    Icon PHOTO_ALBUM_FILE_ICON = getIconUsingClassLoader("/icons/photo-album-file-icon.svg");
    Icon AGENT_FILE_ICON = getIconUsingClassLoader("/icons/agent-file-icon.svg");
    Icon BLK_FILE_ICON = getIconUsingClassLoader("/icons/blk-file-icon.svg");
    Icon IMAGE = getIconUsingClassLoader("/icons/ImagesFileType.svg");

    // Sound Files
    Icon WAV_FILE_ICON = getIconUsingClassLoader("/icons/wav-file-icon.svg");
    Icon MNG_FILE_ICON = getIconUsingClassLoader("/icons/mng-file-icon.svg");

    // Element Icons
    Icon COMMAND = getIconUsingClassLoader("/icons/command-icon.png");
    Icon RVALUE = getIconUsingClassLoader("/icons/right-value-icon.png");
    Icon LVALUE = getIconUsingClassLoader("/icons/left-value-icon.png");
    Icon VALUE_LIST_VALUE = getIconUsingClassLoader("/icons/values-list-value-icon.png");
    Icon VALUES_LIST = getIconUsingClassLoader("/icons/values-list-icon.png");
    Icon HASHTAG = getIconUsingClassLoader("/icons/hashtag-icon.png");

    // Variant Icons
    Icon C1 = getIconUsingClassLoader("/icons/C1.svg");
    Icon C2 = getIconUsingClassLoader("/icons/C2.svg");
    Icon CV = getIconUsingClassLoader("/icons/CV.svg");
    Icon C3 = getIconUsingClassLoader("/icons/C3.svg");
    Icon DS = getIconUsingClassLoader("/icons/DS.svg");
    Icon SM = getIconUsingClassLoader("/icons/SM.svg");

    // Scripts
    Icon JECT = getIconUsingClassLoader("/icons/sdk-icon.svg");
    Icon MACRO = getIconUsingClassLoader("/icons/macro-icon.png");
    Icon INSTALL_SCRIPT = getIconUsingClassLoader("/icons/install-script-icon.png");
    Icon REMOVAL_SCRIPT = getIconUsingClassLoader("/icons/removal-script-icon.png");
    Icon EVENT_SCRIPT = getIconUsingClassLoader("/icons/event-script-icon.png");
    Icon SUBROUTINE = getIconUsingClassLoader("/icons/subroutine-icon.png");

    private static Icon getIconUsingClassLoader(final String filename) {
        return IconLoader.getIcon(filename, CaosScriptIcons.class.getClassLoader());
    }
}
