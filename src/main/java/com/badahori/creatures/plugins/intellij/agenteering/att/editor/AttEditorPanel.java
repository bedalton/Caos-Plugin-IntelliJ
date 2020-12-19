package com.badahori.creatures.plugins.intellij.agenteering.att.editor;

import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileData;
import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileLine;
import com.badahori.creatures.plugins.intellij.agenteering.att.AttFileParser;
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser;
import com.badahori.creatures.plugins.intellij.agenteering.utils.RunWriteKt;
import com.intellij.openapi.editor.Document;
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
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;

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
    int numLines;
    int numPoints;
    final VirtualFile file;
    final VirtualFile spriteFile;
    private AttFileData fileData;
    private CaosVariant variant;
    private int currentPoint = 0;
    private List<String> pointNames = Collections.emptyList();
    private AttSpriteCellList spriteCellList = new AttSpriteCellList(Collections.emptyList(),4.0,300,300);
    private final Project project;

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
    }

    private void initListeners() {
        LocalFileSystem.getInstance().addVirtualFileListener(new VirtualFileListener() {
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event) {
                if (file.getPath().equals(event.getFile().getPath())) {
                    if (event.isFromRefresh()) {
                        fileData = AttFileParser.INSTANCE.parse(file, numLines, numPoints);
                        update();
                    }
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
            switch(value.toUpperCase().trim()) {
                case "C1": newVariant = CaosVariant.C1.INSTANCE; break;
                case "C2": newVariant = CaosVariant.C2.INSTANCE; break;
                case "CV": newVariant = CaosVariant.CV.INSTANCE; break;
                default: newVariant = CaosVariant.C3.INSTANCE; break;
            }
            this.variant = newVariant;
            update(newVariant, value.trim());
        });

        addPointListener(point1, 0);
        addPointListener(point2, 1);
        addPointListener(point3, 2);
        addPointListener(point4, 3);
        addPointListener(point5, 4);
        addPointListener(point6, 5);
    }

    public void setPart(final String part) {
        int a = 'a';
        int index = part.charAt(0) - a;
        this.part.setSelectedIndex(index);
        update(variant, part);
    }

    public void setVariant(final CaosVariant variantIn) {
        this.variant = variantIn;
        int selectedIndex;
        switch(variantIn.getCode()) {
            case "C1": selectedIndex = 0; break;
            case "C2":selectedIndex = 1; break;
            case "CV": selectedIndex = 2; break;
            default: selectedIndex = 3; break;
        }
        this.variantComboBox.setSelectedIndex(selectedIndex);
    }

    public void update(final CaosVariant variant, final String part) {
        Pair<Integer, Integer> linesAndColumns = AttEditorImpl.assumedLinesAndPoints(variant, part);
        this.numLines = linesAndColumns.getFirst();
        this.numPoints = linesAndColumns.getSecond();
        setMaxPoints(this.numPoints);
        this.pointNames = pointNames(part);
        fileData = AttFileParser.INSTANCE.parse(file, numLines, numPoints);
        update();
    }

    public void setNumLines(final int numLines) {
        this.numLines = numLines;
    }

    public void setMaxPoints(int numPoints) {
        this.numPoints = numPoints;
        switch(numPoints) {
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
    }

    private void addPointListener(final JRadioButton button, final int index) {
        button.addItemListener(e -> {
            if (!button.isSelected())
                return;
            currentPoint = index;
            return;
        });
    }

    private void update() {
        assert numLines >= 8 : "There should be at least 8 lines in each att file template";
        assert numPoints > 0 : "There should be at least 1 point in each ATT file template";
        List<BufferedImage> images = SpriteParser.parse(spriteFile).getImages();
        List<AttFileLine> lines = fileData.getLines();
        spriteCellList.setMaxWidth(0);
        spriteCellList.setMaxHeight(0);
        List<AttSpriteCellData> out = new ArrayList<>();
        for (int i=0; i < Math.min(images.size(), numLines); i++) {
            final BufferedImage image = images.get(i);
            if (spriteCellList.getMaxHeight() < image.getHeight())
                spriteCellList.setMaxHeight(image.getHeight());
            if (spriteCellList.getMaxWidth() < image.getWidth())
                spriteCellList.setMaxWidth(image.getWidth());
            out.add(new AttSpriteCellData(i, image, lines.get(i).getPoints(), pointNames, this));
        }
        spriteCellList.setItems(out);
    }

    private List<String> pointNames(final String part) {
        switch(part.trim().toUpperCase()) {
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
            case "Q": return Collections.singletonList("Head");
            default:
                return Arrays.asList(
                        "Start",
                        "End"
                );
        }
    }



    private void createUIComponents() {
        display = spriteCellList;
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

    @Override
    public synchronized void onChangePoint(final int lineNumber, final @NotNull Pair<Integer, Integer> newPoint) {
        final AttFileLine line = fileData.getLines().get(lineNumber);
        final List<Pair<Integer,Integer>> oldPoints = line != null ? line.getPoints() : emptyPointsList(this.numPoints);
        final List<Pair<Integer,Integer>> newPoints =  new ArrayList<>();
        for (int i=0; i < oldPoints.size(); i++) {
            newPoints.add(i == currentPoint ? newPoint : oldPoints.get(i));
        }
        final AttFileLine newLine = new AttFileLine(newPoints);
        final List<AttFileLine> oldLines = fileData.getLines();
        final List<AttFileLine> newLines = new ArrayList<>();
        for(int i=0; i < oldLines.size(); i++) {
            newLines.add(i == lineNumber ? newLine : oldLines.get(i));
        }
        fileData = new AttFileData(newLines);
        try {
            if (!writeFile(fileData)) {
                Logger.getLogger("#AttEditorPanel").severe("Failed to write Att file data");
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
            Logger.getAnonymousLogger().severe("Cannot update ATT file without PSI file");
            return false;
        }

        final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            Logger.getAnonymousLogger().severe("Cannot write ATT file without document");
            return false;
        }

        RunWriteKt.runUndoTransparentWriteAction(() -> {
            document.replaceString(0, psiFile.getTextRange().getEndOffset(), fileData.toFileText());
            return Unit.INSTANCE;
        });
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);
        return true;
    }

    private List<Pair<Integer,Integer>> emptyPointsList(int numPoints) {
        final List<Pair<Integer,Integer>> list = new ArrayList<>();
        for(int i=0; i < numPoints; i++) {
            list.add(new Pair<>(0, 0));
        }
        return list;
    }
    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
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
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, BorderLayout.CENTER);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane1.setViewportView(scrollPane2);
        scrollPane2.setViewportView(display);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel2, BorderLayout.NORTH);
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
}
