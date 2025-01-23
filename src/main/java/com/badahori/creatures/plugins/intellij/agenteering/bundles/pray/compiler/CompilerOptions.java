package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.compiler;

import com.bedalton.creatures.agents.pray.compiler.PrayCompileOptions;
import com.bedalton.creatures.agents.pray.compiler.PrayCompileOptionsImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Locale;

public class CompilerOptions {
    JPanel panel1;
    JCheckBox mergeScriptsCheckBox;
    JCheckBox mergeRSCRCheckBox;
    JCheckBox autogenerateEventScriptRemoversCheckBox;
    JCheckBox autogenerateAgentRemoversCheckBox;
    JCheckBox validatePRAYFileCheckBox;
    private JComboBox compressionLevel;
    JCheckBox addLinkFilenameReference;
    private PrayCompileOptions prayCompileOptionsTemp;

    public JPanel getComponent() {
        final PrayCompileOptions compileOptions = prayCompileOptionsTemp;
        prayCompileOptionsTemp = null;
        if (compileOptions != null) {
            setCompilerOptions(compileOptions);
        }
        return panel1;
    }

    CompilerOptions(
            @Nullable final PrayCompileOptions options) {
        prayCompileOptionsTemp = options;
    }

    void setCompilerOptions(
            @Nullable final PrayCompileOptions options) {
        if (options == null) {
            return;
        }
        mergeRSCRCheckBox.setSelected(options.getMergeRscr());
        mergeScriptsCheckBox.setSelected(options.getMergeScripts());
        autogenerateAgentRemoversCheckBox.setSelected(options.getGenerateAgentRemovers());
        autogenerateEventScriptRemoversCheckBox.setSelected(options.getGenerateScriptRemovers());
        validatePRAYFileCheckBox.setSelected(options.getValidate());
    }

    public PrayCompileOptionsImpl getOptions() {
        return new PrayCompileOptionsImpl(
                mergeScriptsCheckBox.isSelected(),
                mergeRSCRCheckBox.isSelected(),
                autogenerateEventScriptRemoversCheckBox.isSelected(),
                autogenerateAgentRemoversCheckBox.isSelected(),
                validatePRAYFileCheckBox.isSelected(),
                new String[0],
                false,
                getCompressionLevel(),
                null,
                addLinkFilenameReference.isSelected(),
                false
        );
    }

    private Integer getCompressionLevel() {
        final String levelText = (String) compressionLevel.getSelectedItem();
        if (levelText == null) {
            return null;
        }
        final char levelChar = levelText.charAt(0);
        if (levelChar < '0' || levelChar > '9') {
            return null;
        }
        return levelChar - '0';
    }

    /**
     * @noinspection ALL
     */
    private static Font getFontLocal(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) {
            return null;
        }
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

}
