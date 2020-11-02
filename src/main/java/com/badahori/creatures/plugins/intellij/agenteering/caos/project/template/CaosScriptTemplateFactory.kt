package com.badahori.creatures.plugins.intellij.agenteering.caos.project.template

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.platform.ProjectTemplate
import com.intellij.platform.ProjectTemplatesFactory
import icons.CaosScriptIcons
import javax.swing.Icon


/**
 * @author Dennis.Ushakov
 */
class CaosScriptTemplateFactory : ProjectTemplatesFactory() {
    override fun getGroups(): Array<String> {
        return arrayOf(GROUP_NAME)
    }

    override fun getGroupIcon(group: String?): Icon {
        return CaosScriptIcons.MODULE_ICON
    }

    override fun createTemplates(group: String?, context: WizardContext): Array<ProjectTemplate> {
        return arrayOf(
                CaosScriptProjectGenerator()
        )
    }

    companion object {
        private const val GROUP_NAME = "Caos Script"
    }
}