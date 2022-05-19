package com.badahori.creatures.plugins.intellij.agenteering.att.editor;

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.Pose;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditorImpl;
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileData;
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileLine;
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartFiles;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey;
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications;
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;

public class AttEditorPanel implements HasSelectedCell, AttEditorController.View {
    private static final Logger LOGGER = Logger.getLogger("#AttEditorPanel");
    public static final Key<Pose> ATT_FILE_POSE_KEY = Key.create("creatures.att.POSE_DATA");
    public static final Key<Pose> REQUESTED_POSE_KEY = Key.create("creatures.att.REQUESTED_POSE");
    private static final boolean EAGER_LOAD_POSE_EDITOR = true;
    private final AttSpriteCellList spriteCellList = new AttSpriteCellList(Collections.emptyList(), 4.0, 300, 300, true);
    private final Project project;
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
    JPanel attToolbar;
    JPanel posePanel;
    private JPanel mainPanel;
    JCheckBox poseViewCheckbox;
    private JPanel display;
    private List<String> pointNames = Collections.emptyList();
    private PoseEditorImpl poseEditor;
    private int cell = -1;
    private boolean didLoadOnce = false;
    private boolean doNotCommitPose = false;
    private boolean showPoseView = CaosScriptProjectSettings.getShowPoseView();
    private boolean didInit = false;
    private final AttEditorHandler controller;

    protected JButton refreshButton;

    AttEditorPanel(
            @NotNull
            final Project project,
            final AttEditorHandler handler
    ) {
        this.project = project;
        this.controller = handler;
        $$$setupUI$$$();
        init();
    }

    public @NotNull JComponent getComponent() {
        init();
        return mainPanel;
    }

    public synchronized void init() {
        if (didInit) {
            return;
        }
        if (project.isDisposed()) {
            return;
        }
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(this::init);
            return;
        }
        didInit = true;
        initListeners();
        initPopupMenu();
        initDisplay(controller.getVariant());
//        poseEditor.init();

        (new RuntimeException()).printStackTrace();
        poseEditor.setRootPath(controller.getRootPath());
        updateUI();
    }

    private void initListeners() {

        // Add KEY listeners for point selection
        initKeyListeners();

        // Add scale dropdown listener
        scale.setSelectedIndex(CaosScriptProjectSettings.getAttScale());
        scale.addItemListener((e) -> setScale(scale.getSelectedIndex()));

        // Add listener for PART dropdown
        part.addItemListener((e) -> {
            final Object temp = part.getSelectedItem();
            if (temp == null) {
                return;
            }
            final String value = ((String) temp).trim();
            if (value.length() < 1) {
                throw new RuntimeException("Cannot set part to undefined");
            }
            final char part = Character.toLowerCase(value.toCharArray()[0]);
            controller.setPart(part);
            onPartChange(part);
            updateUI();
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
            controller.setVariant(newVariant);
            updateUI();
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
            if (!didLoadOnce) {
                return;
            }
            if (controller.getVariant().isOld() && pose.getBody() >= 8) {
                return;
            }
            if (doNotCommitPose) {
                doNotCommitPose = false;
                return;
            }
            controller.setPose(pose);
        });

        poseViewCheckbox.addItemListener((e) -> showPoseView(poseViewCheckbox.isSelected()));
        refreshButton.addActionListener((e) -> {
            controller.reloadFiles();
            poseEditor.hardReload();
            refresh();
        });
    }


    private void initPopupMenu() {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem lockY = new JCheckBoxMenuItem("Lock Y-axis");
        lockY.setSelected(this.controller.getLockY());
        lockY.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.setLockY(!controller.getLockY());
                lockY.setSelected(controller.getLockY());
            }
        });

        final JMenuItem lockX = new JCheckBoxMenuItem("Lock X-axis");
        lockX.setSelected(this.controller.getLockX());
        lockX.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.setLockX(!controller.getLockX());
                lockX.setSelected(controller.getLockX());
            }
        });
        menu.add(lockX);
        menu.add(lockY);
        menu.addSeparator();
