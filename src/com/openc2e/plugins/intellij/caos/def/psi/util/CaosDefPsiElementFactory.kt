package com.openc2e.plugins.intellij.caos.def.psi.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.caos.def.psi.api.*

object CaosDefPsiElementFactory {

    fun getVariableLinkElement(project: Project, variableName:String) : CaosDefVariableLink {
        val comment = """
            /*
             * @param {$variableName} (null)
             */
        """.trimIndent()
        return createComment(project, comment).docCommentParamList.first().variableLink!!
    }

    fun getVariableNameElement(project:Project, variableName: String) : CaosDefVariableName {
        val file= createFileFromText(project, "TEMP (null) $variableName (null)");
        val command =  file.firstChild as CaosDefCommandDefElement
        return command.parameterList.first().variableName
    }

    private fun createFileFromText(project: Project, text: String): CaosDefFile {
        return PsiFileFactory.getInstance(project).createFileFromText("dummy.caosdef", CaosDefLanguage.instance, text) as CaosDefFile
    }

    private fun createComment(project: Project, text: String) : CaosDefDocComment {
        return (createFileFromText(project,"$text\nTEMP (null);").firstChild as CaosDefCommandDefElement).docComment!!
    }

    fun getCommandWordElement(project: Project, newNameString: String): CaosDefCommandWord {
        val file= createFileFromText(project, "$newNameString (null)");
        val command =  file.firstChild as CaosDefCommandDefElement
        return command.command.commandWordList[0]
    }

    fun getTypeDefName(project: Project, newNameString: String): CaosDefTypeDefName {
        val commentText = """
            /*
             * @param {var} (null@$newNameString)
             */
        """.trimIndent()
        val comment = createComment(project, commentText)
        return comment.docCommentParamList[0].docCommentVariableType!!.typeDefName!!
    }

}