package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose;

import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileData;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.AttEditorPanel;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.CreatureSpriteSet;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.PartVisibility;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.Pose;
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartFiles;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartsIndex;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey;
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile;
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.JBColor;
import kotlin.Pair;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.BreedDataUtil.findBreeds;
import static com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.BreedDataUtil.findMatchingBreedInList;
import static com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.FocusModeHelper.getVisibilityMask;
import static com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseCalculator.*;
import static com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.render;

public class PoseEditor implements Disposable, BreedPoseHolder, DumbAware {
    private static final Logger LOGGER = Logger.getLogger("#PoseEditor");
    private static final char[] ALL_PARTS = PoseEditorSupport.getAllParts();
    private final Project project;
    private final BreedPartKey baseBreed;
    private final String[] directions = new String[]{
            "Up",
            "Straight",
            "Down",
            "Far Down"
    };


    private final Map<Character, VirtualFile> manualAtts = new HashMap<>();
    private final List<PoseChangeListener> poseChangeListeners = Lists.newArrayList();
    JPanel panel1;
    JComboBox<VirtualFile> headBreed;
    JComboBox<VirtualFile> bodyBreed;
    JComboBox<String> rightUpperArmPose;
    JComboBox<String> leftUpperArmPose;
    JComboBox<String> headPose;
    JComboBox<String> bodyDirection;
    JComboBox<String> bodyTilt;
    JComboBox<VirtualFile> armsBreed;
    JComboBox<String> leftForearmPose;
    JComboBox<String> rightForearmPose;
    JComboBox<VirtualFile> legsBreed;
    JComboBox<String> leftThighPose;
    JComboBox<String> leftShinPose;
    JComboBox<String> leftFootPose;
    JComboBox<String> rightThighPose;
    JComboBox<String> rightShinPose;
    JComboBox<String> rightFootPose;
    JComboBox<VirtualFile> tailBreed;
    JComboBox<String> tailTipPose;
    JComboBox<String> tailBasePose;
    JComboBox<VirtualFile> earBreed;
    JComboBox<VirtualFile> hairBreed;
    JComboBox<String> facing;
    JComboBox<String> zoom;
    JPanel imageHolder;
    JComboBox<String> focusMode;
    JLabel earLabel;
    JLabel hairLabel;
    JLabel tailLabel;
    JLabel tailBaseLabel;
    JLabel tailTipLabel;
    JLabel facingLabel;
    JLabel focusModeLabel;
    JComboBox<String> headDirection2;
    JComboBox<VirtualFile> openRelated;
    JLabel openRelatedLabel;
    JComboBox<String> mood;
    JLabel tiltLabel;
    JLabel Eyes;
    JComboBox<String> eyesStatus;
    JScrollPane partsPanel;
    JFormattedTextField poseStringField;
    List<BodyPartFiles> files;
    private CaosVariant variant;
    private CreatureSpriteSet spriteSet;
    private Pose pose;
    private boolean valid;
    private boolean didInitOnce = false;
    private Map<Character, PartVisibility> visibilityMask = new HashMap<>();
    private Character visibilityFocus;
    private String rootPath;
    private boolean drawImmediately = false;
    private Pose defaultPoseAfterInit;
    private boolean wasHidden = false;
    private String lastPoseString = "";

