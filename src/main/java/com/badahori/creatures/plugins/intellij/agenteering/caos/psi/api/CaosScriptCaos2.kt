// This is a generated file. Not intended for manual editing.
package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C2
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.NUMBER_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE_OR_DASH
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile

interface CaosScriptCaos2 : CaosScriptCompositeElement

enum class CobTag(vararg val keys: String, val required:Boolean = false, val variant: CaosVariant? = null) {
    AGENT_NAME("Agent Name", "Agent", "C1Name", "C1-Name", "C2Name", "C2-Name", required = true),
    COB_NAME("COB File", "Cob File Name", "COB Name", "COB", required = true),
    QUANTITY_AVAILABLE("Quantity Available", "Quantity", "Qty", "Qty Available"),
    THUMBNAIL("Thumbnail", "Image", "Picture", "Preview"),
    EXPIRY("Expiry Date", "Expiry", "Expires", "Expires Date", "Expires On"),

    //C1
    REMOVER_NAME("Remover Name", "Remover", variant = C1),
    QUANTITY_USED("Quantity Used", "Qty Used", "Used", variant = C1),

    // C2
    DESCRIPTION("Agent Description", "Description", "Desc", "Agent Desc", variant = C2),
    LAST_USAGE_DATE("Last Usage Date", "Last Usage", variant = C2),
    REUSE_INTERVAL("Reuse Interval", "Interval", variant = C2),

    // C2 Author
    CREATION_DATE("Creation Date", "Created", "Created Date", variant = C2),
    AUTHOR_NAME("Author Name", "Author", "Creator", "Creator Name", variant = C2),
    AUTHOR_EMAIL("Author Email", "Email", "Creator Email", variant = C2),
    AUTHOR_URL("Author URL", "URL", "Website", "Site", "Author Site", "Author Website", variant = C2),
    VERSION("Version", "V", "Ver", variant = C2),
    REVISION("Revision", "Rev", variant = C2),
    AUTHOR_COMMENTS("Comments", "Comment", "Author Comments", "Author Comment", variant = C2)
    ;

    private val keysRegex = ("^(" + keys.joinToString("|") {
        it.replace(WHITESPACE, "\\\\s*") +"|"+it.replace(WHITESPACE, "-")
    } + ")$").toRegex(RegexOption.IGNORE_CASE)

    fun isVariant(variant:CaosVariant) : Boolean {
        return this.variant == variant
    }

    internal fun isTag(value: String): Boolean {
        return value.matches(keysRegex)
    }

    companion object {
        val C1_TAGS:List<CobTag> by lazy {
            values().filter { it.variant == null || it.variant == C1 }
        }

        val C2_TAGS:List<CobTag> by lazy {
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
enum class CobCommand(val keyString: String, val cosFiles:Boolean, val variant: CaosVariant? = null) {
    LINK("Link", true),
    INSTALL_SCRIPTS("Iscr", true),
    REMOVAL_SCRIPTS("Rscr", true),
    ATTACH("Attach", false, variant = C2),
    INLINE("Inline", false, variant = C2),
    DEPEND("Depend", false, variant = C2)
    ;
    val key = "^$keyString$".toRegex(RegexOption.IGNORE_CASE)

    companion object {
        val C1_COMMANDS: List<CobCommand> by lazy {
            values().filter { it.variant == null || it.variant == C1 }
        }

        val C2_COMMANDS: List<CobCommand> by lazy {
            values().filter { it.variant == null || it.variant == C2 }
        }

        fun getCommands(variant: CaosVariant? = null): List<CobCommand> {
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