package com.badahori.creatures.plugins.intellij.agenteering.caos.references
//
//import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.caos2PrayFileExtensions
//import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiFile
//import com.intellij.refactoring.rename.naming.AutomaticRenamer
//import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
//import com.intellij.usageView.UsageInfo
//
//class CaosScriptFileStringRenamerFactory: AutomaticRenamerFactory {
//    override fun isApplicable(element: PsiElement): Boolean {
//        if (element !is PsiFile) {
//            return false
//        }
//        return element.virtualFile?.extension?.lowercase() in caos2PrayFileExtensions
//    }
//
//    override fun getOptionName(): String {
//        return CaosBundle.message("caos.file-renamer-factory.option")
//    }
//
//    override fun isEnabled(): Boolean {
//        return true
//    }
//
//    override fun setEnabled(enabled: Boolean) {
//    }
//
//    override fun createRenamer(
//        element: PsiElement,
//        newName: String,
//        usages: MutableCollection<UsageInfo>?
//    ): AutomaticRenamer {
//        assert(element is PsiFile) { "Cannot rename strings without PSI file" }
//        return CaosScriptFileStringRenamer(element as PsiFile, newName)
//    }
//}