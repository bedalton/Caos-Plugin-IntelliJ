package com.badahori.creatures.plugins.intellij.agenteering.caos.project.template

import com.intellij.facet.ui.ValidationResult
import com.intellij.platform.DirectoryProjectGeneratorBase


/**
 * Extend this class to contribute web project generator to IDEA (available via File -> 'Add Module...' -> 'Web Module')
 * and to small IDE (PhpStorm, WebStorm etc. available via File -> 'New Project...').
 *
 * @author Sergey Simonchik
 */
abstract class CaosScriptProjectGenerator : DirectoryProjectGeneratorBase<CaosProjectGeneratorInfo>() {

    override fun validate(baseDirPath: String): ValidationResult {
        return ValidationResult.OK
    }

    abstract override fun getDescription(): String?
}