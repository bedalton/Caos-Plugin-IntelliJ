package com.badahori.creatures.plugins.intellij.agenteering.att.editor;

import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileData;
import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileLine;
import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileParser;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.Pose;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditor;
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.uiDesigner.core.Spacer;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;

import static com.intellij.openapi.application.ApplicationManager.getApplication;

public class AttEditorPanel implements OnChangePoint, HasSelectedCell {
    private static final Logger LOGGER = Logger.getLogger("#AttEditorPanel");
    private static final Key<Pose> ATT_FILE_POSE_KEY = Key.create("creatures.att.POSE_DATA");
    public static final Key<Pose> REQUESTED_POSE_KEY = Key.create("creatures.att.REQUESTED_POSE");
    @NotNull
    final VirtualFile file;
    final VirtualFile spriteFile;
    private final AttSpriteCellList spriteCellList = new AttSpriteCellList(Collections.emptyList(), 4.0, 300, 300, true);
    private final Project project;
    @SuppressWarnings("FieldCanBeLocal")
    private final String COMMAND_GROUP_ID = "ATTEditor";
    private final JMenuItem[] pointMenuItems = new JMenuItem[6];
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
    JPanel Toolbar;
    JPanel posePanel;
    char partChar;
    int numLines;
    int numPoints;
    private JPanel mainPanel;
    JCheckBox poseViewCheckbox;
    private JPanel display;
    private AttFileData fileData;
    @NotNull
    private CaosVariant variant;
    private int currentPoint = 0;
    private List<String> pointNames = Collections.emptyList();
    private boolean changedSelf = false;
    private Document document;
    private boolean lockY;
    private PoseEditor poseEditor;
    private int cell = - 1;
    private boolean didLoadOnce = false;
    private boolean doNotCommitPose = false;
    private boolean showPoseView = CaosScriptProjectSettings.getShowPoseView();
    private boolean didInit = false;

    AttEditorPanel(
            @NotNull
            final Project project,
            @NotNull
            final CaosVariant variantIn,
            @NotNull
            final VirtualFile virtualFile,
            @NotNull
            final VirtualFile spriteFile
    ) {
        this.project = project;
        this.file = virtualFile;
        this.spriteFile = spriteFile;
        this.variant = variantIn == CaosVariant.DS.INSTANCE ? CaosVariant.C3.INSTANCE : variantIn;
        $$$setupUI$$$();
        init();
    }

    JComponent getComponent() {
        init();
        return mainPanel;
    }

    synchronized void init() {
        if (didInit) {
            return;
        }
        didInit = true;
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(this.file);
        initDocumentListeners(psiFile);
        initListeners();
        initPopupMenu();
        initDisplay(this.variant, this.file);
        poseEditor.init();
    }

    private void initListeners() {

        // Add KEY listeners for point selection
        initKeyListeners();

        // Add scale dropdown listener
        scale.setSelectedIndex(CaosScriptProjectSettings.getAttScale());
        scale.addItemListener((e) -> {
            setScale(scale.getSelectedIndex());
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
            CaosScriptProjectSettings.setShowLabels(labels.isSelected());
            spriteCellList.setLabels(labels.isSelected());
            spriteCellList.reload();
        });


        // Adds a listener to the pose editor
        poseEditor.addPoseChangeListener(false, pose -> {
            if (! didLoadOnce) {
                return;
            }
            if (variant.isOld() && pose.getBody() >= 8) {
                return;
            }
            if (doNotCommitPose) {
                doNotCommitPose = false;
                return;
            }
            file.putUserData(ATT_FILE_POSE_KEY, pose);
        });

        poseViewCheckbox.addItemListener((e) -> {
            showPoseView(poseViewCheckbox.isSelected());
        });
    }

