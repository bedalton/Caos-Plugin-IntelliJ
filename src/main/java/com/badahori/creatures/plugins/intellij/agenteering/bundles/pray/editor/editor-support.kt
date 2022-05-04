package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.editor

import bedalton.creatures.pray.cli.PrayCompilerCliOptions
import bedalton.creatures.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.compiler.CompilePrayFileAction
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.ProjectTopics
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil.TRANSPARENT_COLOR
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


/**
 * A editor notification provider
 * Though not its original purpose, the notification provider functions as a persistent toolbar
 */
class PrayEditorToolbar(val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    init {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                //notifications.updateAllNotifications()
            }
        })
    }

    override fun createNotificationPanel(
        virtualFile: VirtualFile,
        fileEditor: FileEditor,
        project: Project
    ): EditorNotificationPanel? {
        val psiFile = virtualFile.getPsiFile(project)
            ?: return null
        if (psiFile !is PrayFile)
            return null
        val headerComponent = createPrayScriptHeaderComponent(psiFile)
        val panel = EditorNotificationPanel(TRANSPARENT_COLOR)
        panel.add(headerComponent)
        return panel
    }

    companion object {
        private val KEY: Key<EditorNotificationPanel> = Key.create("creatures.pray.PRAYEditorToolbar")

    }
}


private class EditorActionGroup(psiFile: PsiFile): ActionGroup() {

    val pointer = SmartPointerManager.createPointer(psiFile)

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isVisible = pointer.element != null
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(
            CompilePrayFileAction(false)
        )
    }

}

internal fun createPrayScriptHeaderComponent(psiFile: PsiFile): JComponent {

    val toolbar = JPanel()
    toolbar.layout = FlowLayout()
    toolbar.add(JLabel("Compile: "))
    toolbar.add(ActionManager.getInstance().createActionToolbar("PRAY Commands", EditorActionGroup(psiFile), true).component)
//    val compileButton = JButton("Compile")
//    compileButton.addClickListener { e ->
//        if (e.button == MouseEvent.BUTTON1) {
//            PrayCompilerToolbarActions.compile(psiFile.project, psiFile)
//        }
//    }
//    toolbar.add(compileButton)
//    val settings = JButton()
//    settings.icon = CaosScriptIcons.COMPILE
//    settings.addClickListener click@{ e ->
//        if (!e.isButton1) {
//            return@click
//        }
//        PrayCompilerToolbarActions.requestUpdatedOpts(psiFile)
//    }
//    toolbar.add(settings)
    return toolbar
}

internal object PrayCompilerToolbarActions {

    internal fun compile(project: Project, file: PsiFile) {
        getOpts(file) { opts ->
            if (opts == null) {
                CaosNotifications.showError(
                    project,
                    "PRAY Compile Error",
                    "Failed to compile PRAY file. Compiler options not set"
                )
                return@getOpts
            }

            runBackgroundableTask("Compile PRAY Agent") task@{
                val output = CompilePrayFileAction.compile(project, file.virtualFile.path, opts)
                    ?: return@task
                invokeLater {
                    CaosNotifications.showInfo(
                        project,
                        "PRAY Compiler",
                        "Compiled PRAY file '${file.name}' to '${FileNameUtil.getFileNameWithoutExtension(output)}'"
                    )
                }
            }

        }
    }

    private fun getOpts(file: PsiFile, onOpts: (PrayCompilerCliOptions?) -> Unit) {
        val cached = when (file) {
            is CaosScriptFile -> file.compilerSettings
            is PrayFile -> file.compilerSettings
            else -> null
        }
        if (cached != null)
            return onOpts(cached)
        CompilePrayFileAction.getOpts opts@{ opts ->
            if (opts == null) {
                onOpts(null)
                return@opts
            }
            if (file is CaosScriptFile)
                file.compilerSettings = opts
            else if (file is PrayFile)
                file.compilerSettings = opts
            onOpts(opts)
        }
    }


    internal fun requestUpdatedOpts(file: PsiFile) {
        val cached = when (file) {
            is CaosScriptFile -> file.compilerSettings
            is PrayFile -> file.compilerSettings
            else -> null
        }
        CompilePrayFileAction.getOpts(cached) { opts ->
            if (opts == null)
                return@getOpts
            if (file is CaosScriptFile)
                file.compilerSettings = opts
            else if (file is PrayFile)
                file.compilerSettings = opts
            return@getOpts
        }
    }
}