package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose;

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.AttEditorPanel;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.AttFileEditorProvider;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.PartBreedsProvider;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.PartVisibility;
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileData;
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosProjectSettingsService;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartFiles;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey;
import com.badahori.creatures.plugins.intellij.agenteering.utils.*;
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem;
import com.bedalton.creatures.common.structs.BreedKey;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.JBColor;
import kotlin.Pair;
import kotlin.Triple;
import kotlin.Unit;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.BreedDataUtil.findBreeds;
import static com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.BreedDataUtil.findMatchingBreedInList;
import static com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseCalculator.HeadPoseData;

public class PoseEditorImpl implements Disposable, BreedPoseHolder, PartBreedsProvider {
    private static final Logger LOGGER = Logger.getLogger("#PoseEditor");
    private static final char[] ALL_PARTS = PoseEditorSupport.getAllParts();
    private final Project project;
    private final BreedPartKey baseBreed;
    private final String[] directions = new String[]{
            "Down",
            "Straight",
            "Up",
            "Far Up"
    };

    private static final List<BodyPartFiles> EMPTY_BODY_PARTS_LIST = Collections.emptyList();
    private final List<PoseChangeListener> poseChangeListeners = Lists.newArrayList();
    private JPanel panel1;
    private JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> headBreed;
    private JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> bodyBreed;
    private JComboBox<String> rightUpperArmPose;
    private JComboBox<String> leftUpperArmPose;
    private JComboBox<String> headPose;
    private JComboBox<String> bodyDirection;
    private JComboBox<String> bodyTilt;
    private JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> armsBreed;
    private JComboBox<String> leftForearmPose;
    private JComboBox<String> rightForearmPose;
    private JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> legsBreed;
    private JComboBox<String> leftThighPose;
    private JComboBox<String> leftShinPose;
    private JComboBox<String> leftFootPose;
    private JComboBox<String> rightThighPose;
    private JComboBox<String> rightShinPose;
    private JComboBox<String> rightFootPose;
    private JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> tailBreed;
    private JComboBox<String> tailTipPose;
    private JComboBox<String> tailBasePose;
    private JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> earBreed;
    private JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> hairBreed;
    private JComboBox<String> zoom;
    private JPanel imageHolder;
    private JComboBox<String> focusMode;
    private JLabel earLabel;
    private JLabel hairLabel;
    private JLabel tailLabel;
    private JLabel tailBaseLabel;
    private JLabel tailTipLabel;
    private JLabel focusModeLabel;
    private JComboBox<String> headDirection2;
    private JComboBox<BodyPartFiles> openRelated;
    private JLabel openRelatedLabel;
    private JComboBox<String> mood;
    private JLabel tiltLabel;
    private JLabel Eyes;
    private JComboBox<String> eyesStatus;
    private JScrollPane partsPanel;
    private JFormattedTextField poseStringField;
    private JButton hidden;
    private JButton ghost;
    private List<BodyPartFiles> files;
    private CaosVariant variant;
    private Pose pose;
    private boolean valid;
    private boolean didInitOnce = false;
    private Character visibilityFocus;
    private VirtualFile rootPath;
    private boolean drawImmediately = false;
    private Pose defaultPoseAfterInit;
    private boolean wasHidden = false;
    private String lastPoseString = "";
    private boolean didInit = false;
    private final boolean eager;
    private boolean shownOnce = false;
    private boolean hasTail;
    private Function<Boolean, Void> onRedrawCallback;
    private final PoseEditorModel model;
    private boolean variantChanged;
    private boolean didInitComboBoxes = false;
    private final CaosProjectSettingsService settings;

    private boolean didRenderOnce = false;
    private Pose nextPose = null;

    private boolean breedChanged = false;

    private boolean justSetString = false;

    private boolean dirty = true;

    private boolean mirrorPose = false;

    private final Map<Character, PartVisibility> visibilityMap = new HashMap<>();

    private final Map<Character, BodyPartFiles> last = new HashMap<>();

    private final Map<Character, Integer> partIncrementDirection = new HashMap<>();

    private JPopupMenu openRelatedPopup;

    private final List<BreedSelectionChangeListener> breedSelectionChangeListeners = new ArrayList<>();

    public PoseEditorImpl(
            @NotNull final Project project,
            @NotNull final Disposable parent,
            @NotNull final CaosVariant variant,
            @NotNull final BreedPartKey breedKey,
            @Nullable final Boolean eager,
            @Nullable final Function<Boolean, Void> onRedraw
    ) {
        if (!project.isDisposed()) {
            Disposer.register(parent, this);
        }
        this.project = project;
        this.variant = variant;
        this.variantChanged = true;
        this.baseBreed = breedKey.copyWithPart(null);
        this.eager = eager != null ? eager : false;
        this.onRedrawCallback = onRedraw;
        this.settings = CaosProjectSettingsService.getInstance(project);
        String folder = CaosStringUtilsKt.randomString(6);
        int max = 10;
        while (--max > 0 && CaosVirtualFileSystem.getInstance().exists(folder)) {
            folder = CaosStringUtilsKt.randomString(6);
        }
        $$$setupUI$$$();
        model = new PoseEditorModel(project, variant, this);
        init();
    }


