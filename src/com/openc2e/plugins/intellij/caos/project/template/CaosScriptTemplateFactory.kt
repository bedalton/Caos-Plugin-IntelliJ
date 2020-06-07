package com.openc2e.plugins.intellij.caos.project.template

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.platform.ProjectTemplate
import com.intellij.platform.ProjectTemplatesFactory


/**
 * @author Dennis.Ushakov
 */
class CaosScriptTemplateFactory : ProjectTemplatesFactory() {
    override fun getGroups(): Array<String> {
        return arrayOf(GROUP_NAME)
    }

    override fun createTemplates(group: String?, context: WizardContext): Array<ProjectTemplate> {
        return arrayOf(
                CaosScriptVariantTemplate()
        )
    }

    companion object {
        private val GROUP_NAME = "Caos Script";
    }
}