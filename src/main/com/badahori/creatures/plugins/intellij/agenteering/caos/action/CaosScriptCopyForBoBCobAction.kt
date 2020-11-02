package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.copyForBobCob
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CopyAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAware

/**
 * Creates a file
 * @todo implement multiple file types (ie. implementations or protocols)
 */
class CaosScriptCopyForBoBCobAction : CopyAction(), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val dataContext = event.dataContext
        (dataContext.getData(DataKeys.PSI_FILE) as? CaosScriptFile)?.let {
            copyForBobCob(it)
        }
    }

    override fun update(event: AnActionEvent) {
        val presentation = event.presentation
        val dataContext = event.dataContext
        val enabled = (dataContext.getData(DataKeys.PSI_FILE) as? CaosScriptFile)?.variant?.isOld.orFalse()
        presentation.isEnabled = enabled
        presentation.text = CaosBundle.message("caos.actions.copy-for-bob-cob.title")
        presentation.description = CaosBundle.message("caos.actions.copy-for-bob-cob.description")
        presentation.icon = AllIcons.Actions.Copy
    }
}