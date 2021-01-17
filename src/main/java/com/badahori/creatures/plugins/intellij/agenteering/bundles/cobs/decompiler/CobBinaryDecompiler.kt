package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.runInspections
import com.badahori.creatures.plugins.intellij.agenteering.utils.FileNameUtils
import com.badahori.creatures.plugins.intellij.agenteering.utils.cString
import com.badahori.creatures.plugins.intellij.agenteering.utils.littleEndian
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import kotlin.math.floor

class CobBinaryDecompiler : BinaryFileDecompiler {
    override fun decompile(virtualFile: VirtualFile): CharSequence {
        return decompileToString(virtualFile.name, virtualFile.contentsToByteArray())
    }

    companion object {

        private val DATE_FORMAT = SimpleDateFormat("MM/dd/YYYY")

        internal fun decompileToString(fileName: String, byteArray: ByteArray): String {
            val byteBuffer = ByteBuffer.wrap(byteArray).littleEndian()
            if (byteBuffer.cString(4) == "****") {
                return byteArray.contentToString()
            }
            val cobData = CobToDataObjectDecompiler.decompile(byteBuffer, FileNameUtils.getBaseName(fileName))
            return presentCobData(fileName, cobData)
        }

        fun decompileToPsiFile(project: Project, fileName: String, byteArray: ByteArray): PsiFile {
            val cobData = CobToDataObjectDecompiler.decompile(ByteBuffer.wrap(byteArray).littleEndian(), FileNameUtils.getBaseName(fileName))
            val text = presentCobData(fileName, cobData)
            val psiFile = PsiFileFactory.getInstance(project)
                    .createFileFromText("(Decompiled) $fileName", CaosScriptLanguage, text) as CaosScriptFile
            val variant = if (cobData is CobFileData.C1CobData) CaosVariant.C1 else CaosVariant.C2
            psiFile.variant = variant
            psiFile.runInspections = false
            GlobalScope.launch {
                CaosScriptExpandCommasIntentionAction.invoke(project, psiFile)
            }
            return psiFile
        }

        fun cobDataToPsiFile(project: Project, fileName: String, cobData: CobFileData): PsiFile {
            val text = presentCobData(fileName, cobData)
            val psiFile = PsiFileFactory.getInstance(project)
                    .createFileFromText("(Decompiled) $fileName", CaosScriptLanguage, text) as CaosScriptFile
            val variant = if (cobData is CobFileData.C1CobData) CaosVariant.C1 else CaosVariant.C2
            psiFile.variant = variant
            psiFile.runInspections = false
            GlobalScope.launch {
                CaosScriptExpandCommasIntentionAction.invoke(project, psiFile)
            }
            return psiFile
        }

        private fun presentCobData(fileName: String, cobData: CobFileData): String {
            return when (cobData) {
                is CobFileData.C1CobData -> {
                    presentC1CobData(fileName, cobData)
                }
                is CobFileData.C2CobData -> {
                    presentC2CobData(fileName, cobData)
                }
                is CobFileData.InvalidCobData -> {
                    val maxLength = 80
                    val words = cobData.message.split(" ")
                    val topBottom = (0 until maxLength + 4).joinToString("") { "*" }
                    val builder = StringBuilder(topBottom)
                    var line = StringBuilder("* ")
                    for (word in words) {
                        if (line.length + word.length > maxLength) {
                            builder.append(line).append("*").append("\n* ")
                            line = StringBuilder("* ")
                        }
                        line.append(word).append(" ")
                    }
                    builder.append(topBottom).toString()
                }
            }
        }

        private fun presentC1CobData(fileName: String, cobData: CobFileData.C1CobData): String {

            val agentBlock = cobData.cobBlock
            val scriptHeaderLength = 30
            val wrapChar = "="
            val installScript = agentBlock.installScripts.nullIfEmpty()?.let { installScripts ->
                        installScripts
                            .joinToString(""){
                                    installScript -> "\n*** INSTALL SCRIPT ***\n" + installScript.code
                            }
                            .trim()
                            .nullIfEmpty()
            } ?: ""
                val eventScripts = agentBlock.eventScripts.joinToString("\n\n") {
                    "${wrap(it.scriptName, scriptHeaderLength, wrapChar)}\n${it.code}"
                }
                val header = "COB...............$fileName"
                val agentName = "Agent.............${agentBlock.name}"
                val expiry = "Expiry............${agentBlock.expiry.let { DATE_FORMAT.format(it.time) }}"
                val quantity = "Quantity..........${agentBlock.quantityAvailable}"
                val width = listOf(header, agentName, expiry, quantity).map { it.length + 2 }.max()!! + 5
                val top = (1..width).joinToString("") { "*" }
                val offset = 3 // leading asterisk, space and trailing asterisk
                val items = listOfNotNull(
                    top,
                    header.nullIfEmpty()?.let { "* ${pad(it, width - offset)}*" },
                    agentName.nullIfEmpty()?.let { "* ${pad(it, width - offset)}*" },
                    expiry.nullIfEmpty()?.let { "* ${pad(it, width - offset)}*" },
                    quantity.nullIfEmpty()?.let { "* ${pad(it, width - offset)}*" },
                    top,
                    installScript.trim().nullIfEmpty()?.let { "\n$it" },
                    eventScripts.trim().nullIfEmpty()?.let { "\n$it" }

                )
                return items.joinToString("\n")
        }

        private fun presentC2CobData(fileName: String, cobData: CobFileData.C2CobData): String {

            val author = cobData.authorBlocks.firstOrNull()?.let {
                """
                * COB..............$fileName
                * Author...........${it.authorName}
                * Author Email.....${it.authorEmail}
                * Author URL.......${it.authorUrl}
                * Author Comments..${it.authorComments}
                """.trimIndent()
            }
            val agents = cobData.agentBlocks.joinToString("\n\n") { block ->
                val expiry = block.expiry.let { DATE_FORMAT.format(it.time) }
                val dependencies = "[${block.dependencies.joinToString { it.fileName }}]"
                val width = listOfNotNull(
                        block.name.length,
                        block.description.length,
                        expiry.length,
                        dependencies.length,
                        "${block.quantityAvailable}".length,
                        block.useInterval?.let { "$it".length }
                ).max()!! + 5
                val top = (1..width).joinToString("") { "*" }
                val padLength = width - "* Agent.........".length
                val installScript = block.installScripts.joinToString("") {
                    it.code.trim().nullIfEmpty()?.let {
                        "\n\n***** INSTALL SCRIPT *****\n$it"
                    } ?: ""
                }?: ""
                val removalScript = (block.removalScript?.code?.trim()?.nullIfEmpty())?.let {
                    "\n\n***** REMOVAL SCRIPT *****\n$it"
                } ?: ""
                val eventScripts = (block.eventScripts).mapNotNull { script ->
                    script.code.nullIfEmpty()?.let { code ->
                        "\n\n*** ${script.scriptName} ***$code"
                    }
                }.joinToString("")
                """
                $top
                * Agent.........${pad(block.name, padLength)}*
                * Description...${pad(block.description, padLength)}*
                * Expiry........${pad(expiry, padLength)}*
                * Dependencies..${pad(dependencies, padLength)}*
                * Quantity......${pad("${block.quantityAvailable}", padLength)}*
                * Use Interval..${pad("${block.useInterval}", padLength)}*
                $top
                $installScript$removalScript$eventScripts
            """.trimIndent()
            }
            return author + agents
        }

        private fun pad(string: String, length: Int): String {
            return string.padEnd(length, ' ')
        }

        private fun wrap(string: String, length: Int, padChar: String = "="): String {
            assert(string.length <= length - 6) {
                "Cannot wrap string when string is longer than target length. ${string.length} >= ($length - 6)"
            }
            val halfPad = floor((length - string.length - 6) / 2.0).toInt() / padChar.length
            val pad = (0 until halfPad).joinToString("") { padChar }
            return "*$pad $string $pad *"
        }

    }


}