    public synchronized void init() {
        if (project.isDisposed()) {
            return;
        }
        if (didInit) {
            return;
        }
        didInit = true;
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(() -> this.init(variant));
        } else {
            init(variant);
        }
    }

    public synchronized void isShown() {
        if (this.shownOnce) {
            return;
        }
        this.shownOnce = true;
        init();
    }

    @Override
    public Integer getPartPose(char part) {
        final int facing = this.getFacing();
        return getPartPose(part, facing, model.getFacingOffset(variant, facing));
    }

    @Override
    public void incrementPart(final char part) {
        final JComboBox<String> box = getComboBoxForPart(part);
        if (box == null) {
            return;
        }
        final int currentPose = box.getSelectedIndex();
        int directionMod = partIncrementDirection.getOrDefault(part, 1);
        if (Math.abs(directionMod) != 1) {
            directionMod = 1;
        }
        int newPose = currentPose + directionMod;
        if (newPose < 0) {
            newPose = 1;
            partIncrementDirection.put(part, 1);
        } else if (newPose > 3) {
            newPose = 2;
            partIncrementDirection.put(part, -1);
        }
        setTilt(part, newPose);
    }


    @Override
    public void incrementPart(final char part, final boolean forward) {
        final Integer currentPose = getPartPose(part);
        if (currentPose == null) {
            return;
        }
        // Get increment step value
        final int modifier = forward ? 1 : -1;

        // Calculate next pose
        int newPose = currentPose + modifier;

        // Check that pose is in view
        if (newPose < 0 || newPose > 3) {
            return;
        }

        // Actually set the pose
        setPose(part, newPose);
    }

    public Integer getPartPose(final char part, final int facingDirection) {
        final int offset;
        switch (facingDirection) {
            case 0:
                offset = 0;
                break;
            case 1:
                offset = 4;
                break;
            case 2:
                offset = 8;
                break;
            case 3:
                offset = variant.isOld() ? 9 : 12;
                break;
            default:
                return null;
        }
        return getPartPose(part, facingDirection, offset);
    }

    /**
     * Gets the pose for a given combobox
     *
     * @param part            body part to get part pose for
     * @param facingDirection facing direction of creature
     * @param offset          offset into sprite set
     * @return pose index in sprite file
     */
    public Integer getPartPose(final char part, final int facingDirection, final int offset) {
        final JComboBox<String> box = getComboBoxForPart(part);
        if (box == null) {
            return null;
        }
        return PoseCalculator.getBodyPartPose(variant, box.getSelectedIndex(), facingDirection, offset, false);
    }

    @Override
    public int getBodyPoseActual() {
        final int bodyDirection = getFacing();
        final int tilt = this.bodyTilt.getSelectedIndex();
        return model.getBodyPoseActual(bodyDirection, tilt);
    }

    @Override
    public int getHeadPoseActual() {
        return model.getHeadPoseActual(
                getFacing(),
                (String) headPose.getSelectedItem(),
                headPose.getSelectedIndex(),
                headDirection2.getSelectedIndex(),
                mood.getSelectedIndex(),
                eyesStatus.getSelectedIndex() > 0
        );
    }

    @Override
    public int getZoom() {
        return zoom.getSelectedIndex() + 1;
    }

    @Override
    @NotNull
    public BreedPartKey getBaseBreed() {
        return baseBreed;
    }

    @Override
    @NotNull
    public CaosVariant getVariant() {
        return variant;
    }

    @Override
    public void setMirrorPose(final boolean mirror) {
        this.mirrorPose = mirror;

        // Snap other side to mirror
        final char[] parts = new char[]{'c', 'd', 'e', 'i', 'j'};
        for (final char part : parts) {
            final Integer pose = getPartPose(part);
            if (pose == null) {
                continue;
            }
            mirrorPartPose(part, pose);
        }
    }

    /**
     * Sets the variant and loads the files to use
     *
     * @param variant variant to use for the pose editor
     */
    private void setVariant(final CaosVariant variant) {
        if (project.isDisposed()) {
            return;
        }
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(() -> setVariant(variant));
            return;
        }
        if (this.variant != variant) {
            this.variantChanged = true;
            this.variant = variant;
        }
        final VirtualFile path = rootPath;
        if (path != null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> model.setImmediate(path, baseBreed));
        }
    }

    public JPanel getMainPanel() {
        return panel1;
    }

    // Initializes this pose editor given a variant
    private void init(CaosVariant variant) {
        try {
            setVariant(variant);
        } catch (Exception e) {
            LOGGER.severe("Failed to set variant");
            setVariant(variant);
        }
    }

    private void initUI() {
        partsPanel.getVerticalScrollBar().setUnitIncrement(16);
        if (!eager && !shownOnce) {
            return;
        }
        initComboBoxes();
        addChangeHandlers();
        didInitOnce = true;
        updatePose(ALL_PARTS);
        redraw(ALL_PARTS);
    }

    /**
     * Enables/Disables/Shows/Hides controls for certain variants
     *
     * @param variant variant to set controls for
     */
    private void setVariantControls(CaosVariant variant) {
        variantChanged = false;
        // Only CV uses ear and hair breeds
        if (variant.isNotOld()) {
            freeze(earBreed, false, false);
            freeze(hairBreed, false, false);
            setLabelVisibility('o', true);
            setLabelVisibility('p', true);
            setLabelVisibility('q', true);
        } else {
            // Hide ear and hair data for non-CV variants
            freeze(earBreed, true, true);
            freeze(hairBreed, true, true);
            setLabelVisibility('o', false);
            setLabelVisibility('p', false);
            setLabelVisibility('q', false);
        }

        updateTails();
        // If variant is old, it does not have multiple tilts for front facing and back facing head sprites
//        if (false && variant.isOld()) {
//            // Todo ensure that C1e works using the unified system
//            freeze(headDirection2, true, true);
//            tiltLabel.setVisible(false);
//        } else {
        freeze(headDirection2, !headDirection2.isEnabled(), false);
        tiltLabel.setVisible(true);
//        }
    }

    private void updateTails() {

        final boolean hadTail = hasTail;
        hasTail = variant != CaosVariant.C1.INSTANCE || model.hasTail(files);
        if (hasTail == hadTail && didInitComboBoxes) {
            return;
        }
        // If is C1, hide controls for tails as they are not used in C1
        if (hasTail) {
            // Allow tail controls in all variants other than C1
            setLabelVisibility('m', true);
            setLabelVisibility('n', true);
            tailLabel.setVisible(true);
            freeze(tailBreed, false, false);
            freeze(tailBasePose, false, false);
            freeze(tailTipPose, false, false);
        } else {
            setLabelVisibility('m', false);
            setLabelVisibility('n', false);
            tailLabel.setVisible(false);
            freeze(tailBreed, true, true);
            freeze(tailBasePose, true, true);
            freeze(tailTipPose, true, true);
        }
    }

    /**
     * Inits the combo boxes
     */
    private synchronized void initComboBoxes() {
        if (files == null) {
            throw new RuntimeException("Files is null when initializing combo box");
        }

        if (defaultPoseAfterInit == null) {
            String defaultPose = settings.getDefaultPoseString();
            defaultPoseAfterInit = Pose.fromString(variant, 2, null, defaultPose).getSecond();
        }

        final Pose initialPose = defaultPoseAfterInit;

        if (!didInitComboBoxes) {
            zoom.setSelectedIndex(baseBreed.getAgeGroup() == null || baseBreed.getAgeGroup() >= 2 ? 1 : 2);
        }
        initHeadComboBox(didInitComboBoxes ? headPose.getSelectedIndex() : 1, Integer.MAX_VALUE);

        assign(headDirection2, directions, didInitComboBoxes ? headPose.getSelectedIndex() : 2);

        assign(mood, PoseEditorSupport.getMoodOptions(variant), didInitComboBoxes ? mood.getSelectedIndex() : 0);
//        freeze(headDirection2, !headDirection2.isEnabled(), variant.isOld());


        if (!didInitComboBoxes) {
            setFacing(0);
        }
//        if (false && variant == CaosVariant.C1.INSTANCE && baseBreed.getGenus() != null && baseBreed.getGenus() == 1) {
//            reverse(directions);
//            assign(bodyTilt, directions, didInitComboBoxes ? bodyTilt.getSelectedIndex() : 2);
//            reverse(directions);
//        } else {
        assign(bodyTilt, directions, didInitComboBoxes ? bodyTilt.getSelectedIndex() : 2);
//        }
        assign(focusMode, FocusMode.toStringArray(), didInitComboBoxes ? focusMode.getSelectedIndex() : 0);
        assign(focusMode, FocusMode.toStringArray(), didInitComboBoxes ? focusMode.getSelectedIndex() : 0);
        assign(leftThighPose, directions, didInitComboBoxes ? leftThighPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'c', 2));
        assign(leftShinPose, directions, didInitComboBoxes ? leftShinPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'd', 2));
        assign(leftFootPose, directions, didInitComboBoxes ? leftFootPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'e', 2));
        assign(rightThighPose, directions, didInitComboBoxes ? rightThighPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'f', 2));
        assign(rightShinPose, directions, didInitComboBoxes ? rightShinPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'g', 2));
        assign(rightFootPose, directions, didInitComboBoxes ? rightFootPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'h', 2));
        assign(leftUpperArmPose, directions, didInitComboBoxes ? leftUpperArmPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'i', 1));
        assign(leftForearmPose, directions, didInitComboBoxes ? leftForearmPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'j', 1));
        assign(rightUpperArmPose, directions, didInitComboBoxes ? rightUpperArmPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'k', 1));
        assign(rightForearmPose, directions, didInitComboBoxes ? rightForearmPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'l', 1));
        assign(tailBasePose, directions, didInitComboBoxes ? tailBasePose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'm', 1));
        assign(tailTipPose, directions, didInitComboBoxes ? tailTipPose.getSelectedIndex() : initialPose.getTranslatedForComboBox(variant, 'n', 1));

        // Update the actual breeds list here.
        // Must be done before the ear and hair combo boxes
        updateBreedsList();

        // Decide whether to show ear combo boxes or not

        // Freeze ears if not needed or usable
        if (variant.isNotOld()) {
            // Hide ears if no breeds are available
            if (earBreed.getItemCount() < 1) {
                setLabelVisibility('o', false);
                freeze(earBreed, true, true);
            } else {
                setLabelVisibility('o', true);
                freeze(earBreed, false, false);
            }

            // Hide hair if no breed choices are available
            if (hairBreed.getItemCount() < 1) {
                setLabelVisibility('q', false);
                freeze(hairBreed, true, true);
            } else {
                freeze(hairBreed, false, false);
            }
        } else {
            setLabelVisibility('o', false);
            freeze(earBreed, true, true);
            freeze(hairBreed, false, false);
        }
        didInitComboBoxes = true;
        justSetString = true;
        poseStringField.setText(initialPose.poseString(variant, this.getFacing()));
        justSetString = false;
        initOpenRelatedComboBox();
        initHiddenPartsComboBox();
        initGhostPartsComboBox();
    }


    public void openRelatedWithDialog() {
        if (openRelatedPopup == null) {
            initOpenRelatedPopupMenu();
        }
        final JPanel panel = getMainPanel();
        final int centerX = panel.getWidth() / 2;
        final int centerY = panel.getHeight() / 2;
        final int attemptedWidth = 100;
        final int x = centerX - (attemptedWidth / 2);
        openRelatedPopup.show(panel, x, centerY);
    }

    private void initHiddenPartsComboBox() {
        final VisibilityPopup menu = new VisibilityPopup(this, PartVisibility.HIDDEN);
        hidden.addMouseListener(new MouseListenerBase() {
            @Override
            public void mouseClicked(@NotNull MouseEvent e) {
                menu.updateItems();
                menu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override
            public void mousePressed(@NotNull MouseEvent e) {
                menu.updateItems();
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void initGhostPartsComboBox() {
        final VisibilityPopup menu = new VisibilityPopup(this, PartVisibility.GHOST);
        ghost.addMouseListener(new MouseListenerBase() {
            @Override
            public void mouseClicked(@NotNull MouseEvent e) {
                menu.updateItems();
                menu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override
            public void mousePressed(@NotNull MouseEvent e) {
                menu.updateItems();
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void initOpenRelatedPopupMenu() {
        final List<Character> availableRelatedParts = ComboBoxHelper
                .items(openRelated)
                .stream()
                .filter(Objects::nonNull)
                .map(BodyPartFiles::getPart)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        openRelatedPopup = AttEditorSupport.initOpenRelatedPopup(availableRelatedParts, this, (selectedPart) -> {
            if (selectedPart != null) {
                openRelated(selectedPart);
            }
        });
    }

    private void updateBreedsList() {
        populate(headBreed, files, 'a');
        populate(bodyBreed, files, 'b');
        populate(legsBreed, files, 'c', 'd', 'e', 'f', 'g', 'h');
        populate(armsBreed, files, 'i', 'j', 'k', 'l');
//        populate(tailBreed, files, variant == CaosVariant.C1.INSTANCE, 'm', 'n');
        populate(tailBreed, files, true, 'm', 'n');
        populate(earBreed, files, true, 'o', 'p');
        populate(hairBreed, files, true, 'q');
        populate(hairBreed, files, 'q');
    }

    private void initOpenRelatedComboBox() {
        openRelated.setRenderer(new PartFileCellRenderer(true));
        if (rootPath == null || files == null || files.isEmpty()) {
            freeze(openRelated, true, true);
            openRelatedLabel.setVisible(false);
            return;
        }
        final BreedPartKey key = baseBreed.copyWithPart(null);
        final List<BodyPartFiles> relatedFiles = files.stream()
                .filter((f) -> {
                    if (f == null) {
                        return false;
                    }
                    final BreedPartKey partKey = f.getKey();
                    if (partKey == null || !BreedPartKey.isGenericMatch(key, partKey)) {
                        return false;
                    }
                    // Strict match for root path. Must be in same exact root path
                    // Not just a subdirectory
                    return rootPath.equals(f.getBodyDataFile().getParent());
                })
                .sorted(Comparator.comparing(a -> a.getSpriteFile().getNameWithoutExtension()))
                .collect(Collectors.toList());
        if (relatedFiles.isEmpty()) {
            freeze(openRelated, true, true);
            openRelatedLabel.setVisible(false);
            return;
        }
        final int relatedItemCount = relatedFiles.size();
        relatedFiles.add(0, null);
        if (relatedItemCount >= relatedFiles.size()) {
            LOGGER.severe("Null placeholder file has replaced the first real element in list of related files.");
            freeze(openRelated, true, true);
            openRelatedLabel.setVisible(false);
            return;
        }
        assign(openRelated, relatedFiles, 0);
        freeze(openRelated, false, false);
        openRelatedLabel.setVisible(true);
    }

    /**
     * Adds change handlers to all drop down menus
     */
    private void addChangeHandlers() {

        zoom.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                redraw();
            }
        });

        final Highlighter highlighter = new DefaultHighlighter();
        final PopUp poseContextMenu = new PopUp();
        poseStringField.setHighlighter(highlighter);
        poseStringField.addMouseListener(new MouseListenerBase() {
            @Override
            public void mouseClicked(@NotNull MouseEvent e) {
                poseContextMenu.updateItems();
                if (e.getButton() == MouseEvent.BUTTON3) {
                    poseContextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        poseStringField.getDocument().addDocumentListener(new DocumentChangeListener((final Integer type, final String newPoseRaw) -> {
            if (type == DocumentChangeListener.REMOVE) {
                return Unit.INSTANCE;
            }
            if (!justSetString) {
                dirty = true;
                try {
                    highlighter.removeAllHighlights();
                } catch (Exception e) {
                    LOGGER.severe("Failed to remove highlights");
                }
                setPoseFromString(newPoseRaw);
            }
            return Unit.INSTANCE;
        }));

        focusMode.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                dirty = true;
                redraw();
            }
        });

        bodyDirection.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                dirty = true;
                redraw();
            }
        });

        // Breed listener
        addBreedListener(headBreed, 'a');
        addBreedListener(bodyBreed, 'b');
        addBreedListener(legsBreed, 'c', 'd', 'e', 'f', 'g', 'h');
        addBreedListener(armsBreed, 'i', 'j', 'k', 'l');
        addBreedListener(tailBreed, 'm', 'n');
        addBreedListener(earBreed, 'o', 'p');
        addBreedListener(hairBreed, 'q');

        // Pose Listeners
        addPartListener(headPose, 'a');
        addPartListener(headDirection2, 'a');
        addPartListener(mood, 'a');
        addPartListener(eyesStatus, 'a');
        addPartListener(bodyTilt, 'b');

        headPose.addItemListener((e) -> {
            if (variant.isOld()) {
                try {
                    final boolean isBodyFacingForwardsOrBackwards = getFacing() >= 2;
                    final boolean isLookingForwardsOrBackwards = headPose.getSelectedIndex() == 2 || (isBodyFacingForwardsOrBackwards && headPose.getSelectedIndex() == 1);
                    final boolean justSetStringBefore = justSetString;
                    justSetString = true;
                    if (isLookingForwardsOrBackwards) {
                        headDirection2.setSelectedIndex(2);
                        headDirection2.setEditable(false);
                    } else {
                        headDirection2.setEditable(true);
                    }
                    justSetString = justSetStringBefore;
                } catch (final Exception ignored) {

                }
            }
        });

        bodyDirection.addItemListener((e) -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            final int bodyDirectionIndex = getFacing();
            if (bodyDirectionIndex < 0) {
                return;
            }
            dirty = true;
            if (didInitOnce && bodyDirection.getItemCount() > 0) {
                if (drawImmediately) {
                    redraw(ALL_PARTS);
                }
            }
        });
        addPartListener(leftThighPose, 'c');
        addPartListener(leftShinPose, 'd');
        addPartListener(leftFootPose, 'e');
        addPartListener(rightThighPose, 'f');
        addPartListener(rightShinPose, 'g');
        addPartListener(rightFootPose, 'h');
        addPartListener(leftUpperArmPose, 'i');
        addPartListener(leftForearmPose, 'j');
        addPartListener(rightUpperArmPose, 'k');
        addPartListener(rightForearmPose, 'l');
        addPartListener(tailBasePose, 'm');
        addPartListener(tailTipPose, 'n');

        openRelated.addItemListener((e) -> {
            if (openRelated.getSelectedIndex() == 0) {
                return;
            }
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            final Object fileObject = openRelated.getSelectedItem();
            openRelated.setSelectedIndex(0);

            if (!(fileObject instanceof BodyPartFiles)) {
                if (fileObject != null) {
                    LOGGER.severe("File object is " + fileObject.getClass().getName());
                } else {
                    LOGGER.severe("Related file object is null");
                }
                return;
            }
            final BodyPartFiles file = (BodyPartFiles) fileObject;
            file.getBodyDataFile().putUserData(AttEditorPanel.REQUESTED_POSE_KEY, pose);
            file.getBodyDataFile().putUserData(AttEditorPanel.REQUESTED_VISIBILITY_KEY, visibilityMap);
            final PsiFile psiFile = PsiManager.getInstance(project).findFile(file.getBodyDataFile());
            if (psiFile == null) {
                final DialogBuilder builder = new DialogBuilder();
                builder.setTitle("Open Related Error");
                builder.setErrorText("We failed to open the related ATT editor file. Document not properly resolved.");
                builder.showAndGet();
                return;
            }
            psiFile.navigate(true);
            FileEditorManager.getInstance(project).setSelectedEditor(psiFile.getVirtualFile(), AttFileEditorProvider.EDITOR_TYPE_ID);

        });
    }

    public boolean openRelated(final char part) {
        final int itemCount = openRelated.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            final BodyPartFiles item = openRelated.getItemAt(i);
            if (item == null) {
                continue;
            }
            final BreedPartKey key = item.getKey();
            if (key == null) {
                continue;
            }
            final Character itemPart = key.getPart();
            if (itemPart == null) {
                continue;
            }
            if (itemPart == part) {
                openRelated.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    private void addPartListener(final JComboBox<?> box, final char partChar) {
        box.addItemListener((e) -> {
            if (e.getStateChange() != ItemEvent.SELECTED || box.getSelectedIndex() < 0) {
                return;
            }
            dirty = true;
//            final boolean justSetOriginal = justSetString;
            if (!justSetString) {
                justSetString = true;
                updatePoseStringField(partChar);
                ApplicationManager.getApplication().invokeLater(() -> justSetString = false);
            }
            ApplicationManager.getApplication().invokeLater(() -> {

                if (didInitOnce && box.getItemCount() > 0) {
                    if (drawImmediately) {
                        final int pose = box.getSelectedIndex();
                        if (mirrorPose && pose >= 0) {
                            mirrorPartPose(partChar, pose);
                        }
                        updatePose(partChar);
                        redraw(partChar);
                    }
                }
            });
        });
    }


    private void mirrorPartPose(final char part, final int pose) {
        Character otherPart = getMirrorPart(part);
        if (otherPart == null) {
            return;
        }
        final Integer otherPose = getPartPose(otherPart);
        if (otherPose != null && otherPose == pose) {
            return;
        }
        setPose(otherPart, pose);
    }

    @Nullable
    private Character getMirrorPart(final char part) {
        switch (Character.toLowerCase(part)) {
            case 'c':
                return 'f';
            case 'd':
                return 'g';
            case 'e':
                return 'h';
            case 'f':
                return 'c';
            case 'g':
                return 'd';
            case 'h':
                return 'e';
            case 'i':
                return 'k';
            case 'j':
                return 'l';
            case 'k':
                return 'i';
            case 'l':
                return 'j';
            default:
                return null;
        }
    }

    private void addBreedListener(final JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> box, final char... parts) {
        box.addItemListener((e) -> {
            if (box.getItemCount() < 1) {
                return;
            }

            if (e.getStateChange() != ItemEvent.SELECTED && box.getSelectedIndex() > 0) {
                return;
            }

            final List<BodyPartFiles> theseFiles = getSelectedFiles(box);
            for (final char part : parts) {
                clearManualAttOnChange(theseFiles, part);
            }

            final Optional<BreedPartKey> file = theseFiles.stream()
                    .map(BodyPartFiles::getKey)
                    .filter(Objects::nonNull)
                    .findFirst();
            if (file.isPresent()) {
                final BreedKey breedKey = file.get().getBreedKey();
                for (final BreedSelectionChangeListener listener : breedSelectionChangeListeners) {
                    listener.onBreedSelected(breedKey, parts);
                }
            }

            breedChanged = true;
            dirty = true;

            if (didInitOnce) {
                redraw(parts);
            }
        });
    }

    private List<BodyPartFiles> getSelectedFiles(final JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> box) {
        final Object raw = box.getSelectedItem();
        final List<BodyPartFiles> files;
        if (raw != null) {
            @SuppressWarnings("unchecked")
            final Triple<String, BreedPartKey, List<BodyPartFiles>> selected =
                    (Triple<String, BreedPartKey, List<BodyPartFiles>>) raw;
            files = selected.getThird();
        } else {
            files = Lists.newArrayList();
        }
        return files;
    }

    private void clearManualAttOnChange(List<BodyPartFiles> files, char part) {
        final BodyPartFiles lastFile = last.get(part);
        final BodyPartFiles selectedFile = getSelectedPart(files, part);
        last.put(part, selectedFile);

        final BodyPartFiles partFiles = lastFile != null ? lastFile : selectedFile;
        if (partFiles == null) {
            model.removeManualAtt(part);
        } else {
            model.removeManualAttIfSpriteNotMatching(part, partFiles.getSpriteFile());
        }
    }

    @Nullable
    private BodyPartFiles getSelectedPart(List<BodyPartFiles> files, char part) {
        final Optional<BodyPartFiles> file = files.stream()
                .filter((i) -> {
                    final BreedPartKey key = i.getKey();
                    Character partChar = key != null && key.getPart() == null ? key.getPart() : null;
                    if (partChar == null) {
                        partChar = i.getSpriteFile().getName().charAt(0);
                    }
                    return partChar == part;
                })
                .findFirst();
        return file.orElse(null);
    }

    /**
     * @return <b>True</b> if the last render was successful. <b>False</b> if it was not
     */
    public boolean isValid() {
        return valid;
    }


    public void redrawAll() {
        if (project.isDisposed() || !didInitOnce) {
            return;
        }
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(this::redrawAll);
            return;
        }

        drawImmediately = true;
//        clear();
        if (variant != null) {
            redraw(ALL_PARTS);
        } else {
            redraw(ALL_PARTS);
        }
    }

    public void clear() {
        model.clearSpriteSet();
        if (imageHolder != null) {
            ((PoseRenderedImagePanel) imageHolder).clear();
        }
    }

    public void hardReload() {
        model.hardReload();
    }

    /**
     * Reloads files and then queues redraw as necessary
     *
     * @param parts parts that have been changed
     */
    public void redraw(char... parts) {
        if (project.isDisposed()) {
            return;
        }
        if (!didInitOnce) {
            setVariant(variant);
            return;
        }
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(() -> redraw(parts));
        } else {
            redrawActual(parts);
        }
    }

    private void onRedraw(final boolean valid) {
        this.valid = valid;
        final java.util.function.Function<Boolean, Void> run = this.onRedrawCallback;
        if (run != null) {
            run.apply(valid);
        }
    }

    /**
     * Redraws the pose after updating the given part and breed information
     *
     * @param parts parts that have been changed
     */
    private synchronized void redrawActual(char... parts) {
        if (!didInitOnce) {
            return;
        }
        if (!panel1.isVisible()) {
            wasHidden = true;
            return;
        }
        //noinspection StatementWithEmptyBody
        if (model.getRendering()) {
//            return;
        }
        final char[] theParts;
        if (wasHidden) {
            wasHidden = false;
            theParts = ALL_PARTS;
        } else {
            theParts = parts;
        }
        drawImmediately = true;
        if (project == null || project.isDisposed()) {
            LOGGER.severe("Cannot redraw. Project is disposed");
            return;
        }
        if (files == null || files.isEmpty()) {
            if (DumbService.isDumb(project)) {
                DumbService.getInstance(project).runWhenSmart(() -> redrawActual(theParts));
                wasHidden = true;
            }
            return;
        }
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                model.requestRender(theParts, breedChanged);
                dirty = false;
                breedChanged = false;
            } catch (final Exception e) {
                if (e instanceof ProcessCanceledException) {
                    throw e;
                }
                LOGGER.severe("Failed to render; " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void setRendered(@Nullable BufferedImage image) {
        final boolean rendered = image != null;
        if (rendered) {
            if (!didRenderOnce) {
                didRenderOnce = true;
            }
            final Pose nextPose = this.nextPose;
            this.nextPose = null;
            if (nextPose != null) {
                setPose(nextPose, true);
                return;
            }

            ((PoseRenderedImagePanel) imageHolder).updateImage(image);
        } else {
            ((PoseRenderedImagePanel) imageHolder).setInvalid(true);
        }
        valid = rendered;
        onRedraw(rendered);
    }

    @NotNull
    @Override
    public List<BodyPartFiles> getBreedFiles(final PartGroups group) {
        switch (group) {
            case HEAD:
                return getBreedFiles(headBreed);
            case BODY:
                return getBreedFiles(bodyBreed);
            case LEGS:
                return getBreedFiles(legsBreed);
            case ARMS:
                return getBreedFiles(armsBreed);
            case TAIL:
                return getBreedFiles(tailBreed);
            case EARS:
                return getBreedFiles(earBreed);
            case HAIR:
                return getBreedFiles(hairBreed);
            default:
                throw new RuntimeException("Unexpected body part key: " + group.name().toLowerCase());
        }
    }

    @NotNull
    private List<BodyPartFiles> getBreedFiles(JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> comboBox) {
        final Object item = comboBox.getSelectedItem();
        if (item == null) {
            return EMPTY_BODY_PARTS_LIST;
        }
        if (!(item instanceof Triple)) {
            LOGGER.severe("Unexpected item type: " + item.getClass().getSimpleName());
            return EMPTY_BODY_PARTS_LIST;
        }
        final Object third = ((Triple<?, ?, ?>) item).getThird();
        if (!(third instanceof List)) {
            LOGGER.severe("Third parameter is not a list but was: " + (third != null ? third.getClass().getSimpleName() : "null"));
            return EMPTY_BODY_PARTS_LIST;
        }
        //noinspection unchecked
        return (List<BodyPartFiles>) third;
    }

    @Override
    public void updatePose(final char @NotNull ... parts) {
        final Pose oldPose = pose;
        final Pose newPose = PoseCalculator.getUpdatedPose(
                null,
                variant,
                oldPose,
                getFacing(),
                this,
                parts
        );

        if (oldPose != null && newPose.hashCode() == oldPose.hashCode()) {
            return;
        }
        pose = newPose;
        if (dirty) {
            notifyChangeListeners();
        }
    }

    @Override
    @NotNull
    public Pose getPose(final ProgressIndicator progressIndicator, final char @NotNull ... parts) {
        return PoseCalculator.getUpdatedPose(
                progressIndicator,
                variant,
                pose,
                getFacing(),
                this,
                parts
        );
    }

    @Override
    public Map<Character, PartVisibility> getVisibilityMask() {
        final Map<Character, PartVisibility> mask = new HashMap<>();
        applyVisibilityMask(FocusModeHelper.getVisibilityMask(focusMode.getSelectedIndex(), visibilityFocus), mask);
        applyVisibilityMask(visibilityMap, mask);
        return mask;
    }

    public void applyVisibilityMask(
            @Nullable final Map<Character, PartVisibility> from,
            @NotNull final Map<Character, PartVisibility> into
    ) {
        if (from == null) {
            return;
        }
        Set<Map.Entry<Character, PartVisibility>> entries = from.entrySet();
        if (entries.isEmpty()) {
            return;
        }
        for (final Map.Entry<Character, PartVisibility> entry : entries) {
            into.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Sets the att file data manually for a given part
     *
     * @param part part to update att data for
     * @param att  new att file data
     */
    public void setAtt(char part, VirtualFile spriteFile, AttFileData att) {
        final JComboBox<String> comboBox = getComboBoxForPart(part);
        if (comboBox == null) {
            return;
        }
        String breedString = String.valueOf(part) + comboBox.getSelectedItem();
        model.setManualAtt(
                part,
                breedString,
                spriteFile,
                att
        );

    }


    /**
     * Freezes/Unfreezes controls and potentially hides them
     *
     * @param box    Box to freeze control of
     * @param freeze whether to freeze(make readonly)
     * @param hide   whether to hide the control after freezing
     */
    private void freeze(JComboBox<?> box, boolean freeze, Boolean hide) {
        if (box == null) {
            return;
        }
        box.setEnabled(!freeze);
        if (hide != null) {
            box.setVisible(!hide);
        }
    }

    /**
     * Convenience method to freeze/unfreeze a control given its part char
     *
     * @param part   part to alter state of
     * @param freeze whether to freeze or unfreeze this part
     */
    public void freezeBreedForPart(char part, Boolean freeze) {
        final JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> poseComboBox = getComboBoxForBreed(part);
        if (poseComboBox != null) {
            freeze(poseComboBox, freeze, null);
        }
    }

    /**
     * Convenience method to freeze/unfreeze a control given its part char
     *
     * @param part   part to alter state of
     * @param freeze whether to freeze or unfreeze this part
     */
    public void freeze(char part, Boolean freeze) {
        final JComboBox<String> poseComboBox = getComboBoxForPart(part);
        if (poseComboBox != null) {
            freeze(poseComboBox, freeze, null);
        }
        if (part == 'a') {
            freeze(headDirection2, freeze, null);
        }
        if (part == 'b') {
            freeze(bodyDirection, freeze, null);
        }
    }

    /**
     * Gets the Pose combo box given a part char
     *
     * @param part body part char
     * @return Combo box for the pose control
     */
    @Nullable
    private JComboBox<String> getComboBoxForPart(char part) {
        switch (part) {
            case 'a':
                return headDirection2;
            case 'b':
                return bodyTilt;
            case 'c':
                return leftThighPose;
            case 'd':
                return leftShinPose;
            case 'e':
                return leftFootPose;
            case 'f':
                return rightThighPose;
            case 'g':
                return rightShinPose;
            case 'h':
                return rightFootPose;
            case 'i':
                return leftUpperArmPose;
            case 'j':
                return leftForearmPose;
            case 'k':
                return rightUpperArmPose;
            case 'l':
                return rightForearmPose;
            case 'm':
                return tailBasePose;
            case 'n':
                return tailTipPose;
            case 'o':
            case 'p':
            case 'q':
            case 'z':
                return null;

            default:
                throw new IndexOutOfBoundsException("Part: " + part + " is not a valid body part char");
        }
    }

    /**
     * Sets visibility of labels for a few optional controls
     *
     * @param part part of label to show/hide
     * @param show whether to show or hide label
     */
    private void setLabelVisibility(char part, boolean show) {
        switch (part) {
            case 'o':
            case 'p':
                earLabel.setVisible(show);
                break;
            case 'q':
                hairLabel.setVisible(show);
            case 'm':
                tailBaseLabel.setVisible(show);
                break;
            case 'n':
                tailTipLabel.setVisible(show);
            default:
                break;
        }
    }

    /**
     * Gets the breed combo box corresponding to the part
     *
     * @param part body part char
     * @return breed combo box for the given part
     */
    private JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> getComboBoxForBreed(char part) {
        switch (part) {
            case 'a':
                return headBreed;
            case 'b':
                return bodyBreed;
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
                return legsBreed;
            case 'i':
            case 'j':
            case 'k':
            case 'l':
                return armsBreed;
            case 'm':
            case 'n':
                return tailBreed;
            case 'o':
            case 'p':
                return earBreed;
            case 'q':
                return hairBreed;
            case 'z':
                return null;
            default:
                LOGGER.severe("Part: " + part + " is not a valid body part char");
                return null;
        }
    }

    private <T> void assign(JComboBox<T> menu, T[] items, int selectedIndex) {
        assign(menu, Arrays.stream(items).collect(Collectors.toList()), selectedIndex);
    }

    /**
     * Assigns a set of values to a combo box
     *
     * @param menu          combo box to populate
     * @param items         items to populate combo box with
     * @param selectedIndex index to select after filling in items
     * @param <T>           The kind of values to fill this combo box with
     */
    private <T> void assign(JComboBox<T> menu, List<T> items, int selectedIndex) {
        menu.removeAllItems();
        for (T item : items) {
            try {
                menu.addItem(item);
            } catch (Exception e) {
                LOGGER.warning("Failed to set menu item: " + item.toString() + " into combo box");
            }
        }
        final int menuItemCount = menu.getItemCount();
        if (menuItemCount != items.size()) {
            LOGGER.severe("Menu does not have all available items; Expected: " + items.size() + "; Actual: " + menuItemCount);
        }
        if (menuItemCount > selectedIndex) {
            menu.setSelectedIndex(selectedIndex);
        } else if (menuItemCount > 0) {
            menu.setSelectedIndex(0);
        }
    }

    /**
     * Populates a breed combo box with available breed files
     *
     * @param menu      breed combo box
     * @param files     a list of all available body data regardless of actual part
     * @param partChars parts to filter breeds by
     */
    private void populate(JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> menu, final List<BodyPartFiles> files, Character... partChars) {
        boolean allowNull = false;
        for (char part : partChars) {
            if (part >= 'm') {
                allowNull = true;
                break;
            }
        }
        populate(menu, files, allowNull, partChars);
    }

    /**
     * Populates a breed combo box with available breed files
     *
     * @param menu      breed combo box
     * @param files     a list of all available body data regardless of actual part
     * @param partChars parts to filter breeds by
     */
    private void populate(JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> menu, final List<BodyPartFiles> files, boolean allowNull, Character... partChars) {
        if (files == null) {
            if (Arrays.stream(partChars).noneMatch(c -> Character.toLowerCase(c) == 'a')) {
                // Only allow setting immediate through the head part ('a')
                return;
            }
            // Request the immediate sibling files for this part
            final VirtualFile path = rootPath;
            if (path != null) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> model.setImmediate(path, baseBreed));
            }
            return;
        }
        // Set the cell renderer for the Att file list
        menu.setRenderer(new BreedFileCellRenderer());
        final Object temp = menu.getSelectedItem();
        @SuppressWarnings("unchecked")
        final Triple<String, BreedPartKey, List<BodyPartFiles>> selected = temp instanceof Triple
                ? (Triple<String, BreedPartKey, List<BodyPartFiles>>) temp
                : null;

        // Filter list of body part files for breeds applicable to this list of parts
        final List<Triple<String, BreedPartKey, List<BodyPartFiles>>> items = findBreeds(files, baseBreed, partChars);

        if (allowNull) {
            items.add(0, null);
            if (items.size() == 1) {
                return;
            }
        } else if (items.size() < 1) {
            return;
        }
        // Assign values to this drop down
        assign(menu, items, 0);

        if (selected != null && items.contains(selected)) {
            menu.setSelectedItem(selected);
            return;
        }

        // Scope files to use in the pose editor
        final Module module = rootPath != null ? CaosFileUtilKt.getModule(rootPath, project) : null;
        final GlobalSearchScope scope;
        if (module != null) {
            scope = null;//module.getModuleScope();
        } else {
            scope = GlobalSearchScope.projectScope(project);
        }
        final Integer matchingBreedInList = findMatchingBreedInList(variant, items, rootPath, baseBreed, allowNull, scope);

        if (matchingBreedInList != null) {
            if (allowNull) {
                final Triple<String, BreedPartKey, List<BodyPartFiles>> match = matchingBreedInList >= 0 && matchingBreedInList < items.size()
                        ? items.get(matchingBreedInList)
                        : null;
                final BreedPartKey breed = match != null ? match.getSecond() : null;
                final BreedPartKey checkBreed = breed != null ? breed
                        .copyWithPart(null)
                        .copyWithAgeGroup(null)
                        : null;
                if (checkBreed == null || !BreedPartKey.isGenericMatch(checkBreed, baseBreed)) {
                    menu.setSelectedIndex(0);
                    return;
                }
            }
            menu.setSelectedIndex(matchingBreedInList);
            return;
        }

        // Nothing was found, so just select the first item in the list
        menu.setSelectedIndex(0);
    }


    /**
     * Sets the root path for all related sprite and att files
     * There was a problem when multiple files exist with the same name in different folders
     * This matches the breed files to those in the same directory
     *
     * @param path parent path for all related breed files
     */
    public void setRootPath(final VirtualFile path) {
        if (project.isDisposed()) {
            return;
        }
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(() -> {
                if (project.isDisposed()) {
                    return;
                }
                setRootPath(path);
            });
            return;
        }

        this.rootPath = path;
        final String lastDirectory = ((PoseRenderedImagePanel) imageHolder).getLastDirectory();
        if (lastDirectory == null || lastDirectory.length() < 1) {
            ((PoseRenderedImagePanel) imageHolder).setLastDirectory(path.getPath());
        }
        if (didInitOnce) {
            drawImmediately = false;
            initComboBoxes();
            updatePose(ALL_PARTS);
            drawImmediately = true;
            redraw(ALL_PARTS);
        }
    }

    /**
     * Sets facing direction for the pose renderer
     *
     * @param direction direction that the creature will be facing
     */
    public void setFacing(int direction) {
        final int oldFacing = getFacing();
        if (direction != oldFacing) {
            bodyDirection.setSelectedIndex(direction);
            dirty = true;
        }
        final int oldDirection = getFacing();
        if (oldDirection != direction) {
            dirty = true;
            bodyDirection.setSelectedIndex(direction);
        }
        initHeadComboBox(direction, oldFacing);
        try {
            resetIfNeeded();
            updatePose(ALL_PARTS);
        } catch (Exception e) {
            LOGGER.severe("Failed to reset pose combo boxes; " + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        if (drawImmediately) {
            redraw(ALL_PARTS);
        }
    }

    /**
     * Resets pose if needed
     * Pose gets weird if file is resumed from front facing sprite, but another direction is then clicked on
     */
    public void resetIfNeeded() {
        int resetCount = 0;
        for (char part : ALL_PARTS) {
            final JComboBox<String> box = getComboBoxForPart(part);
            if (box == null || box.getItemCount() < 1 || box.getSelectedIndex() == 0) {
                resetCount++;
            }
        }

        if (defaultPoseAfterInit == null) {
            String defaultPose = settings.getDefaultPoseString();
            defaultPoseAfterInit = Pose.fromString(variant, 2, null, defaultPose).getSecond();
        }
        final Pose nextPose = defaultPoseAfterInit;
        if (resetCount > (ALL_PARTS.length - 3) && nextPose != null) {
            int bodyFacing = nextPose.getBodyFacing(variant);
            switch (bodyFacing) {
                case 0:
                    bodyFacing = 3;
                    break;
                case 1:
                    bodyFacing = 2;
                    break;
                case 2:
                    bodyFacing = 0;
                    break;
                case 3:
                    bodyFacing = 1;
                    break;
                default:
                    throw new IndexOutOfBoundsException("Unexpected body index; Expected 0..3; Found: " + bodyFacing);
            }
            final boolean drawImmediatelyBefore = drawImmediately;
            drawImmediately = false;
            bodyDirection.setSelectedIndex(bodyFacing);
//            setHeadPose(bodyFacing, pose.getHead());
            for (final char partChar : ALL_PARTS) {
                if (partChar == 'a' || partChar == 'b') {
                    continue;
                }
                final Integer partPose = defaultPoseAfterInit.get(partChar);
                if (partPose != null) {
                    setPose(partChar, partPose);
                }
            }
            drawImmediately = drawImmediately || drawImmediatelyBefore;
        }
    }

    /**
     * Initializes the head combo box according to variant and direction
     *
     * @param direction    facing direction
     * @param oldDirection the last direction that was being faced
     */
    public void initHeadComboBox(final int direction, final int oldDirection) {
        final Pair<String[], Integer> data = PoseCalculator.getHeadComboBoxOptions(variant, direction, oldDirection);
        if (data == null) {
            return;
        }
        assign(headPose, data.getFirst(), data.getSecond());
    }

    /**
     * Sets the pose manually given a facing direction, a part and a pose
     *
     * @param facing   selected direction index
     * @param charPart part to set
     * @param pose     pose to set part to
     */
    public void setPose(final int facing, final char charPart, final int pose, final boolean updatePoseString) {
        if (!didInitOnce) {
            return;
        }
        setFacing(facing);
        setPose(charPart, pose);
        if (updatePoseString) {
            updatePoseStringField(charPart);
        }
    }

    private void updatePoseStringField(final char @NotNull ... chars) {
        final String oldPoseString = poseStringField.getText().trim();
        final String newPoseString = getPose(null, chars).poseString(variant, getFacing());
        if (oldPoseString.equalsIgnoreCase(newPoseString)) {
            return;
        }
        poseStringField.setText(newPoseString);

    }

    public synchronized void setNextPose(Pose pose) {
        if (didRenderOnce) {
            setPose(pose, true);
        } else {
            nextPose = pose;
        }
    }

    /**
     * Sets the whole body's pose at once.
     *
     * @param pose the full body pose
     */
    public synchronized void setPose(Pose pose, boolean setFacing) {
        drawImmediately = false;
        final int bodyPose = pose.getBody();
        final Integer facing = model.getTrueFacing(bodyPose);
        if (facing == null) {
            drawImmediately = true;
            return;
        }
        if (setFacing) {
            if (facing != getFacing()) {
                dirty = true;
            }
            setFacing(facing);
        }

        for (char part : ALL_PARTS) {
            dirty = false;
            if (part == 'a') {
                setHeadPose(facing, pose.getHead());
            } else {
                final Integer partPose = pose.get(part);
                if (partPose == null) {
                    continue;
                }
                setPose(part, partPose);
            }
        }
        this.pose = pose;
        drawImmediately = true;
//        if (dirty) {
        redrawAll();
//        }
        if (variant.isOld() && pose.getBody() >= 8) {
            this.pose = null;
        }
        if (dirty) {
            notifyChangeListeners();
        }
    }

    private void setHeadPose(final int facing, final int pose) {
        if (pose < 0) {
            LOGGER.severe("Head pose is negative");
            return;
        }
        final HeadPoseData headPoseData = PoseCalculator.getHeadPose(variant, facing, pose);

//        final boolean isForwardBackwardOnC1e = headPoseData.getDirection() >= 2 && variant.isOld();

        if (headPose.getSelectedIndex() != headPoseData.getDirection()) {
            if (headPose.getModel().getSize() > headPoseData.getDirection()) {
                dirty = true;
                headPose.setSelectedIndex(headPoseData.getDirection());
            } else {
                LOGGER.severe("Head pose combo-box is not initialized. Too few options. Expected < " + headPose.getModel().getSize() + "; Got: " + headPoseData.getDirection());
            }
        }
        if (headPoseData.getTilt() != null) {
            if (headPoseData.getTilt() >= 4) {
                LOGGER.severe("Tilt invalid. Pose: " + pose + "; Data: " + headPoseData);
            } else {
                final int tiltIndex = headPoseData.getTilt();
                if (headDirection2.getSelectedIndex() != tiltIndex) {
                    dirty = true;
                    headDirection2.setSelectedIndex(tiltIndex);
                }
            }
        }
        final Integer moodIndex = headPoseData.getMood();
        if (moodIndex != null && mood.getItemCount() > moodIndex) {
            if (moodIndex != mood.getSelectedIndex()) {
                dirty = true;
                mood.setSelectedIndex(moodIndex);
            }
        }
    }

    /**
     * Sets the part to center focus modes around
     *
     * @param partChar part to focus on
     */
    public void setVisibilityFocus(char partChar) {
        visibilityFocus = partChar;
        redraw(partChar);
    }

    public void setLocked(final Map<Character, BodyPartFiles> locked) {
        model.setLocked(locked);
    }

    public void togglePartVisibility(final char part, final PartVisibility visibility) {
        final PartVisibility currentVisibility = this.visibilityMap.get(part);
        if (part == 'x') {
            // Toggle all parts on or off
            final boolean toggleOn = Arrays.stream(PoseEditorModel.getAllPartsChars().toArray(new Character[0]))
                    .noneMatch(this.visibilityMap::containsKey);
            if (toggleOn) {
                // Toggle all points on
                for (final Character p : PoseEditorModel.getAllPartsChars()) {
                    this.visibilityMap.put(p, visibility);
                }
            } else {
                // Toggle all points off
                this.visibilityMap.clear();
            }
        } else if (currentVisibility == null || currentVisibility != visibility) {
            // Toggle visibility on
            this.visibilityMap.put(part, visibility);
        } else {
            // Toggle visibility off
            this.visibilityMap.remove(part);
        }
        redraw(part);
    }

    /**
     * Sets the pose for a body part directly
     * Alters the items in the corresponding dropdown accordingly
     *
     * @param partChar part to set the pose for
     * @param tilt     the pose to apply
     */
    public void setTilt(final char partChar, int tilt) {
        final JComboBox<String> box = getComboBoxForPart(partChar);
        if (box == null) {
            return;
        }
        final int itemCount = box.getItemCount();
        if (itemCount > tilt) {
            box.setSelectedIndex(tilt);
        }
        redraw(partChar);
//        if (!didInitOnce) {
//            return;
//        }
//
//        final int bodyFacing = getFacing();
//        Integer pose;
//        if (partChar == 'a') {
//            pose = model.getHeadPoseActual(
//                    getFacing(),
//                    (String) headPose.getSelectedItem(),
//                    tilt,
//                    headDirection2.getSelectedIndex(),
//                    mood.getSelectedIndex(),
//                    eyesStatus.getSelectedIndex() > 0
//            );
//        } else {
//            pose = getPartPose(partChar, tilt, bodyFacing);
//        }
//        if (pose == null) {
//            return;
//        }
//        setPose(partChar, pose);
    }

    /**
     * Sets the pose for a body part directly
     * Alters the items in the corresponding dropdown accordingly
     *
     * @param partChar part to set the pose for
     * @param pose     the pose to apply
     */
    public void setPose(final char partChar, int pose) {
        if (!didInitOnce) {
            return;
        }

        if (pose < 0) {
            return;
        }
        if (partChar == 'a') {
            setHeadPose(getFacing(), pose);
            return;
        }
        if (partChar == 'b') {
            int newBodyDirection;
            if (variant.isOld()) {
                if (pose > 9) {
                    LOGGER.severe("Cannot set body pose for " + variant + " to ");
                    return;
                }
                if (pose < 4) {
                    newBodyDirection = 0;
                } else if (pose < 8) {
                    newBodyDirection = 1;
                } else if (pose == 8) {
                    newBodyDirection = 2;
                } else {
                    newBodyDirection = 3;
                }
            } else if (pose > 15) {
                LOGGER.severe("Cannot set body pose for " + variant + " to ");
                return;
            } else {
                newBodyDirection = (int) Math.floor(pose / 4.0);
            }
            if (newBodyDirection != getFacing()) {
                dirty = true;
                bodyDirection.setSelectedIndex(newBodyDirection);
            }
        }
        JComboBox<String> comboBox = getComboBoxForPart(partChar);
        if (comboBox == null) {
            return;
        }
        // If variant is old, any part facing front but face forces all other parts front
        if (variant.isOld()) {
            if (comboBox.getItemCount() == 1) {
                // Pose is neither front nor back, so fill in directions if not already filled in
                assign(bodyTilt, directions, 1);
            }
            if (pose < 8) {
                pose = pose % 4;
            } else {
                pose = comboBox.getSelectedIndex();
            }
        } else {
            pose = pose % 4;
        }

        if (comboBox.getItemCount() == 0) {
            LOGGER.severe("No items in combo box: " + comboBox.getToolTipText());
            return;
        }


        // Select the pose in the dropdown box
        if (pose >= comboBox.getItemCount()) {
            LOGGER.severe("Part " + partChar + " pose '" + pose + "' is greater than options (" + comboBox.getItemCount() + ")");
            pose = 0;
        }
        if (comboBox.getSelectedIndex() != pose) {
            dirty = true;
            comboBox.setSelectedIndex(pose);
        }

        // Redraw the image
        if (drawImmediately) { // && dirty) {
            updatePose(partChar);
            redraw(partChar);
        }
    }

    private void createUIComponents() {
        imageHolder = new PoseRenderedImagePanel(project, project.getProjectFilePath());
    }

    private void notifyChangeListeners() {
        final Pose pose = getPose(null);
        poseChangeListeners.forEach((listener) -> listener.onPoseChange(pose));
    }

    public void addPoseChangeListener(final boolean updateImmediately, final PoseChangeListener listener) {
        poseChangeListeners.add(listener);
        if (updateImmediately) {
            listener.onPoseChange(getPose(null));
        }
    }

    @Override
    public void dispose() {
        poseChangeListeners.clear();
        breedSelectionChangeListeners.clear();
    }

    private int getFacing() {
        if (didInitComboBoxes) {
            return this.bodyDirection.getSelectedIndex();
        }
        return 0;
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
        panel1.setMinimumSize(new Dimension(320, 269));
        panel1.setPreferredSize(new Dimension(320, 600));
        partsPanel = new JScrollPane();
        partsPanel.setMinimumSize(new Dimension(260, 19));
        panel1.add(partsPanel, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        panel2.setMinimumSize(new Dimension(260, 650));
        panel2.setPreferredSize(new Dimension(260, 650));
        partsPanel.setViewportView(panel2);
        final JPanel spacer1 = new JPanel();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 13;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(spacer1, gbc);
        headBreed = new JComboBox();
        headBreed.setMinimumSize(new Dimension(100, 25));
        headBreed.setPreferredSize(new Dimension(100, 25));
        headBreed.setToolTipText("Head Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(headBreed, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("Right Arm");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 17;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label1, gbc);
        leftUpperArmPose = new JComboBox();
        leftUpperArmPose.setMinimumSize(new Dimension(84, 25));
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        leftUpperArmPose.setModel(defaultComboBoxModel1);
        leftUpperArmPose.setPreferredSize(new Dimension(50, 25));
        leftUpperArmPose.setToolTipText("Left upper arm pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 18;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(leftUpperArmPose, gbc);
        rightUpperArmPose = new JComboBox();
        rightUpperArmPose.setMinimumSize(new Dimension(84, 25));
        rightUpperArmPose.setPreferredSize(new Dimension(76, 25));
        rightUpperArmPose.setToolTipText("Right upper arm pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 18;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(rightUpperArmPose, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Left Arm");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 17;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label2, gbc);
        leftForearmPose = new JComboBox();
        leftForearmPose.setMinimumSize(new Dimension(84, 25));
        leftForearmPose.setPreferredSize(new Dimension(76, 25));
        leftForearmPose.setToolTipText("Left forearm and hand pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 19;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(leftForearmPose, gbc);
        rightForearmPose = new JComboBox();
        rightForearmPose.setMinimumSize(new Dimension(84, 25));
        rightForearmPose.setPreferredSize(new Dimension(76, 25));
        rightForearmPose.setToolTipText("Right forearm and hand pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 19;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(rightForearmPose, gbc);
        final JLabel label3 = new JLabel();
        label3.setText("Legs");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 21;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label3, gbc);
        legsBreed = new JComboBox();
        legsBreed.setMinimumSize(new Dimension(84, 25));
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        legsBreed.setModel(defaultComboBoxModel2);
        legsBreed.setPreferredSize(new Dimension(76, 25));
        legsBreed.setToolTipText("Legs Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 21;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(legsBreed, gbc);
        leftThighPose = new JComboBox();
        leftThighPose.setMinimumSize(new Dimension(81, 25));
        leftThighPose.setPreferredSize(new Dimension(76, 25));
        leftThighPose.setToolTipText("Left thigh pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 23;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(leftThighPose, gbc);
        leftShinPose = new JComboBox();
        leftShinPose.setMinimumSize(new Dimension(84, 25));
        leftShinPose.setPreferredSize(new Dimension(76, 25));
        leftShinPose.setToolTipText("left shin pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 24;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(leftShinPose, gbc);
        leftFootPose = new JComboBox();
        leftFootPose.setMinimumSize(new Dimension(84, 25));
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        leftFootPose.setModel(defaultComboBoxModel3);
        leftFootPose.setPreferredSize(new Dimension(76, 25));
        leftFootPose.setToolTipText("left foot pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 25;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(leftFootPose, gbc);
        rightThighPose = new JComboBox();
        rightThighPose.setMinimumSize(new Dimension(84, 25));
        rightThighPose.setPreferredSize(new Dimension(76, 25));
        rightThighPose.setToolTipText("Right thigh pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 23;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(rightThighPose, gbc);
        rightShinPose = new JComboBox();
        rightShinPose.setMinimumSize(new Dimension(84, 25));
        rightShinPose.setPreferredSize(new Dimension(76, 25));
        rightShinPose.setToolTipText("Right shin pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 24;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(rightShinPose, gbc);
        rightFootPose = new JComboBox();
        rightFootPose.setMinimumSize(new Dimension(84, 25));
        rightFootPose.setPreferredSize(new Dimension(76, 25));
        rightFootPose.setToolTipText("Right foot pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 25;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(rightFootPose, gbc);
        final JLabel label4 = new JLabel();
        label4.setText("Left Leg");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 22;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label4, gbc);
        final JLabel label5 = new JLabel();
        label5.setText("Head");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label5, gbc);
        final JLabel label6 = new JLabel();
        label6.setText("Body");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label6, gbc);
        headPose = new JComboBox();
        headPose.setMinimumSize(new Dimension(84, 25));
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        defaultComboBoxModel4.addElement("Far Up");
        defaultComboBoxModel4.addElement("Up");
        defaultComboBoxModel4.addElement("Straight");
        defaultComboBoxModel4.addElement("Down");
        defaultComboBoxModel4.addElement("Forward");
        defaultComboBoxModel4.addElement("Back");
        headPose.setModel(defaultComboBoxModel4);
        headPose.setPreferredSize(new Dimension(76, 25));
        headPose.setToolTipText("Head Pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 8;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(headPose, gbc);
        bodyBreed = new JComboBox();
        bodyBreed.setMinimumSize(new Dimension(80, 25));
        final DefaultComboBoxModel defaultComboBoxModel5 = new DefaultComboBoxModel();
        bodyBreed.setModel(defaultComboBoxModel5);
        bodyBreed.setPreferredSize(new Dimension(100, 25));
        bodyBreed.setToolTipText("Body Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(bodyBreed, gbc);
        earBreed = new JComboBox();
        earBreed.setMinimumSize(new Dimension(81, 25));
        final DefaultComboBoxModel defaultComboBoxModel6 = new DefaultComboBoxModel();
        earBreed.setModel(defaultComboBoxModel6);
        earBreed.setPreferredSize(new Dimension(76, 25));
        earBreed.setToolTipText("Hair Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 13;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(earBreed, gbc);
        hairBreed = new JComboBox();
        hairBreed.setMinimumSize(new Dimension(84, 25));
        hairBreed.setPreferredSize(new Dimension(76, 25));
        hairBreed.setToolTipText("Ear Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 14;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(hairBreed, gbc);
        armsBreed = new JComboBox();
        armsBreed.setMinimumSize(new Dimension(84, 25));
        final DefaultComboBoxModel defaultComboBoxModel7 = new DefaultComboBoxModel();
        armsBreed.setModel(defaultComboBoxModel7);
        armsBreed.setPreferredSize(new Dimension(76, 25));
        armsBreed.setToolTipText("Arms Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 16;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(armsBreed, gbc);
        final JLabel label7 = new JLabel();
        label7.setText("Arms");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 16;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label7, gbc);
        tailLabel = new JLabel();
        tailLabel.setText("Tail");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 27;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(tailLabel, gbc);
        tailBreed = new JComboBox();
        tailBreed.setMinimumSize(new Dimension(84, 25));
        tailBreed.setPreferredSize(new Dimension(76, 25));
        tailBreed.setToolTipText("Tail Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 27;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(tailBreed, gbc);
        tailBasePose = new JComboBox();
        tailBasePose.setMinimumSize(new Dimension(84, 25));
        tailBasePose.setPreferredSize(new Dimension(76, 25));
        tailBasePose.setToolTipText("Tail base pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 28;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(tailBasePose, gbc);
        tailTipPose = new JComboBox();
        tailTipPose.setMinimumSize(new Dimension(84, 25));
        tailTipPose.setPreferredSize(new Dimension(76, 25));
        tailTipPose.setToolTipText("Tail tip pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 29;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(tailTipPose, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 26;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel2.add(spacer2, gbc);
        final JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 20;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel2.add(spacer3, gbc);
        final JPanel spacer4 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 15;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel2.add(spacer4, gbc);
        final JLabel label8 = new JLabel();
        label8.setText("Right Leg");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 22;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label8, gbc);
        final JLabel label9 = new JLabel();
        label9.setText("Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label9, gbc);
        final JLabel label10 = new JLabel();
        label10.setText("Direction");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label10, gbc);
        earLabel = new JLabel();
        earLabel.setText("Ears");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 13;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(earLabel, gbc);
        hairLabel = new JLabel();
        hairLabel.setText("Hair");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(hairLabel, gbc);
        final JLabel label11 = new JLabel();
        label11.setText("Up. Arm");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 18;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label11, gbc);
        final JLabel label12 = new JLabel();
        label12.setText("Forearm");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 19;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label12, gbc);
        final JLabel label13 = new JLabel();
        label13.setText("Thigh");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 23;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label13, gbc);
        final JLabel label14 = new JLabel();
        label14.setText("Shin");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 24;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label14, gbc);
        final JLabel label15 = new JLabel();
        label15.setText("Foot");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 25;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label15, gbc);
        tailBaseLabel = new JLabel();
        tailBaseLabel.setText("Base");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 28;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(tailBaseLabel, gbc);
        tailTipLabel = new JLabel();
        tailTipLabel.setMaximumSize(new Dimension(100, 30));
        tailTipLabel.setMinimumSize(new Dimension(40, 16));
        tailTipLabel.setPreferredSize(new Dimension(40, 20));
        tailTipLabel.setText("Tip");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 29;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(tailTipLabel, gbc);
        final JLabel label16 = new JLabel();
        label16.setText("Zoom");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label16, gbc);
        zoom = new JComboBox();
        zoom.setMinimumSize(new Dimension(84, 45));
        final DefaultComboBoxModel defaultComboBoxModel9 = new DefaultComboBoxModel();
        defaultComboBoxModel9.addElement("1x");
        defaultComboBoxModel9.addElement("2x");
        defaultComboBoxModel9.addElement("3x");
        defaultComboBoxModel9.addElement("4x");
        defaultComboBoxModel9.addElement("5x");
        zoom.setModel(defaultComboBoxModel9);
        zoom.setPreferredSize(new Dimension(78, 25));
        zoom.setToolTipText("Zoom");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(zoom, gbc);
        focusModeLabel = new JLabel();
        focusModeLabel.setText("F.Mode");
        focusModeLabel.setToolTipText("Focus Mode");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(focusModeLabel, gbc);
        focusMode = new JComboBox();
        focusMode.setMinimumSize(new Dimension(84, 25));
        final DefaultComboBoxModel defaultComboBoxModel10 = new DefaultComboBoxModel();
        defaultComboBoxModel10.addElement("Everything");
        defaultComboBoxModel10.addElement("Ghost");
        defaultComboBoxModel10.addElement("Ghost (Solo)");
        defaultComboBoxModel10.addElement("Solo");
        defaultComboBoxModel10.addElement("Solo (With Body)");
        defaultComboBoxModel10.addElement("Solo (Ghost Body)");
        focusMode.setModel(defaultComboBoxModel10);
        focusMode.setPreferredSize(new Dimension(114, 25));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(focusMode, gbc);
        openRelatedLabel = new JLabel();
        openRelatedLabel.setText("Open");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(openRelatedLabel, gbc);
        openRelated = new JComboBox();
        openRelated.setMinimumSize(new Dimension(84, 25));
        final DefaultComboBoxModel defaultComboBoxModel11 = new DefaultComboBoxModel();
        openRelated.setModel(defaultComboBoxModel11);
        openRelated.setPreferredSize(new Dimension(78, 25));
        openRelated.setToolTipText("Open related file");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(openRelated, gbc);
        mood = new JComboBox();
        mood.setMinimumSize(new Dimension(81, 25));
        mood.setPreferredSize(new Dimension(76, 25));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(mood, gbc);
        headDirection2 = new JComboBox();
        headDirection2.setMinimumSize(new Dimension(81, 25));
        final DefaultComboBoxModel defaultComboBoxModel12 = new DefaultComboBoxModel();
        defaultComboBoxModel12.addElement("Far Up");
        defaultComboBoxModel12.addElement("Up");
        defaultComboBoxModel12.addElement("Straight");
        defaultComboBoxModel12.addElement("Down");
        headDirection2.setModel(defaultComboBoxModel12);
        headDirection2.setPreferredSize(new Dimension(76, 25));
        headDirection2.setRequestFocusEnabled(false);
        headDirection2.setToolTipText("Head Tilt");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 9;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(headDirection2, gbc);
        final JLabel label17 = new JLabel();
        label17.setText("Mood");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label17, gbc);
        tiltLabel = new JLabel();
        tiltLabel.setText("Tilt");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(tiltLabel, gbc);
        final JPanel spacer5 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel2.add(spacer5, gbc);
        Eyes = new JLabel();
        Eyes.setText("Eyes");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(Eyes, gbc);
        eyesStatus = new JComboBox();
        eyesStatus.setMinimumSize(new Dimension(81, 25));
        final DefaultComboBoxModel defaultComboBoxModel13 = new DefaultComboBoxModel();
        defaultComboBoxModel13.addElement("Open");
        defaultComboBoxModel13.addElement("Closed");
        eyesStatus.setModel(defaultComboBoxModel13);
        eyesStatus.setPreferredSize(new Dimension(76, 25));
        eyesStatus.setToolTipText("Eyes Status");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 11;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(eyesStatus, gbc);
        bodyTilt = new JComboBox();
        bodyTilt.setMinimumSize(new Dimension(84, 25));
        final DefaultComboBoxModel defaultComboBoxModel14 = new DefaultComboBoxModel();
        defaultComboBoxModel14.addElement("Far Up");
        defaultComboBoxModel14.addElement("Up");
        defaultComboBoxModel14.addElement("Straight");
        defaultComboBoxModel14.addElement("Down");
        bodyTilt.setModel(defaultComboBoxModel14);
        bodyTilt.setPreferredSize(new Dimension(76, 25));
        bodyTilt.setToolTipText("Body Pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 9;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(bodyTilt, gbc);
        bodyDirection = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel15 = new DefaultComboBoxModel();
        defaultComboBoxModel15.addElement("Left");
        defaultComboBoxModel15.addElement("Right");
        defaultComboBoxModel15.addElement("Forward");
        defaultComboBoxModel15.addElement("Backward");
        bodyDirection.setModel(defaultComboBoxModel15);
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 8;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(bodyDirection, gbc);
        final JLabel label18 = new JLabel();
        label18.setText("Pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label18, gbc);
        poseStringField = new JFormattedTextField();
        poseStringField.setMinimumSize(new Dimension(49, 25));
        poseStringField.setPreferredSize(new Dimension(49, 25));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(poseStringField, gbc);
        final JPanel spacer6 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel2.add(spacer6, gbc);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setMinimumSize(new Dimension(250, 320));
        scrollPane1.setPreferredSize(new Dimension(300, 320));
        panel1.add(scrollPane1, BorderLayout.NORTH);
        imageHolder.setMinimumSize(new Dimension(250, 320));
        imageHolder.setPreferredSize(new Dimension(250, 320));
        scrollPane1.setViewportView(imageHolder);
        headBreed.setNextFocusableComponent(headPose);
        leftUpperArmPose.setNextFocusableComponent(leftForearmPose);
        rightUpperArmPose.setNextFocusableComponent(rightForearmPose);
        leftForearmPose.setNextFocusableComponent(rightUpperArmPose);
        leftThighPose.setNextFocusableComponent(leftShinPose);
        leftShinPose.setNextFocusableComponent(leftFootPose);
        leftFootPose.setNextFocusableComponent(rightThighPose);
        rightThighPose.setNextFocusableComponent(rightShinPose);
        rightShinPose.setNextFocusableComponent(rightFootPose);
        rightFootPose.setNextFocusableComponent(tailBreed);
        bodyBreed.setNextFocusableComponent(bodyTilt);
        armsBreed.setNextFocusableComponent(armsBreed);
        tailLabel.setLabelFor(tailBreed);
        tailBreed.setNextFocusableComponent(tailBasePose);
        tailTipPose.setNextFocusableComponent(headBreed);
        tailBaseLabel.setLabelFor(tailBasePose);
        tailTipLabel.setLabelFor(tailTipPose);
        focusModeLabel.setLabelFor(focusMode);
        openRelatedLabel.setLabelFor(openRelated);
        label17.setLabelFor(mood);
        tiltLabel.setLabelFor(headDirection2);
        bodyTilt.setNextFocusableComponent(armsBreed);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private void updateDefaultPose() {
        final String poseString = poseStringField.getText();
        settings.setDefaultPoseString(poseString);
    }

    private void setPoseFromString(final String newPoseRaw) {
        final Triple<Pose, Boolean, int[]> response = model.validateNewPose(
                pose,
                newPoseRaw.trim(),
                lastPoseString,
                getFacing()
        );
        if (response == null) {
            return;
        }
        final Pose newPose = response.getFirst();
        if (newPose == null) {
            return;
        }
        final Color highlightColor = JBColor.RED;
        final Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(highlightColor);
        final Border errorBorder = new BasicBorders.FieldBorder(JBColor.red, JBColor.red, JBColor.red, JBColor.red);
        final Border okayBorder = new BasicBorders.FieldBorder(JBColor.white, JBColor.white, JBColor.white, JBColor.white);
        final Highlighter highlighter = poseStringField.getHighlighter();

        for (final int i : response.getThird()) {
            try {
                highlighter.addHighlight(i, i + 1, painter);
            } catch (Exception exc) {
                LOGGER.severe("Invalid highlight position " + i);
            }
        }

        if (response.getSecond()) {
            poseStringField.setBorder(errorBorder);
            return;
        } else {
            poseStringField.setBorder(okayBorder);
        }
        lastPoseString = newPoseRaw;
        justSetString = true;
        if (!newPoseRaw.trim().equals(poseStringField.getText().trim())) {
            poseStringField.setText(newPoseRaw);
        }
        setPose(newPose, /* SetFacing = */ true);
        justSetString = false;
    }

    @SuppressWarnings("unused")
    public void setOnRedrawCallback(@Nullable Function<Boolean, Void> onRedrawCallback) {
        this.onRedrawCallback = onRedrawCallback;
    }

    @Override
    public void setFiles(
            @NotNull final List<BodyPartFiles> files) {
        this.files = files
                .stream()
                .filter((f) ->
                        f.getBodyDataFile().isValid() && f.getSpriteFile().isValid()
                )
                .collect(Collectors.toList());
        PoseEditorImpl.this.variant = (variant == CaosVariant.DS.INSTANCE) ? CaosVariant.C3.INSTANCE : variant;
        final boolean variantChanged = this.variantChanged;
        if (variantChanged) {
            setVariantControls(variant);
        }
        if (!didInitOnce || variantChanged) {
            PoseEditorImpl.this.initUI();
        }
        updateTails();
        updateBreedsList();
        initOpenRelatedComboBox();
        dirty = true;
        if (!files.isEmpty()) {
            model.requestRender(ALL_PARTS, true);
        }
    }

    public interface PoseChangeListener {
        void onPoseChange(Pose pose);
    }

    public void addBreedSelectionChangeListener(final BreedSelectionChangeListener listener) {
        breedSelectionChangeListeners.add(listener);
    }

    public void removeBreedSelectionChangeListener(final BreedSelectionChangeListener listener) {
        breedSelectionChangeListeners.remove(listener);
    }


    @Nullable
    public BreedKey getPartBreed(@Nullable Character part) {
        if (part == null) {
            return null;
        }
        final JComboBox<Triple<String, BreedPartKey, List<BodyPartFiles>>> breedComboBox =
                getComboBoxForBreed(part);
        if (breedComboBox == null) {
            return null;
        }
        final Object selected = breedComboBox.getSelectedItem();
        if (selected == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        final Triple<String, BreedPartKey, List<BodyPartFiles>> breed =
                (Triple<String, BreedPartKey, List<BodyPartFiles>>) selected;

        return breed.getSecond().getBreedKey();
    }


    private class VisibilityPopup extends JPopupMenu implements Disposable {

        final JCheckBoxMenuItem all = new JCheckBoxMenuItem("All Parts");
        final Map<Character, JCheckBoxMenuItem> partItems = new HashMap<>();

        final Map<char[], JCheckBoxMenuItem> multiPartItems = new HashMap<>();
        private final PartVisibility visibility;

        private boolean canClose = true;

        VisibilityPopup(final Disposable parent, final PartVisibility visibility) {
            this.visibility = visibility;
            if (project.isDisposed()) {
                return;
            }
            try {
                Disposer.register(parent, this);
            } catch (final Exception ignored) {
                dispose();
                return;
            }

            init();
            addMouseListeners();
        }

        void updateItems() {
            for (final Map.Entry<Character, JCheckBoxMenuItem> entry : partItems.entrySet()) {
                final boolean checked = visibilityMap.get(entry.getKey()) == visibility;
                entry.getValue().setSelected(checked);
            }
            updateMultiPartUI();
            updateAllCheckbox();
        }

        private void updateMultiPartUI() {
            for (final Map.Entry<char[], JCheckBoxMenuItem> entry : multiPartItems.entrySet()) {
                boolean allChecked = true;
                for (final char part : entry.getKey()) {
                    if (allChecked && visibilityMap.get(part) != visibility) {
                        allChecked = false;
                    }
                }
                entry.getValue().setSelected(allChecked);
            }
        }

        private void init() {
            all.addActionListener((e) -> toggleAll());
            add(all);
            final char[] parts = new char[]{'a', 'o', 'p', 'q', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n'};
            for (final char part : parts) {

                // Add Multipart checkbox before corresponding part
                switch (Character.toLowerCase(part)) {
                    case 'a':
                        initMultiCheckbox("Head", 'a', 'o', 'p', 'q');
                        break;
                    case 'c':
                        initMultiCheckbox("Legs", 'c', 'd', 'e', 'f', 'g', 'h');
                        initMultiCheckbox("  L. Leg", 'c', 'd', 'e');
                        break;
                    case 'f':
                        initMultiCheckbox("  R. Leg", 'f', 'g', 'h');
                        break;
                    case 'i':
                        initMultiCheckbox("Arms", 'i', 'j', 'k', 'l');
                        initMultiCheckbox("  L. Arm", 'i', 'j');
                        break;
                    case 'k':
                        initMultiCheckbox("  R. Arm", 'k', 'l');
                        break;
                    case 'm':
                        initMultiCheckbox("Tail", 'm', 'n');
                        break;
                }
                // Add single part checkbox
                initCheckboxForPart(part);
            }
        }

        private void updateAllCheckbox() {
            boolean checked = false;
            for (final Map.Entry<Character, JCheckBoxMenuItem> entry : partItems.entrySet()) {
                if (!checked && visibilityMap.get(entry.getKey()) == visibility) {
                    checked = true;
                }
            }
            all.setSelected(checked);
        }

        private void initMultiCheckbox(final String label, char... parts) {
            final JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
            boolean allChecked = true;
            for (final char part : parts) {
                if (allChecked && visibilityMap.get(part) != visibility) {
                    allChecked = false;
                }
            }
            item.setSelected(allChecked);
            item.addActionListener(onMultiToggleClicked(item, parts));
            add(item);
            multiPartItems.put(parts, item);
        }

        private void initCheckboxForPart(final char part) {
            String prefix = getPrefix(part);
            final JCheckBoxMenuItem item = new JCheckBoxMenuItem(prefix + part + " - " + PoseEditorSupport.getPartName(part));
            final PartVisibility currentVisibility = visibilityMap.get(part);
            item.setSelected(currentVisibility == visibility);
            item.addActionListener(onPartClick(part, item));
            add(item);
            partItems.put(part, item);
        }


        private final MouseListenerBase mouseListener = new MouseListenerBase() {
            @Override
            public void mouseEntered(@NotNull MouseEvent e) {
                canClose = false;
            }

            @Override
            public void mouseExited(@NotNull MouseEvent e) {
                canClose = true;
            }
        };

        private final MouseMotionListener mouseMotionListener = new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!canClose && !VisibilityPopup.this.contains(e.getX(), e.getY())) {
                    canClose = true;
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!canClose && !VisibilityPopup.this.contains(e.getX(), e.getY())) {
                    canClose = true;
                }
            }
        };

        private void addMouseListeners() {
            addMouseListener(mouseListener);
            addMouseMotionListener(mouseMotionListener);
        }

        private void removeMouseListeners() {
            removeMouseListener(mouseListener);
            removeMouseMotionListener(mouseMotionListener);
        }

        @Override
        public void dispose() {
            removeMouseListeners();
            canClose = true;
            super.setVisible(false);
        }


        @Override
        public void setVisible(boolean visible) {
            if (!visible && !canClose) {
                return;
            }
            if (visible) {
                if (!super.isVisible()) {
                    addMouseListeners();
                }
            } else {
                removeMouseListeners();
            }
            super.setVisible(visible);
        }

        private ActionListener onPartClick(final char part, final JCheckBoxMenuItem item) {
            return e -> {
                if (item.isSelected()) {
                    visibilityMap.put(part, visibility);
                } else if (visibilityMap.get(part) == visibility) {
                    visibilityMap.remove(part);
                }
                model.requestRender(new char[]{part}, false);
                setVisible(true);
            };
        }


        private void toggleAll() {
            final boolean checked = all.isSelected();
            for (final Map.Entry<Character, JCheckBoxMenuItem> entry : partItems.entrySet()) {
                if (checked) {
                    visibilityMap.put(entry.getKey(), visibility);
                    entry.getValue().setSelected(true);
                } else if (visibilityMap.get(entry.getKey()) == visibility) {
                    visibilityMap.remove(entry.getKey());
                    entry.getValue().setSelected(false);
                }
            }
            setVisible(true);
            redraw(ALL_PARTS);
        }

        /**
         * Get actions listener that toggles multiple parts together
         *
         * @param item  menu item
         * @param parts parts to hide/show together
         * @return the action listener
         */
        private ActionListener onMultiToggleClicked(final JCheckBoxMenuItem item, char... parts) {
            return e -> {
                if (item.isSelected()) {
                    for (final char part : parts) {
                        visibilityMap.put(part, visibility);
                        final JCheckBoxMenuItem checkbox = partItems.get(part);
                        if (checkbox != null) {
                            checkbox.setSelected(true);
                        }
                    }
                } else {
                    for (final char part : parts) {
                        if (visibilityMap.get(part) == visibility) {
                            visibilityMap.remove(part);
                            final JCheckBoxMenuItem checkbox = partItems.get(part);
                            if (checkbox != null) {
                                checkbox.setSelected(false);
                            }
                        }
                    }
                }
                model.requestRender(parts, false);
                setVisible(true);
            };
        }

        private String getPrefix(final char part) {
            switch (Character.toLowerCase(part)) {
                case 'b': // Body
                    return "";
                case 'c': // LEGS
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i': // ARMS
                case 'j':
                case 'k':
                case 'l':
                    return "    ";
                case 'a': // HEAD
                case 'o':
                case 'p':
                case 'q':
                case 'm': // TAIL
                case 'n':
                    return "  ";
                default:
                    return "";
            }
        }

    }

    private class PopUp extends JPopupMenu {
        final JMenuItem setDefaultPoseItem;
        final JMenuItem copyPoseItem;

        PopUp() {
            setDefaultPoseItem = new JMenuItem("Set as default pose");
            setDefaultPoseItem.addActionListener((e) -> updateDefaultPose());
            add(setDefaultPoseItem);
            copyPoseItem = new JMenuItem("Copy Pose");
            copyPoseItem.addActionListener((e) -> {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                final Integer trueFacing = model.getTrueFacing(pose.getBody());
                if (trueFacing != null) {
                    clipboard.setContents(new PoseTransferable(pose, trueFacing, variant), null);
                }
            });
            add(copyPoseItem);
        }

        void updateItems() {
            final Integer facing = model.getTrueFacing(pose.getBody());
            final boolean show = facing != null;
            copyPoseItem.setVisible(show);
            final boolean isNotFrontOrBack = facing != null && facing != 2 && facing != 3;
            setDefaultPoseItem.setVisible(show && isNotFrontOrBack);
        }

    }


    public static interface BreedSelectionChangeListener {
        void onBreedSelected(BreedKey key, char... parts);
    }

}
