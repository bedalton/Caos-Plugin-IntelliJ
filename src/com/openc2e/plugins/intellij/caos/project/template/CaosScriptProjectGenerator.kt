package com.openc2e.plugins.intellij.caos.project.template

import com.intellij.facet.ui.ValidationResult
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import javax.swing.JComponent


/**
 * Extend this class to contribute web project generator to IDEA (available via File -> 'Add Module...' -> 'Web Module')
 * and to small IDE (PhpStorm, WebStorm etc. available via File -> 'New Project...').
 *
 * @author Sergey Simonchik
 */
abstract class CaosScriptProjectGenerator : DirectoryProjectGeneratorBase<CaosProjectGeneratorInfo>() {
    /**
     * Always returns [ValidationResult.OK].
     * Real validation should be done in [WebProjectGenerator.GeneratorPeer.validate].
     */
    override fun validate(baseDirPath: String): ValidationResult {
        return ValidationResult.OK
    }

    abstract override fun getDescription(): String?
}