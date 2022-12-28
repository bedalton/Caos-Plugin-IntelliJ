package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.FileContentUtilCore

internal fun rerunAnalyzer(element: PsiElement) {
    if (!element.isValid) {
        return
    }
    if (!ApplicationManager.getApplication().isDispatchThread) {
        LOGGER.info("Is not Dispatch thread")
        return
    }
    val file = element.containingFile
        ?: return
    if (!file.isValid) {
        return
    }

    runWriteAction {
        FileContentUtilCore.reparseFiles(file.virtualFile)
        DaemonCodeAnalyzer.getInstance(file.project).restart(file)
    }
}

internal fun rerunAnalyzer(file: PsiFile) {
    if (!file.isValid) {
        return
    }
    if (!ApplicationManager.getApplication().isDispatchThread) {
        return
    }
    runWriteAction {
        FileContentUtilCore.reparseFiles(file.virtualFile)
        DaemonCodeAnalyzer.getInstance(file.project).restart(file)
    }
}