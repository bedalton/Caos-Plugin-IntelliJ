package com.openc2e.plugins.intellij.caos.sprites.spr;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.List;

public class SprFileEditor {
    private JPanel main;
    private JList<Image> imageList;
    private JComboBox backgroundColor;
    private final VirtualFile virtualFile;
    private final List<Image> images;
    public static final String TRANSPARENT = "Transparent";
    public static final String BLACK = "Black";
    public static final String LIGHT_GREY = "Light Grey";
    public static final String DARK_GREY = "Dark Grey";
    public static final String WHITE = "White";
    public static final String RED = "Red";
    public static final String BLUE = "Blue";
    public static final String GREEN = "Green";


    SprFileEditor(VirtualFile file) {
        this.virtualFile = file;
        this.images = SprParser.INSTANCE.parseSprite$CaosPlugin(file);
        $$$setupUI$$$();
        backgroundColor.addItemListener((itemEvent) -> {
            final String color = (String) itemEvent.getItem();
            switch (color) {
                case TRANSPARENT:
                    SpriteCellRenderer.INSTANCE.setColor(new Color(0, 0, 0, 0));
                    break;
                case BLACK:
                    SpriteCellRenderer.INSTANCE.setColor(JBColor.BLACK);
                    break;
                case DARK_GREY:
                    SpriteCellRenderer.INSTANCE.setColor(JBColor.DARK_GRAY);
                    break;
                case WHITE:
                    SpriteCellRenderer.INSTANCE.setColor(JBColor.WHITE);
                    break;

                case LIGHT_GREY:
                    SpriteCellRenderer.INSTANCE.setColor(JBColor.LIGHT_GRAY);
                    break;
                case RED:
                    SpriteCellRenderer.INSTANCE.setColor(JBColor.RED);
                    break;
                case BLUE:
                    SpriteCellRenderer.INSTANCE.setColor(JBColor.BLUE);
                    break;
                case GREEN:
                    SpriteCellRenderer.INSTANCE.setColor(JBColor.GREEN);
                    break;
            }
            imageList.updateUI();
        });
        imageList.updateUI();
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
        main = new JPanel();
        main.setLayout(new BorderLayout(10, 0));
        final JScrollPane scrollPane1 = new JScrollPane();
        main.add(scrollPane1, BorderLayout.CENTER);
        scrollPane1.setViewportView(imageList);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        main.add(panel1, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setText("Background Color");
        panel1.add(label1);
        panel1.add(backgroundColor);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return main;
    }

    private void createUIComponents() {
        imageList = new JBList<>(images);
        imageList.setCellRenderer(SpriteCellRenderer.INSTANCE);
        backgroundColor = new ComboBox<>(new String[]{
                TRANSPARENT,
                BLACK,
                WHITE,
                DARK_GREY,
                LIGHT_GREY,
                RED,
                GREEN,
                BLUE
        });

    }
}
