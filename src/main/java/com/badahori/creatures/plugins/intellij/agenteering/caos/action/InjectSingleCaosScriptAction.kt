package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfNotConcrete
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor.EDITOR_INJECTOR_KEY
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor.EDITOR_VARIANT_KEY
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptInstallScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptMacro
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRemovalScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosInjectorApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.gameInterfaceNames
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.injectionCheckDisabled
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.inferVariantHard
import com.badahori.creatures.plugins.intellij.agenteering.injector.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.bedalton.common.util.formatted
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CodeSmellDetector
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import icons.CaosScriptIcons
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class InjectSingleCaosScriptAction : AnAction(
    {
        CaosBundle.message(
            "caos.injector.action.inject-script.title",
            SCRIPT_NAME_DEFAULT
        )
    },
    {
        CaosBundle.message(
            "caos.injector.action.inject-script.description", SCRIPT_NAME_DEFAULT
        )
    },
    CaosScriptIcons.MODULE_ICON
) {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        val script = getScriptFromAction(e)

        val presentation = e.presentation

        presentation.isEnabledAndVisible = script != null && e.getData(PlatformDataKeys.EDITOR) != null

        val scriptDescriptor = script?.getDescriptor() ?: SCRIPT_NAME_DEFAULT
        presentation.text = CaosBundle.message(
            "caos.injector.action.inject-script.title",
            scriptDescriptor
        )
    }

    private fun getScriptFromAction(e: AnActionEvent): CaosScriptScriptElement? {
        val element = e.getData(PlatformDataKeys.PSI_ELEMENT)
            ?: e.getData(PlatformDataKeys.NAVIGATABLE) as? PsiElement
            ?: return null
        return element.getSelfOrParentOfType(CaosScriptScriptElement::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            LOGGER.severe("Project is null when injecting script")
            return
        }
        val element = e.getData(PlatformDataKeys.PSI_ELEMENT)
            ?: e.getData(PlatformDataKeys.NAVIGATABLE) as? PsiElement

        // Get editor if any
        // TODO ask for injector if no injector is available
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: element?.editor

        inject(project, editor, element)
    }


}

private val SCRIPT_NAME_DEFAULT by lazy {
    CaosBundle.message("caos.injector.action.inject-script.default-script-descriptor")
}




class InjectSingleCaosScriptIntentionAction : PsiElementBaseIntentionAction(), IntentionAction, LocalQuickFix, DumbAware {
    override fun getFamilyName(): String = CAOSScript

    override fun getName(): String {
        return CaosBundle.message("caos.injector.action.inject-script.title", "Script")
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getText(): String {
        return CaosBundle.message("caos.injector.action.inject-script.title", "Script")
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.isOrHasParentOfType(CaosScriptScriptElement::class.java) && element.editor != null
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val editor = element.editor
        inject(project, editor, element)
    }


    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        inject(project, editor, element)
    }

}

private fun inject(project: Project, editorIn: Editor?, element: PsiElement?) {

    if (project.isDisposed) {
        LOGGER.severe("InjectSingleCaosScriptAction called while project was closing or disposed")
        return
    }

    val script = getScriptElement(project, element)
        ?: return

    val editor = editorIn
        ?: getEditor(project, script)

    val virtualFile = getVirtualFile(project, script)
        ?: editor?.virtualFile
        ?: return

    var variant = getVariant(
        project,
        editor,
        virtualFile,
        script
    )

    val injector = getInjector(project, editor, variant)
        ?: return

    if (variant == null) {
        variant = injector.variant
    }

    if (variant == null) {
        postError(
            project,
            CaosBundle.message("caos.inject.errors.ject-error-title"),
            CaosBundle.message("caos.injector.action.inject-script.variant-not-resolved")
        )
        return
    }

    val scriptPointer = SmartPointerManager.createPointer(script)

    // Actually inject
    caosInject(project, variant, injector, virtualFile, scriptPointer)
}

private fun getEditor(project: Project, element: PsiElement): Editor? {
    element.editor?.let {
        return it
    }
    postError(
        project,
        CaosBundle.message("caos.inject.errors.ject-error-title"),
        CaosBundle.message("caos.injector.action.inject-script.editor-not-resolved")
    )
    return null
}

private fun getScriptElement(project: Project, element: PsiElement?): CaosScriptScriptElement? {
    val script = element as? CaosScriptScriptElement
        ?: element?.getSelfOrParentOfType(CaosScriptScriptElement::class.java)

    if (script != null) {
        return script
    }
    postError(
        project,
        CaosBundle.message("caos.inject.errors.ject-error-title"),
        CaosBundle.message("caos.injector.action.inject-script.description.not-a-script")
    )
    return null
}

private fun getVirtualFile(project: Project, element: PsiElement): VirtualFile? {
    element.virtualFile?.let {
        return it
    }
    postError(
        project,
        CaosBundle.message("caos.inject.errors.ject-error-title"),
        CaosBundle.message("caos.injector.action.inject-script.virtual-file-not-resolved")
    )
    return null

}

private fun getInjector(project: Project, editor: Editor?, variant: CaosVariant?): GameInterfaceName? {
    editor?.getUserData(EDITOR_INJECTOR_KEY)?.let {
        return it
    }
    if (variant.nullIfNotConcrete() != null) {
        CaosInjectorApplicationSettingsService
            .getInstance()
            .gameInterfaceNames(variant)
            .singleOrNull()
            ?.let {
                return it
            }
    }
    postError(
        project,
        CaosBundle.message("caos.inject.errors.ject-error-title"),
        CaosBundle.message("caos.injector.action.inject-script.editor-injector-not-resolved")
    )
    return null
}

