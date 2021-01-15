package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.intellij.openapi.actionSystem.AnActionEvent

val AnActionEvent.files get() = dataContext.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: emptyArray()