    public PoseEditor(final Project project, final CaosVariant variant, final BreedPartKey breedKey) {
        this.project = project;
        $$$setupUI$$$();
        this.baseBreed = breedKey.copyWithPart(null);
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(() -> this.init(variant));
        } else {
            init(variant);
        }
    }

    /**
     * Reverses the items in an array
     *
     * @param items array to reverse
     * @param <T>   the type of items in array
     */
    private static <T> void reverse(T[] items) {
        final int n = items.length;
        T t;
        for (int i = 0; i < n / 2; i++) {
            t = items[i];
            items[i] = items[n - i - 1];
            items[n - i - 1] = t;
        }
    }

    @Override
    public Integer getPartPose(char part) {
        final Integer facing = getTrueFacing(pose.getBody());
        if (facing == null)
            return null;
        return getPartPose(part, facing, getFacingOffset(facing));
    }

    /**
     * Gets the pose for a given combobox
     *
     * @param part body part to get part pose for
     * @param facingDirection facing direction of creature
     * @param offset          offset into sprite set
     * @return pose index in sprite file
     */
    public Integer getPartPose(final char part, final int facingDirection, final int offset) {
        final JComboBox<String> box = getComboBoxForPart(part);
        if (box == null)
            return null;
        return PoseCalculator.getBodyPartPose(variant, box.getSelectedIndex(), facingDirection, offset, true);
    }

    @Override
    public int getBodyPoseActual() {
        final int bodyDirection = this.bodyDirection.getSelectedIndex();
        final int tilt = this.bodyTilt.getSelectedIndex();
        if (variant.isOld()) {
            if (bodyDirection == 0)
                return (3 - tilt);
            else if (bodyDirection == 1)
                return 4 + (3 - tilt);
            else if (bodyDirection == 2)
                return 8;
            else
                return 9;
        }
        return (bodyDirection * 4) + (3 - tilt);
    }

    @Override
    public int getHeadPoseActual() {
        final String item = (String)headPose.getSelectedItem();
        final boolean faceIsBack = item != null && item.toLowerCase().startsWith("back");
        final int pose = calculateHeadPose(variant,
                headPose.getSelectedIndex() == 2 && faceIsBack ? 3 : facing.getSelectedIndex(),
                headPose.getSelectedIndex(),
                headDirection2.getSelectedIndex(),
                mood.getSelectedIndex(),
                eyesStatus.getSelectedIndex() > 0
        );
        if (variant.isOld() && pose == 8) {
            if (faceIsBack)
                return pose + 1;
        }
        return pose;
    }

    @Override
    @NotNull
    public BreedPartKey getBaseBreed() {
        return baseBreed;
    }

    @Override
    @Nullable
    public VirtualFile getPartBreed(final char part) {
        final JComboBox<VirtualFile> menu = getComboBoxForBreed(part);
        if (menu == null) {
            return null;
        }
        return menu.getItemCount() > 0 ? (VirtualFile) menu.getSelectedItem() : null;
    }

    @Override
    @NotNull
    public CaosVariant getVariant() {
        return variant;
    }

    /**
     * Sets the variant and loads the files to use
     *
     * @param variant variant to use for the pose editor
     */
    private void setVariant(final CaosVariant variant) {
        Runnable runner = () -> {
            this.variant = (variant == CaosVariant.DS.INSTANCE) ? CaosVariant.C3.INSTANCE : variant;
            files = BodyPartsIndex.variantParts(project, variant);
            setVariantControls(variant);
        };
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(runner);
        } else {
            runner.run();
        }
    }

    public JPanel getMainPanel() {
        return panel1;
    }

    // Initializes this pose editor given a variant
    private void init(CaosVariant variant) {
        setVariant(variant);
        partsPanel.getVerticalScrollBar().setUnitIncrement(16);
        initComboBoxes();
        addChangeHandlers();
        didInitOnce = true;
        try {
            update(variant);
        } catch (IndexNotReadyException e) {
            DumbService.getInstance(project).runWhenSmart(() -> update(variant));
        }
    }

    /**
     * Enables/Disables/Shows/Hides controls for certain variants
     *
     * @param variant variant to set controls for
     */
    private void setVariantControls(CaosVariant variant) {

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

        // If variant is old, it does not have multiple tilts for front facing and back facing head sprites
        if (variant.isOld()) {
            freeze(headDirection2, true, true);
            tiltLabel.setVisible(false);
        } else {
            freeze(headDirection2, ! headDirection2.isEnabled(), false);
            tiltLabel.setVisible(true);
        }

        // If is C1, hide controls for tails as they are not used in C1
        if (variant == CaosVariant.C1.INSTANCE) {
            setLabelVisibility('m', false);
            setLabelVisibility('n', false);
            tailLabel.setVisible(false);
            freeze(tailBreed, true, true);
            freeze(tailBasePose, true, true);
            freeze(tailTipPose, true, true);
        } else {
            // Allow tail controls in all variants other than C1
            setLabelVisibility('m', true);
            setLabelVisibility('n', true);
            tailLabel.setVisible(true);
            freeze(tailBreed, false, false);
            freeze(tailBasePose, false, false);
            freeze(tailTipPose, false, false);
        }
    }

    /**
     * Inits the combo boxes
     */
    private void initComboBoxes() {
        zoom.setSelectedIndex(baseBreed.getAgeGroup() == null || baseBreed.getAgeGroup() >= 2 ? 1 : 2);
        if (headPose.getItemCount() > 0)
            headPose.setSelectedIndex(0);
        if (variant.isNotOld()) {
            headDirection2.setSelectedIndex(2);
        }
        initHeadComboBox(0, Integer.MAX_VALUE);
        assign(mood, PoseEditorSupport.getMoodOptions(variant), 0);
        freeze(headDirection2, ! headDirection2.isEnabled(), variant.isOld());
        setFacing(0);
        if (variant == CaosVariant.C1.INSTANCE && baseBreed.getGenus() != null && baseBreed.getGenus() == 1) {
            reverse(directions);
            assign(bodyTilt, directions, 1);
            reverse(directions);
        } else {
            assign(bodyTilt, directions, 1);
        }
        assign(focusMode, FocusMode.toStringArray(), 0);
        assign(leftThighPose, directions, 2);
        assign(leftShinPose, directions, 2);
        assign(leftFootPose, directions, 1);
        assign(rightThighPose, directions, 2);
        assign(rightShinPose, directions, 2);
        assign(rightFootPose, directions, 1);
        assign(leftUpperArmPose, directions, 2);
        assign(leftForearmPose, directions, 2);
        assign(rightUpperArmPose, directions, 2);
        assign(rightForearmPose, directions, 2);
        assign(tailBasePose, directions, 2);
        assign(tailTipPose, directions, 2);

        populate(headBreed, files, 'a');
        populate(bodyBreed, files, 'b');
        populate(legsBreed, files, 'c');
        populate(armsBreed, files, 'i');
        populate(tailBreed, files, true,'m', 'n');
        populate(earBreed, files,  true,'o', 'p');
        populate(hairBreed, files,  true,'q');
        if (earBreed.getItemCount() < 1) {
            setLabelVisibility('o', false);
            freeze(earBreed, true, true);
        } else {
            setLabelVisibility('o', true);
            freeze(earBreed, false, false);
        }
        populate(hairBreed, files, 'q');
        if (variant.isNotOld()) {
            // Hide ears if no breeds are available
            if (earBreed.getItemCount() < 1) {
                freeze(earBreed, true, true);
            } else {
                freeze(earBreed, false, false);
            }

            // Hide hair if no breed choices are available
            if (hairBreed.getItemCount() < 1) {
                freeze(hairBreed, true, true);
            } else {
                freeze(hairBreed, false, false);
            }
        }
        defaultPoseAfterInit = getUpdatedPose(ALL_PARTS);
        initOpenRelatedComboBox();
    }

    private void initOpenRelatedComboBox() {
        openRelated.setRenderer(new PartFileCellRenderer());
        if (rootPath == null || files == null || files.isEmpty()) {
            freeze(openRelated, true, true);
            openRelatedLabel.setVisible(false);
            return;
        }
        final BreedPartKey key = baseBreed.copyWithPart(null);
        final List<VirtualFile> relatedFiles = files.stream()
                .filter((f) -> {
                    if (f == null) {
                        return false;
                    }
                    final BreedPartKey partKey = f.getKey();
                    if (partKey == null || ! BreedPartKey.isGenericMatch(key, partKey)) {
                        return false;
                    }
                    // Strict match for root path. Must be in same exact root path
                    // Not just a sub directory
                    return rootPath.equals(f.getBodyDataFile().getParent().getPath());
                })
                .map(BodyPartFiles::getBodyDataFile)
                .sorted(Comparator.comparing(VirtualFile::getPath))
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
        assign(openRelated, relatedFiles.toArray(new VirtualFile[0]), 0);
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
        final Color highlightColor = JBColor.RED;
        final Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(highlightColor);
        final Border errorBorder = new BasicBorders.FieldBorder(JBColor.red, JBColor.red, JBColor.red, JBColor.red);
        final Border okayBorder = new BasicBorders.FieldBorder(JBColor.white, JBColor.white, JBColor.white, JBColor.white);
        poseStringField.setHighlighter(highlighter);
        poseStringField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                highlighter.removeAllHighlights();
                final String newPoseRaw = poseStringField.getText();
                final StringBuilder newPoseStringBuilder = new StringBuilder(newPoseRaw);
                while (newPoseStringBuilder.length() < 15) {
                    newPoseStringBuilder.append(" ");
                }
                final String newPoseString = newPoseStringBuilder.toString();
                boolean hasError = false;
                if (newPoseRaw.length() < 15) {
                    hasError = true;
                } else if (lastPoseString.equals(newPoseString)) {
                    return;
                }
                final int facing = PoseEditor.this.facing.getSelectedIndex();
                final Pose lastPose = PoseEditor.this.pose;
                final Pair<Integer, Pose> result = Pose.fromString(variant, facing, lastPose, newPoseString);
                final Pose newPose = result.getSecond();
                for (int i = 0; i < 15; i++) {
                    final char part = (char) ('a' + i);
                    final Integer thisPose = newPose.get(part);
                    if (thisPose == null || thisPose < - 1) {
                        hasError = true;
                        try {
                            highlighter.addHighlight(i+1, i+2, painter);
                        } catch (Exception exc) {
                            LOGGER.severe("Invalid highlight position " + i);
                        }
                    }
                }
                if (hasError) {
                    poseStringField.setBorder(errorBorder);
                    return;
                } else {
                    poseStringField.setBorder(okayBorder);
                }
                setPose(newPose, true);
            }
        });
        focusMode.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (visibilityFocus != null) {
                    visibilityMask = getVisibilityMask(focusMode.getSelectedIndex(), visibilityFocus);
                } else {
                    visibilityMask = PartVisibility.getAllVisible();
                }
                redraw();
            }
        });

        facing.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
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

        bodyDirection.addItemListener((e) -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            final int facingDirection = facing.getSelectedIndex();
            final int bodyDirectionIndex = bodyDirection.getSelectedIndex();
            if (bodyDirectionIndex < 0)
                return;
            if (bodyDirectionIndex != facingDirection) {
                if (facingDirection < 2 && bodyDirectionIndex < 2) {
                    bodyDirection.setSelectedIndex(facingDirection);
                } else if (facingDirection >= 2 && bodyDirectionIndex >= 2) {
                    bodyDirection.setSelectedIndex(facingDirection);
                }
            }
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

            if (! (fileObject instanceof VirtualFile)) {
                return;
            }
            final VirtualFile file = (VirtualFile) fileObject;
            file.putUserData(AttEditorPanel.REQUESTED_POSE_KEY, pose);
            final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                final DialogBuilder builder = new DialogBuilder();
                builder.setTitle("Open Related Error");
                builder.setErrorText("We failed to open the related ATT editor file. Document not properly resolved.");
                builder.showAndGet();
                return;
            }
            psiFile.navigate(true);
        });
    }

    private void addPartListener(final JComboBox<?> box, final char partChar) {
        box.addItemListener((e) -> {
            if (e.getStateChange() != ItemEvent.SELECTED || box.getSelectedIndex() < 0) {
                return;
            }
            if (didInitOnce && box.getItemCount() > 0) {
                if (drawImmediately) {
                    redraw(partChar);
                }
            }
        });
    }


    private void addBreedListener(final JComboBox<VirtualFile> box, final char... parts) {
        box.addItemListener((e) -> {
            if (box.getItemCount() < 1)
                return;

            if (e.getStateChange() != ItemEvent.SELECTED && box.getSelectedIndex() > 0) {
                return;
            }
            for (char part : parts) {
                manualAtts.remove(part);
            }
            if (didInitOnce) {
                redraw(parts);
            }
        });
    }

    public void update(final CaosVariant variant) {
        setVariant(variant);
        valid = redraw(ALL_PARTS);
    }

    /**
     * @return <b>True</b> if the last render was successful. <b>False</b> if it was not
     */
    public boolean isValid() {
        return valid;
    }


    public void redrawAll() {
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(this::redrawAll);
            return;
        }
        drawImmediately = true;
        spriteSet = null;
        clear();
        if (variant != null)
            files = BodyPartsIndex.variantParts(project, variant);
        redraw(ALL_PARTS);
    }

    public void clear() {
        spriteSet = null;
        files = null;
        if (imageHolder != null) {
            ((PoseRenderedImagePanel) imageHolder).clear();
        }
    }

    /**
     * Reloads files and then queues redraw as necessary
     *
     * @param parts parts that have been changed
     * @return <b>True</b> if redraw was successful; <b>False</b> otherwise
     */
    public boolean redraw(char... parts) {
        if (! didInitOnce) {
            return false;
        }
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            if (DumbService.isDumb(project)) {
                DumbService.getInstance(project).runWhenSmart(() -> {
                    redrawActual(parts);
                });
                return false;
            } else {
                return redrawActual(parts);
            }
        });
    }

    /**
     * Redraws the pose after updating the given part part and breed information
     *
     * @param parts parts that have been changed
     * @return <b>True</b> if redraw was successful; <b>False</b> otherwise
     */
    private boolean redrawActual(char... parts) {
        if (!panel1.isVisible()) {
            wasHidden = true;
            return false;
        }
        if (wasHidden) {
            wasHidden = false;
            parts = ALL_PARTS;
        }
        drawImmediately = true;
        final CreatureSpriteSet updatedSprites;
        try {
            if (files == null && project != null && !DumbService.isDumb(project)) {
                files = BodyPartsIndex.variantParts(project, variant);
            } else if (files == null) {
                wasHidden = true;
                return false;
            }
            spriteSet = updatedSprites = SpriteSetUtil.getUpdatedSpriteSet(
                    project,
                    this,
                    spriteSet,
                    files,
                    manualAtts,
                    parts
            );
        } catch (Exception e) {
            LOGGER.severe("Failed to located required sprites Error:(" + e.getClass().getSimpleName() + ") " + e.getLocalizedMessage());
            e.printStackTrace();
            return valid = false;
        }
        final Pose updatedPose = getUpdatedPose(parts);
        final BufferedImage image;
        try {
            image = render(variant, updatedSprites, updatedPose, visibilityMask, zoom.getSelectedIndex() + 1);
        } catch (Exception e) {
            LOGGER.severe("Failed to render pose. Error:(" + e.getClass().getSimpleName() + ") " + e.getLocalizedMessage());
            e.printStackTrace();
            return valid = false;
        }
        ((PoseRenderedImagePanel) imageHolder).updateImage(image);
        return valid = true;
    }

    private Pose getUpdatedPose(char... parts) {
        final int lastPoseHash = pose != null ? pose.hashCode() : -1;
        final Pose poseTemp = PoseCalculator.getUpdatedPose(
                variant,
                pose,
                bodyDirection.getSelectedIndex(),
                this,
                parts
        );
        pose = poseTemp;
        // Set the pose object back to itself or with a new version if it doesn't already exist.

        if (poseTemp.hashCode() != lastPoseHash) {
            poseChangeListeners.forEach((it) -> it.onPoseChange(poseTemp));
            final String newPoseString = poseTemp.poseString(variant, facing.getSelectedIndex());
            if (newPoseString != null) {
                lastPoseString = newPoseString;
                poseStringField.setText(newPoseString);
            }
        }
        return poseTemp;
    }

    /**
     * Sets the att file data manually for a given part
     *
     * @param part part to update att data for
     * @param att  new att file data
     */
    public void setAtt(char part, AttFileData att) {
        if (att == null || variant == null) {
            return;
        }
        final JComboBox<String> comboBox = getComboBoxForPart(part);
        if (comboBox == null) {
            return;
        }
        String breedString = "" + part + comboBox.getSelectedItem();
        CaosVirtualFile file = new CaosVirtualFile(breedString + ".att", att.toFileText(variant));
        CaosVirtualFileSystem.getInstance().addFile(file, true);
        manualAtts.put(part, file);
        spriteSet = spriteSet.replacing(part, att);
        redraw(part);
    }

    /**
     * Shows/Hides facing controls
     *
     * @param show whether to show the facing controls or not
     */
    public void showFacing(boolean show) {
        freeze(facing, show, ! show);
        facingLabel.setVisible(show);
    }

    /**
     * Freezes/Unfreezes controls and potentially hides them
     *
     * @param box    Box to freeze control of
     * @param freeze whether or not to freeze(make readonly)
     * @param hide   whether to hide the control after freezing
     */
    private void freeze(JComboBox<?> box, boolean freeze, Boolean hide) {
        if (box == null) {
            return;
        }
        box.setEnabled(! freeze);
        if (hide != null) {
            box.setVisible(! hide);
        }
    }

    /**
     * Convenience method to freeze/unfreeze a control given its part char
     *
     * @param part   part to alter state of
     * @param freeze whether to freeze or unfreeze this part
     */
    public void freeze(char part, Boolean freeze) {
        freeze(getComboBoxForBreed(part), freeze, null);
        final JComboBox<String> poseComboBox = getComboBoxForPart(part);
        if (poseComboBox != null) {
            freeze(getComboBoxForPart(part), freeze, null);
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
                return headPose;
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
    private JComboBox<VirtualFile> getComboBoxForBreed(char part) {
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
            default:
                LOGGER.severe("Part: " + part + " is not a valid body part char");
                return null;
        }
    }

    /**
     * Assigns a set of values to a combo box
     *
     * @param menu          combo box to populate
     * @param items         items to populate combo box with
     * @param selectedIndex index to select after filling in items
     * @param <T>           The kind of values to fill this combo box with
     */
    private <T> void assign(JComboBox<T> menu, T[] items, int selectedIndex) {
        menu.removeAllItems();
        for (T item : items) {
            menu.addItem(item);
        }
        if (menu.getItemCount() > selectedIndex) {
            menu.setSelectedIndex(selectedIndex);
        } else if (items.length > 0)
            menu.setSelectedIndex(0);

    }
    /**
     * Populates a breed combo box with available breed files
     *
     * @param menu      breed combo box
     * @param files     a list of all available body data regardless of actual part
     * @param partChars parts to filter breeds by
     */
    private void populate(JComboBox<VirtualFile> menu, final List<BodyPartFiles> files, Character... partChars) {
        boolean allowNull = false;
        for(char part : partChars) {
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
    private void populate(JComboBox<VirtualFile> menu, final List<BodyPartFiles> files, boolean allowNull, Character... partChars) {
        // Set the cell renderer for the Att file list
        menu.setRenderer(new BreedFileCellRenderer());

        // Filter list of body part files for breeds applicable to this list of parts
        List<VirtualFile> items = findBreeds(files, baseBreed, partChars);
        if (items.size() > 0 && allowNull) {
            items.add(0, null);
        }
        // Assign values to this drop down
        assign(menu, items.toArray(new VirtualFile[0]), 0);

        // No matching files, skip item selectors
        if (items.isEmpty()) {
            return;
        }
        final Integer matchingBreedInList = findMatchingBreedInList(items, rootPath, baseBreed, allowNull);
        if (matchingBreedInList != null) {
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
    public void setRootPath(String path) {
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(() -> setRootPath(path));
            return;
        }
        this.rootPath = path;
        drawImmediately = false;
        final String lastDirectory = ((PoseRenderedImagePanel) imageHolder).getLastDirectory();
        if (lastDirectory == null || lastDirectory.length() < 1) {
            ((PoseRenderedImagePanel) imageHolder).setLastDirectory(path);
        }
        initComboBoxes();
        redraw(ALL_PARTS);
    }

    /**
     * Sets facing direction for the pose renderer
     *
     * @param direction direction that the creature will be facing
     */
    public void setFacing(int direction) {
        final int oldDirection = facing.getSelectedIndex();
        facing.setSelectedIndex(direction);
        bodyDirection.setSelectedIndex(direction);
        initHeadComboBox(direction, oldDirection);
        try {
            resetIfNeeded();
        } catch (Exception e) {
            LOGGER.severe("Failed to reset pose combo boxes");
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

        if (resetCount > (ALL_PARTS.length - 3) && defaultPoseAfterInit != null) {
            if (facing.getSelectedIndex() < 2) {
                headPose.setSelectedIndex(3);
                bodyTilt.setSelectedIndex(1);
            }
            leftThighPose.setSelectedIndex(2);
            leftShinPose.setSelectedIndex(2);
            leftFootPose.setSelectedIndex(1);
            rightThighPose.setSelectedIndex(2);
            rightShinPose.setSelectedIndex(2);
            rightFootPose.setSelectedIndex(1);
            leftUpperArmPose.setSelectedIndex(2);
            leftForearmPose.setSelectedIndex(2);
            rightUpperArmPose.setSelectedIndex(2);
            rightForearmPose.setSelectedIndex(2);
            if (tailBasePose.getItemCount() > 2 && tailBasePose.getItemCount() > 2) {
                tailTipPose.setSelectedIndex(2);
                tailBasePose.setSelectedIndex(2);
            }
        }
    }

    /**
     * Initializes the head combo box according to variant and direction
     * @param direction facing direction
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
    public void setPose(final int facing, final char charPart, final int pose) {
        if (! didInitOnce) {
            return;
        }
        setFacing(facing);
        setPose(charPart, pose);
    }

    /**
     * Sets the whole body's pose at once.
     *
     * @param pose the full body pose
     */
    public void setPose(Pose pose, boolean setFacing) {
        drawImmediately = false;
        final int bodyPose = pose.getBody();
        final Integer facing = getTrueFacing(bodyPose);
        if (facing == null) {
            return;
        }
        if (setFacing) {
            setFacing(facing);
        }
        for(char part : ALL_PARTS) {
            if (part == 'a') {
                setHeadPose(facing, pose.getHead());
                continue;
            }
            final Integer partPose = pose.get(part);
            if (partPose == null)
                continue;
            setPose(part, partPose);
        }
        drawImmediately = true;
        this.pose = pose;
        redraw();
        if (variant.isOld() && pose.getBody() >= 8) {
            this.pose = null;
        }
    }

    /**
     * Get the actual direction that is faced based on other drop downs
     * Direction is determined by a 3 direction combo box, but there are 4 directions
     * @param bodyPose pose of the body
     * @return true facing direction 0..4 from the pose 0..3
     */
    @Nullable
    private Integer getTrueFacing(final int bodyPose) {
        if (bodyPose < 4) {
            return 0;
        } else if (bodyPose < 8) {
            return 1;
        } else if (variant.isOld()) {
            if (bodyPose == 8) {
                return 2;
            } else if (bodyPose == 9) {
                return 3;
            } else {
                LOGGER.severe("Invalid body pose '" + bodyPose + "' for facing test");
                return null;
            }
        } else {
            if (bodyPose < 12) {
                return 2;
            } else if (bodyPose < 16) {
                return 3;
            } else {
                LOGGER.severe("Invalid body pose '" + bodyPose + "' for facing test");
                return null;
            }
        }
    }

    /**
     * Gets Sprite offset for this facing direction
     * @param facing direction
     * @return sprite offset to first pose of direction
     */
    private int getFacingOffset(final int facing) {
        if (facing == 0) {
            return 0;
        } else if (facing == 1) {
            return 4;
        } else if (facing == 2) {
            if (variant.isOld()) {
                return 0;
            } else {
                return 8;
            }
        } else {
            if (variant.isOld()) {
                return 0;
            } else {
                return 12;
            }
        }
    }

    private void setHeadPose(final int facing, final int pose) {
        final HeadPoseData headPoseData = PoseCalculator.getHeadPose(variant, facing, pose);
        if (headPoseData == null) {
            return;
        }
        if (pose < 0)
            return;
        headPose.setSelectedIndex(headPoseData.getDirection());
        if (headPoseData.getTilt() != null) {
            if (headPoseData.getTilt() >= 4) {
                LOGGER.severe("Pose: " + pose + "; Data: " + headPoseData);
            } else {
                headDirection2.setSelectedIndex(headPoseData.getTilt());
            }
        }
        final Integer moodIndex = headPoseData.getMood();
        if (moodIndex != null && mood.getItemCount() > moodIndex) {
            mood.setSelectedIndex(moodIndex);
        }
    }

    /**
     * Sets the part to center focus modes around
     *
     * @param partChar part to focus on
     */
    public void setVisibilityFocus(char partChar) {
        visibilityFocus = partChar;
        visibilityMask = getVisibilityMask(focusMode.getSelectedIndex(), partChar);
        redraw(partChar);
    }


    /**
     * Sets the pose for a body part directly
     * Alters the items in the corresponding drop down accordingly
     *
     * @param charPart part to set the pose for
     * @param pose     the pose to apply
     */
    public void setPose(char charPart, int pose) {
        if (! didInitOnce) {
            return;
        }

        if (pose < 0)
            return;
        if (charPart == 'a') {
            setHeadPose(facing.getSelectedIndex(), pose);
            return;
        }
        if (charPart == 'b') {
            if (variant.isOld()) {
                if (pose < 0 || pose > 9) {
                    LOGGER.severe("Cannot set body pose for " + variant + " to ");
                    return;
                }
                if (pose < 4) {
                    bodyDirection.setSelectedIndex(0);
                } else if (pose < 8)
                    bodyDirection.setSelectedIndex(1);
                else if (pose == 8)
                    bodyDirection.setSelectedIndex(2);
                else
                    bodyDirection.setSelectedIndex(3);
            } else if (pose < 0 || pose > 15) {
                LOGGER.severe("Cannot set body pose for " + variant + " to ");
                return;
            } else {
                bodyDirection.setSelectedIndex((int)Math.floor(pose / 4.0));
            }
        }
        JComboBox<String> comboBox = getComboBoxForPart(charPart);
        if (comboBox == null) {
            return;
        }
        // If variant is old, any part facing front but face forces all other parts front
        if (variant.isOld()) {
           if (comboBox.getItemCount() == 1) {
                // Pose is neither front nor back, so fill in directions if not already filled in
                assign(bodyTilt, directions, 1);
            }
            if (pose < 8)
                pose = 3 - (pose % 4);
        } else {
            pose = 3 - (pose % 4);
        }

        if (comboBox.getItemCount() == 0) {
            LOGGER.severe("No items in combo box: " + comboBox.getToolTipText());
            return;
        }



        // Select the pose in the dropdown box
        if (pose >= comboBox.getItemCount()) {
            LOGGER.severe("Part " + charPart + " pose '"+pose+"' is greater than options ("+comboBox.getItemCount()+")");
            pose = 0;
        }
        comboBox.setSelectedIndex(pose);
        // Redraw the image
        if (drawImmediately) {
            redraw(charPart);
        }
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
        panel1.setPreferredSize(new Dimension(250, 600));
        imageHolder.setMinimumSize(new Dimension(250, 250));
        imageHolder.setPreferredSize(new Dimension(250, 250));
        panel1.add(imageHolder, BorderLayout.NORTH);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        scrollPane1.setViewportView(panel2);
        final JPanel spacer1 = new JPanel();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(spacer1, gbc);
        headBreed = new JComboBox();
        headBreed.setPreferredSize(new Dimension(76, 25));
        headBreed.setToolTipText("Head Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(headBreed, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("Right Arm");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 8;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label1, gbc);
        leftUpperArmPose = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        leftUpperArmPose.setModel(defaultComboBoxModel1);
        leftUpperArmPose.setPreferredSize(new Dimension(76, 25));
        leftUpperArmPose.setToolTipText("Left upper arm pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 9;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(leftUpperArmPose, gbc);
        rightUpperArmPose = new JComboBox();
        rightUpperArmPose.setPreferredSize(new Dimension(76, 25));
        rightUpperArmPose.setToolTipText("Right upper arm pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 9;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(rightUpperArmPose, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Left Arm");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 8;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label2, gbc);
        leftForearmPose = new JComboBox();
        leftForearmPose.setPreferredSize(new Dimension(76, 25));
        leftForearmPose.setToolTipText("Left forearm and hand pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(leftForearmPose, gbc);
        rightForearmPose = new JComboBox();
        rightForearmPose.setPreferredSize(new Dimension(76, 25));
        rightForearmPose.setToolTipText("Right forearm and hand pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(rightForearmPose, gbc);
        final JLabel label3 = new JLabel();
        label3.setText("Legs");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label3, gbc);
        legsBreed = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("H");
        legsBreed.setModel(defaultComboBoxModel2);
        legsBreed.setPreferredSize(new Dimension(76, 25));
        legsBreed.setToolTipText("Legs Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 12;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(legsBreed, gbc);
        leftThighPose = new JComboBox();
        leftThighPose.setMinimumSize(new Dimension(81, 25));
        leftThighPose.setPreferredSize(new Dimension(76, 25));
        leftThighPose.setToolTipText("Left thigh pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 14;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(leftThighPose, gbc);
        leftShinPose = new JComboBox();
        leftShinPose.setPreferredSize(new Dimension(76, 25));
        leftShinPose.setToolTipText("left shin pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 15;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(leftShinPose, gbc);
        leftFootPose = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        leftFootPose.setModel(defaultComboBoxModel3);
        leftFootPose.setPreferredSize(new Dimension(76, 25));
        leftFootPose.setToolTipText("left foot pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 16;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(leftFootPose, gbc);
        rightThighPose = new JComboBox();
        rightThighPose.setPreferredSize(new Dimension(76, 25));
        rightThighPose.setToolTipText("Right thigh pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 14;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(rightThighPose, gbc);
        rightShinPose = new JComboBox();
        rightShinPose.setPreferredSize(new Dimension(76, 25));
        rightShinPose.setToolTipText("Right shin pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 15;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(rightShinPose, gbc);
        rightFootPose = new JComboBox();
        rightFootPose.setPreferredSize(new Dimension(76, 25));
        rightFootPose.setToolTipText("Right foot pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 16;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(rightFootPose, gbc);
        final JLabel label4 = new JLabel();
        label4.setText("Left Leg");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 13;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label4, gbc);
        final JLabel label5 = new JLabel();
        label5.setText("Head");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label5, gbc);
        final JLabel label6 = new JLabel();
        label6.setText("Body");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label6, gbc);
        headPose = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        defaultComboBoxModel4.addElement("Far Down");
        defaultComboBoxModel4.addElement("Down");
        defaultComboBoxModel4.addElement("Straight");
        defaultComboBoxModel4.addElement("Up");
        defaultComboBoxModel4.addElement("Forward");
        defaultComboBoxModel4.addElement("Back");
        headPose.setModel(defaultComboBoxModel4);
        headPose.setPreferredSize(new Dimension(76, 25));
        headPose.setToolTipText("Head Pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(headPose, gbc);
        bodyBreed = new JComboBox();
        bodyBreed.setPreferredSize(new Dimension(76, 25));
        bodyBreed.setToolTipText("Body Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(bodyBreed, gbc);
        bodyTilt = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel5 = new DefaultComboBoxModel();
        bodyTilt.setModel(defaultComboBoxModel5);
        bodyTilt.setPreferredSize(new Dimension(76, 25));
        bodyTilt.setToolTipText("Body Pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(bodyTilt, gbc);
        earBreed = new JComboBox();
        earBreed.setMinimumSize(new Dimension(81, 25));
        final DefaultComboBoxModel defaultComboBoxModel6 = new DefaultComboBoxModel();
        earBreed.setModel(defaultComboBoxModel6);
        earBreed.setPreferredSize(new Dimension(76, 25));
        earBreed.setToolTipText("Hair Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(earBreed, gbc);
        hairBreed = new JComboBox();
        hairBreed.setPreferredSize(new Dimension(76, 25));
        hairBreed.setToolTipText("Ear Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(hairBreed, gbc);
        armsBreed = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel7 = new DefaultComboBoxModel();
        armsBreed.setModel(defaultComboBoxModel7);
        armsBreed.setPreferredSize(new Dimension(76, 25));
        armsBreed.setToolTipText("Arms Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(armsBreed, gbc);
        final JLabel label7 = new JLabel();
        label7.setText("Arms");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label7, gbc);
        final JLabel label8 = new JLabel();
        label8.setText("Tail");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 18;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label8, gbc);
        tailBreed = new JComboBox();
        tailBreed.setPreferredSize(new Dimension(76, 25));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 18;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(tailBreed, gbc);
        tailBasePose = new JComboBox();
        tailBasePose.setPreferredSize(new Dimension(76, 25));
        tailBasePose.setToolTipText("Tail base pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 19;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(tailBasePose, gbc);
        tailTipPose = new JComboBox();
        tailTipPose.setPreferredSize(new Dimension(76, 25));
        tailTipPose.setToolTipText("Tail tip pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 20;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(tailTipPose, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 17;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel2.add(spacer2, gbc);
        final JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 11;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel2.add(spacer3, gbc);
        final JPanel spacer4 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel2.add(spacer4, gbc);
        final JLabel label9 = new JLabel();
        label9.setText("Right Leg");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 13;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label9, gbc);
        final JLabel label10 = new JLabel();
        label10.setText("Breed");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label10, gbc);
        final JLabel label11 = new JLabel();
        label11.setText("Pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label11, gbc);
        final JLabel label12 = new JLabel();
        label12.setText("Ears");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label12, gbc);
        final JLabel label13 = new JLabel();
        label13.setText("Hair");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label13, gbc);
        final JLabel label14 = new JLabel();
        label14.setText("Up. Arm");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label14, gbc);
        final JLabel label15 = new JLabel();
        label15.setText("Forearm");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label15, gbc);
        final JLabel label16 = new JLabel();
        label16.setText("Thigh");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label16, gbc);
        final JLabel label17 = new JLabel();
        label17.setText("Shin");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 15;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label17, gbc);
        final JLabel label18 = new JLabel();
        label18.setText("Foot");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 16;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label18, gbc);
        final JLabel label19 = new JLabel();
        label19.setText("Base");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 19;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label19, gbc);
        final JLabel label20 = new JLabel();
        label20.setText("Tip");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 20;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label20, gbc);
        final JLabel label21 = new JLabel();
        label21.setText("Facing");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(label21, gbc);
        facing = new JComboBox();
        facing.setMinimumSize(new Dimension(81, 10));
        final DefaultComboBoxModel defaultComboBoxModel8 = new DefaultComboBoxModel();
        defaultComboBoxModel8.addElement("Left");
        defaultComboBoxModel8.addElement("Right");
        defaultComboBoxModel8.addElement("Front");
        defaultComboBoxModel8.addElement("Back");
        facing.setModel(defaultComboBoxModel8);
        facing.setPreferredSize(new Dimension(76, 25));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(facing, gbc);
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
        bodyTilt.setNextFocusableComponent(armsBreed);
        armsBreed.setNextFocusableComponent(armsBreed);
        tailBreed.setNextFocusableComponent(tailBasePose);
        tailTipPose.setNextFocusableComponent(headBreed);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private void createUIComponents() {
        imageHolder = new PoseRenderedImagePanel(project.getProjectFilePath());
    }

    public void addPoseChangeListener(final boolean updateImmediately, final PoseChangeListener listener) {
        poseChangeListeners.add(listener);
        if (updateImmediately) {
            listener.onPoseChange(getUpdatedPose());
        }
    }

    @Override
    public void dispose() {
        poseChangeListeners.clear();
    }

    public interface PoseChangeListener {
        void onPoseChange(Pose pose);
    }


}
