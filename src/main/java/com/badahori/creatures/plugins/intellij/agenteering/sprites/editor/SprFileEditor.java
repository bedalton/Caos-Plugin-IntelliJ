package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor;

import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteFileHolder;
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.apache.commons.compress.utils.Lists;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@SuppressWarnings("UseJBColor")
public class SprFileEditor {
    private JPanel main;
    private JList<ImageTransferItem> imageList;
    private JComboBox<String> backgroundColor;
    private JComboBox<String> scale;
    private JScrollPane scrollPane;
    private JButton reloadSpriteButton;
    private final List<ImageTransferItem> images;
    public static final String TRANSPARENT = "Transparent";
    public static final String BLACK = "Black";
    public static final String LIGHT_GREY = "Light Grey";
    public static final String DARK_GREY = "Dark Grey";
    public static final String WHITE = "White";
    public static final String RED = "Red";
    public static final String BLUE = "Blue";
    public static final String GREEN = "Green";
    private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
    private final SpriteCellRenderer cellRenderer = new SpriteCellRenderer();
    private final VirtualFile file;
    private boolean didInit = false;
    private JLabel loadingLabel;
    private final Logger LOGGER = Logger.getLogger("#SprFileEditor");

    /**
     * Basic constructor
     *
     * @param file sprite file to construct panel for
     */
    SprFileEditor(VirtualFile file) {
        this.file = file;
        images = new ArrayList<>();
        $$$setupUI$$$();
        init();
    }

    synchronized void clearInit() {
        didInit = false;
    }

    synchronized void init() {
        if (didInit) {
            return;
        }
        didInit = true;
        initPlaceholder();
        // Load sprite images
        // Executed on background thread
        loadSprite();
        // Initialize the UI controls
        initUI();
    }

    JComponent getComponent() {
        init();
        return main;
    }

    /**
     * Loads sprite images into view
     * Executed on background thread
     */
    private void loadSprite() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<BufferedImage> rawImages;

            // Initialize sprites
            try {
                rawImages = SpriteEditorImpl.fromCacheAsAwt(file);
                if (rawImages == null) {
                    final SpriteFileHolder holder = SpriteParser.parse(file, (i, total) -> {
                        final double progress = Math.ceil((i * 100.0) / total);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (loadingLabel != null) {
                                loadingLabel.setText("Loading sprite... " + ((int) progress) + "%");
                            }
                        });
                        return null;
                    });
                    SpriteEditorImpl.cache(file, holder.getBitmaps());
                    rawImages = holder.getImages();
                }
            } catch (Exception e) {
                rawImages = Lists.newArrayList();
                ApplicationManager.getApplication().invokeLater(() -> {
                    e.printStackTrace();
                    showException(e);
                });
            }

            // Find number padding for image names for file copy
            final int padLength = (rawImages.size() + "").length();
            final String prefix = FileUtil.getNameWithoutExtension(file.getName()) + ".";
            final String suffix = ".png";
            final int lastSize = images.size();
            images.clear();
            final int[] selection = imageList.getSelectedIndices();
            final List<ImageTransferItem> images = Lists.newArrayList();

            for (int i = 0; i < rawImages.size(); i++) {
                final String fileName = prefix + pad(i, padLength) + suffix;
                images.add(new ImageTransferItem(fileName, rawImages.get(i)));
            }
            this.images.addAll(images);
            ApplicationManager.getApplication().invokeLater(() -> {
                imageList.setListData(new ImageTransferItem[0]);
                imageList.updateUI();
                imageList.setListData(images.toArray(new ImageTransferItem[0]));
                if (lastSize == images.size()) {
                    try {
                        imageList.setSelectedIndices(selection);
                    } catch (Exception e) {
                        LOGGER.severe("Failed to preserve selection on reload in file " + file.getName());
                    }
                }
                if (scrollPane != null) {
                    if (loadingLabel != null) {
                        scrollPane.remove(loadingLabel);
                    }
                    loadingLabel = null;
                    if (imageList != null) {
                        imageList.setVisible(true);
                        scrollPane.setViewportView(imageList);
                    }
                }
            });
        });
    }


    private void initUI() {
        initBackgroundColors();
        imageList.updateUI();
        scale.addItemListener((e) -> {
            final String value = (String) Objects.requireNonNull(scale.getSelectedItem());
            final float newScale = Float.parseFloat(value.substring(0, value.length() - 1));
            cellRenderer.setScale(newScale);
            imageList.updateUI();
        });
        reloadSpriteButton.setIcon(AllIcons.Actions.Refresh);
        reloadSpriteButton.addActionListener((e) -> reloadSprite());
        reloadSpriteButton.setOpaque(false);
        reloadSpriteButton.setContentAreaFilled(false);
        reloadSpriteButton.setBorderPainted(false);
    }

    void reloadSprite() {
        SpriteEditorImpl.clearCache(file);
        loadSprite();
    }

    private void initPlaceholder() {
        imageList.setVisible(false);
        loadingLabel = new JLabel("Loading sprite...", SwingConstants.CENTER) {
            @Override
            public Dimension getPreferredSize() {
                return scrollPane.getSize();
            }
        };
        scrollPane.setViewportView(loadingLabel);
    }

    private void initBackgroundColors() {
        backgroundColor.addItemListener((itemEvent) -> {
            final String color = (String) itemEvent.getItem();
            switch (color) {
                case TRANSPARENT:
                    cellRenderer.setColor(TRANSPARENT_COLOR);
                    break;
                case BLACK:
                    cellRenderer.setColor(Color.BLACK);
                    break;
                case DARK_GREY:
                    cellRenderer.setColor(Color.DARK_GRAY);
                    break;
                case WHITE:
                    cellRenderer.setColor(Color.WHITE);
                    break;

                case LIGHT_GREY:
                    cellRenderer.setColor(Color.LIGHT_GRAY);
                    break;
                case RED:
                    cellRenderer.setColor(JBColor.RED);
                    break;
                case BLUE:
                    cellRenderer.setColor(JBColor.BLUE);
                    break;
                case GREEN:
                    cellRenderer.setColor(JBColor.GREEN);
                    break;
            }
            imageList.updateUI();
        });
    }

    private void showException(Exception exception) {
        main.removeAll();
        main.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        main.add(new JLabel("Failed to load sprite images with error: " + exception.getLocalizedMessage()), gbc);
        exception.printStackTrace();
    }

    private String pad(final int index, final int padLength) {
        StringBuilder out = new StringBuilder(index + "");
        while (out.length() < padLength) {
            out.insert(0, "0");
        }
        return out.toString();
    }

    private void createUIComponents() {
        imageList = new ImageListPanel<>(images);
        cellRenderer.setColor(TRANSPARENT_COLOR);
        imageList.setCellRenderer(cellRenderer);
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
        scale = new ComboBox<>(new String[]{
                "1x",
                "1.25x",
                "1.5x",
                "2x",
                "2.5x",
                "3x",
                "4x",
                "5x"
        });
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
        final JLabel label2 = new JLabel();
        label2.setText("Scale");
        panel1.add(label2);
        panel1.add(scale);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return main;
    }
}
