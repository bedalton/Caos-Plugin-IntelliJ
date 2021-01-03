package com.badahori.creatures.plugins.intellij.agenteering.att.editor;

import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileData;
import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileLine;
import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileParser;
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import javafx.scene.control.RadioButton;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;

import static com.intellij.openapi.application.ApplicationManager.getApplication;

public class AttEditorPanel implements OnChangePoint {
    private JPanel panel1;
    private JPanel display;
    JRadioButton point1;
    JRadioButton point2;
    JRadioButton point3;
    JRadioButton point4;
    JRadioButton point5;
    JRadioButton point6;
    JComboBox<String> scale;
    JComboBox<String> part;
    JComboBox<String> variantComboBox;
    JScrollPane scrollPane;
    JCheckBox labels;
    int numLines;
    int numPoints;
    final VirtualFile file;
    final VirtualFile spriteFile;
    private AttFileData fileData;
    private CaosVariant variant;
    private int currentPoint = 0;
    private List<String> pointNames = Collections.emptyList();
    private final AttSpriteCellList spriteCellList = new AttSpriteCellList(Collections.emptyList(), 4.0, 300, 300, true);
    private final Project project;
    private static final Logger LOGGER = Logger.getLogger("#AttEditorPanel");
    private final String COMMAND_GROUP_ID = "ATTEditor";
    private boolean changedSelf = false;
    private Document document;

    AttEditorPanel(final Project project, final CaosVariant variantIn, final VirtualFile virtualFile, final VirtualFile spriteFile) {
        this.project = project;
        this.file = virtualFile;
        this.spriteFile = spriteFile;
        this.variant = variantIn;
        $$$setupUI$$$();
        initListeners();
        final String part = virtualFile.getName().substring(0, 1);
        this.setVariant(variantIn);
        this.setPart(part);
        this.point1.setSelected(true);
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile != null) {
            this.document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            if (document != null) {
                this.document.addDocumentListener(new DocumentListener() {
                    @Override
                    public void documentChanged(@NotNull DocumentEvent event) {
                        if (changedSelf) {
                            changedSelf = false;
                            return;
                        }
                        final String text = event.getDocument().getText();
                        if (fileData.toFileText(variant).equals(text)) {
                            return;
                        }
                        fileData = AttFileParser.INSTANCE.parse(text, numLines, numPoints);
                        update();
                    }
                });
            }
        }

