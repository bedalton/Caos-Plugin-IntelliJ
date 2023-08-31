package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.editor


import com.bedalton.creatures.agents.pray.compiler.PrayCompileOptions
import com.bedalton.common.util.PathUtil
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.compiler.CompilePrayFileAction
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil.TRANSPARENT_COLOR
import kotlinx.coroutines.runBlocking
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


/**
 * An editor notification provider
 * Though not its original purpose, the notification provider functions as a persistent toolbar
 */
class PrayEditorToolbar(val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {

    override fun getKey(): Key<EditorNotificationPanel> = KEY

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


private class EditorActionGroup(psiFile: PsiFile) : ActionGroup() {

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
    toolbar.add(
        ActionManager.getInstance().createActionToolbar("PRAY Commands", EditorActionGroup(psiFile), true).component
    )
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

@Suppress("unused")
internal object PrayCompilerToolbarActions {

    internal fun compile(project: Project, file: PsiFile) {
        val opts = getOpts(file)

        if (opts == null) {
            CaosNotifications.showError(
                project,
                "PRAY Compile Error",
                "Failed to compile PRAY file. Compiler options not set"
            )
            return
        }

        runBackgroundableTask("Compile PRAY Agent") task@{
            val output = runBlocking { CompilePrayFileAction.compile(project, file.virtualFile.path, opts) }
                ?: return@task
            invokeLater {
                CaosNotifications.showInfo(
                    project,
                    "PRAY Compiler",
                    "Compiled PRAY file '${file.name}' to '${PathUtil.getFileNameWithoutExtension(output)}'"
                )
            }
        }
    }

    private fun getOpts(file: PsiFile): PrayCompileOptions? {
        val cached = when (file) {
            is CaosScriptFile -> file.compilerSettings
            is PrayFile -> file.compilerSettings
            else -> null
        }
        if (cached != null) {
            return cached
        }
        val opts = CompilePrayFileAction.getOpts()
            ?: return null
        if (file is CaosScriptFile) {
            file.compilerSettings = opts
        } else if (file is PrayFile) {
            file.compilerSettings = opts
        }
        return opts
    }


    internal fun requestUpdatedOpts(file: PsiFile) {
        val cached = when (file) {
            is CaosScriptFile -> file.compilerSettings
            is PrayFile -> file.compilerSettings
            else -> null
        }
        val opts = CompilePrayFileAction.getOpts(cached)
            ?: return
        if (file is CaosScriptFile)
            file.compilerSettings = opts
        else if (file is PrayFile)
            file.compilerSettings = opts
        return
    }
}