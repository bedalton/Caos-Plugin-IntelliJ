package com.badahori.creatures.plugins.intellij.agenteering.bundles.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobException
import com.badahori.creatures.plugins.intellij.agenteering.bundles.fixes.Caos2CobRemoveFileFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Tag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrBlank
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class Caos2CobIsExpectedTagValueType : LocalInspectionTool() {

    override fun getDisplayName(): String = "Invalid property value"
    override fun getGroupDisplayName(): String = CaosBundle.message("cob.caos2cob.inspections.group")
    override fun getGroupPath(): Array<String> {
        return arrayOf(CaosBundle.message("caos.intentions.family"))
    }
    override fun getShortName(): String = "Caos2CobInvalidTagValue"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2Tag(o: CaosScriptCaos2Tag) {
                super.visitCaos2Tag(o)
                if (!o.containingCaosFile?.isCaos2Cob.orFalse())
                    return
                val tag = CobTag.fromString(o.tagName)
                    ?: return
                val type = when (tag) {
                    AGENT_NAME -> STRING
                    COB_NAME -> STRING
                    QUANTITY_AVAILABLE -> INT
                    THUMBNAIL -> STRING
                    EXPIRY -> DATE
                    ISCR -> STRING
                    RSCR -> STRING
                    REMOVER_NAME -> STRING
                    QUANTITY_USED -> INT
                    DESCRIPTION -> STRING
                    LAST_USAGE_DATE -> DATE
                    REUSE_INTERVAL -> INT
                    RESERVED -> INT
                    LINK -> STRING
                    DEPENDS -> STRING
                    ATTACH -> STRING
                    INLINE -> STRING
                    CREATION_DATE -> DATE
                    AUTHOR_NAME -> STRING
                    AUTHOR_EMAIL -> STRING
                    AUTHOR_URL -> STRING
                    VERSION -> INT
                    REVISION -> INT
                    AUTHOR_COMMENTS -> STRING
                }
                val element = o.caos2CommentValue
                if (element == null) {
                    holder.registerProblem(o.equalSign, "Blank values are ignored", ProblemHighlightType.WEAK_WARNING)
                    return
                }
                val value = o.value

                val error = when (type) {
                    STRING -> if (value?.toIntSafe() != null || value?.matches(DATE_REGEX).orFalse())
                        "CAOS2Cob property '${o.tagName}' expects text value"
                    else if (value.isNullOrBlank())
                        "CAOS2Cob property '${o.tagName}' expects a non-blank string value"
                    else
                        null
                    INT -> if (value?.toIntSafe() != null)
                        null
                    else
                        "Property '${o.tagName}' expects an integer value"
                    DATE -> if (value.isNotNullOrBlank() && value.matches(DATE_REGEX))
                        null
                    else
                        "${o.tagName} expects a date in the format YYYY-MM-DD"
                    STRING_NULLABLE -> {
                        if (value?.matches(DATE_REGEX).orFalse())
                            "CAOS2Cob property '${o.tagName}' expects text value"
                        if (value?.toIntSafe() == null)
                            "CAOS2Cob property '${o.tagName}' expects string value"
                        else
                            null
                    }
                    else -> throw Caos2CobException("Failed to understand expected tag value type: '$type'")
                } ?: return
                holder.registerProblem(element, error, Caos2CobRemoveFileFix(element))
            }
        }

    }
    companion object {
        private const val INT = 0
        private const val STRING = 1
        private const val STRING_NULLABLE = 3
        private const val DATE = 2
        private val DATE_REGEX = "^\\d{4}-\\d{2}-\\d{2}$".toRegex()
    }
}