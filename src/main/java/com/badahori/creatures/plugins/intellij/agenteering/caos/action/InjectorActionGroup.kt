package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.bedalton.common.util.OS
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosConstants
import com.badahori.creatures.plugins.intellij.agenteering.injector.CreateInjectorDialog
import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.injector.NativeInjectorInterface
import com.badahori.creatures.plugins.intellij.agenteering.utils.OsUtil
import com.bedalton.common.util.className
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.like
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfNotConcrete
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer


//val CAOSEditorKey = Key<Editor>("creatures.caos.EDITOR")

class InjectorActionGroup(file: CaosScriptFile) : ActionGroup(
    "Inject CAOS",
    "Select CAOS injection interface",
    AllIcons.Toolwindows.ToolWindowRun
) {

    private val pointer: SmartPsiElementPointer<CaosScriptFile> = SmartPointerManager.createPointer(file)

    init {
        isPopup = true
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return getActions()
    }


    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun getActions(makeText: MakeName = GameInterfaceName::defaultDisplayName): Array<AnAction> {
        val file = pointer.element
            ?: return emptyArray()

        val gameInterfaces = getGameInterfaceNames(file.variant)
        val interfaceActions = gameInterfaces
            .distinct()
            .sortedBy { it.name }
            .map {
                CaosInjectorAction(it, pointer, makeText(it))
            }
        val addInterfaceName = AddGameInterfaceAction(file.project, file.variant)
        return (interfaceActions + addInterfaceName).toTypedArray()
    }

    companion object {
        fun getGameInterfaceNames(variant: CaosVariant?): List<GameInterfaceName> {
            val projectGameInterfaces = CaosApplicationSettingsService.getInstance().gameInterfaceNames(variant)
            val variantInterfaces = getDefaultInjectors(variant)
            return (projectGameInterfaces + variantInterfaces)
                .distinctBy { it.serialize().replace("\\[.+]$".toRegex(), "") }
        }

        private fun getDefaultInjectors(variant: CaosVariant?): List<GameInterfaceName> {
            if (!OS.Companion.isWindows) {
                return emptyList()
            }
            if (variant == null) {
                return CaosConstants.VARIANTS
                    .filter { it != CaosVariant.UNKNOWN && it != CaosVariant.ANY }
                    .map {
                        NativeInjectorInterface.simple(it)
                    }
            }
            if (variant.isC3DS) {
                return listOf(
                    NativeInjectorInterface.simple(CaosVariant.C3),
                    NativeInjectorInterface.simple(CaosVariant.DS),
                )
            }
            return listOf(NativeInjectorInterface.simple(variant))
        }


        @Suppress("unused")
        fun getActions(
            file: CaosScriptFile,
            variant: CaosVariant? = file.variant,
            makeText: MakeName = GameInterfaceName::defaultDisplayName,
        ): Array<AnAction> {
            val pointer = SmartPointerManager.createPointer(file)
            val gameInterfaces = getGameInterfaceNames(variant)
            val interfaceActions = gameInterfaces
                .distinct()
                .sortedBy { it.name }
                .map {
                    CaosInjectorAction(it, pointer, makeText(it))
                }
            val addInterfaceName = AddGameInterfaceAction(file.project, file.variant)
            return (interfaceActions + addInterfaceName).toTypedArray()
        }

        @Suppress("unused")
        fun getActions(
            pointer: SmartPsiElementPointer<CaosScriptFile>,
            makeText: MakeName = GameInterfaceName::defaultDisplayName,
        ): Array<AnAction> {
            val file = pointer.element
                ?: return emptyArray()

            val gameInterfaces = getGameInterfaceNames(file.variant)
            var interfaceActions = gameInterfaces
                .distinct()
                .sortedBy { it.name }
                .map {
                    CaosInjectorAction(it, pointer, makeText(it))
                }
            if (!OsUtil.isWindows) {
                interfaceActions = interfaceActions.filter {
                    it.gameInterfaceName !is NativeInjectorInterface
                }
            }
            val addInterfaceName = AddGameInterfaceAction(file.project, file.variant)
            return (interfaceActions + addInterfaceName).toTypedArray()
        }
    }

}

internal class CaosInjectorAction(
    internal val gameInterfaceName: GameInterfaceName,
    private val pointer: SmartPsiElementPointer<CaosScriptFile>,
    title: String = gameInterfaceName.defaultDisplayName(),
) : AnAction(
    title,
    (gameInterfaceName.code.let { if (it == "*" || it == "AL" || it == "ANY") "Any variant" else it } + " CAOS injector interface").trim(),
    AllIcons.Toolwindows.ToolWindowRun) {

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isVisible = isValid
    }

    val isValid: Boolean
        get() {
            val variant = pointer.element?.variant
            return (OsUtil.isWindows || gameInterfaceName !is NativeInjectorInterface) &&
                    pointer.element != null &&
                    variant != null &&
                    (gameInterfaceName.variant == null || variant like gameInterfaceName.variant || (variant.isC3DS && gameInterfaceName.variant.let { it == null || it is CaosVariant.ANY || it.isC3DS }))
        }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
            ?: return
        val caosFile = pointer.element
            ?: return
        val variant = caosFile.variant?.nullIfNotConcrete()
            ?: return
        caosInject(project, variant, gameInterfaceName, caosFile)
    }


}

