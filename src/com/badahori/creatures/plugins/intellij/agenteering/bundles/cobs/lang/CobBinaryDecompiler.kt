package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.lang

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobDecompiler
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobFileData
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.vfs.VirtualFile
import java.nio.ByteBuffer
import java.util.*

class CobBinaryDecompiler : BinaryFileDecompiler {
    override fun decompile(virtualFile: VirtualFile): CharSequence {
        return decompileToString(virtualFile.name, virtualFile.contentsToByteArray())
    }
    companion object {

        internal fun decompileToString(fileName:String, byteArray:ByteArray) : String {
            val cobData = CobDecompiler.decompile(ByteBuffer.wrap(byteArray))
            return presentCobData(fileName, cobData)
        }

        internal fun presentCobData(fileName: String, cobData:CobFileData) : String{
            return if (cobData is CobFileData.C1CobData) {
                presentC1CobData(fileName, cobData)
            } else if (cobData is CobFileData.C2CobData) {
                presentC2CobData(fileName, cobData)
            } else
                "**** INVALID COB FORMAT ****"
        }

        private fun presentC1CobData(fileName:String, cobData:CobFileData.C1CobData) : String {

            val agentBlock = cobData.cobBlock
            val installScript = (agentBlock.installScript.code.nullIfEmpty()?.let { "\n\n* ===== Install Script ===== *\n$it" } ?: "")
            val eventScripts = agentBlock.eventScripts.joinToString("\n\n") {
                "**** ${it.scriptName} ****\n" + it.code
            }
            val header = "COB: $fileName"
            val agentName = "Agent: ${agentBlock.name}"
            val expiry = "Expiry:  + ${agentBlock.expiry.let { "${it.get(Calendar.MONTH)}/${it.get(Calendar.DAY_OF_MONTH)}/${it.get(Calendar.YEAR)}"}}"
            val quantity = "Quantity: ${agentBlock.quantityAvailable}"
            val width = listOf(header, agentName, expiry, quantity).map { it.length + 2 }.max()!! + 5
            val top = (1 .. width).joinToString("") { "*" }
            val offset = 3 // leading asterisk, space and trailing asterisk
            return """
            $top
            * ${ pad(header, width - offset)}*
            * ${pad(agentName, width - offset)}*
            * ${pad(expiry, width - offset)}*
            * ${pad(quantity, width - 3)}*
            $top
            $installScript
            $eventScripts
        """.trimIndent()
        }

        private fun presentC2CobData(fileName: String, cobData: CobFileData.C2CobData) : String {
            val header = "COB: $fileName"
            val author = cobData.authorBlocks.firstOrNull()?.let {
                """
                * Author...........${it.authorName}
                * Author Email.....${it.authorEmail}
                * Author URL.......${it.authorUrl}
                * Author Comments..${it.authorComments}
            """.trimIndent()
            }
            val agents = cobData.agentBlocks.joinToString("\n\n") {
                val expiry = it.expiry.let { "${it.get(Calendar.MONTH)}/${it.get(Calendar.DAY_OF_MONTH)}/${it.get(Calendar.YEAR)}"}
                val dependencies = "[${it.dependencies.joinToString { it.fileName }}]"
                val width = listOfNotNull(
                        it.name.length,
                        it.description.length,
                        expiry.length,
                        dependencies.length,
                        "${it.quantityAvailable}".length,
                        it.useInterval?.let { "$it".length }
                ).max()!! + 5
                val top = (1..width).joinToString("") { "*" }
                val padLength = width - "* Agent.........".length
                val installScript = (it.installScript.code.trim().nullIfEmpty())?.let {
                    "\n\n***** INSTALL SCRIPT *****\n$it"
                } ?: ""
                val removalScript = (it.removalScript?.code?.trim()?.nullIfEmpty())?.let {
                    "\n\n***** REMOVAL SCRIPT *****\n$it"
                } ?: ""
                val eventScripts = (it.eventScripts).mapNotNull {script ->
                    script.code.nullIfEmpty()?.let { code ->
                        "\n\n*** ${script.scriptName} ***$code"
                    }
                }.joinToString("")
                """
                $top
                * Agent.........${pad(it.name, padLength)}*
                * Description...${pad(it.description, padLength)}*
                * Expiry........${pad(expiry, padLength)}*
                * Dependencies..${pad(dependencies, padLength)}*
                * Quantity......${pad("${it.quantityAvailable}", padLength)}*
                * Use Interval..${pad("${it.useInterval}", padLength)}*
                $top
                $installScript$removalScript$eventScripts
            """.trimIndent()
            }
            return author + agents
        }

        private fun pad(string:String, length:Int) : String {
            return string.padEnd(length, ' ')
        }
    }


}