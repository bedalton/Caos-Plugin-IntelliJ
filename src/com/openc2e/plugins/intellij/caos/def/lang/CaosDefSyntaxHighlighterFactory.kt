package com.openc2e.plugins.intellij.caos.def.lang

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.openc2e.plugins.intellij.caos.lang.CaosSyntaxHighlighter

class CaosDefSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return CaosDefSyntaxHighlighter()
    }
}