        if (this.document == null) {
            LocalFileSystem.getInstance().addVirtualFileListener(new VirtualFileListener() {
                @Override
                public void contentsChanged(
                        @NotNull VirtualFileEvent event) {
                    if (file.getPath().equals(event.getFile().getPath())) {
                        getApplication().invokeLater(() -> {
                            fileData = AttFileParser.INSTANCE.parse(project, event.getFile(), numLines, numPoints);
                            update();
                        });
                    }
                }
            });
        }
    }

    private void initListeners() {
        panel1.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == '1') {
                    point1.setSelected(true);
                    currentPoint = 0;
                } else if (e.getKeyChar() == '2') {
                    point2.setSelected(true);
                    currentPoint = 1;
                } else if (e.getKeyChar() == '3') {
                    point3.setSelected(true);
                    currentPoint = 2;
                } else if (e.getKeyChar() == '4') {
                    point4.setSelected(true);
                    currentPoint = 3;
                } else if (e.getKeyChar() == '5') {
                    point5.setSelected(true);
                    currentPoint = 4;
                } else if (e.getKeyChar() == '6') {
                    point6.setSelected(true);
                    currentPoint = 5;
                }
            }
        });
        scale.setSelectedIndex(6);
        scale.addItemListener((e) -> {
            final String value = (String) Objects.requireNonNull(scale.getSelectedItem());
            final float newScale = Float.parseFloat(value.substring(0, value.length() - 1));
            spriteCellList.setScale(newScale);
        });
        part.addItemListener((e) -> {
            final String value = (String) Objects.requireNonNull(part.getSelectedItem());
            update(variant, value.trim());
        });

        variantComboBox.addItemListener((e) -> {
            final String value = (String) Objects.requireNonNull(variantComboBox.getSelectedItem());
            CaosVariant newVariant;
            switch (value.toUpperCase().trim()) {
                case "C1":
                    newVariant = CaosVariant.C1.INSTANCE;
                    break;
                case "C2":
                    newVariant = CaosVariant.C2.INSTANCE;
                    break;
                case "CV":
                    newVariant = CaosVariant.CV.INSTANCE;
                    break;
                default:
                    newVariant = CaosVariant.C3.INSTANCE;
                    break;
            }
            this.variant = newVariant;
            AttEditorImpl.cacheVariant(file, variant);
            update(newVariant, (String) part.getSelectedItem());
        });

        addPointListener(point1, 0);
        addPointListener(point2, 1);
        addPointListener(point3, 2);
        addPointListener(point4, 3);
        addPointListener(point5, 4);
        addPointListener(point6, 5);
        labels.addChangeListener((e) -> {
            spriteCellList.setLabels(labels.isSelected());
            spriteCellList.reload();
        });
        scrollPane.setViewportView(spriteCellList);
    }

    public void setPart(final String part) {
        int a = 'a';
        int index = part.toLowerCase().charAt(0) - a;
        this.part.setSelectedIndex(index);
        update(variant, part);
    }

    public void setVariant(final CaosVariant variantIn) {
        this.variant = variantIn;
        int selectedIndex;
        switch (variantIn.getCode()) {
            case "C1":
                selectedIndex = 0;
                break;
            case "C2":
                selectedIndex = 1;
                break;
            case "CV":
                selectedIndex = 2;
                break;
            default:
                selectedIndex = 3;
                break;
        }
        this.variantComboBox.setSelectedIndex(selectedIndex);
    }

    public void update(final CaosVariant variant, final String part) {
        Pair<Integer, Integer> linesAndColumns = AttEditorImpl.assumedLinesAndPoints(variant, part);
        this.numLines = linesAndColumns.getFirst();
        this.numPoints = linesAndColumns.getSecond();
        setMaxPoints(this.numPoints);
        this.pointNames = pointNames(part);
        fileData = AttFileParser.INSTANCE.parse(project, file, numLines, numPoints);
        update();
    }

    public void setNumLines(final int numLines) {
        this.numLines = numLines;
    }

    public void setMaxPoints(int numPoints) {
        this.numPoints = numPoints;
        switch (numPoints) {
            case 1:
                point2.setVisible(false);
            case 2:
                point3.setVisible(false);
            case 3:
                point4.setVisible(false);
            case 4:
                point5.setVisible(false);
            case 5:
                point6.setVisible(false);
                break;
        }
        if (numPoints > 1) {
            point2.setVisible(true);
            if (numPoints > 2) {
                point3.setVisible(true);
                if (numPoints > 3) {
                    point4.setVisible(true);
                    if (numPoints > 4) {
                        point5.setVisible(true);
                        if (numPoints > 5) {
                            point6.setVisible(true);
                        }
                    }
                }
            }
        }
    }

    private void addPointListener(final JRadioButton button, final int index) {
        button.addItemListener(e -> {
            if (! button.isSelected()) {
                return;
            }
            currentPoint = index;
            return;
        });
    }

    private void update() {
        assert numLines >= 8 : "There should be at least 8 lines in each att file template";
        assert numPoints > 0 : "There should be at least 1 point in each ATT file template";
        List<BufferedImage> images = getImages();
        List<AttFileLine> lines = fileData.getLines();
        spriteCellList.setMaxWidth(0);
        spriteCellList.setMaxHeight(0);
        List<AttSpriteCellData> out = new ArrayList<>();
        for (int i = 0; i < Math.min(images.size(), numLines); i++) {
            final BufferedImage image = images.get(i);
            if (spriteCellList.getMaxHeight() < image.getHeight()) {
                spriteCellList.setMaxHeight(image.getHeight());
            }
            if (spriteCellList.getMaxWidth() < image.getWidth()) {
                spriteCellList.setMaxWidth(image.getWidth());
            }
            out.add(new AttSpriteCellData(i, image, lines.get(i).getPoints(), pointNames, this));
        }
        spriteCellList.setItems(out);
    }

    private List<BufferedImage> getImages() {
        return AttEditorImpl.getImages(variant, (String) part.getSelectedItem(), spriteFile);
    }

    private List<String> pointNames(final String part) {
        switch (part.trim().toUpperCase()) {
            case "A":
                return Arrays.asList(
                        "Neck",
                        "Mouth",
                        "(L)Ear",
                        "(R)Ear",
                        "Hair"
                );
            case "B":
                return Arrays.asList(
                        "Neck",
                        "(L)Thigh",
                        "(R)Thigh",
                        "(L)Arm",
                        "(R)Arm",
                        "Tail"
                );
            case "Q":
                return Collections.singletonList("Head");
            default:
                return Arrays.asList(
                        "Start",
                        "End"
                );
        }
    }


    private void createUIComponents() {
        display = spriteCellList;
        scrollPane = new JScrollPane(display) {
            @Override
            public Dimension getPreferredSize() {
                setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                final Dimension superPreferredSize = super.getPreferredSize();
                Dimension dim = new Dimension(superPreferredSize.width + getVerticalScrollBar().getSize().width, superPreferredSize.height);
                setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                return dim;
            }
        };
        scrollPane.setViewportView(display);
        scale = new ComboBox<>(new String[]{
                "1x",
                "1.25x",
                "1.5x",
                "2x",
                "2.5x",
                "3x",
                "4x",
                "5x",
                "6x",
                "7x"
        });
    }

    @Override
    public synchronized void onChangePoint(final int lineNumber, final @NotNull
            Pair<Integer, Integer> newPoint) {
        final AttFileLine line = fileData.getLines().get(lineNumber);
        final List<Pair<Integer, Integer>> oldPoints = line != null ? line.getPoints() : emptyPointsList(this.numPoints);
        final List<Pair<Integer, Integer>> newPoints = new ArrayList<>();
        for (int i = 0; i < oldPoints.size(); i++) {
            newPoints.add(i == currentPoint ? newPoint : oldPoints.get(i));
        }
        final AttFileLine newLine = new AttFileLine(newPoints);
        final List<AttFileLine> oldLines = fileData.getLines();
        final List<AttFileLine> newLines = new ArrayList<>();
        for (int i = 0; i < oldLines.size(); i++) {
            newLines.add(i == lineNumber ? newLine : oldLines.get(i));
        }
        fileData = new AttFileData(newLines);
        try {
            if (! writeFile(fileData)) {
                LOGGER.severe("Failed to write Att file data");
                return;
            }
            update();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean writeFile(final AttFileData fileData) {
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            LOGGER.severe("Cannot update ATT file without PSI file");
            return false;
        }

        final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            LOGGER.severe("Cannot write ATT file without document");
            return false;
        }
        changedSelf = true;
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);
        WriteCommandAction.runWriteCommandAction(
                project,
                "Move Points",
                COMMAND_GROUP_ID,
                () -> {
                    document.replaceString(0, psiFile.getTextRange().getEndOffset(), fileData.toFileText(variant));
                });
        return true;
    }

    private List<Pair<Integer, Integer>> emptyPointsList(int numPoints) {
        final List<Pair<Integer, Integer>> list = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            list.add(new Pair<>(0, 0));
        }
        return list;
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
        panel1.setLayout(new BorderLayout(0, 0));
        panel1.add(scrollPane, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel2, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setText("Variant");
        panel2.add(label1);
        variantComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("C1");
        defaultComboBoxModel1.addElement("C2");
        defaultComboBoxModel1.addElement("CV");
        defaultComboBoxModel1.addElement("C3+");
        variantComboBox.setModel(defaultComboBoxModel1);
        panel2.add(variantComboBox);
        final JSeparator separator1 = new JSeparator();
        panel2.add(separator1);
        final JLabel label2 = new JLabel();
        label2.setText("Part");
        panel2.add(label2);
        part = new JComboBox();
        part.setEditable(false);
        part.setEnabled(true);
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("A");
        defaultComboBoxModel2.addElement("B");
        defaultComboBoxModel2.addElement("C");
        defaultComboBoxModel2.addElement("D");
        defaultComboBoxModel2.addElement("E");
        defaultComboBoxModel2.addElement("F");
        defaultComboBoxModel2.addElement("G");
        defaultComboBoxModel2.addElement("H");
        defaultComboBoxModel2.addElement("I");
        defaultComboBoxModel2.addElement("J");
        defaultComboBoxModel2.addElement("K");
        defaultComboBoxModel2.addElement("L");
        defaultComboBoxModel2.addElement("M");
        defaultComboBoxModel2.addElement("M");
        defaultComboBoxModel2.addElement("O");
        defaultComboBoxModel2.addElement("P");
        defaultComboBoxModel2.addElement("Q");
        part.setModel(defaultComboBoxModel2);
        panel2.add(part);
        final JSeparator separator2 = new JSeparator();
        panel2.add(separator2);
        final JLabel label3 = new JLabel();
        label3.setText("Point");
        panel2.add(label3);
        point1 = new JRadioButton();
        point1.setText("1");
        panel2.add(point1);
        point2 = new JRadioButton();
        point2.setText("2");
        panel2.add(point2);
        point3 = new JRadioButton();
        point3.setText("3");
        panel2.add(point3);
        point4 = new JRadioButton();
        point4.setText("4");
        panel2.add(point4);
        point5 = new JRadioButton();
        point5.setText("5");
        panel2.add(point5);
        point6 = new JRadioButton();
        point6.setText("6");
        panel2.add(point6);
        labels = new JCheckBox();
        labels.setSelected(true);
        labels.setText("Labels");
        panel2.add(labels);
        final JSeparator separator3 = new JSeparator();
        panel2.add(separator3);
        final JLabel label4 = new JLabel();
        label4.setText("Scale");
        panel2.add(label4);
        panel2.add(scale);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(point6);
        buttonGroup.add(point5);
        buttonGroup.add(point4);
        buttonGroup.add(point3);
        buttonGroup.add(point2);
        buttonGroup.add(point1);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    void dispose() {
    }

}