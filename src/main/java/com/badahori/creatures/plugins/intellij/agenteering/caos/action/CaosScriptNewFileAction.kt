package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.common.MyNewFileAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.psi.PsiDirectory
import icons.CaosScriptIcons


class CaosScriptNewFileAction : MyNewFileAction(
    CaosBundle.message("caos.actions.new-file.title"),
    "CAOS File",
    CaosBundle.message("caos.actions.new-file.description"),
    "cos-macro",
    "cos",
    CaosScriptIcons.CAOS_FILE_ICON
) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle(title)
            .setValidator(object : InputValidatorEx {
                override fun canClose(inputString: String?) = checkInput(inputString)
                override fun getErrorText(inputString: String?) = CaosBundle.message("caos.actions.new-file.invalid", inputString.orEmpty())
                override fun checkInput(inputString: String?) = inputString != null && !inputString.contains(':')
            })
            .addKind(kind, icon, templateName)
    }
}
