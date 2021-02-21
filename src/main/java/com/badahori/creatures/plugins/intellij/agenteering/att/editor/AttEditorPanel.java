package com.badahori.creatures.plugins.intellij.agenteering.att.editor;

import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileData;
import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileLine;
import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileParser;
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings;
import com.intellij.openapi.application.ApplicationManager;
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
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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
    @SuppressWarnings("FieldCanBeLocal")
    private final String COMMAND_GROUP_ID = "ATTEditor";
    private boolean changedSelf = false;
    private Document document;
    private boolean lockY;
    private final JMenuItem[] pointMenuItems = new JMenuItem[6];

    AttEditorPanel(final Project project, final CaosVariant variantIn, final VirtualFile virtualFile, final VirtualFile spriteFile) {
        this.project = project;
        this.file = virtualFile;
        this.spriteFile = spriteFile;
        this.variant = variantIn;
        $$$setupUI$$$();
        final String part = virtualFile.getName().substring(0, 1);
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
                            update();
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
        initListeners();
        initPopupMenu();
        display.setFocusable(true);
        scrollPane.setFocusable(true);
        spriteCellList.setFocusable(true);
        this.setVariant(variantIn);
        this.setPart(part);
        update(variantIn, part);
        labels.setSelected(CaosScriptProjectSettings.INSTANCE.getShowLabels());
    }

    private void initListeners() {

        // Add KEY listeners for point selection
        initKeyListeners();

        // Add scale dropdown listener
        scale.setSelectedIndex(6);
        scale.addItemListener((e) -> {
            final String value = (String) Objects.requireNonNull(scale.getSelectedItem());
            final float newScale = Float.parseFloat(value.substring(0, value.length() - 1));
            spriteCellList.setScale(newScale);
        });

        // Add listener for PART dropdown
        part.addItemListener((e) -> {
            final String value = (String) Objects.requireNonNull(part.getSelectedItem());
            update(variant, value.trim());
        });

        // Add Variant dropdown listener
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

        // Add Point radio-buttons listener
        addPointListener(point1, 0);
        addPointListener(point2, 1);
        addPointListener(point3, 2);
        addPointListener(point4, 3);
        addPointListener(point5, 4);
        addPointListener(point6, 5);

        // Add LABELS checkbox listener
        labels.addChangeListener((e) -> {
            spriteCellList.setLabels(labels.isSelected());
            spriteCellList.reload();
        });

        // Add Sprite cell list to scroll pane
        scrollPane.setViewportView(spriteCellList);

        panel1.setFocusable(true);
        spriteCellList.setFocusable(true);
        spriteCellList.requestFocusInWindow();
    }
    private void initPopupMenu() {
        final JPopupMenu menu = new JPopupMenu();
        /*final JMenuItem labels = new JCheckBoxMenuItem("Labels");
        labels.setSelected(this.labels.isSelected());
        labels.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                labels.setSelected(!labels.isSelected());
                panel1.repaint();
                update();
            }
        });
        menu.add(labels);*/
        final JMenuItem lockY = new JCheckBoxMenuItem("Lock Y-axis");
        lockY.setSelected(this.lockY);
        lockY.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AttEditorPanel.this.lockY = ! AttEditorPanel.this.lockY;
                lockY.setSelected(AttEditorPanel.this.lockY);
            }
        });
        menu.add(lockY);
        menu.addSeparator();
        for(int i=0; i < 6; i++) {
            final JMenuItem pointMenuItem = new JMenuItem("Point " + i);
            JRadioButton button;
            switch(i) {
                case 0: button = point1; break;
                case 1: button = point2; break;
                case 2: button = point3; break;
                case 3: button = point4; break;
                case 4: button = point5; break;
                case 5: button = point6; break;
                default:
                    throw new IndexOutOfBoundsException("Invalid point number '"+i+"' passed  in initMenuItems");
            }
            pointMenuItem.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    button.setSelected(true);
                }
            });
            pointMenuItems[i] = pointMenuItem;
        }
        panel1.setComponentPopupMenu(menu);
        display.setInheritsPopupMenu(true);
        scrollPane.setInheritsPopupMenu(true);
        spriteCellList.setInheritsPopupMenu(true);
    }
    /**
     * Inits the key listeners to set the current point to edit
     */
    private void initKeyListeners() {
        final InputMap inputMap = panel1.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        final ActionMap actionMap = panel1.getActionMap();
        addNumberedPointKeyBinding(inputMap, actionMap,0);
        addNumberedPointKeyBinding(inputMap, actionMap,1);
        addNumberedPointKeyBinding(inputMap, actionMap,2);
        addNumberedPointKeyBinding(inputMap, actionMap,3);
        addNumberedPointKeyBinding(inputMap, actionMap,4);
        addNumberedPointKeyBinding(inputMap, actionMap,5);

        final String SELECT_NEXT_POINT = "Next Point";
        inputMap.put(KeyStroke.getKeyStroke("W"), SELECT_NEXT_POINT);
        actionMap.put(SELECT_NEXT_POINT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentPoint(wrapCurrentPoint(currentPoint+1));
            }
        });
        final String SELECT_PREVIOUS_POINT = "Next Point";
        inputMap.put(KeyStroke.getKeyStroke("Q"), SELECT_PREVIOUS_POINT);
        actionMap.put(SELECT_PREVIOUS_POINT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentPoint(wrapCurrentPoint(currentPoint - 1));
            }
        });
        final String LOCK_Y = "LockY";
        inputMap.put(KeyStroke.getKeyStroke("Y"), LOCK_Y);
        actionMap.put(LOCK_Y, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lockY = !lockY;
            }
        });

        final String LABELS = "Labels";
        inputMap.put(KeyStroke.getKeyStroke("L"), LABELS);
        actionMap.put(LABELS, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean show = !labels.isSelected();
                CaosScriptProjectSettings.INSTANCE.setShowLabels(show);
                labels.setSelected(show);
            }
        });
    }

    private int wrapCurrentPoint(final int point) {
        if (point < 0)
            return this.numPoints-1;
        else if (point >= this.numPoints)
            return 0;
        else
            return point;
    }

    private void addNumberedPointKeyBinding(final InputMap inputMap, final ActionMap actionMap, final int point) {
        final String TEXT = "Set Point " + point;
        inputMap.put(KeyStroke.getKeyStroke(""+(point+1)), TEXT);
        actionMap.put(TEXT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentPoint(point);
            }
        });
    }

    private void setCurrentPoint(final int newPoint) {
        if (newPoint < 0) {
            throw new IndexOutOfBoundsException("New point cannot be less than zero. Found: " + newPoint);
        }
        if (newPoint > 5) {
            throw new IndexOutOfBoundsException("New point '"+newPoint+"' is out of bound. Should be (0..5)");
        }
        this.currentPoint = newPoint;
        switch(newPoint) {
            case 0:
                point1.setSelected(true);
                break;
            case 1:
                point2.setSelected(true);
                break;
            case 2:
                point3.setSelected(true);
                break;
            case 3:
                point4.setSelected(true);
                break;
            case 4:
                point5.setSelected(true);
                break;
            case 5:
                point6.setSelected(true);
                break;
        }
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
        final List<String> pointNames = pointNames(part);
        this.pointNames = pointNames;
        for(int i=0; i < pointNames.size(); i++) {
            pointMenuItems[i].setText(i + "- " + pointNames.get(i));
        }
        setMaxPoints(this.numPoints);
        fileData = AttFileParser.INSTANCE.parse(project, file, numLines, numPoints);
        update();
    }

    public void setMaxPoints(int numPoints) {
        this.numPoints = numPoints;
        switch (numPoints) {
            case 1:
                point2.setVisible(false);
                pointMenuItems[1].setVisible(false);
            case 2:
                point3.setVisible(false);
                pointMenuItems[2].setVisible(false);
            case 3:
                point4.setVisible(false);
                pointMenuItems[3].setVisible(false);
            case 4:
                point5.setVisible(false);
                pointMenuItems[4].setVisible(false);
            case 5:
                point6.setVisible(false);
                pointMenuItems[5].setVisible(false);
                break;
        }
        if (numPoints > 1) {
            point2.setVisible(true);
            pointMenuItems[1].setVisible(true);
            if (numPoints > 2) {
                point3.setVisible(true);
                pointMenuItems[2].setVisible(true);
                if (numPoints > 3) {
                    point4.setVisible(true);
                    pointMenuItems[3].setVisible(true);
                    if (numPoints > 4) {
                        point5.setVisible(true);
                        pointMenuItems[4].setVisible(true);
                        if (numPoints > 5) {
                            point6.setVisible(true);
                            pointMenuItems[5].setVisible(true);
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
        final String selected = (String)part.getSelectedItem();
        if (selected == null) {
            return Collections.emptyList();
        }
        return AttEditorImpl.getImages(variant, selected, spriteFile);
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
    public synchronized void onChangePoint(final int lineNumber, @NotNull
            Pair<Integer, Integer> newPoint) {
        final AttFileLine line = fileData.getLines().get(lineNumber);
        final List<Pair<Integer, Integer>> oldPoints = line != null ? line.getPoints() : emptyPointsList(this.numPoints);
        final List<Pair<Integer, Integer>> newPoints = new ArrayList<>();
        for (int i = 0; i < oldPoints.size(); i++) {
            if (currentPoint == i && lockY) {
                newPoint = new Pair<>(newPoint.getFirst(), oldPoints.get(i).getSecond());
            }
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
            if (!writeFile(fileData)) {
                LOGGER.severe("Failed to write Att file data");
                return;
            }
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
                () -> document.replaceString(0, psiFile.getTextRange().getEndOffset(), fileData.toFileText(variant)));
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
