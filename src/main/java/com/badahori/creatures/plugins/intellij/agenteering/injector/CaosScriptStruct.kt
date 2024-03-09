package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.bedalton.common.util.PathUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.TextRange

sealed class CaosScriptStruct {
    abstract val variant: CaosVariant?
    abstract val path: String?
    abstract val originalText: String
    abstract val descriptor: String?
    abstract val range: TextRange


    data class EventScript(
        override val variant: CaosVariant?,
        override val path: String?,
        val family: Int,
        val genus: Int,
        val species: Int,
        val eventNumber: Int,
        override val originalText: String,
        override val range: TextRange,
        override val descriptor: String?
    ): CaosScriptStruct()

    data class Macro(
        override val variant: CaosVariant?,
        override val path: String?,
        override val originalText: String,
        override val range: TextRange,
        override val descriptor: String?
    ): CaosScriptStruct()


    data class RemovalScript(
        override val variant: CaosVariant?,
        override val path: String?,
        override val originalText: String,
        override val range: TextRange,
        override val descriptor: String?
    ): CaosScriptStruct()

    val collapsed: Boolean by lazy {
        originalText.trim().length >= InjectorHelper.MAX_CAOS_FILE_LENGTH
    }

    val fileName by lazy {
        path?.let {
            PathUtil.getLastPathComponent(it, true) ?: it
        }
    }

    val text: String by lazy {
        formatCaos(variant, originalText, collapsed || variant?.isOld == true).also {
            LOGGER.info("FlattenedCAOS: $it")
        }
    }

    internal fun collapsedLengthIsValid(conn: CaosConnection): Boolean {
        return text.length < conn.maxCaosLength
    }
}

internal fun Collection<CaosScriptScriptElement>.structs(variant: CaosVariant?): List<CaosScriptStruct> {
    return runReadAction {
        map {
            it.toStruct(variant)
        }
    }
}


internal fun CaosScriptScriptElement.toStruct(variant: CaosVariant?): CaosScriptStruct {
    val text = this.codeBlock?.text?.trim() ?: ""

    val fileName = containingFile?.virtualFile?.path?.nullIfEmpty()
        ?: originalElement?.containingFile?.virtualFile?.path
            ?.nullIfEmpty()

    return when (this) {
        is CaosScriptEventScript -> {
            CaosScriptStruct.EventScript(
                variant = variant,
                path = fileName,
                family = family,
                genus = genus,
                species = species,
                eventNumber = eventNumber,
                originalText = text,
                range = textRange,
                descriptor = getDescriptor()
            )
        }

        is CaosScriptRemovalScript -> {
            CaosScriptStruct.RemovalScript(
                variant = variant,
                path = fileName,
                originalText = text,
                range = textRange,
                descriptor = getDescriptor()
            )
        }

        else -> {
            CaosScriptStruct.Macro(
                variant = variant,
                path = fileName,
                originalText = text,
                range = textRange,
                descriptor = getDescriptor()
            )
        }
    }
}

internal fun CaosScriptScriptElement.getDescriptor(): String {
    return when (this) {
        is CaosScriptRemovalScript -> "Removal script"
        is CaosScriptEventScript -> "SCRP $family $genus $species $eventNumber"
        is CaosScriptInstallScript -> "Install script"
        is CaosScriptMacro -> "Body script"
        else -> "Code block"
    }
}