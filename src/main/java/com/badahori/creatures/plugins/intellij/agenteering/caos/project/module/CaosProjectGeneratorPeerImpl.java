package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module;

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;

public class CaosProjectGeneratorPeerImpl {
    private JPanel mainPanel;
    private JComboBox<CaosVariant> variantComboBox;
    private JTextArea ignoredFileName;
    private JTextArea injectorGameNames;

    public JComponent getComponent() {
        return mainPanel;
    }

    public CaosVariant getSelectedVariant() {
        return (CaosVariant) variantComboBox.getSelectedItem();
    }

    public JComboBox<CaosVariant> getVariantComboBox() {
        return variantComboBox;
    }

    public void setSelectedVariant(CaosVariant variant) {
        variantComboBox.setSelectedItem(variant);
    }

    public void setIgnoredFileNames(final List<String> filenames) {
        set(ignoredFileName, filenames);
    }

    public String[] getIgnoredFileNames() {
        return get(ignoredFileName);
    }

    public void setInjectorGameNames(final List<String> items) {
        set(injectorGameNames, items);
    }

    public String[] getInjectorGameNames() {
        return get(injectorGameNames);
    }

    public void set(final JTextComponent field, final List<String> items) {
        final StringBuilder builder = new StringBuilder();
        for (String item : items) {
            builder.append(item).append("\n");
        }
        field.setText(builder.toString());
    }

    public String[] get(final JTextComponent field) {
        return field.getText().trim().split("\n");
    }

    private void createUIComponents() {
        variantComboBox = new ComboBox<>(new CaosVariant[]{
                CaosVariant.C1.INSTANCE,
                CaosVariant.C2.INSTANCE,
                CaosVariant.CV.INSTANCE,
                CaosVariant.C3.INSTANCE,
                CaosVariant.DS.INSTANCE,
                CaosVariant.SM.INSTANCE
        });
        variantComboBox.setToolTipText("CAOS Variant");
        variantComboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            if (value == null) {
                return new JLabel("UNKNOWN");
            } else {
                return new JLabel(value.getFullName());
            }
        });
    }

}