//        scrollPane.addMouseListener(new MouseListenerBase() {
//            @Override
//            public void mouseClicked(@NotNull MouseEvent e) {
//                menu.show(scrollPane, e.getX(), e.getY());
//            }
//        });
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
        spriteCellList.setComponentPopupMenu(menu);
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
        final String selectNextPoint = "Next Point";
        inputMap.put(KeyStroke.getKeyStroke("W"), selectNextPoint);
        actionMap.put(selectNextPoint, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentPoint(wrapCurrentPoint(controller.getCurrentPoint() + 1));
            }
        });

        // Initializes the arrow key for previous point
        final String selectPreviousPoint = "Previous Point";
        inputMap.put(KeyStroke.getKeyStroke("Q"), selectPreviousPoint);
        actionMap.put(selectPreviousPoint, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentPoint(wrapCurrentPoint(controller.getCurrentPoint() - 1));
            }
        });

        // Sets a key to lock the y-axis
        final String lockY = "LockY";
        inputMap.put(KeyStroke.getKeyStroke("Y"), lockY);
        actionMap.put(lockY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.setLockY(!controller.getLockY());
            }
        });
        // Sets a key to lock the y-axis
        final String lockX = "LockX";
        inputMap.put(KeyStroke.getKeyStroke("X"), lockX);
        actionMap.put(lockX, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.setLockX(!controller.getLockX());
            }
        });

        // Sets a shortcut key to hide the labels
        final String labels = "Labels";
        inputMap.put(KeyStroke.getKeyStroke("L"), labels);
        actionMap.put(labels, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean show = !AttEditorPanel.this.labels.isSelected();
                CaosScriptProjectSettings.setShowLabels(show);
                AttEditorPanel.this.labels.setSelected(show);
            }
        });

        final String previousFrame = "Previous Frame";
        inputMap.put(KeyStroke.getKeyStroke("shift tab"), previousFrame);
        actionMap.put(previousFrame, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.setSelected(controller.getSelectedCell() - 1);
            }
        });

        final String nextFrame = "Next Frame";
        inputMap.put(KeyStroke.getKeyStroke("shift tab"), nextFrame);
        actionMap.put(nextFrame, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.setSelected(controller.getSelectedCell() + 1);
            }
        });

        scrollPane.setInputMap(JComponent.WHEN_FOCUSED, inputMap);
        scrollPane.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);
        scrollPane.setActionMap(actionMap);
    }

    /**
     * Initializes the miscellaneous editor parts
     *
     * @param variantIn the variant to set controls to
     */
    private void initDisplay(final CaosVariant variantIn) {

        if (project.isDisposed()) {
            return;
        }
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(() -> initDisplay(variantIn));
            return;
        }


        // Add Sprite cell list to scroll pane
        scrollPane.setViewportView(spriteCellList);

        // Make panel focusable
        mainPanel.setFocusable(true);
        spriteCellList.setFocusable(true);
        spriteCellList.requestFocusInWindow();


        // If part is known, hide the part control.
        // Part control could be confusing as one
        // might think it controls the part they are editing,
        // not the parts number of points and formatting
        final BreedPartKey partKey = controller.getBreedPartKey();
        final boolean knowsPart = partKey != null && partKey.getPart() != null && partKey.getPart() >= 'a' && partKey.getPart() <= 'q';
        this.part.setVisible(!knowsPart);
        this.part.setEnabled(!knowsPart);
        this.part.setEditable(!knowsPart);

        // Make editor focusable
        display.setFocusable(true);
        scrollPane.setFocusable(true);
        spriteCellList.setFocusable(true);

        this.setSelectedVariant(variantIn);
        this.setPart(getPart());
        labels.setSelected(CaosScriptProjectSettings.getShowLabels());
        poseEditor.setRootPath(controller.getRootPath());
        pushAttToPoseEditor(controller.getAttData());
        final Pose pose = controller.getPose();
        controller.setPose(null);
        if (pose != null && (getVariant().isNotOld() || pose.getBody() < 8)) {
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
        updateUI();

        // Set sprite scale
        setScale(CaosScriptProjectSettings.getAttScale());
        labels.setSelected(CaosScriptProjectSettings.getShowLabels());
        if (this.controller.getPart() == 'z') {
            showPoseView(false);
            return;
        }

        poseViewCheckbox.setSelected(showPoseView);
        showPoseView(showPoseView);


        loadRequestedPose();
        poseEditor.setAtt(controller.getPart(), controller.getSpriteFile(), controller.getAttData());
    }

    private void pushAttToPoseEditor(final AttFileData attFile) {
        if (this.controller.getPart() == 'z') {
            return;
        }
        final HashMap<Character, BodyPartFiles> locked = new HashMap<>();
        final VirtualFile spriteFile = controller.getSpriteFile();
        CaosVariant variant = getVariant();
        final String attText = attFile.toFileText(variant);
        final VirtualFile virtualFile = CaosVirtualFileSystem.getInstance().createTemporaryFile(attText, "att");
        locked.put(getPart(), new BodyPartFiles(spriteFile, virtualFile));
        poseEditor.setLocked(locked);
    }

    /**
     * Wraps the given point from zero to end and end to zero
     * Used for setting the points when using arrow keys in the menu
     *
     * @param point point to wrap
     * @return the wrapped point
     */
    private int wrapCurrentPoint(final int point) {
        final int pointCount = controller.getPointCount();
        if (point < 0) {
            return pointCount - 1;
        } else if (point >= pointCount) {
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
        controller.setCurrentPoint(newPoint);

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
        if (this.controller.getPart() != 'z') {
            CaosScriptProjectSettings.setShowPoseView(show);
        }
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
    public void setPart(final Character part) {
        try {
            this.part.setSelectedIndex(part - 'a');
            controller.setPart(part);
            onPartChange(part);
        } catch (Exception ignored) {

        }
    }

    private void onPartChange(
            @NotNull
            final Character part) {
        final char oldChar = controller.getPart();
        if (oldChar >= 'a' && oldChar <= 'q') {
            poseEditor.freeze(oldChar, false);
            poseEditor.freezeBreedForPart(oldChar, false);
        }
        poseEditor.freeze(part, true);
        poseEditor.setVisibilityFocus(part);
        poseEditor.freezeBreedForPart(part, true);
        updateUI();
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
     * Update the editor and image
     */
    public void updateUI() {
        final List<String> pointNames = controller.getPointNames();
        this.pointNames = pointNames;
        for (int i = 0; i < pointNames.size(); i++) {
            pointMenuItems[i].setText(i + "- " + pointNames.get(i));
        }
        setMaxPoints(controller.getPointCount());
        updateCells();
    }

    /**
     * Sets the maximum number of points to show in the editor for selection
     *
     * @param numPoints number of point in att row to edit
     */
    public void setMaxPoints(final int numPoints) {
        // Removes the points after max
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

        // Adds the points before max
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
            if (!button.isSelected()) {
                return;
            }
            controller.setCurrentPoint(index);
        });
    }


    /**
     * Updates the cells in this view
     */
    void updateCells() {
        final String error;

        final List<AttFileLine> lines = controller.getAttLines();
        if (lines.size() < 8) {
            error = "There should be at least 8 lines in each att file template";
        } else if (controller.getPointCount() < 0) {
            error = "There should be at least 1 point in each ATT file template";
        } else {
            error = null;
        }
        if (error != null) {
            if (project == null || project.isDisposed()) {
                return;
            }
            CaosNotifications.INSTANCE.showWarning(project, "Malformed ATT", error);
            return;
        }
        final List<BufferedImage> images = getImages();
        final List<AttSpriteCellData> out = new ArrayList<>();
        int maxWidth = 0;
        int maxHeight = 0;
        final int linesCount = controller.getLinesCount();
        // Adds point to list as well as sets size of all cells
        for (int i = 0; i < Math.min(images.size(), linesCount); i++) {
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
            out.add(new AttSpriteCellData(i, image, lines.get(i).getPoints(), pointNames, controller, this));
        }
        spriteCellList.setMaxWidth(maxWidth);
        spriteCellList.setMaxHeight(maxHeight);
        spriteCellList.setItems(out);
        redrawPose();
    }

    private List<BufferedImage> getImages() {
        return controller.getImages();
    }


    @Override
    public void onAttUpdate(int @NotNull ... lines) {

        if (this.controller.getPart() != 'z') {
            pushAttToPoseEditor(controller.getAttData());
            poseEditor.setAtt(controller.getPart(), controller.getSpriteFile(), controller.getAttData());
        }
        // Update display
        updateCells();
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

        final BreedPartKey key = controller.getBreedPartKey();

        poseEditor = new PoseEditorImpl(project, getVariant(), Objects.requireNonNull(key), EAGER_LOAD_POSE_EDITOR, (rendered) -> {
            if (posePanel.isVisible() != rendered) {
                posePanel.setVisible(rendered);
            }
            return null;
        });
        poseEditor.init();
        posePanel = poseEditor.getMainPanel();
        poseEditor.showFacing(false);
    }


    private void redrawPose() {

        if (this.controller.getPart() == 'z') {
            return;
        }
        if (project.isDisposed()) {
            return;
        }

        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(this::redrawPose);
            return;
        }

        final char partChar = getPart();
        poseEditor.freeze(partChar, true);
        if (partChar >= 'a' && partChar <= 'q') {
            poseEditor.redrawAll();
        }
    }

    public void dispose() {
        poseEditor.dispose();
    }

    public void refresh() {

        if (project.isDisposed()) {
            dispose();
            return;
        }
        if (controller.getPart() != 'z' && !poseEditor.isValid()) {
            return;
        }
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(this::refresh);
            return;
        }

        poseEditor.isShown();
        poseEditor.init();
        loadRequestedPose();
        poseEditor.redrawAll();
    }

    private void loadRequestedPose() {
        final Pose requestedPose = controller.getRequestedPose();
        if (requestedPose == null) {
            return;
        }
        doNotCommitPose = true;
        if (didLoadOnce) {
            controller.setPose(null);
        }
        poseEditor.setPose(requestedPose, true);
        final Integer pose = requestedPose.get(getPart());
        if (pose != null) {
            setSelected(pose);
        }
    }

    @Override
    public void scrollCellIntoView() {
        final Pose currentPose = controller.getPose();
        if (currentPose == null) {
            return;
        }
        final Integer pose = currentPose.get(getPart());
        if (pose == null) {
            return;
        }
        final JComponent component = spriteCellList.get(pose);
        scrollPane.scrollRectToVisible(component.getVisibleRect());
    }

    public void clearPose() {
        poseEditor.clear();
    }

    @Override
    public int getSelectedCell() {
        return cell;
    }

    @Override
    public void setSelected(final int index) {
        int direction;
        if (getVariant().isOld()) {
            if (index < 4) {
                direction = 0;
            } else if (index < 8) {
                direction = 1;
            } else if (index == 8) {
                direction = 2;
            } else if (index == 9) {
                direction = 3;
            } else {
                throw new IndexOutOfBoundsException("Failed to parse direction for part " + getPart() + "; Index: " + index);
            }
        } else {
            direction = (int) Math.floor((index % 16) / 4.0);
        }
        poseEditor.setPose(direction, getPart(), index);
        if (getVariant().isNotOld()) {
            cell = index % 16;
        } else {
            cell = index % 10;
        }
        spriteCellList.reload();
        redrawPose();
        spriteCellList.scrollTo(cell);
        controller.setSelected(cell);
    }

    private Character getPart() {
        return controller.getPart();
    }

    private CaosVariant getVariant() {
        return controller.getVariant();
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
        attToolbar = new JPanel();
        attToolbar.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.add(attToolbar, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setText("Variant");
        attToolbar.add(label1);
        variantComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("C1");
        defaultComboBoxModel1.addElement("C2");
        defaultComboBoxModel1.addElement("CV");
        defaultComboBoxModel1.addElement("C3+");
        variantComboBox.setModel(defaultComboBoxModel1);
        attToolbar.add(variantComboBox);
        final JSeparator separator1 = new JSeparator();
        attToolbar.add(separator1);
        final JLabel label2 = new JLabel();
        label2.setText("Part");
        attToolbar.add(label2);
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
        attToolbar.add(part);
        final JSeparator separator2 = new JSeparator();
        attToolbar.add(separator2);
        final JLabel label3 = new JLabel();
        label3.setText("Point");
        attToolbar.add(label3);
        point1 = new JRadioButton();
        point1.setText("1");
        attToolbar.add(point1);
        point2 = new JRadioButton();
        point2.setText("2");
        attToolbar.add(point2);
        point3 = new JRadioButton();
        point3.setText("3");
        attToolbar.add(point3);
        point4 = new JRadioButton();
        point4.setText("4");
        attToolbar.add(point4);
        point5 = new JRadioButton();
        point5.setText("5");
        attToolbar.add(point5);
        point6 = new JRadioButton();
        point6.setText("6");
        attToolbar.add(point6);
        labels = new JCheckBox();
        labels.setSelected(true);
        labels.setText("Labels");
        attToolbar.add(labels);
        final JSeparator separator3 = new JSeparator();
        attToolbar.add(separator3);
        final JLabel label4 = new JLabel();
        label4.setText("Scale");
        attToolbar.add(label4);
        attToolbar.add(scale);
        final Spacer spacer1 = new Spacer();
        attToolbar.add(spacer1);
        poseViewCheckbox = new JCheckBox();
        poseViewCheckbox.setText("Pose View");
        attToolbar.add(poseViewCheckbox);
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

    @NotNull
    @Override
    public Object getToolbar() {
        return attToolbar;
    }
}
