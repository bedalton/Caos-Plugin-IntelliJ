package com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.intellij.psi.util.PsiTreeUtil

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
        val file= createFileFromText(project, "TEMP (null) $variableName (null)")
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
        val file= createFileFromText(project, "$newNameString (null)")
        val command =  file.firstChild as CaosDefCommandDefElement
        return command.command.commandWordList[0]
    }

    fun getValuesListName(project: Project, newNameString: String): CaosDefValuesListName {
        val commentText = """
            /*
             * @param {var} (null@$newNameString)
             */
        """.trimIndent()
        val comment = createComment(project, commentText)
        return comment.docCommentParamList[0].docCommentVariableType!!.valuesListName!!
    }

    private val HASHTAG_VALIDATION = "[#][A-Za-z][_A-Za-z0-9]*".toRegex()

    fun createHashTag(project: Project, newHashTag:String) : CaosDefDocCommentHashtag? {
        if (!HASHTAG_VALIDATION.matches(newHashTag))
            return null
        val commentText = """
            /*
             * #$newHashTag
             */
        """.trimIndent()
        val comment = createComment(project, commentText)
        return comment.docCommentHashtagList[0]
    }

    fun createCodeBlock(project: Project, text: String): CaosDefCodeBlock {
        val code = """
            /* #{$text} */
        """.trimIndent()
        val comment = createComment(project, code)
        return comment.docCommentFrontComment!!.docCommentLineList.first().codeBlockList.first()
    }

    fun createNewValuesListValueKey(project: Project, newNameString: String): CaosDefValuesListValueKey? {
        val code = """
            @List {
                $newNameString = A value
            }
        """.trimIndent()
        val script = createFileFromText(project, code)
        return PsiTreeUtil.findChildOfType(script, CaosDefValuesListValueKey::class.java)
    }

}