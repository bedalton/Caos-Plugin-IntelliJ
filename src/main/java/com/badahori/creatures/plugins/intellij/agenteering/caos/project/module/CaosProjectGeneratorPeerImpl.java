package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module;

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.*;

public class CaosProjectGeneratorPeerImpl {
    private JPanel panel1;
    private JComboBox<CaosVariant> variantComboBox;

    public CaosVariant getSelectedVariant() {
        return (CaosVariant) variantComboBox.getSelectedItem();
    }

    public JComboBox<CaosVariant> getVariantComboBox() {
        return variantComboBox;
    }

    public void setSelectedVariant(CaosVariant variant) {
        variantComboBox.setSelectedItem(variant);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        final JLabel label1 = new JLabel();
        label1.setText("CAOS Variant");
        panel1.add(label1);
        panel1.add(variantComboBox);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
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