    /**
     * Inits change listeners for this att file
     *
     * @param psiFile the psi file for this att editor
     */
    private void initDocumentListeners(final @Nullable PsiFile psiFile) {
        // If file is not null. add commit file listeners
        if (psiFile != null) {
            // Get document for psi file
            this.document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            if (document != null) {
                // Add a listener to the document, to listen for change events
                this.document.addDocumentListener(new DocumentListener() {
                    @Override
                    public void documentChanged(
                            @NotNull
                                    DocumentEvent event) {
                        // If this change was made internally by this class
                        // update display, but do not update att file data
                        // As it was just set by the application
                        if (changedSelf) {
                            changedSelf = false;
                            update();
                            return;
                        }
                        // Get the document text from the changed document
                        // Changed due to external file changes, or an undo/redo operation
                        final String text = event.getDocument().getText();

                        // Check if new data, equals old data
                        // Return if it does
                        if (fileData.toFileText(variant).equals(text)) {
                            return;
                        }
                        // Parse the file data and set it to the ATT instance data
                        fileData = AttFileParser.parse(text, numLines, numPoints);
                        // Update display
                        update();
                    }
                });
            }
        }

        // If this document is null
        // Add a local file system watcher, which watches for changes to any file
        if (this.document == null) {
            // Add listener for all files
            LocalFileSystem.getInstance().addVirtualFileListener(new VirtualFileListener() {
                @Override
                public void contentsChanged(
                        @NotNull
                                VirtualFileEvent event) {
                    // Check that changed file is THIS att file
                    if (file.getPath().equals(event.getFile().getPath())) {
                        getApplication().invokeLater(() -> {
                            // Update file data from document
                            fileData = AttFileParser.parse(project, event.getFile(), numLines, numPoints);
                            update();
                        });
                    }
                }
            });
        }
    }

