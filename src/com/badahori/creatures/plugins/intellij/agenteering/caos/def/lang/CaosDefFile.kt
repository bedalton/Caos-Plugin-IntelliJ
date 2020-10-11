package com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefHeader
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefFileStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariants

class CaosDefFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, CaosDefLanguage.instance), HasVariants {

    override fun getFileType(): FileType {
        return CaosScriptFileType.INSTANCE
    }

    override fun getStub(): CaosDefFileStub? {
        return super.getStub() as? CaosDefFileStub
    }

    override val variants: List<CaosVariant>
        get() {
            var variants = stub?.variants
            if (variants == null)
                variants = PsiTreeUtil.findChildOfType(this, CaosDefHeader::class.java)?.variants
            return variants ?: emptyList()
        }

    fun <PsiT : PsiElement> getChildOfType(childClass: Class<PsiT>): PsiT? =
            PsiTreeUtil.getChildOfType(this, childClass)


    fun <PsiT : PsiElement> getChildrenOfType(childClass: Class<PsiT>): List<PsiT> =
            PsiTreeUtil.getChildrenOfTypeAsList(this, childClass)

    override fun toString(): String {
        return "Caos Def"
    }
}
