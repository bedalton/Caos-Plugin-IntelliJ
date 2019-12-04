package brightscript.intellij.parser

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType


class CaosDefParser(val isDefinitionsFile:Boolean) : BrsParserBase() {
    override fun parse_root_(t: IElementType, b: PsiBuilder): Boolean {
        b.putUserData(BrsParserUtil.IS_DEFINITIONS_FILE_USERDATA_KEY, isDefinitionsFile)
        return super.parse_root_(t, b)
    }
}