package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.computeDelegated
import com.intellij.openapi.project.Project

internal object RunOnVariantSelect {

    inline fun <T> runOn(
        project: Project,
        action: (variant: CaosVariant) -> T?
    ): T? {
        val variant = askUserForVariant(project)
            ?: return null
        return computeDelegated {
            action(variant)
        }
    }
}