internal class AddGameInterfaceAction(private val project: Project, private val variant: CaosVariant?) :
    AnAction("Add GAME Interface Name") {
    override fun actionPerformed(e: AnActionEvent) {
        create(e.files.getOrNull(0))
    }

    fun create(file: VirtualFile?): GameInterfaceName? {
        val newInterface = getGameInterface(project, variant)
            ?: return null

        if (file == null) {
            LOGGER.severe("VirtualFile is null in create undoable add game interface in class: ${this.className}")
            return null
        }

        val undoableAction = object : BasicUndoableAction(file) {
            override fun undo() {
                if (project.isDisposed)
                    return
                CaosApplicationSettingsService.getInstance().removeGameInterfaceName(newInterface)
            }

            override fun redo() {
                if (project.isDisposed)
                    return
                CaosApplicationSettingsService.getInstance().addGameInterfaceName(newInterface)
            }
        }
        WriteCommandAction.writeCommandAction(project)
            .withName("Add Game Interface")
            .withGroupId("caos.ADD_GAME_INTERFACE")
            .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
            .run<Exception> write@{
                if (project.isDisposed)
                    return@write
                UndoManager.getInstance(project).undoableActionPerformed(undoableAction)
            }

        undoableAction.redo()
        return newInterface
    }

    companion object {
        @Suppress("MemberVisibilityCanBePrivate")
        private fun getGameInterface(project: Project, variant: CaosVariant?): GameInterfaceName? {
//            val codes = arrayOf(
//                "C1",
//                "C2",
//                "CV",
//                "C3",
//                "DS",
//                "SM",
//                "*"
//            )
//            val comboBox = JComboBox(codes)
//            comboBox.selectedItem = variant?.code ?: "*"
//            val dialog = DialogBuilder()
//            comboBox.updateUI()
//            comboBox.toolTipText = "Select CAOS variant or '*' if any variant"
//
//            // Add game interface name
//            val gameInterface = JTextField()
//            val gameInterfacePrompt = JLabel("Game interface name. (from machine.cfg)")
//            gameInterface.add(gameInterfacePrompt)
//            gameInterface.toolTipText = "Enter CAOS interface name as set in Machine.cfg for C3/DS"
//            gameInterface.addChangeListener {
//                val isEmpty = gameInterface.text.isEmpty()
//                gameInterfacePrompt.isVisible = isEmpty
//                dialog.okActionEnabled(!isEmpty)
//            }
//
//            // Nickname
//            val nickname = JTextField()
//            val nicknamePrompt = JLabel("Display name")
//            nickname.add(nicknamePrompt)
//
//            nickname.addChangeListener {
//                val isEmpty = nickname.text.isEmpty()
//                nicknamePrompt.isVisible = isEmpty
//            }
//            nickname.toolTipText = "Display name"
//            nickname.minimumSize = Dimension(0, 200)
//
//            val layout = FormLayout(
//                "right:pref, 3dlu, pref, pref, pref",
//                "p,3dlu,p,3dlu,p"
//            )
//            layout.columnGroups = arrayOf(intArrayOf(1, 3, 4, 5))
//            val builder = PanelBuilder(layout)
//            builder.setDefaultDialogBorder()
//            val cc = CellConstraints()
//            builder.addLabel("Variant", cc.xy(1, 1))
//            builder.add(comboBox, cc.xyw(2, 1, 3))
//            builder.addLabel("Game Interface Name", cc.xy(1, 3))
//            builder.add(gameInterface, cc.xyw(2, 3, 3))
//            builder.addLabel("Display Name", cc.xy(1, 5))
//            builder.add(nickname, cc.xyw(2, 5, 3))
//            dialog.setNorthPanel(builder.panel)
//
//            val okay = dialog.addOkAction()
//            okay.setText("Add Interface")
//            if (dialog.showAndGet()) {
//                val code = (comboBox.selectedItem as String).nullIfEmpty()
//                val gameInterfaceName = gameInterface.text.trim().nullIfEmpty()
//                    ?: return null
//                val displayName = nickname.text.trim().nullIfEmpty()
//                return GameInterfaceName(
//                    code,
//                    gameInterfaceName,
//                    displayName
//                )
//            }
//            return null
            return CreateInjectorDialog(project, variant)
                .showAndGetInterface()
        }
    }

}

private typealias MakeName = (gameInterfaceName: GameInterfaceName) -> String

internal fun GameInterfaceName.defaultDisplayName(): String {
    return name
}