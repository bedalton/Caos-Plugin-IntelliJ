package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.like
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfNotConcrete
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosInjectorApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.addGameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.gameInterfaceNames
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.removeGameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosConstants
import com.badahori.creatures.plugins.intellij.agenteering.injector.CreateInjectorDialog
import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.injector.NativeInjectorInterface
import com.badahori.creatures.plugins.intellij.agenteering.injector.TCPInjectorInterface
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.OsUtil
import com.bedalton.common.util.OS
import com.bedalton.common.util.className
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

class InjectorActionGroup(file: CaosScriptFile) : ActionGroup(
    { CaosBundle.message("caos.injector.action.group") },
    { CaosBundle.message("caos.injector.action.description") },
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
                CaosInjectFileAction(it, pointer, makeText(it))
            }
        val addInterfaceName = AddGameInterfaceAction(file.project, file.variant)
        return (interfaceActions + addInterfaceName).toTypedArray()
    }

    companion object {
        fun getGameInterfaceNames(variant: CaosVariant?): List<GameInterfaceName> {
            val projectGameInterfaces = CaosInjectorApplicationSettingsService.getInstance().gameInterfaceNames(variant)
            val variantInterfaces = getDefaultInjectors(variant)
            return (projectGameInterfaces + variantInterfaces)
                .distinctBy { it.id }
        }

        private fun getDefaultInjectors(variant: CaosVariant?): List<GameInterfaceName> {
            return if (OS.Companion.isWindows) {
                getDefaultInjectorsWindows(variant)
            } else {
                getDefaultInjectorsNix(variant)
            }
        }


        private fun getDefaultInjectorsNix(variant: CaosVariant?): List<GameInterfaceName> {
            return when {
                variant == null -> emptyList()
                variant.isC3DS -> listOf(TCPInjectorInterface(variant))
                variant == CaosVariant.CV -> listOf(TCPInjectorInterface(CaosVariant.CV))
                else -> emptyList()
            }
        }

        private fun getDefaultInjectorsWindows(variant: CaosVariant?): List<GameInterfaceName> {
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
                    CaosInjectFileAction(it, pointer, makeText(it))
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
                    CaosInjectFileAction(it, pointer, makeText(it))
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

internal class CaosInjectFileAction(
    internal val gameInterfaceName: GameInterfaceName,
    private val pointer: SmartPsiElementPointer<CaosScriptFile>,
    title: String = gameInterfaceName.defaultDisplayName(),
) : AnAction(
    { title },
    {
        CaosBundle.message(
            "caos.injector.action.add-interface.action-description",
            gameInterfaceName.code.let { if (it == "*" || it == "AL" || it == "ANY") "Any Variant" else it }
        ).trim()
    },
    AllIcons.Toolwindows.ToolWindowRun
) {

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
    AnAction({ CaosBundle.message("caos.injector.action.add-interface") }) {
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
                CaosInjectorApplicationSettingsService.getInstance().removeGameInterfaceName(newInterface)
            }

            override fun redo() {
                if (project.isDisposed)
                    return
                CaosInjectorApplicationSettingsService.getInstance().addGameInterfaceName(newInterface)
            }
        }
        WriteCommandAction.writeCommandAction(project)
            .withName(CaosBundle.message("caos.injector.action.add-interface.undo-label"))
            .withGroupId("caos.ADD_GAME_INTERFACE")
            .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
            .run<Exception> write@{
                if (project.isDisposed) {
                    return@write
                }
                UndoManager.getInstance(project).undoableActionPerformed(undoableAction)
            }

        undoableAction.redo()
        return newInterface
    }

    companion object {
        @Suppress("MemberVisibilityCanBePrivate")
        private fun getGameInterface(project: Project, variant: CaosVariant?): GameInterfaceName? {
            return CreateInjectorDialog(project, variant)
                .showAndGetInterface()
        }
    }

}

private typealias MakeName = (gameInterfaceName: GameInterfaceName) -> String

internal fun GameInterfaceName.defaultDisplayName(): String {
    return name
}