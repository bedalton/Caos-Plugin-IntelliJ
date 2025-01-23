// This is a generated file. Not intended for manual editing.
package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CobTagFormat
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C2
import com.badahori.creatures.plugins.intellij.agenteering.utils.Case
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE_OR_DASH
import com.badahori.creatures.plugins.intellij.agenteering.utils.matchCase

interface CaosScriptCaos2Statement : CaosScriptCompositeElement


enum class CobTag(vararg val keys: String, val required:Boolean = false, val variant: CaosVariant? = null, val format: CobTagFormat = CobTagFormat.STRING) {
    AGENT_NAME("COB Name", "C1 Name", "C2 Name", "C1Name", "C2Name","Agent-Name", required = true),
    COB_NAME("COB File", "Cob File Name", "COB Name", "COB", required = true),
    QUANTITY_AVAILABLE("Quantity Available", "Quantity", "Qty", "Qty Available", format = CobTagFormat.NUMBER),
    THUMBNAIL("Thumbnail", "Image", "Picture", "Preview", required = true, format = CobTagFormat.SINGLE_IMAGE),
    EXPIRY("Expiry Date", "Expiry", "Expires", "Expires Date", "Expires On", format = CobTagFormat.DATE),

    //C1
    REMOVER_NAME("Remover Name", "Remover", variant = C1),
    QUANTITY_USED("Quantity Used", "Qty Used", "Used", variant = C1, format = CobTagFormat.NUMBER),

    // C2
    DESCRIPTION("Agent Description", "Description", "Desc", "Agent Desc", variant = C2),
    LAST_USAGE_DATE("Last Usage Date", "Last Usage", variant = C2, format = CobTagFormat.DATE),
    REUSE_INTERVAL("Reuse Interval", "Interval", variant = C2, format = CobTagFormat.NUMBER),

    // C2 Author
    CREATION_DATE("Creation Date", "Created", "Created Date", variant = C2, format = CobTagFormat.DATE),
    AUTHOR_NAME("Author Name", "Author", "Creator", "Creator Name", variant = C2),
    AUTHOR_EMAIL("Author Email", "Email", "Creator Email", variant = C2, format = CobTagFormat.EMAIL),
    AUTHOR_URL("Author URL", "URL", "Website", "Site", "Author Site", "Author Website", variant = C2, format = CobTagFormat.URL),
    VERSION("Version", "V", "Ver", variant = C2, format = CobTagFormat.NUMBER),
    REVISION("Revision", "Rev", variant = C2, format = CobTagFormat.NUMBER),
    AUTHOR_COMMENTS("Comments", "Comment", "Author Comments", "Author Comment", variant = C2)
    ;

    private val keysRegex = ("^(" + keys.joinToString("|") {
        it.replace(WHITESPACE, "\\\\s*").trim()+ "|" + it.replace(WHITESPACE, "").trim() +"|"+it.replace(WHITESPACE, "-").trim()
    } + ")$").trim().toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    fun isVariant(variant:CaosVariant) : Boolean {
        return this.variant == variant
    }

    internal fun isTag(value: String): Boolean {
        return keysRegex.matches(value.trim())
    }

    @Suppress("unused")
    companion object {
        private val C1_TAGS:List<CobTag> by lazy {
            values().filter { it.variant == null || it.variant == C1 }
        }

        private val C2_TAGS:List<CobTag> by lazy {
            values().filter { it.variant == null || it.variant == C2 }
        }

        fun getTags(variant:CaosVariant? = null):List<CobTag> {
           return when (variant) {
                C1 -> C1_TAGS
                C2 -> C2_TAGS
                null -> values().toList()
                else -> emptyList()
            }
        }

        fun allKeys(variant:CaosVariant? = null): List<String> {
            return getTags(variant).flatMap { it.keys.toList() }
        }

        fun primaryKeys(variant:CaosVariant? = null): Set<String> {
            return getTags(variant).map { it.keys.first() }.toSet()
        }

        fun fromString(keyIn:String, variant:CaosVariant? = null):CobTag? {
            val key = keyIn.replace(WHITESPACE_OR_DASH, " ")
            return getTags(variant).firstOrNull {
                it.isTag(key)
            }
        }
    }
}
enum class CobCommand(val keyStrings: Array<String>, val cosFiles:Boolean, val variant: CaosVariant? = null, val singleton: Boolean) {
    LINK(arrayOf("Link"), true, singleton = false),
    COB_FILE(arrayOf("Cob-File", "CobFile"), false, singleton = true),
    INSTALL_SCRIPTS(arrayOf("Iscr", "InstallScript", "Install-Script"), true, singleton = true),
    REMOVAL_SCRIPTS(arrayOf("Rscr", "Remove Script", "Remover Script", "Removal Script", "Remover"), true, singleton = true),
    ATTACH(arrayOf("Attach"), false, variant = C2, singleton = false),
    INLINE(arrayOf("Inline"), false, variant = C2, singleton = false),
    DEPEND(arrayOf("Depend"), false, variant = C2, singleton = false)
    ;
    val key by lazy { "(${keyStrings.joinToString("|") { "(^$it$)" }})".toRegex(RegexOption.IGNORE_CASE) }

    companion object {
        private val C1_COMMANDS: List<CobCommand> by lazy {
            values().filter { it.variant == null || it.variant == C1 }
        }

        private val C2_COMMANDS: List<CobCommand> by lazy {
            values().filter { it.variant == null || it.variant == C2 }
        }

        fun getCommands(variant: CaosVariant?): List<CobCommand> {
            return when (variant) {
                C1 -> C1_COMMANDS
                C2 -> C2_COMMANDS
                null -> values().toList()
                else -> emptyList()
            }
        }

        fun fromString(keyIn: String, variant: CaosVariant? = null): CobCommand? {
            val key = keyIn.trim()
            return getCommands(variant).firstOrNull {
                it.key.matches(key)
            }
        }
    }
}
enum class PrayCommand(val keyStrings: Array<String>, val cosFiles:Boolean, val singleton: Boolean) {
    PRAY_FILE(arrayOf("Pray-File", "PrayFile", "Pray File"), false, true),
    REMOVAL_SCRIPTS(arrayOf("Rscr", "Remove Script", "Remover Script", "Removal Script", "Remover"), true, true),
    ATTACH(arrayOf("Attach"), false, false),
    INLINE(arrayOf("Inline"), false, false),
    DEPEND(arrayOf("Depend"), false, false),
    LINK(arrayOf("Link"), true, false),
    JOIN(arrayOf("Join"), true, false)
    ;
    val key = "(${keyStrings.joinToString("|") { "(^$it$)" }})".toRegex(RegexOption.IGNORE_CASE)
    companion object {

        fun getCommands(): List<PrayCommand> {
            return values().toList()
        }

        fun getActionCommands(): List<String> {
            return PrayCommand.getCommands().mapNotNull {
                if (it.singleton) {
                    null
                } else {
                    it.name.lowercase().matchCase(Case.CAPITAL_FIRST)
                }
            }
        }

        fun fromString(keyIn: String, variant: CaosVariant? = null): PrayCommand? {
            if (variant?.isOld == true)
                return null
            val key = keyIn.trim()
            return getCommands().firstOrNull {
                it.key.matches(key)
            }
        }
    }
}