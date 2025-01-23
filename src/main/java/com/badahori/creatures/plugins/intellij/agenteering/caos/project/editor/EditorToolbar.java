package com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

public class EditorToolbar {
    private JComboBox<String> variant;
    private JPanel panel;
    private JButton docsButton;
    private JButton injectButton;
    JButton compileButton;
    JButton compilerOptionsButton;
    JPanel compilerSettings;

    public JPanel getPanel() {
        return panel;
    }

    public void setVariantIsVisible(boolean visible) {
        variant.setVisible(visible);
    }

    public void addVariantListener(ItemListener itemListener) {
        variant.addItemListener(itemListener);
    }

    public void addDocsButtonClickListener(ActionListener actionListener) {
        docsButton.addActionListener(actionListener);
    }

    public void addInjectionHandler(ActionListener listener) {
        injectButton.addActionListener(listener);
    }

    public void showInjectionButton(boolean show) {
        injectButton.setVisible(show);
    }

    public void removeInjectionHandler(ActionListener listener) {
        injectButton.removeActionListener(listener);
    }

    public void setInjectButtonEnabled(boolean enable) {
        injectButton.setEnabled(enable);
    }

    public void setDocsButtonEnabled(boolean enabled) {
        docsButton.setEnabled(enabled);
    }

    public void selectVariant(String variantString) {
        variant.setSelectedItem(variantString);
    }

    public void selectVariant(int variantIndex) {
        variant.setSelectedIndex(variantIndex);
    }

    public String getSelectedVariant() {
        return (String) variant.getSelectedItem();
    }
}