private fun getVariant(project: Project, editor: Editor?, virtualFile: VirtualFile, element: CaosScriptScriptElement): CaosVariant? {
    return editor?.getUserData(EDITOR_VARIANT_KEY)
        ?: virtualFile.getUserData(EDITOR_VARIANT_KEY)
        ?: element.variant
        ?: element.containingCaosFile?.variant
        ?: project.inferVariantHard()
        ?: askUserForVariant(project)
}

private fun caosInject(
    project: Project,
    variant: CaosVariant,
    gameInterfaceName: GameInterfaceName,
    virtualFile: VirtualFile,
    scriptPointer: SmartPsiElementPointer<CaosScriptScriptElement>,
) {

    val script = scriptPointer.element

    if (script == null) {
        postError(project, "Stale Editor State", "Script state has become invalid. Please try again")
        return
    }

    // Check for errors in script
    if (!isValidForInject(project, virtualFile, script)) {
        setActionForInvalidScript(project, variant, gameInterfaceName, scriptPointer)
        return
    }

    try {
        injectActual(project, variant, gameInterfaceName, scriptPointer)
    } catch (e: Exception) {
        e.rethrowAnyCancellationException()
        LOGGER.severe("Inject actual failed: ${e.message}")
        e.printStackTrace()
    }
}

private fun setActionForInvalidScript(
    project: Project,
    variant: CaosVariant,
    gameInterfaceName: GameInterfaceName,
    scriptPointer: SmartPsiElementPointer<CaosScriptScriptElement>
) {

    val ignoreAlways = IgnoreAndInject(
        CaosBundle.message("caos.injector.action.inject.ignore-errors-for-session.title"),
        CaosBundle.message("caos.injector.action.inject.ignore-errors-for-session.description"),
        ignoreAlways = true,
        variant,
        gameInterfaceName,
        scriptPointer
    )

    val ignoreOnce = IgnoreAndInject(
        CaosBundle.message("caos.injector.action.inject.ignore-errors-once.title"),
        CaosBundle.message("caos.injector.action.inject.ignore-errors-once.description"),
        ignoreAlways = true,
        variant,
        gameInterfaceName,
        scriptPointer
    )

    // If virtual file is valid run check for validity
    CaosInjectorNotifications
        .createErrorNotification(project, "Syntax Errors", "Cannot inject CAOS code with known errors.")
        .addAction(ignoreAlways)
        .addAction(ignoreOnce)
        .show()

    return
}

private class IgnoreAndInject(
    title: String,
    description: String,
    val ignoreAlways: Boolean,
    val variant: CaosVariant,
    val gameInterfaceName: GameInterfaceName,
    val scriptPointer: SmartPsiElementPointer<CaosScriptScriptElement>,
) : AnAction(title, description, CaosScriptIcons.JECT) {


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project == null || project.isDisposed) {
            return
        }

        if (ignoreAlways) {
            project.settings.injectionCheckDisabled = true
        }

        try {
            injectActual(project, variant, gameInterfaceName, scriptPointer)
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            LOGGER.severe("Inject ignored for session failed: ${e.message}")
            e.printStackTrace()
        }
    }

}

private fun injectActual(
    project: Project,
    variant: CaosVariant,
    gameInterfaceName: GameInterfaceName,
    scriptPointer: SmartPsiElementPointer<CaosScriptScriptElement>,
) {
    // Get contents of file and format for injection
    WriteCommandAction.writeCommandAction(project)
        .withUndoConfirmationPolicy(UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
        .shouldRecordActionForActiveDocument(false)
        .run<Throwable> run@{

            val script = scriptPointer
                .element

            if (script == null) {
                postError(project, "Stale Editor State", "Script state has become invalid. Please try again")
                return@run
            }

            val content = script.text

            if (content.isBlank()) {
                postWarning(project, "Empty Injection", "Empty code body was not injected")
                return@run
            }

            val struct = script.toStruct(gameInterfaceName.variant)

            val jectType = when (struct) {
                is CaosScriptStruct.EventScript -> JectScriptType.EVENT
                is CaosScriptInstallScript -> JectScriptType.REMOVAL
                is CaosScriptMacro -> JectScriptType.INSTALL
                is CaosScriptRemovalScript -> JectScriptType.REMOVAL
                else -> {
                    postWarning(project, "Ject Error", "Failed to ascertain script type")
                    return@run
                }
            }

            val fileName = script.containingCaosFile?.name ?: "Editor"
            // Add inject command to thread pool
            GlobalScope.launch {
                try {
                    Injector.inject(
                        project = project,
                        variant = variant,
                        gameInterfaceName = gameInterfaceName,
                        fileName = fileName,
                        scripts = mapOf(jectType to listOf(struct)),
                    )
                } catch (e: Exception) {
                    e.rethrowAnyCancellationException()
                    LOGGER.severe("Failed to inject script through command; ${e.formatted(true)}")
                    e.printStackTrace()
                }
            }
        }

}


private fun isValidForInject(project: Project, virtualFile: VirtualFile, script: CaosScriptScriptElement): Boolean {
    if (project.isDisposed) {
        return false
    }
    if (project.settings.injectionCheckDisabled) {
        return true
    }
    val detector = CodeSmellDetector.getInstance(project)
    val range = script.textRange
    val smells = detector.findCodeSmells(listOf(virtualFile))
        .filter {
            it.severity == HighlightSeverity.ERROR && it.textRange.intersects(range)
        }
    return smells.isEmpty()
}