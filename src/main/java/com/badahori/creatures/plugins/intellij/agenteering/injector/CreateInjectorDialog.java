package com.badahori.creatures.plugins.intellij.agenteering.injector;

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle;
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant;
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosInjectorApplicationSettingsService;
import com.badahori.creatures.plugins.intellij.agenteering.utils.DocumentChangeListener;
import com.bedalton.common.util.OS;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import kotlin.text.MatchResult;
import kotlin.text.Regex;
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
    private JLabel portLabel;
    private JTextField port;

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
                                @Nullable final CaosVariant variant) {
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
        if (text.isEmpty() || variant.isDefaultInjectorName(text)) {
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
            @Nullable final ActionEvent e) {
        CaosVariant variant = getSelectedVariant();
        nameIsDefault = isNameDefault(variant, gameName.getText());
        if (nameIsDefault) {
            final String interfaceName = variant.getDefaultInjectorInterfaceName();
            if (interfaceName != null && !interfaceName.trim().isEmpty()) {
                gameName.setText("");
            }
        }
        updateInjector();
        if (getSelectedInjectorKind() == WINE) {
            setDefaultWineGameDirectory(null);
        }
    }

    private void updateInjector(
            @Nullable final ActionEvent e) {
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
                setPOST();
                break;
            case TCP:
                setTCP();
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
        showGameDirectory(true);
        showPort(false);
        showPrefix(false);
        showNickname(true);
        showWineBinary(false);
        okActionEnabled(true);
    }

    private void setPOST() {
        showGameNameOrUrl(true);
        showNetOptions(true);
        setDefaultURL();
        showPrefix(false);
        showGameDirectory(false);
        showPort(true);
        showNickname(true);
        showWineBinary(false);
        okActionEnabled(true);
    }

    private void setTCP() {
        showGameNameOrUrl(true);
        showNetOptions(true);
        setDefaultURL();
        showPrefix(false);
        showGameDirectory(false);
        showPort(true);
        showNickname(true);
        showWineBinary(false);
        okActionEnabled(true);
    }

    private void setWINE() {
        showPrefix(true);
        showGameDirectory(true);
        showPort(false);
        showGameNameOrUrl(true);
        showNetOptions(false);
        clearURL();
        showNickname(true);
        showWineBinary(true);
        okActionEnabled(true);
    }

    private void setInvalid() {
        showPrefix(false);
        showGameDirectory(false);
        showPort(false);
        showGameNameOrUrl(false);
        showNetOptions(false);
        clearURL();
        showNickname(false);
        showWineBinary(false);
        okActionEnabled(false);
    }

    private void showPrefix(final boolean show) {
        winePrefixLabel.setVisible(show);
        winePrefix.setVisible(show);
    }

    private void showGameDirectory(final boolean show) {
        gameFolderLabel.setVisible(show);
        gameFolder.setVisible(show);
    }

    private void showPort(final boolean show) {
        portLabel.setVisible(show);
        port.setVisible(show);
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
        final String label;
        if (is(POST)) {
            label = CaosBundle.message("caos.injector.dialog.url");
        } else if (is(TCP)) {
            label = CaosBundle.message("caos.injector.dialog.host");
        } else {
            label = CaosBundle.message("caos.injector.dialog.game-name.machine-cfg");
        }
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

    private boolean is(final int kind) {
        final int injectorKind = this.getSelectedInjectorKind();
        return injectorKind == kind;
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
            newURL = is(TCP) ? TCPInjectorInterface.DEFAULT_URL : "http://";
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

        final String otherURL = getDefaultURL(injectorKind == POST ? POST : TCP);
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
            return TCPInjectorInterface.DEFAULT_URL;
        }
        if (injectorKind == POST) {
            return "http://" + PostInjectorInterface.DEFAULT_URL;
        }
        return null;
    }

    private void updateGameNameLabelText(
            @NotNull final CaosVariant variant) {
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
            throw new InvalidValueException(CaosBundle.message("caos.injector.dialog.variant.invalid"), this.variant);
        }

        // Get net type if any
        final boolean isPost = is(POST);
        final boolean isTCP = is(TCP);
        final boolean isNet = isPost || isTCP;

        final String gameName = isNet ? null : getGameNameThrowing();
        final String code = variant != CaosVariant.ANY.INSTANCE ? variant.getCode() : "*";
        final String nickname = getNickname();

        // Wine and Native
        final String gamePath = getGamePath(injectorKind);
        final String prefixPath = getPrefixPathThrowing(injectorKind);
        final String wineBinary = getWineBinaryThrowing();

        // URL and Port
        String url = isNet ? getUrlThrowing(injectorKind) : null;
        Integer port = isNet ? getPortThrowing(url) : null;

        // Add or Remove port as necessary
        url = isPost ? appendPort(url, port) : stripPort(url, port);

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
                        port,
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
                throw new InvalidValueException(CaosBundle.message("caos.injector.dialog.kind.invalid"), this.injectorKind);
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
            throw new InvalidValueException(CaosBundle.message("caos.injector.dialog.wine-prefix.invalid"), this.winePrefix);
        }
        final String prefixPath = prefixData.getSecond() != null ? prefixData.getSecond().getPath() : null;
        if (prefixPath == null || prefixPath.isBlank()) {
            throw new InvalidValueException(CaosBundle.message("caos.injector.dialog.wine-prefix.blank"), this.winePrefix);
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
            throw new InvalidValueException(CaosBundle.message("caos.injector.dialog.game-name.blank"), this.gameName);
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
        if (path.isBlank()) {
            return null;
        }
        if (path.contains("\\") && !path.endsWith("\\")) {
            path += '\\';
        } else if (!path.endsWith("/")) {
            path += '/';
        }
        if (injectorKind == WINE) {
            String prefixPath = this.winePrefixText();
            if (prefixPath.isEmpty()) {
                return null;
            }
            if (prefixPath.contains("\\") && !prefixPath.endsWith("\\")) {
                prefixPath += '\\';
            } else if (!prefixPath.endsWith("/")) {
                prefixPath += '/';
            }

            if (path.equals(prefixPath)) {
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
        final String executable = ((TextFieldWithBrowseButton) wineExecutable).getText();
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
        throw new InvalidValueException(CaosBundle.message("caos.injector.dialog.wine.binary-invalid"), wineExecutable);
    }

    @Nullable
    private String getUrlThrowing(final int injectorKind) {
        if (injectorKind != TCP && injectorKind != POST) {
            return null;
        }
        final String url = getGameNameThrowing();
        if (is(TCP)) {
            return url.trim();
        }
        if (url.isBlank()) {
            throw new InvalidValueException(CaosBundle.message("caos.injector.dialog.url.blank"), this.gameName);
        }
        final String error = IsNet.getErrorMessageIfAny(url);
        if (error != null) {
            throw new InvalidValueException(error, this.gameName);
        }
        return url;
    }

    @Nullable
    private Integer getPortThrowing(@Nullable final String url) {
        final String portText = port.getText().trim();
        if (portText.isBlank()) {
            return getUrlPort(url);
        }
        if (!portText.matches("^\\d+$")) {
            throw new InvalidValueException(CaosBundle.message("caos.injector.dialog.port.invalid"), this.port);
        }
        try {
            return Integer.parseInt(portText);
        } catch (Exception e) {
            throw new InvalidValueException(CaosBundle.message("caos.injector.dialog.port.invalid"), this.port);
        }
    }

    @Nullable
    private Integer getUrlPort(@Nullable final String url) {
        if (url == null) {
            return null;
        }
        final Regex regex = new Regex("^.+:(\\d+)$");
        final MatchResult matches = regex.matchEntire(url.trim());
        if (matches == null) {
            return null;
        }
        final List<String> groups = matches.getGroupValues();
        final String portString = groups.get(1);
        return Integer.parseInt(portString);
    }

    @Nullable
    private String appendPort(@Nullable final String url, @Nullable final Integer port) {
        if (url == null || port == null) {
            return url;
        }
        if (url.endsWith(":" + port)) {
            return url;
        }
        if (url.matches("^.+:\\d+$")) {
            throw new InvalidValueException(
                    CaosBundle.message("caos.injector.dialog.url.url-port-mismatch"),
                    this.gameName
            );
        }
        return url + ":" + port;
    }

    @Nullable
    private String stripPort(@Nullable final String url, @Nullable final Integer port) {
        if (url == null) {
            return null;
        }

        final Regex regex = new Regex("^(.+)(:\\d+)?$");

        final MatchResult result = regex.matchEntire(url);
        if (result == null) {
            return url;
        }

        final List<String> groups = result.getGroupValues();
        final String urlWithoutPort = groups.get(1);

        if (groups.size() == 2) {
            return urlWithoutPort;
        }

        if (port == null || groups.get(1).endsWith(":" + port)) {
            return urlWithoutPort;
        }

        throw new InvalidValueException(
                CaosBundle.message("caos.injector.dialog.url.url-port-mismatch"),
                this.gameName
        );
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
        String portString = "";

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
            final Integer port = getUrlPort(gameNameOrURL);
            portString = port != null ? port.toString() : null;
            gameNameOrURL = stripPort(gameNameOrURL, null);
            setPOST();
        } else if (interfaceName instanceof TCPInjectorInterface) {
            kind = TCP;
            injectorKind.setSelectedItem(TCP);
            gamePath = null;
            gameNameOrURL = interfaceName.getPath();
            final Integer port = ((TCPInjectorInterface) interfaceName).getPort();
            portString = port != null ? port.toString() : "";
            setTCP();
        } else if (interfaceName instanceof WineInjectorInterface) {
            kind = WINE;
            injectorKind.setSelectedItem(WINE);
            final String prefixPath = ((WineInjectorInterface) interfaceName).getPrefix();

            if (this.winePrefix != null) {
                ((TextFieldWithBrowseButton) winePrefix).setText(prefixPath);
            }
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
        port.setText(portString);
        String wineExecutablePath = getWineExecutablePathValue(interfaceName);
        ((TextFieldWithBrowseButton) wineExecutable).setText(wineExecutablePath);
        ((TextFieldWithBrowseButton) gameFolder).setText(gamePath != null ? gamePath : "");
    }

    @NotNull
    private static String getWineExecutablePathValue(@NotNull GameInterfaceName interfaceName) {
        String wineExecutablePath = null;
        if (interfaceName instanceof WineInjectorInterface) {
            wineExecutablePath = ((WineInjectorInterface) interfaceName).getWineExecutable();
        } else if (interfaceName instanceof TCPInjectorInterface) {
            Integer port = ((TCPInjectorInterface) interfaceName).getPort();
            if (port != null) {
                wineExecutablePath = port.toString();
            }
        }
        if (wineExecutablePath == null || wineExecutablePath.isBlank()) {
            wineExecutablePath = "";
        }
        return wineExecutablePath;
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
        final String path = winePrefixText();
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
                CaosBundle.message("caos.injector.dialog.wine-prefix.popup.label"),
                CaosBundle.message("caos.injector.dialog.wine-prefix.popup.description"),
                getDefaultWineDirectory(state, home),
                (event, newText) -> {
                    final String homeText = newText != null && newText.isBlank()
                            ? newText
                            : winePrefixText();
                    setDefaultWineGameDirectory(homeText);
                    return null;
                }
        );

        final Runnable onChange = () -> {
            final String text = prefix.getText().trim();
            if (text.isBlank()) {
                return;
            }
            if ((new File(text)).exists()) {
                state.setLastWineDirectory(text);
                if (!((TextFieldWithBrowseButton) gameFolder).getText().startsWith(text)) {
                    ((TextFieldWithBrowseButton) gameFolder).setText(text);
                    ((TextFieldWithBrowseButton) gameFolder).getTextField().setText(text);
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

    @NotNull
    private String winePrefixText() {
        return (winePrefix != null ? (((TextFieldWithBrowseButton) winePrefix).getText()) : "");
    }

    private void initializeWineBinaryField(final File home) {
        this.wineExecutable = createSelectFileField(
                CaosBundle.message("caos.injector.dialog.wine-binary.popup.label"),
                CaosBundle.message("caos.injector.dialog.wine-binary.popup.description"),
                getDefaultWineBinaryDirectory(home)
        );
    }

    private void setDefaultWineGameDirectory(@Nullable final String prefix) {
        final String text = prefix != null && !prefix.isBlank() ? prefix : winePrefixText();
        if (text.isBlank()) {
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
        if (gameDirectory.isBlank() && !gameDirectory.equals(home) && gameDirectory.startsWith(text) && !gameDirectory.equals(text)) {
            return;
        }
        String winePath = getWineGamePath(getSelectedVariant(), winePrefix);
        if (winePath == null) {
            winePath = text;
        }
        gameDirectoryField.setText(winePath);
    }

    private File getDefaultWineDirectory(final CaosInjectorApplicationSettingsService state, final File home) {
        String lastWineDirectory = state.getLastWineDirectory();
        File wineDir = lastWineDirectory != null ? new File(lastWineDirectory) : new File(home, ".wine");
        return wineDir.exists() ? wineDir : home;
    }


    private File getDefaultWineBinaryDirectory(final File home) {
        final CaosInjectorApplicationSettingsService state = CaosInjectorApplicationSettingsService.getInstance();
        String lastWineDirectory = state.getWinePath();
        if (lastWineDirectory == null) {
            lastWineDirectory = WineHelper.getDefault(true, true);
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
        if (home == null || home.isEmpty()) {
            return null;
        }
        File homeFolder = new File(home);
        return homeFolder.exists() ? homeFolder : null;
    }

    private TextFieldWithBrowseButton createSelectFolderField(
            final String label,
            final String description,
            @Nullable
            File defaultFolder
    ) {
        return createSelectFolderField(label, description, defaultFolder, null);
    }

    private TextFieldWithBrowseButton createSelectFolderField(
            final String label,
            final String description,
            @Nullable
            File defaultFolder,
            @Nullable final Function2<Integer, String, Unit> handler
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
        if (handler != null) {
            final DocumentChangeListener listener = new com.badahori.creatures.plugins.intellij.agenteering.utils.DocumentChangeListener(handler);
            field.getTextField().getDocument().addDocumentListener(listener);
        }
        if (file != null) {
            field.setText(file.getPath());
        }
        return field;
    }

    private TextFieldWithBrowseButton createSelectFileField(
            final String label,
            final String description,
            @Nullable
            File defaultFileOrStartingFolder
    ) {
        return createSelectFileField(
                label,
                description,
                defaultFileOrStartingFolder,
                null
        );
    }

    private TextFieldWithBrowseButton createSelectFileField(
            final String label, final String description,
            @Nullable
            File defaultFileOrStartingFolder,
            @Nullable kotlin.jvm.functions.Function2<Integer, String, Unit> handler
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
        if (handler != null) {
            final DocumentChangeListener listener = new com.badahori.creatures.plugins.intellij.agenteering.utils.DocumentChangeListener(handler);
            field.getTextField().getDocument().addDocumentListener(listener);
        }
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
        if (!isWindows) {
            injectorKind.addItem(TCP);
        }

        injectorKind.addItem(POST);
        injectorKind.setSelectedItem(isWindows ? NATIVE : TCP);
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
                enabled = !isWindows;
            } else if (injectorKind == POST) {
                text = "POST";
                enabled = true;
            } else {
                throw new RuntimeException(CaosBundle.message("caos.injector.dialog.id.invalid", injectorKind));
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
                label.setToolTipText(CaosBundle.message("caos.injector.dialog.kind.disabled", value));
                if (index == TCP) {
                    label.setToolTipText(CaosBundle.message("caos.injector.dialog.kind.not-implemented", value));
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
