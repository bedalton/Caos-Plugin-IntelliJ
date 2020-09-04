package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.elementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.orFalse
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import kotlin.math.min

class CaosScriptMultipleScriptsInC1File : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspections.scripts-after-event-script.display-name")
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = CaosBundle.message("caos.inspections.scripts-after-event-script.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitMacro(o: CaosScriptMacro) {
                super.visitMacro(o)
                annotate(o, holder)
            }

            override fun visitEventScript(o: CaosScriptEventScript) {
                super.visitEventScript(o)
                annotate(o, holder)
            }

            override fun visitRemovalScript(o: CaosScriptRemovalScript) {
                super.visitRemovalScript(o)
                annotate(o, holder)
            }

            override fun visitInstallScript(o: CaosScriptInstallScript) {
                super.visitInstallScript(o)
                annotate(o, holder)
            }
        }
    }

    private fun annotate(script: CaosScriptScriptElement, problemsHolder: ProblemsHolder) {
        val file = script.containingCaosFile
                ?: return
        if (file.variant?.isNotOld.orFalse())
            return
        val thisScriptStart = script.startOffset
        val isNotDeclaredAfterEventScript =
                PsiTreeUtil.collectElementsOfType(file, CaosScriptEventScript::class.java)
                        .none {
                            it.startOffset < thisScriptStart
                        }
        if (isNotDeclaredAfterEventScript) {
            LOGGER.info("Script: ${script.text.substring(0, min(15, script.textLength))} does not follow an event script")
            return
        }
        LOGGER.info("Script: ${script.text.substring(0, min(15, script.textLength))} follows an event script")
        problemsHolder.registerProblem(script, CaosBundle.message("caos.inspections.scripts-after-event-script.message"), ExtractEventScriptToFile)
    }
}

private object ExtractEventScriptToFile : LocalQuickFix {
    override fun getName(): String = CaosBundle.message("caos.inspections.fixes.extract-event-script-to-new-file")
    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")
    override fun startInWriteAction(): Boolean = true
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val script = descriptor.psiElement.getSelfOrParentOfType(CaosScriptEventScript::class.java)
                ?: return
        val directory = script.containingFile.containingDirectory
        val name = "${script.family} ${script.genus} ${script.species} ${script.eventNumber}"
        invokeLater { // Run on UI thread
            showFileNameDialog(project, directory, name, script.text) {
                runUndoTransparentWriteAction {
                    script.delete()
                }
            }
        }
    }

    private fun showFileNameDialog(project: Project, directory: PsiDirectory, defaultValue: String, script: String, onCreate: () -> Unit) {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("Enter file name"))
        val textField = JTextField(defaultValue)
        panel.add(textField)
        val errorField = JLabel("")
        errorField.foreground = JBColor.RED
        errorField.isVisible = false
        panel.add(errorField)
        val dialog = DialogBuilder()
                .title("Extract CAOS Script")
        dialog.apply {
            textField.addActionListener listener@{
                okActionEnabled(validate(directory, textField, errorField))
            }
            setOkOperation ok@{
                if (!validate(directory, textField, errorField)) {
                    return@ok
                }
                runWriteAction run@{
                    val fileName = textField.text
                    val template = FileTemplateManager.getInstance(project).getInternalTemplate("with-script.cos")
                    val templateProperties = FileTemplateManager.getInstance(project).defaultProperties.apply {
                        this["script"] = script
                    }
                    try {
                        val element = FileTemplateUtil.createFromTemplate(template, fileName, templateProperties, directory)
                        LOGGER.info("Element created has text: ${element.elementType}; ${element.text}")
                        onCreate()
                        val newFile = element.containingFile?.virtualFile
                                ?: element.originalElement?.containingFile?.virtualFile
                                ?: return@run
                        FileEditorManager.getInstance(project).openFile(newFile, false)
                    } catch (e: Exception) {
                        errorField.text = e.localizedMessage
                        errorField.isVisible = true
                    }
                    dialogWrapper.close(0)
                }
            }
            setCenterPanel(panel)
        }
        dialog.showModal(true)
    }


    private fun validate(directory: PsiDirectory, textField: JTextField, errorField: JLabel): Boolean {
        if (textField.text.isEmpty()) {
            errorField.text = ""
            errorField.isVisible = true
            return false
        }
        val fileName = textField.text.let {
            if (!it.endsWith(".com"))
                "$it.cos"
            else
                it
        }
        if (directory.findFile(fileName) != null) {
            errorField.text = "File already exists"
            errorField.isVisible = true
            return false
        }
        errorField.text = ""
        errorField.isVisible = false
        return true
    }
}