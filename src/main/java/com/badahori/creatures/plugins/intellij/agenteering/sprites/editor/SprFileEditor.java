package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor;

import com.bedalton.creatures.sprite.parsers.PhotoAlbum;
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteFileHolder;
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader;
import com.bedalton.io.bytes.ByteStreamReader;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import korlibs.image.bitmap.Bitmap32;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@SuppressWarnings("UseJBColor")
public class SprFileEditor implements DumbAware {
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

    @Nullable
    private String moniker = null;

    private final Project project;

    /**
     * Basic constructor
     *
     * @param file sprite file to construct panel for
     */
    SprFileEditor(final Project project, final VirtualFile file) {
        this.project = project;
        this.file = file;
        images = new ArrayList<>();
        $$$setupUI$$$();
        init();
    }

    @SuppressWarnings("unused")
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
        main.setFocusable(true);
        imageList.setFocusable(true);
        main.setTransferHandler(imageList.getTransferHandler());
    }

    JComponent getComponent() {
        init();
        return main;
    }

    @Nullable
    private List<List<BufferedImage>> readFileAsPhotoAlbum() {

        final ByteStreamReader stream = new VirtualFileStreamReader(file, null, null);
        final PhotoAlbum album = SpriteEditorViewParser.parse(file, stream, () -> loadingLabel, () -> {
            initPlaceholder();
            return loadingLabel;
        });
        if (album == null) {
            if (loadingLabel == null) {
                initPlaceholder();
            }
            assert loadingLabel != null;
            loadingLabel.setText("Failed to load photo album");
            return null;
        }
        moniker = album.getMoniker();
        if (moniker != null && moniker.trim().length() != 4) {
            moniker = null;
        }
        final List<Bitmap32> bitmaps = SpriteEditorImpl.toBitmapList(album);
        final List<List<Bitmap32>> images = new ArrayList<>();
        images.add(bitmaps);
        SpriteEditorImpl.cache(file, images);
        final List<BufferedImage> bufferedImages = SpriteEditorImpl.toBufferedImages(album);
        final List<List<BufferedImage>> out = new ArrayList<>();
        out.add(bufferedImages);
        return out;
    }

    private List<List<BufferedImage>> readFileAsRegularSprite() {
        final SpriteFileHolder holder = SpriteEditorViewParser.parseSprite(file, () -> {
            if (loadingLabel == null) {
                initPlaceholder();
            }
            return loadingLabel;
        });
        SpriteEditorImpl.cache(file, holder.getBitmaps());
        return holder.getImageSets();
    }

    /**
     * Loads sprite images into view
     * Executed on background thread
     */
    private void loadSprite() {

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (!file.isValid()) {
                return;
            }
            final String fullFileName = file.getName();
            List<List<BufferedImage>> rawImages;

            // Initialize sprites
            try {
                rawImages = SpriteEditorImpl.fromCacheAsAwt(file);
                if (rawImages == null) {
                    final String extension = file.getExtension();
                    if (extension != null && extension.equalsIgnoreCase("PHOTO ALBUM")) {
                        rawImages = readFileAsPhotoAlbum();
                    } else {
                        rawImages = readFileAsRegularSprite();
                    }
                }
            } catch (Exception e) {
                rawImages = Lists.newArrayList();
                ApplicationManager.getApplication().invokeLater(() -> {
                    e.printStackTrace();
                    showException(e);
                });
            }

            if (rawImages == null) {
                rawImages = Collections.emptyList();
            }


            // Find number padding for image names for file copy
            final int padLength = (rawImages.size() + "").length();
            final String prefix = moniker != null ? moniker : FileUtil.getNameWithoutExtension(fullFileName);
            final String suffix = ".png";
            final int lastSize = images.size();
            images.clear();
            final int[] selection = imageList.getSelectedIndices();
            final List<ImageTransferItem> images = Lists.newArrayList();
            final int spriteSetCounts = rawImages.size();
            final int setPrefixLength = (spriteSetCounts + "").length();
            for (int i = 0; i < spriteSetCounts; i++) {
                String setPrefix;
                if (spriteSetCounts == 1) {
                    setPrefix = "";
                } else {
                    setPrefix = "." + pad(i, setPrefixLength);
                    images.add(new ImageTransferItem(prefix + "[" + pad(i, setPrefixLength) + "]", null));
                }
                final List<BufferedImage> spriteSet = rawImages.get(i);
                final int spriteCount = spriteSet.size();
                for (int j = 0; j < spriteCount; j++) {
                    final String fileName = prefix + setPrefix + "." + pad(j, padLength) + suffix;
                    images.add(new ImageTransferItem(fileName, spriteSet.get(j)));
                }
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
                        LOGGER.severe("Failed to preserve selection on reload in file " + fullFileName);
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
        if (loadingLabel == null) {
            loadingLabel = new JLabel("Loading sprite...", SwingConstants.CENTER) {
                @Override
                public Dimension getPreferredSize() {
                    return scrollPane.getSize();
                }
            };
        }
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
        main.add(new JLabel("Failed to load sprite images with " + exception.getClass().getSimpleName() + ": " + exception.getMessage()), gbc);
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
        imageList = new ImageListPanel<>(project, images);
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