    private void initPopupMenu() {
        final JPopupMenu menu = new JPopupMenu();
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
        for (int i = 0; i < 6; i++) {
            final JMenuItem pointMenuItem = new JMenuItem("Point " + i);
            JRadioButton button;
            switch (i) {
                case 0:
                    button = point1;
                    break;
                case 1:
                    button = point2;
                    break;
                case 2:
                    button = point3;
                    break;
                case 3:
                    button = point4;
                    break;
                case 4:
                    button = point5;
                    break;
                case 5:
                    button = point6;
                    break;
                default:
                    throw new IndexOutOfBoundsException("Invalid point number '" + i + "' passed  in initMenuItems");
            }
            pointMenuItem.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    button.setSelected(true);
                }
            });
            pointMenuItems[i] = pointMenuItem;
        }
        mainPanel.setComponentPopupMenu(menu);
        display.setInheritsPopupMenu(true);
        scrollPane.setInheritsPopupMenu(true);
        spriteCellList.setInheritsPopupMenu(true);
    }

    /**
     * Inits the key listeners to set the current point to edit
     */
    private void initKeyListeners() {
        final InputMap inputMap = mainPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        final ActionMap actionMap = mainPanel.getActionMap();
        addNumberedPointKeyBinding(inputMap, actionMap, 0);
        addNumberedPointKeyBinding(inputMap, actionMap, 1);
        addNumberedPointKeyBinding(inputMap, actionMap, 2);
        addNumberedPointKeyBinding(inputMap, actionMap, 3);
        addNumberedPointKeyBinding(inputMap, actionMap, 4);
        addNumberedPointKeyBinding(inputMap, actionMap, 5);

        // Initializes the key controls for the next point
        final String SELECT_NEXT_POINT = "Next Point";
        inputMap.put(KeyStroke.getKeyStroke("W"), SELECT_NEXT_POINT);
        actionMap.put(SELECT_NEXT_POINT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentPoint(wrapCurrentPoint(currentPoint + 1));
            }
        });

        // Initializes the arrow key for previous point
        final String SELECT_PREVIOUS_POINT = "Previous Point";
        inputMap.put(KeyStroke.getKeyStroke("Q"), SELECT_PREVIOUS_POINT);
        actionMap.put(SELECT_PREVIOUS_POINT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentPoint(wrapCurrentPoint(currentPoint - 1));
            }
        });

        // Sets a key to lock the y axis
        final String LOCK_Y = "LockY";
        inputMap.put(KeyStroke.getKeyStroke("Y"), LOCK_Y);
        actionMap.put(LOCK_Y, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lockY = ! lockY;
            }
        });

        // Sets a shortcut key to hide the labels
        final String LABELS = "Labels";
        inputMap.put(KeyStroke.getKeyStroke("L"), LABELS);
        actionMap.put(LABELS, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean show = ! labels.isSelected();
                CaosScriptProjectSettings.setShowLabels(show);
                labels.setSelected(show);
            }
        });
    }

    /**
     * Initializes the miscellaneous editor parts
     *
     * @param variantIn   the variant to set controls to
     * @param virtualFile the ATT file for this editor
     */
    private void initDisplay(final CaosVariant variantIn, final VirtualFile virtualFile) {

        // Add Sprite cell list to scroll pane
        scrollPane.setViewportView(spriteCellList);

        // Make panel focusable
        mainPanel.setFocusable(true);
        spriteCellList.setFocusable(true);
        spriteCellList.requestFocusInWindow();


        // If part is known, hide part control
        // Part control could be confusing as one
        // might think it control the part they are editing,
        // not the parts number of points and formatting
        final BreedPartKey partKey = BreedPartKey.fromFileName(virtualFile.getNameWithoutExtension(), variantIn);
        final boolean knowsPart = partKey != null && partKey.getPart() != null && partKey.getPart() >= 'a' && partKey.getPart() <= 'q';
        this.part.setVisible(! knowsPart);
        this.part.setEnabled(! knowsPart);
        this.part.setEditable(! knowsPart);

        // Make editor focusable
        display.setFocusable(true);
        scrollPane.setFocusable(true);
        spriteCellList.setFocusable(true);

        this.setSelectedVariant(variantIn);
        final String part = virtualFile.getName().substring(0, 1);
        this.setPart(part);
        labels.setSelected(CaosScriptProjectSettings.getShowLabels());
        poseEditor.setRootPath(virtualFile.getParent().getPath());
        final Pose pose = virtualFile.getUserData(ATT_FILE_POSE_KEY);
        if (pose != null && (variant.isNotOld() || pose.getBody() < 8)) {
            try {
                poseEditor.setPose(pose, true);
            } catch (Exception e) {
                LOGGER.severe("Failed to set pose during init");
                e.printStackTrace();
            }
        }
        didLoadOnce = true;

        // Select defaults
        this.point1.setSelected(true);
        //poseEditor.setPose(0, partChar, 0);
        update(variantIn, part);

        poseViewCheckbox.setSelected(showPoseView);
        showPoseView(showPoseView);

        // Set sprite scale
        setScale(CaosScriptProjectSettings.getAttScale());
        labels.setSelected(CaosScriptProjectSettings.getShowLabels());
        loadRequestedPose();
    }

    /**
     * Wraps the given point from zero to end and end to zero
     * Used for setting the points when using error keys in the menu
     *
     * @param point point to wrap
     * @return the wrapped point
     */
    private int wrapCurrentPoint(final int point) {
        if (point < 0) {
            return this.numPoints - 1;
        } else if (point >= this.numPoints) {
            return 0;
        } else {
            return point;
        }
    }

    /**
     * Enables a number key to set the point for editing in the att file
     *
     * @param inputMap  input map for all keys
     * @param actionMap action map for all keys
     * @param point     the point to edit when the number key is pressed
     */
    private void addNumberedPointKeyBinding(final InputMap inputMap, final ActionMap actionMap, final int point) {
        final String TEXT = "Set Point " + point;
        inputMap.put(KeyStroke.getKeyStroke("" + (point + 1)), TEXT);
        actionMap.put(TEXT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentPoint(point);
            }
        });
    }

    /**
     * Sets the current point to be edited by the editor.
     * The point being head, body, start attachment, end attachment, etc.
     *
     * @param newPoint point to edit in att line
     */
    private void setCurrentPoint(final int newPoint) {
        // Ensure point is within valid range
        if (newPoint < 0) {
            throw new IndexOutOfBoundsException("New point cannot be less than zero. Found: " + newPoint);
        }
        if (newPoint > 5) {
            throw new IndexOutOfBoundsException("New point '" + newPoint + "' is out of bound. Should be (0..5)");
        }

        // Sets the current point to edit when point change events are fires
        this.currentPoint = newPoint;

        // Select the correct point in the menu
        switch (newPoint) {
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

    private void showPoseView(final boolean show) {
        this.showPoseView = show;
        this.posePanel.setVisible(show);
        CaosScriptProjectSettings.setShowPoseView(show);
        if (show) {
            this.poseEditor.redraw();
        }
    }

    private void setScale(final int index) {
        final String value = Objects.requireNonNull(scale.getItemAt(index));
        final float newScale = Float.parseFloat(value.substring(0, value.length() - 1));
        CaosScriptProjectSettings.setAttScale(scale.getSelectedIndex());
        spriteCellList.setScale(newScale);
    }

    /**
     * Sets the part to be edited
     * Parts have different argument/point list lengths
     *
     * @param part part to be editing
     */
    public void setPart(final String part) {
        if (partChar >= 'a' && partChar <= 'q') {
            poseEditor.freeze(partChar, false);
        }
        final int a = 'a';
        final int index = part.toLowerCase().charAt(0) - a;
        partChar = part.toLowerCase().charAt(0);
        update(variant, part);
        poseEditor.freeze(partChar, true);
        poseEditor.setVisibilityFocus(partChar);
    }

    /**
     * Selects the variant in the editor menu panel
     *
     * @param variantIn the variant to select in the editor.
     */
    public void setSelectedVariant(final CaosVariant variantIn) {
        CaosVariant variant = variantIn;
        if (variant == CaosVariant.DS.INSTANCE) {
            variant = CaosVariant.C3.INSTANCE;
        }
        final int selectedIndex;
        switch (variant.getCode()) {
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

    /**
     * Update the editor image
     *
     * @param variant variant of att file
     * @param part    part to refresh
     */
    public void update(final CaosVariant variant, final String part) {
        final Pair<Integer, Integer> linesAndColumns = AttEditorImpl.assumedLinesAndPoints(variant, part);
        this.numLines = linesAndColumns.getFirst();
        this.numPoints = linesAndColumns.getSecond();
        final List<String> pointNames = pointNames(part);
        this.pointNames = pointNames;
        for (int i = 0; i < pointNames.size(); i++) {
            pointMenuItems[i].setText(i + "- " + pointNames.get(i));
        }
        setMaxPoints(this.numPoints);
        fileData = AttFileParser.parse(project, file, numLines, numPoints);
        update();
    }

    /**
     * Sets the maximum number of points to show in the editor for selection
     *
     * @param numPoints number of point in att row to edit
     */
    public void setMaxPoints(final int numPoints) {
        this.numPoints = numPoints;

        // Removes points after max
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

        // Adds points before max
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

    /**
     * Adds a listener on the Point editor radio button
     *
     * @param button radio button for a point
     * @param index  point index in att row
     */
    private void addPointListener(final JRadioButton button, final int index) {
        button.addItemListener(e -> {
            if (! button.isSelected()) {
                return;
            }
            currentPoint = index;
        });
    }

    /**
     * Updates the file
     */
    private void update() {
        assert numLines >= 8 : "There should be at least 8 lines in each att file template";
        assert numPoints > 0 : "There should be at least 1 point in each ATT file template";
        final List<BufferedImage> images = getImages();
        final List<AttFileLine> lines = fileData.getLines();
        final List<AttSpriteCellData> out = new ArrayList<>();
        int maxWidth = 0;
        int maxHeight = 0;
        // Adds point to list as well as sets size of all cells
        for (int i = 0; i < Math.min(images.size(), numLines); i++) {
            final BufferedImage image = images.get(i);

            // Find the largest cell size width
            if (maxWidth < image.getWidth()) {
                maxWidth = image.getWidth();
            }
            // Find the largest cell size height
            if (maxHeight < image.getHeight()) {
                maxHeight = image.getHeight();
            }
            // Adds this point and image to the list
            out.add(new AttSpriteCellData(i, image, lines.get(i).getPoints(), pointNames, this, this));
        }
        spriteCellList.setMaxWidth(maxWidth);
        spriteCellList.setMaxHeight(maxHeight);
        spriteCellList.setItems(out);
        redrawPose();
    }

    private List<BufferedImage> getImages() {
        final String selected = (String) part.getSelectedItem();
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
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
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

        if (file == null) {
            throw new RuntimeException("File is null in att editor in createUIComponents");
        }
        if (variant == null) {
            throw new RuntimeException("Variant is null in att editor in createUIComponents");
        }
        final BreedPartKey key = BreedPartKey.fromFileName(file.getName(), variant);
        poseEditor = new PoseEditor(project, variant, Objects.requireNonNull(key));
        poseEditor.init();
        posePanel = poseEditor.getMainPanel();
        poseEditor.showFacing(false);
    }

    void commit() {
        if (! didLoadOnce) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);
                PsiDocumentManager.getInstance(project).commitDocument(document);
            });
        });
    }

    @Override
    public void onShiftPoint(final int lineNumber, final @NotNull Pair<Integer, Integer> offset) {
        final AttFileLine line = fileData.getLines().get(lineNumber);
        final List<Pair<Integer, Integer>> oldPoints = line != null ? line.getPoints() : emptyPointsList(this.numPoints);
        final Pair<Integer, Integer> oldPoint = oldPoints.get(currentPoint);
        final Pair<Integer, Integer> point = new Pair<>(oldPoint.getFirst() + offset.getFirst(), oldPoint.getSecond() + offset.getSecond());
        onChangePoint(lineNumber, point);
    }

    @Override
    public synchronized void onChangePoint(final int lineNumber,
                                           @NotNull
                                                   Pair<Integer, Integer> newPoint) {
        if (cell != lineNumber) {
            return;
        }
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
            if (! writeFile(fileData)) {
                LOGGER.severe("Failed to write Att file data");
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

        psiFile.navigate(true);
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
                    redrawPose();
                });
        return true;
    }

    private void redrawPose() {
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(() -> {
                redrawPose();
            });
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                LOGGER.severe("Cannot update POSE without ATT PSI file");
                return;
            }
            final Document refreshedDocument = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            if (refreshedDocument != null) {
                PsiDocumentManager.getInstance(project).commitDocument(refreshedDocument);
            }
            ApplicationManager.getApplication().runReadAction(() -> {
                poseEditor.freeze(partChar, true);
                if (partChar >= 'a' && partChar <= 'q') {
                    posePanel.setVisible(poseEditor.redraw(partChar));
                }
            });
        }));
    }

    private List<Pair<Integer, Integer>> emptyPointsList(int numPoints) {
        final List<Pair<Integer, Integer>> list = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            list.add(new Pair<>(0, 0));
        }
        return list;
    }

    void dispose() {
        poseEditor.dispose();
    }

    void refresh() {
        loadRequestedPose();
        poseEditor.redrawAll();
    }

    private void loadRequestedPose() {
        final Pose requestedPose = file.getUserData(REQUESTED_POSE_KEY);
        if (requestedPose == null) {
            return;
        }
        doNotCommitPose = true;
        if (didLoadOnce) {
            file.putUserData(REQUESTED_POSE_KEY, null);
        }
        poseEditor.setPose(requestedPose, true);
        final Integer pose = requestedPose.get(file.getName().charAt(0));
        if (pose != null) {
            setSelected(pose);
            final JComponent component = spriteCellList.get(pose);
            scrollPane.scrollRectToVisible(component.getVisibleRect());
        }
    }

    void clearPose() {
        poseEditor.clear();
    }

    @Override
    public int getSelectedCell() {
        return cell;
    }

    @Override
    public void setSelected(final int index) {
        int direction;
        if (variant.isOld()) {
            if (index < 4) {
                direction = 0;
            } else if (index < 8) {
                direction = 1;
            } else if (index == 8) {
                direction = 2;
            } else if (index == 9) {
                direction = 3;
            } else {
                throw new IndexOutOfBoundsException("Failed to parse direction for part " + partChar + "; Index: " + index);
            }
        } else {
            direction = (int) Math.floor((index % 16) / 4.0);
        }
        poseEditor.setPose(direction, partChar, index);
        if (variant.isNotOld()) {
            cell = index % 16;
        } else {
            cell = index % 10;
        }
        spriteCellList.reload();
        redrawPose();
        spriteCellList.scrollTo(cell);
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        Toolbar = new JPanel();
        Toolbar.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.add(Toolbar, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setText("Variant");
        Toolbar.add(label1);
        variantComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("C1");
        defaultComboBoxModel1.addElement("C2");
        defaultComboBoxModel1.addElement("CV");
        defaultComboBoxModel1.addElement("C3+");
        variantComboBox.setModel(defaultComboBoxModel1);
        Toolbar.add(variantComboBox);
        final JSeparator separator1 = new JSeparator();
        Toolbar.add(separator1);
        final JLabel label2 = new JLabel();
        label2.setText("Part");
        Toolbar.add(label2);
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
        Toolbar.add(part);
        final JSeparator separator2 = new JSeparator();
        Toolbar.add(separator2);
        final JLabel label3 = new JLabel();
        label3.setText("Point");
        Toolbar.add(label3);
        point1 = new JRadioButton();
        point1.setText("1");
        Toolbar.add(point1);
        point2 = new JRadioButton();
        point2.setText("2");
        Toolbar.add(point2);
        point3 = new JRadioButton();
        point3.setText("3");
        Toolbar.add(point3);
        point4 = new JRadioButton();
        point4.setText("4");
        Toolbar.add(point4);
        point5 = new JRadioButton();
        point5.setText("5");
        Toolbar.add(point5);
        point6 = new JRadioButton();
        point6.setText("6");
        Toolbar.add(point6);
        labels = new JCheckBox();
        labels.setSelected(true);
        labels.setText("Labels");
        Toolbar.add(labels);
        final JSeparator separator3 = new JSeparator();
        Toolbar.add(separator3);
        final JLabel label4 = new JLabel();
        label4.setText("Scale");
        Toolbar.add(label4);
        Toolbar.add(scale);
        final Spacer spacer1 = new Spacer();
        Toolbar.add(spacer1);
        poseViewCheckbox = new JCheckBox();
        poseViewCheckbox.setText("Pose View");
        Toolbar.add(poseViewCheckbox);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        mainPanel.add(panel1, BorderLayout.CENTER);
        posePanel.setMinimumSize(new Dimension(300, 0));
        posePanel.setPreferredSize(new Dimension(300, 0));
        panel1.add(posePanel, BorderLayout.WEST);
        panel1.add(scrollPane, BorderLayout.CENTER);
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
        return mainPanel;
    }

}
