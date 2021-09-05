package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.caos.highlighting.CaosScriptSyntaxHighlighter
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class PraySyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        if (virtualFile?.fileType == CaosScriptFileType.INSTANCE)
            return CaosScriptSyntaxHighlighter()
        return PraySyntaxHighlighter()
    }
}
