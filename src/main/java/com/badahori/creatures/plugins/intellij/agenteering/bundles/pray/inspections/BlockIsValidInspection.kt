package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections

import bedalton.creatures.pray.compiler.pray.PrayDataValidator
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Pray
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.PRAY
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFileDetector
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertAfterFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertBeforeFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Block
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2BlockComment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.utils.lineNumber
import com.badahori.creatures.plugins.intellij.agenteering.utils.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.tokenType
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PlainTextTokenTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiPlainTextFile
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.max
import kotlin.math.min

open class PrayBlockIsValidInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = AgentMessages.message("inspections.pray.block-is-valid.display-name")
    override fun getGroupDisplayName(): String = PRAY
    override fun getGroupPath(): Array<String> = CAOS2Path

    override fun getShortName(): String = "PRAYBlockIsInvalid"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element.tokenType == PlainTextTokenTypes.PLAIN_TEXT_FILE) {
                    validateBlock(element, holder)
                }
            }
        }
    }

    protected open fun validateBlock(block: PsiElement, holder: ProblemsHolder) {
        val containingFile = block.containingFile
        val blockText = block.text
        if (blockText.isNullOrBlank())
            return
        val errors = try {
            PrayDataValidator.validate(containingFile.virtualFile.path, blockText, false)
                .ifEmpty { null }
        } catch (e: Exception) {
            holder.registerProblem(
                block,
                TextRange.create(block.endOffset - 1, block.endOffset),
                AgentMessages.message("pray.caos2pray.validation-failed", e.message ?: "UNKNOWN")
            )
            e.printStackTrace()
            null
        } ?: return
        val children = block.children
        for (error in errors) {
            val lineNumber = error.lineNumber + 1
            val range = if (lineNumber == 0) {
                children.first().textRange.let {
                    TextRange.create(max(0, min(it.endOffset, it.endOffset - 2)), it.endOffset)
                }
            } else if (error.startIndex < 0 || error.endIndex < 0)
                children.firstOrNull { it.lineNumber == lineNumber }?.textRange
                    ?: block.textRange.let {
                        TextRange(max(0, min(it.endOffset, it.endOffset - 2)), it.endOffset)
                    }
            else {
                TextRange(error.startIndex, error.endIndex + 1)
            }
            holder.registerProblem(
                block,
                range,
                error.message
            )
        }


    }
}

class PrayPlainTextBlockIsValidInspection: PrayBlockIsValidInspection() {
    override fun getShortName(): String = "PRAYTXTBlockIsInvalid"
    override fun getGroupDisplayName(): String = PRAY

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitPlainTextFile(file: PsiPlainTextFile) {
                super.visitPlainTextFile(file)
                if (PrayFileDetector.isPrayFile(file.text)) {
                    validateBlock(file, holder)
                }
            }
        }
    }

    override fun validateBlock(block: PsiElement, holder: ProblemsHolder) {
        if (PrayFileDetector.isPrayFile(block.text))
            super.validateBlock(block, holder)
    }
}

class Caos2PrayBlockIsValidInspection : PrayBlockIsValidInspection() {
    override fun getShortName(): String = "CAOS2PrayBlockIsInvalid"
    override fun getGroupDisplayName(): String = CAOS2Pray

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2Block(block: CaosScriptCaos2Block) {
                super.visitCaos2Block(block)
                if (!block.isCaos2Pray) {
                    return
                }
                val before = PsiTreeUtil.collectElementsOfType(block, CaosScriptCaos2BlockComment::class.java)
                    .firstOrNull()

                val fix = { command: String ->
                    val newText = "*# $command \"\""
                    val label = "Insert '$command' directive"
                    if (before != null) {
                        CaosScriptInsertBeforeFix(label, newText + "\n", before, null) { editor ->
                            editor.caretModel.moveToOffset(editor.caretModel.offset + (newText.length - 1))
                        }
                    } else {
                        CaosScriptInsertAfterFix(
                            label,
                            "\n$newText",
                            block,
                            0
                        ) { editor ->
                            editor.caretModel.moveToOffset(editor.caretModel.offset + newText.length)
                        }
                    }
                }

                val text = "\n" + block.text.lowercase()
                if (!text.contains("^\\s*\\*#\\s+pray-?file +\\S+".toRegex(setOf(RegexOption.IGNORE_CASE,RegexOption.MULTILINE)))) {
                    holder.registerProblem(
                        block,
                        TextRange(0, 2),
                        AgentMessages.message("caos2pray.block-valid-inspection.missing-pray-file"),
                        fix("Pray-File")
                    )
                }
                if (!text.contains("^\\s*\\*#\\s+([a-z\\d]{4}|[Dd][Ss]|[Cc]3)-name +\\S+".toRegex(setOf(RegexOption.IGNORE_CASE,RegexOption.MULTILINE)))) {
                    holder.registerProblem(
                        block,
                        TextRange(0, 2),
                        AgentMessages.message("caos2pray.block-valid-inspection.missing-agent-name"),
                        fix("DSAG-Name"),
                        fix("AGNT-Name")
                    )
                }
            }
        }
    }
}