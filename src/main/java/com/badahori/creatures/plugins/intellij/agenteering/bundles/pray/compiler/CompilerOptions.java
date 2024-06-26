package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.compiler;

import com.bedalton.creatures.agents.pray.compiler.PrayCompileOptions;
import com.bedalton.creatures.agents.pray.compiler.PrayCompileOptionsImpl;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.ResourceBundle;

public class CompilerOptions {
    JPanel panel1;
    JCheckBox mergeScriptsCheckBox;
    JCheckBox mergeRSCRCheckBox;
    JCheckBox autogenerateEventScriptRemoversCheckBox;
    JCheckBox autogenerateAgentRemoversCheckBox;
    JCheckBox validatePRAYFileCheckBox;
    private JComboBox compressionLevel;
    JCheckBox addLinkFilenameReference;

    public JPanel getComponent() {
        return panel1;
    }

    CompilerOptions(
            @Nullable final PrayCompileOptions options) {
        $$$setupUI$$$();
        setCompilerOptions(options);
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
                validatePRAYFileCheckBox.isSelected()
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
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FormLayout("fill:d:grow", "center:max(d;4px):noGrow,top:3dlu:noGrow,center:d:noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow"));
        panel1.add(panel2, BorderLayout.CENTER);
        mergeScriptsCheckBox = new JCheckBox();
        mergeScriptsCheckBox.setSelected(false);
        this.$$$loadButtonText$$$(mergeScriptsCheckBox, this.$$$getMessageFromBundle$$$("com/badahori/creatures/plugins/intellij/caos-bundle", "pray.compiler.options.merge-script"));
        mergeScriptsCheckBox.setToolTipText(this.$$$getMessageFromBundle$$$("com/badahori/creatures/plugins/intellij/caos-bundle", "pray.compiler.options.merge-scripts.details"));
        CellConstraints cc = new CellConstraints();
        panel2.add(mergeScriptsCheckBox, cc.xy(1, 3));
        mergeRSCRCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(mergeRSCRCheckBox, this.$$$getMessageFromBundle$$$("com/badahori/creatures/plugins/intellij/caos-bundle", "pray.compiler.options.merger-rscr"));
        mergeRSCRCheckBox.setToolTipText(this.$$$getMessageFromBundle$$$("com/badahori/creatures/plugins/intellij/caos-bundle", "pray.compiler.options.merge-rscr.details"));
        panel2.add(mergeRSCRCheckBox, cc.xy(1, 5));
        autogenerateEventScriptRemoversCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(autogenerateEventScriptRemoversCheckBox, this.$$$getMessageFromBundle$$$("com/badahori/creatures/plugins/intellij/caos-bundle", "pray.compiler.options.autogen-script-removers"));
        autogenerateEventScriptRemoversCheckBox.setToolTipText(this.$$$getMessageFromBundle$$$("com/badahori/creatures/plugins/intellij/caos-bundle", "pray.compiler.options.autogen-event-scripts-removers"));
        panel2.add(autogenerateEventScriptRemoversCheckBox, cc.xy(1, 7));
        autogenerateAgentRemoversCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(autogenerateAgentRemoversCheckBox, this.$$$getMessageFromBundle$$$("com/badahori/creatures/plugins/intellij/caos-bundle", "pray.compiler.options.autogen-agent-removers"));
        autogenerateAgentRemoversCheckBox.setToolTipText(this.$$$getMessageFromBundle$$$("com/badahori/creatures/plugins/intellij/caos-bundle", "pray.compiler.options.autogen-agent-removers.details"));
        panel2.add(autogenerateAgentRemoversCheckBox, cc.xy(1, 9));
        validatePRAYFileCheckBox = new JCheckBox();
        validatePRAYFileCheckBox.setSelected(true);
        this.$$$loadButtonText$$$(validatePRAYFileCheckBox, this.$$$getMessageFromBundle$$$("com/badahori/creatures/plugins/intellij/caos-bundle", "pray.compiler.options.validate"));
        panel2.add(validatePRAYFileCheckBox, cc.xy(1, 1));
        final JLabel label1 = new JLabel();
        Font label1Font = this.getFontLocal(null, -1, 18, label1.getFont());
        if (label1Font != null) {
            label1.setFont(label1Font);
        }
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("com/badahori/creatures/plugins/intellij/caos-bundle", "pray.compiler.options"));
        panel1.add(label1, BorderLayout.NORTH);
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
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
