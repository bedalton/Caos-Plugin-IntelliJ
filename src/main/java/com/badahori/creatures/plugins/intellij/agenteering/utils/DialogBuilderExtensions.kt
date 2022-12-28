package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper


internal fun DialogBuilder.close(code: Int) {
    dialogWrapper.close(code)
}

internal fun DialogBuilder.closeWithOkExitCode() {
    dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
}

internal fun DialogBuilder.closeWithCancelExitCode() {
    dialogWrapper.close(DialogWrapper.CANCEL_EXIT_CODE)
}

internal fun DialogBuilder.closeWithCloseExitCode() {
    dialogWrapper.close(DialogWrapper.CLOSE_EXIT_CODE)
}