package com.badahori.creatures.plugins.intellij.agenteering.injector;

import com.bedalton.common.util.OS;
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle;
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosInjectorApplicationSettingsService;
import com.badahori.creatures.plugins.intellij.agenteering.utils.DocumentChangeListener;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CreateInjectorDialog extends DialogBuilder {
    private JPanel contentPane;
    private JComboBox<Integer> injectorKind;
    private JComboBox<String> variant;
    private JTextField gameName;
    private JLabel gameNameLabel;
    private JComponent gameFolder;
    private JComponent winePrefix;
    private JLabel gameFolderLabel;
    private JLabel urlHelpText;
    private JTextField nickname;
    private JLabel winePrefixLabel;
    private JLabel nicknameLabel;
    private JComponent wineExecutable;
    private JLabel wineExecutableLabel;

    private boolean nameIsDefault = true;

    private static final int NATIVE = 0;
    private static final int WINE = 1;
    private static final int POST = 2;
    private static final int TCP = 3;
    private static final boolean isWindows = OS.isWindows();

    private static final int ANY_VARIANT = 6;
    private final Project project;

    private GameInterfaceName out = null;

    public CreateInjectorDialog(final Project project,
                                @Nullable
                                final CaosVariant variant) {
        super();
        this.project = project;
        setCenterPanel(contentPane);
        initListeners();
        initInjectorKinds();
        if (variant != null) {
            this.variant.setSelectedIndex(variant.isBase() ? (variant.getIndex() - 1) : ANY_VARIANT);
        }
    }

    @Nullable
    public GameInterfaceName showAndGetInterface() {
        return showAndGet() ? out : null;
    }

    private void initListeners() {
        addOkAction();
        setOkOperation(this::onOK);
        addCancelAction();
        setCancelOperation(this::onCancel);

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        variant.addActionListener(this::updateVariant);

        injectorKind.addActionListener(this::updateInjector);

        gameName.addActionListener(this::onGameNameChange);

    }

    private void onGameNameChange(final ActionEvent e) {
        final String text = gameName.getText().trim();
        final CaosVariant variant = getSelectedVariant();
        nameIsDefault = isNameDefault(variant, text);

    }

    private boolean isNameDefault(final CaosVariant variant, final String text) {
        if (text.length() == 0 || variant.isDefaultInjectorName(text)) {
            return true;
        }
        // Match loosely based on game name or code,
        // as it is unlikely that the user would specify a game name that matches another variant
        final String textWithoutSpaces = text.replace(" ", "");
        final String variantNameWithoutSpaces = variant.getFullName().replace(" ", "");
        nameIsDefault = textWithoutSpaces.equalsIgnoreCase(variantNameWithoutSpaces) ||
                textWithoutSpaces.equalsIgnoreCase(variant.getCode());
        if (nameIsDefault) {
            return true;
        }
        if (isNet()) {
            final String url = getDefaultURL();
            return text.equalsIgnoreCase(url);
        }
        return false;
    }

    @NotNull
    private CaosVariant getSelectedVariant() {
        if (variant == null) {
            return CaosVariant.UNKNOWN.INSTANCE;
        }
        final String variantText = (String) variant.getSelectedItem();
        if (variantText == null) {
            return CaosVariant.UNKNOWN.INSTANCE;
        }
        return CaosVariant.fromVal(variantText.trim());
    }

    private int getSelectedInjectorKind() {
        final Object selectedObject = injectorKind.getSelectedItem();
        if (selectedObject == null) {
            final int newSelection = isWindows ? NATIVE : WINE;
            injectorKind.setSelectedItem(newSelection);
            return newSelection;
        }
        return (int) selectedObject;
    }

    private void updateVariant(
            @Nullable
            final ActionEvent e) {
        CaosVariant variant = getSelectedVariant();
        nameIsDefault = isNameDefault(variant, gameName.getText());
        if (nameIsDefault) {
            final String interfaceName = variant.getDefaultInjectorInterfaceName();
            if (interfaceName != null && interfaceName.trim().length() != 0) {
                gameName.setText("");
            }
        }
        updateInjector();
        if (getSelectedInjectorKind() == WINE) {
            setDefaultWineGameDirectory(null);
        }
    }

    private void updateInjector(
            @Nullable
            final ActionEvent e) {
        updateInjector();
    }

    private void updateInjector() {
        switch (getSelectedInjectorKind()) {
            case NATIVE:
                if (isWindows) {
                    setNative();
                } else {
                    setInvalid();
                }
                break;
            case WINE:
                if (isWindows) {
                    setInvalid();
                } else {
                    setWINE();
                }
                break;
            case POST:
            case TCP:
                setNet();
                break;
            default:
                setInvalid();
                break;
        }
    }

    private void setNative() {
        showNetOptions(false);
        showGameNameOrUrl(true);
        clearURL();
        showDirectory(true);
        showPrefix(false);
        showNickname(true);
        showWineBinary(false);
        okActionEnabled(true);
    }

    private void setNet() {
        showGameNameOrUrl(true);
        showNetOptions(true);
        setDefaultURL();
        showPrefix(false);
        showDirectory(false);
        showNickname(true);
        showWineBinary(false);
        okActionEnabled(true);
    }

    private void setWINE() {
        showPrefix(true);
        showDirectory(true);
        showGameNameOrUrl(true);
        showNetOptions(false);
        clearURL();
        showNickname(true);
        showWineBinary(true);
        okActionEnabled(true);
    }

    private void setInvalid() {
        showPrefix(false);
        showDirectory(false);
        showGameNameOrUrl(false);
        showNetOptions(false);
        clearURL();
        showNickname(false);
        showWineBinary(false);
        okActionEnabled(false);
    }

    private void showDirectory(final boolean show) {
        gameFolder.setVisible(show);
        gameFolderLabel.setVisible(show);
    }

    private void showPrefix(final boolean show) {
        winePrefixLabel.setVisible(show);
        winePrefix.setVisible(show);
    }

    private void showGameNameOrUrl(final boolean show) {
        final CaosVariant variant = getSelectedVariant();
        final boolean isNet = isNet();
        final boolean showNet = show && isNet;
        final boolean showForVariant = show && variant.isBase() && variant.isNotOld();
        final boolean actuallyShow = show && (showNet || showForVariant);
        if (actuallyShow) {
            updateGameNameLabelText(variant);
        }
        showNetOptions(showNet);
        final String label = isNet ?
                CaosBundle.message("caos.injector.dialog.url") :
                CaosBundle.message("caos.injector.dialog.game-name.machine-cfg");
        gameNameLabel.setText(label);
        gameNameLabel.setVisible(show);
        gameName.setVisible(show);
    }

    private void showNetOptions(final boolean show) {
        // Update net option fields
        urlHelpText.setVisible(show);
    }

    private boolean isNet() {
        final int injectorKind = this.getSelectedInjectorKind();
        return injectorKind == POST || injectorKind == TCP;
    }

    private void clearURL() {
        if (IsNet.getErrorMessageIfAny(gameName.getText()) != null) {
            gameName.setText("");
        }
    }

    private void setDefaultURL() {
        if (!isNet()) {
            return;
        }
        final int injectorKind = this.getSelectedInjectorKind();
        String newURL = getDefaultURL();
        if (newURL == null) {
            newURL = "http://";
        }
        final String currentText = gameName.getText();
        if (isNameDefault(getSelectedVariant(), currentText)) {
            gameName.setText(newURL);
            return;
        }
        if (currentText.equalsIgnoreCase(newURL)) {
            return;
        }
        if (IsNet.getErrorMessageIfAny(currentText) == null) {
            return;
        }
        final String otherURL = getDefaultURL(injectorKind == POST ? TCP : POST);
        if (otherURL != null && !otherURL.equalsIgnoreCase(currentText)) {
            gameName.setText(newURL);
        }
    }

    @Nullable
    private String getDefaultURL() {
        return getDefaultURL(this.getSelectedInjectorKind());
    }

    @Nullable
    private String getDefaultURL(final int injectorKind) {
        if (injectorKind == TCP) {
            return "http://" + TCPInjectorInterface.defaultURL + ":" + TCPInjectorInterface.DEFAULT_PORT;
        }
        if (injectorKind == POST) {
            return "http://" + PostInjectorInterface.defaultURL;
        }
        return null;
    }

    private void updateGameNameLabelText(
            @NotNull
            final CaosVariant variant) {
        // Get variant for determining show
        final String currentText = gameNameLabel.getText();

        final String newText;
        if (isNet()) {
            newText = CaosBundle.message("caos.injector.dialog.url");
        } else if (variant.isOld()) {
            newText = CaosBundle.message("caos.injector.dialog.game-name.dde-server");
        } else {
            newText = CaosBundle.message("caos.injector.dialog.game-name.machine-cfg");
        }
        if (newText.equals(currentText)) {
            return;
        }
        gameNameLabel.setText(newText);
    }

    private void showNickname(final boolean show) {
        nickname.setVisible(show);
        nicknameLabel.setVisible(show);
    }


    private void showWineBinary(final boolean show) {
        wineExecutable.setVisible(show);
        wineExecutableLabel.setVisible(show);
    }

    private void onOK() {
        try {
            out = buildInterface();
            getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
        } catch (Exception e) {
            final DialogBuilder dialog = new DialogBuilder();
            dialog.setTitle("Invalid Values");
            dialog.setCenterPanel(new JLabel(e.getMessage()));
            dialog.okActionEnabled(true);
            dialog.addOkAction();
            dialog.setOkOperation(() -> dialog.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE));
            dialog.showModal(true);
        }
    }

    @NotNull
    private GameInterfaceName buildInterface() {
        final int injectorKind = this.getSelectedInjectorKind();
        final CaosVariant variant = getSelectedVariant();
        if (variant == CaosVariant.UNKNOWN.INSTANCE) {
            throw new InvalidValueException("Invalid game variant value", this.variant);
        }
        final boolean isNet = isNet();
        final String gameName = isNet ? null : getGameNameThrowing();
        final String code = variant != CaosVariant.ANY.INSTANCE ? variant.getCode() : "*";
        final String nickname = getNickname();
        final String gamePath = getGamePath(injectorKind);
        final String prefixPath = getPrefixPathThrowing(injectorKind);
        final String url = isNet ? getUrlThrowing(injectorKind) : null;
        final String wineBinary = getWineBinaryThrowing();
        GameInterfaceName gameInterface;
        switch (injectorKind) {
            case NATIVE:
                final boolean isDefault = variant.isBase()
                        && (gameName == null || variant.isDefaultInjectorName(gameName))
                        && (gamePath == null || gamePath.isBlank())
                        && (nickname == null || nickname.isBlank());
                gameInterface = new NativeInjectorInterface(code, gameName, gamePath, nickname, isDefault);
                break;
            case WINE:
                final String prefixPathNotNull = Objects.requireNonNull(prefixPath);
                if ((new File(prefixPathNotNull)).exists()) {
                    CaosInjectorApplicationSettingsService.getInstance().setLastWineDirectory(prefixPathNotNull);
                }
                gameInterface = new WineInjectorInterface(
                        code,
                        gameName,
                        Objects.requireNonNull(prefixPath),
                        gamePath,
                        nickname,
                        wineBinary
                );
                break;
            case TCP:
                gameInterface = new TCPInjectorInterface(
                        code,
                        gameName,
                        Objects.requireNonNull(url),
                        nickname
                );
                break;
            case POST:
                gameInterface = new PostInjectorInterface(
                        code,
                        gameName,
                        Objects.requireNonNull(url),
                        null,
                        nickname
                );
                break;
            default:
                throw new InvalidValueException("Injector kind is invalid", this.injectorKind);
        }
        return gameInterface;
    }

    @Nullable
    private String getPrefixPathThrowing(final int injectorKind) {
        if (injectorKind != WINE) {
            return null;
        }
        Pair<String, File> prefixData = getPrefixDirectory(false);
        if (prefixData == null || prefixData.getSecond() == null) {
            prefixData = getPrefixDirectory(true);
        }
        if (prefixData == null) {
            throw new InvalidValueException("Wine prefix path is invalid", this.winePrefix);
        }
        final String prefixPath = prefixData.getSecond() != null ? prefixData.getSecond().getPath() : null;
        if (prefixPath == null || prefixPath.isBlank()) {
            throw new InvalidValueException("Wine prefix path cannot be blank", this.winePrefix);
        }
        return prefixPath;
    }

    @NotNull
    private String getGameNameThrowing() {
        final String gameName = this.gameName.getText().trim();
        if (gameName.isBlank()) {
            final CaosVariant variant = getSelectedVariant();
            if (variant.isBase()) {
                final String temp = variant.getDefaultInjectorInterfaceName();
                if (temp != null && !temp.isBlank()) {
                    return temp;
                }
            }
            throw new InvalidValueException("Game name cannot be blank", this.gameName);
        }
        return gameName;
    }

    @Nullable
    private String getGamePath(final int injectorKind) {
        if (injectorKind == POST || injectorKind == TCP) {
            return null;
        }
        final Pair<String, File> gamePathData = getGameDirectory();
        if (gamePathData == null) {
            return null;
        }
        String path = (gamePathData.getSecond() != null) ? gamePathData.getSecond().getPath() : gamePathData.getFirst();
        if (path.trim().length() == 0) {
            return null;
        }
        if (path.contains("\\") && !path.endsWith("\\")) {
            path += '\\';
        } else if (!path.endsWith("/")) {
            path += '/';
        }
        if (injectorKind == WINE) {
            String prefixPath = ((TextFieldWithBrowseButton)winePrefix).getText().trim();
            if (prefixPath.contains("\\") && !prefixPath.endsWith("\\")) {
                prefixPath += '\\';
            } else if (!prefixPath.endsWith("/")) {
                prefixPath += '/';
            }
            if (prefixPath.length() > 0 && path.equals(prefixPath)) {
                return null;
            }
        }
        return path;

    }

    @Nullable
    private String getNickname() {
        final String nickName = this.nickname.getText().trim();
        return nickName.isBlank() ? null : nickName;
    }

    @Nullable
    private String getWineBinaryThrowing() {
        final String executable = ((TextFieldWithBrowseButton)wineExecutable).getText();
        if (executable.isBlank()) {
            return null;
        }
        File file = new File(executable);
        if (file.exists()) {
            return executable;
        }
        file = new File(executable.trim());
        if (file.exists()) {
            return file.getPath();
        }
        throw new InvalidValueException("Wine binary path is invalid", wineExecutable);
    }

    @Nullable
    private String getUrlThrowing(final int injectorKind) {
        if (injectorKind != TCP && injectorKind != POST) {
            return null;
        }
        final String url = getGameNameThrowing();
        if (url.isBlank()) {
            throw new InvalidValueException(CaosBundle.message("caos.injector.dialog.url.blank"), this.gameName);
        }
        final String error = IsNet.getErrorMessageIfAny(url);
        if (error != null) {
            throw new InvalidValueException(error, this.gameName);
        }
        return url;
    }

    private void onCancel() {
        onGameNameChange(null);
        // add your code here if necessary
        getDialogWrapper().close(DialogWrapper.CANCEL_EXIT_CODE);
    }

    public void setInterface(@NotNull GameInterfaceName interfaceName) {

        CaosVariant variant = CaosVariant.fromVal(interfaceName.getCode());
        if (variant == CaosVariant.UNKNOWN.INSTANCE) {
            setInvalid();
            return;
        }

        if (variant.isBase()) {
            this.variant.setSelectedIndex(variant.getIndex() - 1);
        } else {
            this.variant.setSelectedIndex(6);
        }
        // Get path now, so we can set it if blank and is wine prefix
        String gamePath = interfaceName.getPath();
        String gameNameOrURL = interfaceName.getGameName();

        // Change format depending on interface kind
        final int kind;
        if (interfaceName instanceof NativeInjectorInterface) {
            kind = NATIVE;
            injectorKind.setSelectedItem(NATIVE);
            setNative();
        } else if (interfaceName instanceof PostInjectorInterface) {
            kind = POST;
            injectorKind.setSelectedItem(POST);
            gamePath = null;
            gameNameOrURL = interfaceName.getPath();
            setNet();
        } else if (interfaceName instanceof TCPInjectorInterface) {
            kind = TCP;
            injectorKind.setSelectedItem(TCP);
            gamePath = null;
            gameNameOrURL = interfaceName.getPath();
            setNet();
        } else if (interfaceName instanceof WineInjectorInterface) {
            kind = WINE;
            injectorKind.setSelectedItem(WINE);
            final String prefixPath = ((WineInjectorInterface) interfaceName).getPrefix();

            ((TextFieldWithBrowseButton) winePrefix).setText(prefixPath);
            if (gamePath == null || gamePath.isBlank() || !gamePath.startsWith(prefixPath)) {
                try {
                    gamePath = getWineGamePath(variant, new File(prefixPath));
                } catch (Exception ignored) {
                }
            }
            setWINE();
        } else {
            setInvalid();
            return;
        }

        if (kind == NATIVE || kind == WINE) {
            ((TextFieldWithBrowseButton) gameFolder).setText(interfaceName.getPath() != null ? interfaceName.getPath() : "");
        }

        if (interfaceName instanceof WineInjectorInterface) {
            ((TextFieldWithBrowseButton) gameFolder).setText(((WineInjectorInterface) interfaceName).getPrefix());
        }

        gameName.setText(gameNameOrURL != null ? gameNameOrURL : "");
        nickname.setText(interfaceName.getNickname() != null ? interfaceName.getNickname() : "");
        String wineExecutablePath = (interfaceName instanceof WineInjectorInterface)
                ? ((WineInjectorInterface)interfaceName).getWineExecutable()
                : null;
        if (wineExecutablePath == null || wineExecutablePath.isBlank()) {
            wineExecutablePath = "";
        }
        ((TextFieldWithBrowseButton)wineExecutable).setText(wineExecutablePath);
        ((TextFieldWithBrowseButton)gameFolder).setText(gamePath != null ? gamePath : "");
    }

    private void createUIComponents() {
        // Add Injector kind combobox and renderer
        injectorKind = new ComboBox<>();
        injectorKind.setRenderer(new InjectorRenderer());

        // Set start paths for wine and bootstrap
        final File home = getHomeDirectory();
        initializeBootstrapField(home);
        initializeWinePrefixField(home);
        initializeWineBinaryField(home);
    }

    @Nullable
    private Pair<String, File> getGameDirectory() {
        final String path = ((TextFieldWithBrowseButton) gameFolder).getText().trim();
        if (path.isBlank()) {
            return null;
        }
        File file = new File(path);
        if (!file.exists() || !hasChild(file, "images")) {
            return new Pair<>(path, null);
        }
        final CaosVariant variant = getSelectedVariant();
        if (variant.isNotOld() && !hasChild(file, "bootstrap")) {
            return new Pair<>(path, null);
        }
        return new Pair<>(path, file);
    }

    /**
     * Gets the prefix directory based on the prefix field contents
     *
     * @param raw whether to try to get parent PREFIX folder if given directory seems invalid or like a child
     * @return (Entered Path, resolved File)
     */
    @Nullable
    private Pair<String, File> getPrefixDirectory(final boolean raw) {
        final String path = ((TextFieldWithBrowseButton) winePrefix).getText().trim();
        if (path.isBlank()) {
            return null;
        }
        File temp = new File(path);
        if (!temp.exists()) {
            return new Pair<>(path, null);
        }
        if (raw) {
            return new Pair<>(path, temp);
        }
        final String driveC = "drive_c";
        while (!hasChild(temp, driveC)) {
            temp = temp.getParentFile();
            if (temp == null) {
                return null;
            }
        }
        return new Pair<>(path, hasChild(temp, driveC) ? temp : null);
    }

    private static boolean hasChild(final File file, final String childName) {
        if (!file.exists()) {
            return false;
        }
        final String[] children = file.list();
        if (children == null) {
            return false;
        }
        for (String child : children) {
            if (child.equalsIgnoreCase(childName)) {
                return true;
            }
        }
        return false;
    }

    private void initializeBootstrapField(final File home) {
        gameFolder = createSelectFolderField(
                CaosBundle.message("caos.injector.dialog.bootstrap.file-selector.label"),
                CaosBundle.message("caos.injector.dialog.bootstrap.file-selector.description"),
                home
        );
        gameFolder.setToolTipText(CaosBundle.message("caos.injector.dialog.bootstrap.file-selector.description"));
    }

    /**
     * Initializes the WINE prefix field
     *
     * @param home the user's home directory / default directory if no wine folders automatically found
     */
    private void initializeWinePrefixField(final File home) {
        final CaosInjectorApplicationSettingsService state = CaosInjectorApplicationSettingsService.getInstance();
        // Initialize WINE directory
        final TextFieldWithBrowseButton prefix = createSelectFolderField(
                "Wine Root",
                "The root folder of the wine prefix. Sometimes at ~/.wine",
                getDefaultWineDirectory(state, home)
        );

        final Runnable onChange = () -> {
            final String text = prefix.getText().trim();
            if (text.length() == 0) {
                return;
            }
            if ((new File(text)).exists()) {
                state.setLastWineDirectory(text);
                if (!((TextFieldWithBrowseButton)gameFolder).getText().startsWith(text)) {
                    ((TextFieldWithBrowseButton)gameFolder).setText(text);
                    ((TextFieldWithBrowseButton)gameFolder).getTextField().setText(text);
                }
            }
        };
        prefix.addActionListener((e) -> onChange.run());

        prefix.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                onChange.run();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                onChange.run();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                onChange.run();
            }
        });
        prefix.setToolTipText(CaosBundle.message("caos.injector.dialog.wine-prefix.tooltip"));
        prefix.addActionListener((e) -> setDefaultWineGameDirectory(prefix.getText()));
        winePrefix = prefix;
    }

    private void initializeWineBinaryField(final File home) {
        this.wineExecutable = createSelectFileField(
                "Wine Binary",
                "The wine binary used to run this prefix. Sometimes at /usr/bin/wine",
                getDefaultWineBinaryDirectory(home)
        );
    }

    private void setDefaultWineGameDirectory(@Nullable final String prefix) {
        final String text = prefix != null && !prefix.isBlank() ? prefix : ((TextFieldWithBrowseButton) winePrefix).getText().trim();
        if (text.length() == 0) {
            return;
        }
        final File winePrefix = new File(text);
        if (!winePrefix.exists()) {
            return;
        }
        final TextFieldWithBrowseButton gameDirectoryField = (TextFieldWithBrowseButton) gameFolder;
        final String gameDirectory = gameDirectoryField.getText().trim();
        final File homeDirectoryFile = getHomeDirectory();
        final String home = homeDirectoryFile != null ? homeDirectoryFile.getPath() : null;
        if (gameDirectory.length() != 0 && !gameDirectory.equals(home) && gameDirectory.startsWith(text) && !gameDirectory.equals(text)) {
            return;
        }
        String winePath = getWineGamePath(getSelectedVariant(), winePrefix);
        if (winePath == null) {
            winePath = text;
        }
        gameDirectoryField.setText(winePath);
    }

    private File getDefaultWineDirectory(final CaosApplicationSettingsService state, final File home) {
        String lastWineDirectory = state.getLastWineDirectory();
        File wineDir = lastWineDirectory != null ? new File(lastWineDirectory) : new File(home, ".wine");
        return wineDir.exists() ? wineDir : home;
    }


    private File getDefaultWineBinaryDirectory(final File home) {
        final CaosApplicationSettingsService state = CaosApplicationSettingsService.getInstance();
        String lastWineDirectory = state.getWinePath();
        if (lastWineDirectory == null) {
            lastWineDirectory = WineHelper.getDefault(false, true);
        }
        File wineDir = lastWineDirectory != null ? new File(lastWineDirectory) : new File("/usr/local/bin/wine");
        return wineDir.exists() ? wineDir : (new File("/usr/local/bin/wine"));
    }

    private String getWineGamePath(final CaosVariant variant, final File prefix) {
        final String variantFolder = variant.isOld() ? variant.getFullName() : variant.getDefaultInjectorInterfaceName();
        File out = null;
        if (variant.isNotOld()) {
            out = new File(prefix, "GOG Galaxy/Games/Creatures Exodus/" + variantFolder);
            if (out.exists()) {
                return out.getPath();
            }
            out = new File(prefix, "Program Files (x86)/" + variantFolder);
            if (out.exists()) {
                return out.getPath();
            }
            out = new File(prefix, "Program Files/" + variantFolder);
            if (out.exists()) {
                return out.getPath();
            }
        }
        final File users = new File(prefix, "users");
        if (!users.exists()) {
            return null;
        }
        final File[] userFolders = users.listFiles();
        if (userFolders == null) {
            return null;
        }
        for (File file : userFolders) {
            out = new File(file, "/Documents/Creatures/" + variantFolder);
            if (out.exists()) {
                break;
            }
        }
        return out != null ? out.getPath() : null;
    }

    /**
     * @return Users default home directory
     */
    @Nullable
    private File getHomeDirectory() {
        final String home = System.getProperty("user.home");
        if (home == null || home.length() == 0) {
            return null;
        }
        File homeFolder = new File(home);
        return homeFolder.exists() ? homeFolder : null;
    }

    private TextFieldWithBrowseButton createSelectFolderField(
            final String label, final String description,
            @Nullable
            File defaultFolder
    ) {
        File file = (defaultFolder != null && defaultFolder.exists()) ? defaultFolder : null;
        if (file != null && !file.exists()) {
            file = null;
        }
        final TextFieldWithBrowseButton field = new TextFieldWithBrowseButton();
        field.addBrowseFolderListener(
                label,
                description,
                project,
                new FileChooserDescriptor(
                        false,
                        true,
                        false,
                        false,
                        false,
                        false
                )
        );
        if (file != null) {
            field.setText(file.getPath());
        }
        return field;
    }

    private TextFieldWithBrowseButton createSelectFileField(
            final String label, final String description,
            @Nullable
            File defaultFileOrStartingFolder
    ) {
        File file = null;
        if (defaultFileOrStartingFolder != null) {
            if (defaultFileOrStartingFolder.exists()) {
                file = defaultFileOrStartingFolder;
            } else {
                file = defaultFileOrStartingFolder.getParentFile();
            }
        }

        if (file != null && !file.exists()) {
            file = null;
        }
        final TextFieldWithBrowseButton field = new TextFieldWithBrowseButton();
        field.addBrowseFolderListener(
                label,
                description,
                project,
                new FileChooserDescriptor(
                        true,
                        false,
                        false,
                        false,
                        false,
                        false
                )
        );
        if (file != null) {
            field.setText(file.getPath());
        }
        return field;
    }

    private void initInjectorKinds() {
        if (isWindows) {
            injectorKind.addItem(NATIVE);
        } else {
            injectorKind.addItem(WINE);
        }
        if (TCPConnection.isImplemented()) {
            injectorKind.addItem(TCP);
        }
        injectorKind.addItem(POST);

        injectorKind.setSelectedItem(isWindows ? NATIVE : WINE);
        injectorKind.updateUI();

    }

    static class InjectorRenderer implements ListCellRenderer<Integer> {
        private final List<JLabel> items = new ArrayList<>();

        @Override
        public Component getListCellRendererComponent(JList<? extends Integer> list, Integer value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label;
            final boolean enabled;
            final String text;
            if (value == null) {
                return new JLabel();
            }
            final int injectorKind = value;
            if (injectorKind == NATIVE) {
                text = "Native";
                enabled = isWindows;
            } else if (injectorKind == WINE) {
                text = "WINE";
                enabled = !isWindows;
            } else if (injectorKind == TCP) {
                text = "TCP";
                enabled = TCPConnection.isImplemented();
            } else if (injectorKind == POST) {
                text = "POST";
                enabled = true;
            } else {
                throw new RuntimeException("Failed to understand Injector type with id: " + injectorKind);
            }
            if (index < 0) {
                label = new JLabel(text);
            } else if (items.size() <= index) {
                label = new JLabel(text);
                items.add(label);
            } else {
                label = items.get(index);
                label.setText(text);
            }

            label.setVisible(enabled);
            label.setEnabled(false);
            if (enabled) {
                label.setToolTipText(text);
                label.setMaximumSize(new Dimension(100, 80));
            } else {
                label.setToolTipText(value + " is disabled on this platform");
                if (index == TCP) {
                    label.setToolTipText(value + " is not yet implemented");
                }
                label.setMaximumSize(new Dimension(1, 1));
            }
            return label;
        }
    }

    static class InvalidValueException extends RuntimeException {
        final JComponent component;

        InvalidValueException(final String message, final JComponent component) {
            super(message);
            this.component = component;
        }
    }
}
