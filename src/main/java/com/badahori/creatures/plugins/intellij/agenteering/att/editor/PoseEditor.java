package com.badahori.creatures.plugins.intellij.agenteering.att.editor;

import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileData;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.PoseRenderer.CreatureSpriteSet;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.PoseRenderer.PartVisibility;
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.PoseRenderer.Pose;
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartFiles;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartsIndex;
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey;
import com.badahori.creatures.plugins.intellij.agenteering.indices.SpriteBodyPart;
import com.badahori.creatures.plugins.intellij.agenteering.utils.BufferedImageExtensionsKt;
import com.badahori.creatures.plugins.intellij.agenteering.utils.FileNameUtils;
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile;
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.JBColor;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SuppressWarnings("SpellCheckingInspection")
public class PoseEditor implements Disposable {
    private static final Logger LOGGER = Logger.getLogger("#PoseEditor");
    private static final char[] allParts = "abcdefghijklmnopq".toCharArray();
    private static final int ERROR_HEAD_POSE_C1E = 8;
    private static final int ERROR_HEAD_POSE_C2E = 10;
    private final Project project;
    private final BreedPartKey baseBreed;
    private final String[] directions = new String[]{
            "Up",
            "Straight",
            "Down",
            "Far Down"
    };

    private final String[] moodsC1 = new String[]{
            "Neutral",
            "Happy",
            "Sad",
            "Angry",
    };

    private final String[] moodsCV = new String[]{
            "Neutral",
            "Happy",
            "Sad",
            "Angry",
            "Surprised",
            "Sick",
            "Sick (Mouth Open)",
            "Elated",
            "Angry 2",
            "Concerned",
            "Sick",
            "Tongue Out",
            "Neutral 2",
            "Happy 3",
            "Cry 1",
            "Cry 2",
            "Neutral 3",
            "Neutral 4",
    };

    private final String[] moods = new String[]{
            "Neutral",
            "Happy",
            "Sad",
            "Angry",
            "Surprised",
            "Sick"
    };

    private final Map<Character, VirtualFile> manualAtts = new HashMap<>();
    private final List<PoseChangeListener> poseChangeListeners = Lists.newArrayList();
    JPanel panel1;
    JComboBox<VirtualFile> headBreed;
    JComboBox<VirtualFile> bodyBreed;
    JComboBox<String> rightUpperArmPose;
    JComboBox<String> leftUpperArmPose;
    JComboBox<String> headPose;
    JComboBox<String> bodyPose;
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

