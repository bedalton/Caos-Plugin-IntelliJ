package com.badahori.creatures.plugins.intellij.agenteering.bundles.agents.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.utils.charAt
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.io.ByteArraySequence
import com.intellij.openapi.util.io.ByteSequence
import com.intellij.openapi.vfs.VirtualFile

/**
 * File Overrider to mark files as AGENT files
 */
@Suppress("UnstableApiUsage")
class AgentFileOverrider : FileTypeOverrider {

    /**
     * Checks a virtual file to see if it is a PRAY file
     */
    override fun getOverriddenFileType(virtualFile: VirtualFile): AgentFileType? {
        if (virtualFile.isDirectory)
            return null

        // Get and normalize extension if any
        val extension = virtualFile.extension
            ?.lowercase()
            ?.nullIfEmpty()
            ?: return null

        // Make sure file has valid PRAY file extension
        if (extension in AgentFileDetector.AGENT_FILE_EXTENSIONS)
            return AgentFileType

        val contents = try {
            val bytes = virtualFile.inputStream.readNBytes(4)
            ByteArraySequence(bytes)
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            return null
        }
        // If file passes pray validation, return pray file type
        return if (AgentFileDetector.isAgentFile(virtualFile, contents)) {
            AgentFileType
        } else {
            null
        }
    }
}

/**
 *
 */
class AgentFileDetector : FileTypeRegistry.FileTypeDetector {

    override fun detect(file: VirtualFile, firstBytes: ByteSequence, firstCharsIfText: CharSequence?): AgentFileType? {
        return if (isAgentFile(file, firstBytes)) {
            AgentFileType
        } else {
            null
        }
    }

    companion object {
        private const val VERSION = 1
        internal val AGENT_FILE_EXTENSIONS = listOf(
            "agent",
            "agents"
        )

        private val KNOWN_AGENT_TAGS = listOf(
            token("AGNT"),
            token("DSAG"),
            token("EGGS"),
            token("DSGB"),
            token("MEDI"),
            token("EDAG"),
            token("SKIN")
        )

        internal fun isAgentFile(virtualFile: VirtualFile, firstBytes: ByteSequence): Boolean {
            if (virtualFile.extension?.lowercase() in AGENT_FILE_EXTENSIONS) {
                return true
            }

            if (firstBytes.length() >= 4) {
                return false
            }
            val magic = token(firstBytes.charAt(0),firstBytes.charAt(1), firstBytes.charAt(2), firstBytes.charAt(3))
            return magic in KNOWN_AGENT_TAGS
        }
    }
}
