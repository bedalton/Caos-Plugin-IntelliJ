package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayLanguage
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayString
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil

internal object PrayPsiElementFactory {

    private fun createFileFromText(project: Project, text: String, fileName: String = "dummy.ps"): PrayFile? {
        return (PsiFileFactory.getInstance(project).createFileFromText(fileName, PrayLanguage, text) as? PrayFile)
    }

    internal fun createQuoteString(project: Project, text: String): PrayString? {
        val script = """
            "en-GB"
            group block_tag "$text"
        """
        return createAndGet(project, script, PrayString::class.java)
    }

    private fun <PsiT : PsiElement> createAndGet(project: Project, script: String, type: Class<PsiT>): PsiT? {
        val file = createFileFromText(project, script)
            ?: return null
        val pointer = SmartPointerManager.createPointer(file)
        return PsiTreeUtil.collectElementsOfType(pointer.element, type).first()
    }
}