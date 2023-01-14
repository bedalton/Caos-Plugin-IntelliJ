package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.fixes.Caos2CobRemoveFileFix
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.getFileNameWithArrayAccess
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.bedalton.common.util.FileNameUtil
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElementVisitor

class Caos2CobIsExpectedTagValueType : LocalInspectionTool(), DumbAware {

    override fun getDisplayName(): String = "Invalid property value"
    override fun getGroupDisplayName(): String = CAOS2Cob
    override fun getGroupPath(): Array<String> = CAOS2Path
    override fun getShortName(): String = "Caos2CobInvalidTagValue"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2Tag(tagElement: CaosScriptCaos2Tag) {
                super.visitCaos2Tag(tagElement)
                if (!tagElement.containingCaosFile?.isCaos2Cob.orFalse())
                    return
                val tag = CobTag.fromString(tagElement.tagName)
                    ?: return

                val tagTagElement = tagElement.caos2TagName

                val tagValueElement = tagElement.caos2Value
                    ?: return
                validateTag(tag, tagTagElement, tagValueElement, holder)
            }
        }

    }
}


private fun validateTag(
    tag: CobTag,
    tagElement: CaosScriptCaos2TagName,
    tagValueElement: CaosScriptCaos2Value,
    holder: ProblemsHolder
) {
    if (tag == CobTag.THUMBNAIL) {
        validateThumbnail(tag, tagValueElement, holder)
        return
    }
    val format = tag.format
    if (!format.validate(tagValueElement.text)) {
        val error =  AgentMessages.message(
            "errors.tags.invalid-tag-value-format",
            tagElement.stringValue,
            format.formatDescriptor
        )
        holder.registerProblem(tagValueElement, error)
    }
}

private fun validateThumbnail(
    tag: CobTag,
    tagValueElement: CaosScriptCaos2Value,
    holder: ProblemsHolder
) {
    if (tag != CobTag.THUMBNAIL) {
        return
    }

    val fileName = tagValueElement
        .valueAsString
        ?.let { getFileNameWithArrayAccess(it) }
        ?: return

    val extension = FileNameUtil
        .getExtension(fileName)
        ?.lowercase()

    // Check is sprite extension is valid
    val check = extension in listOf("gif", "jpeg", "jpg", "png", "spr", "s16", "c16", "bmp")

    if (check) {
        return
    }
    val fixes = mutableListOf<LocalQuickFix>(Caos2CobRemoveFileFix(tagValueElement))

    val error = AgentMessages.message(
        "cob.caos2cob.inspections.included-file-type-valid.expects-image",
        "${tag.keys.first()} property"
    )
    holder.registerProblem(tagValueElement, error, *fixes.toTypedArray())
}