    // Initializes this pose editor given a variant
    private void init(CaosVariant variant) {
        setVariant(variant);
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
        headPose.setSelectedIndex(0);
        if (variant.isNotOld()) {
            headDirection2.setSelectedIndex(2);
        }
        initHeadComboBox(0, Integer.MAX_VALUE);
        if (variant == CaosVariant.C1.INSTANCE) {
            assign(mood, moodsC1, 0);
        } else if (variant == CaosVariant.CV.INSTANCE) {
            assign(mood, moodsCV, 0);
        } else {
            assign(mood, moods, 0);
        }
        freeze(headDirection2, ! headDirection2.isEnabled(), variant.isOld());
        setFacing(0);
        if (variant == CaosVariant.C1.INSTANCE && baseBreed.getGenus() != null && baseBreed.getGenus() == 1) {
            reverse(directions);
            assign(bodyPose, directions, 1);
            reverse(directions);
        } else {
            assign(bodyPose, directions, 1);
        }

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
        populate(tailBreed, files, 'm', 'n');
        populate(earBreed, files, 'o', 'p');
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
        defaultPoseAfterInit = getUpdatedPose(allParts);
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
        focusMode.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (visibilityFocus != null) {
                    visibilityMask = getVisibilityMask(visibilityFocus);
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
        // Head
        headBreed.addItemListener(e -> {
            if (didInitOnce) {
                manualAtts.remove('a');
                redraw('a');
            }
        });

        // Body
        bodyBreed.addItemListener(e -> {
            if (didInitOnce) {
                manualAtts.remove('b');
                redraw('b');
            }
        });
        // Legs
        legsBreed.addItemListener(e -> {
            if (didInitOnce) {
                manualAtts.remove('c');
                manualAtts.remove('d');
                manualAtts.remove('e');
                manualAtts.remove('f');
                manualAtts.remove('g');
                manualAtts.remove('h');
                redraw('c', 'd', 'e', 'f', 'g', 'h');
            }
        });
        // Arms
        armsBreed.addItemListener(e -> {
            if (didInitOnce) {
                manualAtts.remove('i');
                manualAtts.remove('j');
                manualAtts.remove('k');
                manualAtts.remove('l');
                redraw('i', 'j', 'k', 'l');
            }
        });
        // Tail
        tailBreed.addItemListener(e -> {
            if (didInitOnce) {
                manualAtts.remove('m');
                manualAtts.remove('n');
                redraw('m', 'n');
            }
        });
        // Ears
        earBreed.addItemListener(e -> {
            if (didInitOnce) {
                manualAtts.remove('o');
                manualAtts.remove('p');
                redraw('o', 'p');
            }
        });
        // Hair
        hairBreed.addItemListener(e -> {
            if (didInitOnce) {
                manualAtts.remove('q');
                redraw('q');
            }
        });

        // Poses
        headPose.addItemListener(e -> {
            if (didInitOnce && headPose.getItemCount() > 0) {
                if (drawImmediately) {
                    redraw('a');
                }
            }
        });

        headDirection2.addItemListener(e -> {
            if (didInitOnce && headPose.getItemCount() > 0) {
                if (drawImmediately) {
                    redraw('a');
                }
            }
        });

        mood.addItemListener(e -> {
            if (didInitOnce && mood.getItemCount() > 0) {
                if (drawImmediately) {
                    redraw('a');
                }
            }
        });

        eyesStatus.addItemListener(e -> {
            if (didInitOnce && mood.getItemCount() > 0) {
                if (drawImmediately) {
                    redraw('a');
                }
            }
        });

        bodyPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('b');
                }
            }
        });
        leftThighPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('c');
                }
            }
        });
        leftShinPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('d');
                }
            }
        });
        leftFootPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('e');
                }
            }
        });
        rightThighPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('f');
                }
            }
        });
        rightShinPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('g');
                }
            }
        });
        rightFootPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('h');
                }
            }
        });
        leftUpperArmPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('i');
                }
            }
        });
        leftForearmPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('j');
                }
            }
        });
        rightUpperArmPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('k');
                }
            }
        });
        rightForearmPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('l');
                }
            }
        });
        tailBasePose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('m');
                }
            }
        });
        tailTipPose.addItemListener(e -> {
            if (didInitOnce) {
                if (drawImmediately) {
                    redraw('n');
                }
            }
        });

        openRelated.addItemListener((e) -> {
            if (openRelated.getSelectedIndex() == 0) {
                return;
            }
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final Object fileObject = openRelated.getSelectedItem();
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
                openRelated.setSelectedIndex(0);
            }
        });
    }

    public void update(final CaosVariant variant) {
        setVariant(variant);
        valid = redraw(allParts);
    }

    /**
     * @return <b>True</b> if the last render was successful. <b>False</b> if it was not
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Gets the file given for selected breed and part
     *
     * @param baseFile virtual file referenced in the breed drop down
     * @param part     the part to get body/sprite data for
     * @return the selected Sprite/Body data object
     */
    @Nullable
    private SpriteBodyPart file(VirtualFile baseFile, char part) {
        if (baseFile == null) {
            return null;
        }
        return getPart(part, baseFile);
    }

    public void redrawAll() {
        drawImmediately = true;
        spriteSet = null;
        clear();
        redraw(allParts);
    }

    public void clear() {
        spriteSet = null;
        files = null;
        if (imageHolder != null) {
            ((CenteredImagePanel) imageHolder).clear();
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
                    files = BodyPartsIndex.variantParts(project, variant);
                    redrawActual(parts);
                });
                return false;
            } else {
                files = BodyPartsIndex.variantParts(project, variant);
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
        drawImmediately = true;
        final CreatureSpriteSet updatedSprites;
        try {
            updatedSprites = getUpdatedSpriteSet(parts);
        } catch (Exception e) {
            LOGGER.severe("Failed to located required sprites Error:(" + e.getClass().getSimpleName() + ") " + e.getLocalizedMessage());
            e.printStackTrace();
            return valid = false;
        }
        final Pose updatedPose = getUpdatedPose(parts);
        final BufferedImage image;
        try {
            image = PoseRenderer.render(variant, updatedSprites, updatedPose, visibilityMask, zoom.getSelectedIndex() + 1);
        } catch (Exception e) {
            LOGGER.severe("Failed to render pose. Error:(" + e.getClass().getSimpleName() + ") " + e.getLocalizedMessage());
            e.printStackTrace();
            return valid = false;
        }
        ((CenteredImagePanel) imageHolder).updateImage(image);
        return valid = true;
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
        facingLabel.setVisible(! show);
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
                return bodyPose;
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
     * Update the sprite set for changed chars and returns it
     *
     * @param parts parts that have changed
     * @return update sprite set
     */
    private CreatureSpriteSet getUpdatedSpriteSet(char... parts) {
        CreatureSpriteSet spriteTemp = spriteSet;
        if (spriteTemp == null) {
            spriteTemp = spriteSet = defaultSpriteSet();
        }
        if (parts.length < 1) {
            parts = allParts;
        }
        for (char part : parts) {
            switch (part) {
                case 'a':
                    spriteTemp.setHead(Objects.requireNonNull(getBodyData(headBreed, 'a')));
                    break;
                case 'b':
                    spriteTemp.setBody(Objects.requireNonNull(getBodyData(bodyBreed, 'b')));
                    break;
                case 'c':
                    spriteTemp.setLeftThigh(Objects.requireNonNull(getBodyData(legsBreed, 'c')));
                    break;
                case 'd':
                    spriteTemp.setLeftShin(Objects.requireNonNull(getBodyData(legsBreed, 'd')));
                    break;
                case 'e':
                    spriteTemp.setLeftFoot(Objects.requireNonNull(getBodyData(legsBreed, 'e')));
                    break;
                case 'f':
                    spriteTemp.setRightThigh(Objects.requireNonNull(getBodyData(legsBreed, 'f')));
                    break;
                case 'g':
                    spriteTemp.setRightShin(Objects.requireNonNull(getBodyData(legsBreed, 'g')));
                    break;
                case 'h':
                    spriteTemp.setRightFoot(Objects.requireNonNull(getBodyData(legsBreed, 'h')));
                    break;
                case 'i':
                    spriteTemp.setLeftUpperArm(Objects.requireNonNull(getBodyData(armsBreed, 'i')));
                    break;
                case 'j':
                    spriteTemp.setLeftForearm(Objects.requireNonNull(getBodyData(armsBreed, 'j')));
                    break;
                case 'k':
                    spriteTemp.setRightUpperArm(Objects.requireNonNull(getBodyData(armsBreed, 'k')));
                    break;
                case 'l':
                    spriteTemp.setRightForearm(Objects.requireNonNull(getBodyData(armsBreed, 'l')));
                    break;
                case 'm':
                    spriteTemp.setTailBase(getBodyData(tailBreed, 'm'));
                    break;
                case 'n':
                    spriteTemp.setTailTip(getBodyData(tailBreed, 'n'));
                    break;
                case 'o':
                    spriteTemp.setLeftEar(getBodyData(earBreed, 'o'));
                    break;
                case 'p':
                    spriteTemp.setRightEar(getBodyData(earBreed, 'p'));
                    break;
                case 'q':
                    spriteTemp.setHair(getBodyData(hairBreed, 'q'));
                    break;
                default:
                    break;
            }
        }
        spriteSet = spriteTemp;
        return spriteTemp;
    }

    /**
     * Generate a set of default sprites for this pose editor
     *
     * @return sprite set with default breeds applied to the parts
     */
    private CreatureSpriteSet defaultSpriteSet() {
        return new CreatureSpriteSet(
                Objects.requireNonNull(file(getBreed(headBreed), 'a')),
                Objects.requireNonNull(file(getBreed(bodyBreed), 'b')),
                Objects.requireNonNull(file(getBreed(legsBreed), 'c')),
                Objects.requireNonNull(file(getBreed(legsBreed), 'd')),
                Objects.requireNonNull(file(getBreed(legsBreed), 'e')),
                Objects.requireNonNull(file(getBreed(legsBreed), 'f')),
                Objects.requireNonNull(file(getBreed(legsBreed), 'g')),
                Objects.requireNonNull(file(getBreed(legsBreed), 'h')),
                Objects.requireNonNull(file(getBreed(armsBreed), 'i')),
                Objects.requireNonNull(file(getBreed(armsBreed), 'j')),
                Objects.requireNonNull(file(getBreed(armsBreed), 'k')),
                Objects.requireNonNull(file(getBreed(armsBreed), 'l')),
                file(getBreed(tailBreed), 'm'),
                file(getBreed(tailBreed), 'n'),
                file(getBreed(earBreed), 'o'),
                file(getBreed(earBreed), 'p'),
                file(getBreed(hairBreed), 'q')
        );
    }

    /**
     * Gets the breed body data file for the combo box
     *
     * @param menu breed combo box
     * @return virtual file of body data for breed selected
     */
    private VirtualFile getBreed(JComboBox<VirtualFile> menu) {
        return menu.getItemCount() > 0 ? (VirtualFile) menu.getSelectedItem() : null;
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
        if (items.length > selectedIndex) {
            menu.setSelectedIndex(selectedIndex);
        }

    }

    /**
     * Populates a breed combo box with available breed files
     *
     * @param menu      breed combo box
     * @param files     a list of all available body data regardless of actual part
     * @param partChars parts to filter breeds by
     */
    private void populate(JComboBox<VirtualFile> menu, final List<BodyPartFiles> files, Character... partChars) {
        // Set the cell renderer for the Att file list
        menu.setRenderer(new BreedFileCellRenderer());

        // Filter list of body part files for breeds applicable to this list of parts
        List<VirtualFile> items = findBreeds(files, partChars);

        // Assign values to this drop down
        assign(menu, items.toArray(new VirtualFile[0]), 0);

        // No matching files, skip item selectors
        if (items.isEmpty()) {
            return;
        }

        // Find all breed files matching this path
        List<Pair<Integer, VirtualFile>> matchingBreedFiles = Lists.newArrayList();
        String baseBreedString = "" + baseBreed.get(1) + "" + baseBreed.get(2) + "" + baseBreed.get(3);
        for (int i = 0; i < items.size(); i++) {
            VirtualFile file = items.get(i);
            String thisBreed = file.getNameWithoutExtension().substring(1);

            if (thisBreed.equals(baseBreedString)) {
                if (rootPath != null && file.getPath().startsWith(rootPath)) {
                    menu.setSelectedIndex(i);
                    return;
                }
                // Breed string matches
                matchingBreedFiles.add(new Pair<>(i, file));
            }
        }


        // TODO: figure out if I should prioritize matching breed or matching path
        //  I think breed though. Not sure.

        // If root path was set, find matching att files for this part.
        if (rootPath != null) {
            // If a matching breed was not found in root folder
            // Look for any other breed file in the root folder.
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getPath().startsWith(rootPath)) {
                    menu.setSelectedIndex(i);
                    return;
                }
            }
        }

        // If there are any breed files matching the base breed
        // Use them first.
        if (! matchingBreedFiles.isEmpty()) {
            menu.setSelectedIndex(matchingBreedFiles.get(0).first);
            return;
        }

        // Nothing was found, so just select the first item in the list
        menu.setSelectedIndex(0);
    }

    /**
     * Finds breeds for a given part
     *
     * @param files     all available breed files for any part
     * @param partChars parts to filter the breed list for
     * @return breed body files available for the parts
     */
    private List<VirtualFile> findBreeds(List<BodyPartFiles> files, Character... partChars) {
        BreedPartKey key = baseBreed
                .copyWithBreed(null)
                .copyWithPart(null);
        List<Character> parts = Arrays.asList(partChars);
        Stream<VirtualFile> out = files
                .stream()
                .filter(part -> {
                    BreedPartKey thisKey = part.getKey();
                    if (thisKey == null || thisKey.getBreed() == null) {
                        return false;
                    }
                    return BreedPartKey.isGenericMatch(thisKey, key) && parts.contains(thisKey.getPart());
                })
                .map(BodyPartFiles::getBodyDataFile);
        return out.collect(Collectors.toList());
    }

    /**
     * Gets body data from a breed combo box for a given part
     * Breed drop downs can cover multiple body parts, but hold a reference to only one part.
     *
     * @param menu     breed combo box
     * @param partChar part to look for
     * @return Sprite body part data
     */
    @Nullable
    private SpriteBodyPart getBodyData(JComboBox<VirtualFile> menu, final char partChar) {
        VirtualFile breedFile = ((VirtualFile) menu.getSelectedItem());
        if (breedFile == null) {
            for (int i = 0; i < menu.getItemCount(); i++) {
                breedFile = menu.getItemAt(i);
                if (breedFile != null) {
                    break;
                }
            }
        }
        if (breedFile == null) {
            return null;
        }
        return getPart(partChar, breedFile);
    }

    /**
     * Gets the body part file given a specific or related breed file
     *
     * @param partChar  char to look for
     * @param breedFile selected breed file in drop down
     * @return Sprite body part data for a part and breed file
     */
    @Nullable
    private SpriteBodyPart getPart(char partChar,
                                   @NotNull
                                           VirtualFile breedFile) {
        // Ensure that breed file has parent
        if (breedFile.getParent() == null) {
            LOGGER.severe("Breed file parent is null");
            return null;
        }
        final String parentPath = breedFile.getParent().getPath();
        final String partString = partChar + "";
        List<BodyPartFiles> matching;
        // Generate a key from this breed file
        BreedPartKey key = BreedPartKey.fromFileName("" + partChar + breedFile.getNameWithoutExtension().substring(1), variant);

        // Find matching breed file for creature
        matching = files.stream()
                .filter(b -> b.getKey() != null && BreedPartKey.isGenericMatch(b.getKey(), key) && b.getBodyDataFile().getPath().startsWith(parentPath) && b.getBodyDataFile().getName().startsWith(partString))
                .collect(Collectors.toList());

        // If item was found for this breed
        // Wrap it to return
        Optional<BodyPartFiles> selected;
        if (! matching.isEmpty()) {
            selected = Optional.of(matching.get(0));
        } else {
            // Body part data does not exist for this part and breed
            // Happens when upper arm has breed sprite but lower arm doesn't, etc
            // Get a breed free key
            BreedPartKey fallback = baseBreed
                    .copyWithBreed(null)
                    .copyWithPart(partChar);
            // Filter file age, gender, genus and part
            selected = files.stream()
                    .filter(b -> b.getKey() != null && BreedPartKey.isGenericMatch(b.getKey(), fallback))
                    .findFirst();
        }

        // If still nothing was found, bail out
        if (! selected.isPresent()) {
            return null;
        }

        // Get body part Files
        BodyPartFiles out = selected.get();

        // If manual att was passed in, use it instead
        if (manualAtts.containsKey(partChar)) {
            out = out.copy(out.getSpriteFile(), manualAtts.get(partChar));
        }

        // Return the resolved sprite and att file from the two virtual files
        return out.data(project);
    }

    /**
     * Sets the root path for all related sprite and att files
     * There was a problem when multiple files exist with the same name in different folders
     * This matches the breed files to those in the same directory
     *
     * @param path parent path for all related breed files
     */
    public void setRootPath(String path) {
        this.rootPath = path;
        drawImmediately = false;
        if (((CenteredImagePanel) imageHolder).lastDirectory == null || ((CenteredImagePanel) imageHolder).lastDirectory.length() < 1) {
            ((CenteredImagePanel) imageHolder).lastDirectory = path;
        }
        selectWithRoot(headBreed, path);
        selectWithRoot(bodyBreed, path);
        selectWithRoot(legsBreed, path);
        selectWithRoot(armsBreed, path);
        selectWithRoot(tailBreed, path);
        selectWithRoot(earBreed, path);
        selectWithRoot(hairBreed, path);
        initOpenRelatedComboBox();
        redraw(allParts);
    }

    /**
     * Selects the combo box option matching the root path
     *
     * @param box      breed combo box
     * @param rootPath path to find children for
     */
    private void selectWithRoot(JComboBox<VirtualFile> box, String rootPath) {
        if (box.getItemCount() == 0) {
            return;
        }
        final String breedKey = "" + baseBreed.get(1) + baseBreed.get(2) + baseBreed.get(3);
        Integer matches = null;
        for (int i = 0; i < box.getItemCount(); i++) {
            final VirtualFile file = box.getItemAt(i);
            if (file.getPath().startsWith(rootPath)) {
                if (breedKey.equals(file.getNameWithoutExtension().substring(1))) {
                    box.setSelectedIndex(i);
                    return;
                }
                if (matches == null) {
                    matches = i;
                }
            }
        }
        if (matches != null) {
            box.setSelectedIndex(matches);
        }
    }

    /**
     * Sets facing direction for the pose renderer
     *
     * @param direction direction that the creature will be facing
     */
    public void setFacing(int direction) {
        final int oldDirection = facing.getSelectedIndex();
        initHeadComboBox(direction, oldDirection);
        facing.setSelectedIndex(direction);
        try {
            resetIfNeeded();
        } catch (Exception e) {
            LOGGER.severe("Failed to reset pose combo boxes");
        }
        if (drawImmediately) {
            redraw(allParts);
        }
    }

    public void resetIfNeeded() {
        int resetCount = 0;
        for (char part : allParts) {
            final JComboBox<String> box = getComboBoxForPart(part);
            if (box == null || box.getItemCount() < 1 || box.getSelectedIndex() == 0) {
                resetCount++;
            }
        }

        if (resetCount > (allParts.length - 3) && defaultPoseAfterInit != null) {
            if (facing.getSelectedIndex() < 2) {
                headPose.setSelectedIndex(3);
                bodyPose.setSelectedIndex(1);
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
            tailBasePose.setSelectedIndex(2);
            tailTipPose.setSelectedIndex(2);
        }
    }

    public void initHeadComboBox(final int direction, final int oldDirection) {
        if (variant.isOld()) {
            if (direction >= 2 && oldDirection != direction) {
                assign(headPose, new String[]{
                        "L. Far Up",
                        "L. Up",
                        "L. Straight",
                        "L. Down",
                        "R. Up",
                        "R. Far Up",
                        "R. Straight",
                        "R. Down",
                        direction == 2 ? "Forward" : "Backward"
                }, 8);
            } else if (oldDirection >= 2 && direction < 2) {
                assign(headPose, new String[]{
                        "Far Up",
                        "Up",
                        "Straight",
                        "Down",
                        "Forward",
                        "Backward"
                }, 2);
            }
        } else {
            if (direction == 2 && oldDirection != 2) {
                assign(headPose, new String[]{
                        "Left",
                        "Right",
                        "Forward"
                }, 2);
            } else if (direction == 3 && oldDirection != 3) {
                assign(headPose, new String[]{
                        "Left",
                        "Right",
                        "Backward"
                }, 2);
            } else if (direction < 2 && oldDirection != direction) {
                assign(headPose, new String[]{
                        direction == 0 ? "Left" : "Right",
                        "Forward",
                        "Backward"
                }, 0);
            }
        }
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
        final int facing;
        if (bodyPose < 4) {
            facing = 0;
        } else if (bodyPose < 8) {
            facing = 1;
        } else if (variant.isOld()) {
            if (bodyPose == 8) {
                facing = 2;
            } else if (bodyPose == 9) {
                facing = 3;
            } else {
                LOGGER.severe("Invalid body pose '" + bodyPose + "' for facing test");
                return;
            }
        } else {
            if (bodyPose < 12) {
                facing = 2;
            } else if (bodyPose < 16) {
                facing = 3;
            } else {
                LOGGER.severe("Invalid body pose '" + bodyPose + "' for facing test");
                return;
            }
        }
        final int offset;
        if (facing == 0) {
            offset = 0;
        } else if (facing == 1) {
            offset = 4;
        } else if (facing == 2) {
            if (variant.isOld()) {
                offset = 0;
            } else {
                offset = 8;
            }
        } else {
            if (variant.isOld()) {
                offset = 0;
            } else {
                offset = 12;
            }
        }
        int headPose = pose.getHead();
        if (setFacing) {
            setFacing(facing);
        }
        translateAndSetPart('a', facing, offset, headPose);
        translateAndSetPart('b', facing, offset, bodyPose);
        translateAndSetPart('c', facing, offset, pose.getLeftThigh());
        translateAndSetPart('d', facing, offset, pose.getLeftShin());
        translateAndSetPart('e', facing, offset, pose.getLeftFoot());
        translateAndSetPart('f', facing, offset, pose.getRightThigh());
        translateAndSetPart('g', facing, offset, pose.getRightShin());
        translateAndSetPart('h', facing, offset, pose.getRightFoot());
        translateAndSetPart('i', facing, offset, pose.getLeftUpperArm());
        translateAndSetPart('j', facing, offset, pose.getLeftForearm());
        translateAndSetPart('k', facing, offset, pose.getRightUpperArm());
        translateAndSetPart('l', facing, offset, pose.getRightForearm());
        translateAndSetPart('m', facing, offset, pose.getTailBase());
        translateAndSetPart('n', facing, offset, pose.getTailTip());
        translateAndSetPart('o', facing, offset, pose.getEars());
        translateAndSetPart('p', facing, offset, pose.getEars());
        translateAndSetPart('q', facing, offset, pose.getHead());
        drawImmediately = true;
        this.pose = pose;
        redraw();
        if (variant.isOld() && pose.getBody() >= 8) {
            this.pose = null;
        }
    }

    private void translateAndSetPart(final char part, final int facing, final int offset, final int pose) {
        final int translatedPose;
        if (part == 'a') {
            setHeadPose(facing, pose);
            return;
        }
        if (variant.isOld()) {
            if (facing == 2) {
                translatedPose = 4;
            } else if (facing == 3) {
                translatedPose = 5;
            } else {
                final int offsetPose = (pose - offset);
                if (offsetPose > 3) {
                    LOGGER.severe("Part '" + part + "' has invalid pose offset '" + offsetPose + "' expected 0..3");
                    return;
                }
                translatedPose = 3 - offsetPose;
            }
        } else {
            final int offsetPose = pose % 4;
            if (offsetPose < 0) {
                LOGGER.severe("Part '" + part + "' has invalid pose offset '" + offsetPose + "' expected 0..3; Pose: " + pose + "; Offset: " + offset + "; Facing: " + facing);
                return;
            }
            translatedPose = 3 - offsetPose;
        }
        setPose(part, translatedPose);
    }


    private void setHeadPose(final int facing, final int pose) {
        if (variant.isOld()) {
            setHeadPoseOldVariant(facing, pose);
        } else {
            setHeadPoseNewVariant(facing, pose);
        }
    }


    private void setHeadPoseOldVariant(final int facing, final int pose) {
        final int translatedPose;
        if (facing == 2 || facing == 3) {
            translatedPose = pose;
        } else {
            final int offsetPose = pose % 4;
            translatedPose = 3 - offsetPose;
        }
        final int moodIndex;
        if (variant == CaosVariant.C1.INSTANCE) {
            if (pose < 10) {
                moodIndex = 0;
            } else {
                moodIndex = (pose - 9);
            }
        } else {
            moodIndex = (int) Math.floor(pose / 20.0);
        }
        if (mood.getItemCount() > moodIndex) {
            mood.setSelectedIndex(moodIndex);
        }
        headPose.setSelectedIndex(translatedPose);
    }

    private void setHeadPoseNewVariant(final int facing, final int pose) {
        final int translatedPose = 3 - (pose % 4);
        if (pose < 4) {
            headPose.setSelectedIndex(0);
        } else if (pose < 8) {
            if (facing < 2) {
                headPose.setSelectedIndex(0);
            } else {
                headPose.setSelectedIndex(1);
            }
        } else if (pose < 12) {
            // If face is facing forward
            if (facing < 2) {
                headPose.setSelectedIndex(1);
            } else {
                headPose.setSelectedIndex(2);
            }
        } else if (pose < 16) {
            // If Face is facing backwards
            headPose.setSelectedIndex(2);
        }
        final int moodIndex = (int) Math.floor(pose / 20.0);
        if (mood.getItemCount() > moodIndex) {
            mood.setSelectedIndex(moodIndex);
        }
        headDirection2.setSelectedIndex(translatedPose);
        LOGGER.info("Setting head selected index to " + headPose.getSelectedIndex() + "; Set head tilt to: " + headDirection2.getSelectedIndex());
    }

    /**
     * Sets the part to center focus modes around
     *
     * @param partChar part to focus on
     */
    public void setVisibilityFocus(char partChar) {
        visibilityFocus = partChar;
        visibilityMask = getVisibilityMask(partChar);
        redraw(partChar);
    }

    /**
     * Sets the visibility mask given a focus part
     *
     * @param part part to focus
     * @return visibility mask for the part and selected focus mode
     */
    public Map<Character, PartVisibility> getVisibilityMask(char part) {
        Map<Character, PartVisibility> parts = Collections.emptyMap();
        PartVisibility associatedPartVisibility = null;
        switch (focusMode.getSelectedIndex()) {
            // 0 - Everything
            case 0:
                parts = PartVisibility.getAllVisible();
                break;
            // 1 - Ghost
            case 1:
                parts = PartVisibility.getAllGhost();
                associatedPartVisibility = PartVisibility.GHOST;
                break;
            // 2 - Ghost (Solo)
            case 2:
                parts = PartVisibility.getAllHidden();
                associatedPartVisibility = PartVisibility.GHOST;
                parts.put('b', PartVisibility.HIDDEN);
                break;
            // 4 - Solo
            case 3:
                parts = PartVisibility.getAllHidden();
                associatedPartVisibility = PartVisibility.VISIBLE;
                break;
            // 5 - Solo (With Body)
            case 4:
                parts = PartVisibility.getAllHidden();
                associatedPartVisibility = PartVisibility.VISIBLE;
                parts.put('b', PartVisibility.VISIBLE);
                break;
            // 6 - Solo (Ghost Body)
            case 5:
                parts = PartVisibility.getAllHidden();
                associatedPartVisibility = PartVisibility.VISIBLE;
                parts.put('b', PartVisibility.GHOST);
                break;
        }
        if (associatedPartVisibility != null) {
            applyVisibility(parts, associatedParts(part), associatedPartVisibility);
        }
        parts.put(part, PartVisibility.VISIBLE);
        return parts;
    }

    /**
     * Applies the visibility to the body parts for use in the renderer
     *
     * @param parts      all parts in pose system
     * @param associated which parts to apply visibility to
     * @param visibility what visibility to apply
     */
    private void applyVisibility(Map<Character, PartVisibility> parts, List<Character> associated, PartVisibility visibility) {
        for (char part : associated) {
            parts.put(part, visibility);
        }
    }

    /**
     * Finds associated parts given a char for use in focus modes
     *
     * @param part part to find related for
     * @return related parts
     */
    public List<Character> associatedParts(final char part) {
        String partString = "";
        switch (part) {
            case 'a':
                partString = "opq";
                break;
            case 'b':
                partString = "acfikm";
                break;
            case 'c':
                partString = "de";
                break;
            case 'd':
                partString = "ce";
                break;
            case 'e':
                partString = "cd";
                break;
            case 'f':
                partString = "gh";
                break;
            case 'g':
                partString = "fh";
                break;
            case 'h':
                partString = "fg";
                break;
            case 'i':
                return Collections.singletonList('j');
            case 'j':
                return Collections.singletonList('i');
            case 'k':
                return Collections.singletonList('l');
            case 'l':
                return Collections.singletonList('k');
            case 'm':
                return Collections.singletonList('n');
            case 'n':
                return Collections.singletonList('m');
            case 'o':
            case 'p':
            case 'q':
                return Collections.singletonList('a');
        }
        return partString
                .chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
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
        if (charPart == 'a') {
            setHeadPose(facing.getSelectedIndex(), pose);
            return;
        }
        JComboBox<String> comboBox = getComboBoxForPart(charPart);
        if (comboBox == null) {
            return;
        }
        // If variant is old, any part facing front but face forces all other parts front
        if (variant.isOld()) {
            // Face all parts front
            if (pose == 4) {
                pose = 0;
            } else if (pose == 5) {
                pose = 0;
            } else if (comboBox.getItemCount() == 1) {
                // Pose is neither front nor back, so fill in directions if not already filled in
                assign(bodyPose, directions, 1);
            }
        }

        if (comboBox.getItemCount() == 0) {
            LOGGER.severe("No items in combo box: " + comboBox.getToolTipText());
            return;
        }

        // Select the pose in the dropdown box
        if (pose >= comboBox.getItemCount()) {
            pose = 0;
        }
        comboBox.setSelectedIndex(pose);
        // Redraw the image
        if (drawImmediately) {
            redraw(charPart);
        }
    }

    /**
     * Updates the pose object with whatever parts may have changed according to the char parts given
     *
     * @param parts parts that need recalculating
     * @return the updated pose, though the instance pose object is also updated
     */
    public Pose getUpdatedPose(char... parts) {
        final int offset;

        // Get facing direction to calculate the sprite offset in the sprite file
        int facingDirection = facing.getSelectedIndex();
        if (facingDirection == 0) {
            offset = 0;
        } else if (facingDirection == 1) {
            offset = 4;
        } else if (facingDirection == 2) {
            offset = 8;
        } else if (facingDirection == 3) {
            offset = variant.isOld() ? 9 : 12;
        } else {
            LOGGER.severe("Invalid direction offset");
            return pose;
        }
        // Gets the pose object for editing
        Pose poseTemp = pose;
        final int lastPoseHash;
        // If the pose object is not yet initialized, initialize it
        if (poseTemp == null) {
            int def = offset + 3;
            lastPoseHash = 0;
            poseTemp = pose = new Pose(offset + 2, def, def, def, def, def, def, def, def, def, def, def, def, def, def);
        } else {
            lastPoseHash = poseTemp.hashCode();
        }
        // Go through each part passed in for updating, and update it.
        for (char part : parts) {
            switch (part) {
                case 'a':
                    poseTemp.setHead(getActualHeadPose(facingDirection));
                    break;
                case 'b':
                    poseTemp.setBody(getBodyPartPose(bodyPose, facingDirection, offset));
                    break;
                case 'c':
                    poseTemp.setLeftThigh(getBodyPartPose(leftThighPose, facingDirection, offset));
                    break;
                case 'd':
                    poseTemp.setLeftShin(getBodyPartPose(leftShinPose, facingDirection, offset));
                    break;
                case 'e':
                    poseTemp.setLeftFoot(getBodyPartPose(leftFootPose, facingDirection, offset));
                    break;
                case 'f':
                    poseTemp.setRightThigh(getBodyPartPose(rightThighPose, facingDirection, offset));
                    break;
                case 'g':
                    poseTemp.setRightShin(getBodyPartPose(rightShinPose, facingDirection, offset));
                    break;
                case 'h':
                    poseTemp.setRightFoot(getBodyPartPose(rightFootPose, facingDirection, offset));
                    break;
                case 'i':
                    poseTemp.setLeftUpperArm(getBodyPartPose(leftUpperArmPose, facingDirection, offset));
                    break;
                case 'j':
                    poseTemp.setLeftForearm(getBodyPartPose(leftForearmPose, facingDirection, offset));
                    break;
                case 'k':
                    poseTemp.setRightUpperArm(getBodyPartPose(rightUpperArmPose, facingDirection, offset));
                    break;
                case 'l':
                    poseTemp.setRightForearm(getBodyPartPose(rightForearmPose, facingDirection, offset));
                    break;
                case 'm':
                    poseTemp.setTailBase(getBodyPartPose(tailBasePose, facingDirection, offset));
                    break;
                case 'n':
                    poseTemp.setTailTip(getBodyPartPose(tailTipPose, facingDirection, offset));
                    break;
                case 'o':
                case 'p':
                    poseTemp.setEars(getEars(getActualHeadPose(facingDirection)));
                default:
                    break;
            }
        }
        // Set the pose object back to itself or with a new version if it doesn't already exist.
        pose = poseTemp;
        if (poseTemp.hashCode() != lastPoseHash) {
            final Pose out = poseTemp;
            poseChangeListeners.forEach((it) -> it.onPoseChange(out));
        }
        return poseTemp;
    }

    private int getActualHeadPose(final int facingDirection) {
        // Head is funny, and needs special handling
        int temp = headPose.getSelectedIndex();
        final int mood = this.mood.getSelectedIndex();
        final boolean eyesClosed = eyesStatus.getSelectedIndex() > 0;
        if (variant.isOld()) {
            if (facingDirection >= 2) {
                if (temp < 0) {
                    LOGGER.severe("No head pose selected");
                    return ERROR_HEAD_POSE_C1E;
                } else if (temp > 9) {
                    LOGGER.severe("Invalid head pose encountered for pose '" + temp + "' expected 0..10");
                    return ERROR_HEAD_POSE_C1E;
                }
                if (temp < 4) {
                    temp = 3 - temp;
                } else if (temp < 8) {
                    temp -= 4;
                    temp = 4 + (3 - temp);
                } else if (facingDirection == 3) {
                    temp = 9;
                }
            } else if (temp == 4) {
                temp = 8;
            } else if (temp == 5) {
                temp = 9;
            } else if (facingDirection == 0) {
                temp = 3 - temp;
            } else if (facingDirection == 1) {
                temp = 4 + (3 - temp);
            } else if (temp > 9) {
                LOGGER.severe("Invalid head pose " + temp + " found. SelectedIndex: " + headPose.getSelectedIndex() + ";");
                return ERROR_HEAD_POSE_C1E;
            }
        } else if (temp >= 0) {
            if (headPose.getItemCount() == 3) {
                // If first option (Can be left or right)
                if (temp == 0) {
                    // If facing is right, value is right (1)
                    if (facingDirection == 1) {
                        temp = 1;
                    } // Else: Value is truly left (0)
                } else if (temp == 1) {
                    // Second slot can be right or forward
                    // If facing left or right
                    if (facingDirection < 2) {
                        // third slot is Forward
                        temp = 2;
                    } // else slot is right
                } else if (temp == 2) {
                    // Can be Forward or back
                    if (facingDirection == 3 || facingDirection < 2) {
                        // If not expressly facing forward, then the last slot is back
                        temp = 3;
                    }
                }
            }
            final int offsetBase = temp * 4;
            temp = (3 - headDirection2.getSelectedIndex());
            temp += offsetBase;
        } else {
            LOGGER.severe("Head pose is set to a number less than 0. Perhaps no head pose index is selected");
            temp = ERROR_HEAD_POSE_C2E;
        }
        if (variant != CaosVariant.C1.INSTANCE) {
            if (variant == CaosVariant.C2.INSTANCE) {
                temp += (mood * 20) + (eyesClosed ? 10 : 0);
            } else {
                temp += (mood * 32) + (eyesClosed ? 16 : 0);
            }
        } else {
            if (temp == 8) {
                if (mood != 0) {
                    temp += mood + 1;
                }
            }
            if (eyesClosed) {
                temp += 8;
            }
        }
        return temp;
    }

    private int getEars(final int headPose) {
        final int index = headPose % 16;
        if (headPose >= 160) {
            return index + 16;
        }
        if (headPose >= 128) {
            return index + 48;
        }
        if (headPose >= 96) {
            return index + 32;
        }
        if (headPose >= 64) {
            return index + 16;
        }
        if (headPose >= 32) {
            return index + 32;
        }
        return index;
    }

    /**
     * Gets the pose for a given combobox
     *
     * @param box             combo box to check
     * @param facingDirection facing direction of creature
     * @param offset          offset into sprite set
     * @return pose index in sprite file
     */
    private int getBodyPartPose(JComboBox<String> box, int facingDirection, int offset) {
        return getBodyPartPose(box, facingDirection, offset, true);
    }

    /**
     * Gets the pose for a given combobox
     *
     * @param box             combo box to check
     * @param facingDirection facing direction of creature
     * @param offset          offset into sprite set
     * @param invert          whether or not to invert the pose from 1-4
     * @return pose index in sprite file
     */
    private int getBodyPartPose(JComboBox<String> box, int facingDirection, int offset, boolean invert) {
        int pose = box.getSelectedIndex();
        if (pose < 0) {
            return 0;
        }
        if (variant.isOld()) {
            if (invert && pose < 4) {
                pose = 3 - pose;
            }
            if (facingDirection == 2 || pose == 4) {
                pose = 8;
            } else if (facingDirection == 3 || pose == 5) {
                pose = 9;
            } else {
                pose += offset;
            }
            if (pose > 9 || pose < 0) {
                LOGGER.severe("Invalid body pose " + pose + "found. SelectedIndex: " + bodyPose.getSelectedIndex() + "; Offset: " + offset + ";");
                if (facingDirection == 0) {
                    pose = 2;
                } else if (facingDirection == 1) {
                    pose = 6;
                } else {
                    pose = 8;
                }
            }
        } else {
            if (invert) {
                pose = 3 - pose;
            }
            pose += offset;
        }
        return pose;
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
        bodyPose = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel5 = new DefaultComboBoxModel();
        bodyPose.setModel(defaultComboBoxModel5);
        bodyPose.setPreferredSize(new Dimension(76, 25));
        bodyPose.setToolTipText("Body Pose");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(bodyPose, gbc);
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
        bodyBreed.setNextFocusableComponent(bodyPose);
        bodyPose.setNextFocusableComponent(armsBreed);
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
        imageHolder = new CenteredImagePanel(project.getProjectFilePath());
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

    interface PoseChangeListener {
        void onPoseChange(Pose pose);
    }

    /**
     * Panel to draw the rendered pose with
     */
    private static class CenteredImagePanel extends JPanel {
        private final String defaultDirectory;
        private BufferedImage image;
        private Dimension minSize;
        private String lastDirectory;
        private final PopUp popUp = new PopUp();

        public CenteredImagePanel(final String startingDirectory) {
            this.defaultDirectory = startingDirectory;
            initHandlers();
        }

        private void initHandlers() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showPopUp(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    showPopUp(e);
                }
            });
        }

        private void showPopUp(MouseEvent e) {
            if (e.isPopupTrigger() || (((e.getModifiers() | Event.CTRL_MASK) == Event.CTRL_MASK) && e.getButton() == 1)) {
                popUp.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        public void saveImageAs() {
            final BufferedImage image = this.image;
            if (image == null) {
                final DialogBuilder builder = new DialogBuilder();
                builder.setTitle("Pose save error");
                builder.setErrorText("Cannot save unrendered image");
                builder.show();
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Specify a file to save");
            File targetDirectory = null;
            if (lastDirectory != null && lastDirectory.length() > 3) {
                targetDirectory = new File(lastDirectory);
            }
            if (targetDirectory == null || ! targetDirectory.exists()) {
                targetDirectory = new File(defaultDirectory);
            }

            if (targetDirectory.exists()) {
                fileChooser.setCurrentDirectory(targetDirectory);
            }

            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File outputFileTemp = fileChooser.getSelectedFile();
            final String extension = FileNameUtils.getExtension(outputFileTemp.getName());
            if (extension == null || !extension.equalsIgnoreCase("png")) {
                outputFileTemp = new File(outputFileTemp.getPath() + ".png");
            }
            lastDirectory = outputFileTemp.getParent();
            final File outputFile = outputFileTemp;
            ApplicationManager.getApplication().runWriteAction(() -> {
                if (!outputFile.exists()) {
                    boolean didCreate = false;
                    try {
                        didCreate = outputFile.createNewFile();
                    } catch (IOException ignored) {
                    }
                    if (! didCreate) {
                        final DialogBuilder builder = new DialogBuilder();
                        builder.setTitle("Pose save error");
                        builder.setErrorText("Failed to create pose file '" + outputFile.getName() + "' for writing");
                        builder.show();
                        return;
                    }
                }
                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    byte[] bytes = null;
                    try {
                        bytes = BufferedImageExtensionsKt.toPngByteArray(image);
                    } catch (AssertionError e) {
                        LOGGER.severe(e.getLocalizedMessage());
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (bytes == null || bytes.length < 20) {
                        final DialogBuilder builder = new DialogBuilder();
                        builder.setTitle("Pose save error");
                        builder.setErrorText("Failed to prepare rendered pose for writing");
                        builder.show();
                        return;
                    }
                    outputStream.write(bytes);
                } catch (IOException e) {
                    final DialogBuilder builder = new DialogBuilder();
                    builder.setTitle("Pose save error");
                    builder.setErrorText("Failed to save pose image to '" + outputFile.getPath() + "'");
                    builder.show();
                }
                final VirtualFile thisFile = VfsUtil.findFileByIoFile(outputFile.getParentFile(), true);
                if (thisFile != null && thisFile.getParent() != null) {
                    thisFile.getParent().refresh(false, true);
                }
            });
        }

        public void copyToClipboard() {
            if (image == null) {
                return;
            }
            BufferedImageExtensionsKt.copyToClipboard(image);
        }

        public void clear() {
            image = null;
            revalidate();
            repaint();
        }

        public void updateImage(
                @NotNull
                        BufferedImage image) {
            this.image = image;
            if (minSize == null) {
                minSize = getSize();
            }
            Dimension size = new Dimension(Math.max(minSize.width, image.getWidth()), Math.max(minSize.width, image.getHeight()));
            setPreferredSize(size);
            setMinimumSize(minSize);
            revalidate();
            repaint();
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            if (image == null) {
                g2d.clearRect(0, 0, getWidth(), getHeight());
                return;
            }
            g2d.translate(this.getWidth() / 2, this.getHeight() / 2);
            g2d.translate(- image.getWidth(null) / 2, - image.getHeight(null) / 2);
            g2d.drawImage(image, 0, 0, null);
        }

        class PopUp extends JPopupMenu {

            public PopUp() {
                JMenuItem item = new JMenuItem("Save image as..");
                item.addActionListener(e -> saveImageAs());
                add(item);
                item = new JMenuItem("Copy image to clipboard");
                item.addActionListener(e -> copyToClipboard());
                add(item);
            }
        }
    }

    /**
     * Renders the virtual file for the breed in the drop down list
     */
    private static class BreedFileCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final VirtualFile file = (VirtualFile) value;
            if (file == null) {
                setVisible(false);
                return this;
            } else if (! isVisible()) {
                setVisible(true);
            }
            setText(file.getNameWithoutExtension().substring(1));
            return this;
        }
    }

    /**
     * Renders the virtual file for the breed in the drop down list
     */
    private static class PartFileCellRenderer extends DefaultListCellRenderer {

        private static final Color TRANSLUCENT = new JBColor(new Color(0, 0, 0, 140), new Color(0, 0, 0, 140));
        private static final Color BLACK = JBColor.BLACK;

        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final VirtualFile file = (VirtualFile) value;
            if (file == null) {
                setText("..related part");
                setForeground(TRANSLUCENT);
                return this;
            }
            setForeground(BLACK);
            String text = file.getNameWithoutExtension();
            final String part = partName(text.charAt(0));
            if (part != null) {
                setText(text + " - " + part);
            } else {
                setText(text);
            }
            return this;
        }

        @Nullable
        private String partName(char part) {
            switch (part) {
                case 'a':
                    return "Head";
                case 'b':
                    return "Body";
                case 'c':
                    return "L. Thigh";
                case 'd':
                    return "L. Shin";
                case 'e':
                    return "L. Foot";
                case 'f':
                    return "R. Thigh";
                case 'g':
                    return "R. Shin";
                case 'h':
                    return "R. Foot";
                case 'i':
                    return "L. Upper Arm";
                case 'j':
                    return "L. Lower Arm";
                case 'k':
                    return "R. Upper Arm";
                case 'l':
                    return "R. Lower Arm";
                case 'm':
                    return "Tail Base";
                case 'n':
                    return "Tail Tip";
                case 'o':
                    return "Left Ear";
                case 'p':
                    return "Right Ear";
                case 'q':
                    return "Hair";
                default:
                    return null;
            }
        }
    }

}
