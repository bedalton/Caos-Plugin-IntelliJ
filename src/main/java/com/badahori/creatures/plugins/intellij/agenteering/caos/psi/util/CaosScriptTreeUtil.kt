@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.intellij.psi.PsiFile
import java.util.logging.Logger


internal val LOGGER:Logger by lazy {
    Logger.getLogger("#CaosScriptTreeUtil")
}


fun PsiElement?.getChildrenOfType(iElementType: IElementType): List<PsiElement> {
    val out:MutableList<PsiElement> = mutableListOf()
    if (this == null) {
        return out
    }
    for (child in children) {
        if (child.node.elementType === iElementType) {
            out.add(child)
        }
    }
    return out
}

fun PsiElement?.getPreviousSiblingOfType(siblingElementType: IElementType): PsiElement? {
    var element:PsiElement? = this ?: return null
    while (element?.prevSibling != null) {
        element = element.prevSibling
        if (element.hasElementType(siblingElementType)) {
            return element
        }
    }
    return null
}

fun PsiElement?.getNextSiblingOfType(siblingElementType: IElementType): PsiElement? {
    var element:PsiElement? = this ?: return null
    while (element?.nextSibling != null) {
        element = element.nextSibling
        if (element.hasElementType(siblingElementType)) {
            return element
        }
    }
    return null
}

fun <PsiT:PsiElement> PsiElement?.getNextSiblingOfType(siblingClass: Class<PsiT>): PsiElement? {
    var element:PsiElement? = this ?: return null
    while (element?.nextSibling != null) {
        element = element.nextSibling
        if (siblingClass.isInstance(element)) {
            return element
        }
    }
    return null
}

private fun PsiElement?.hasElementType(elementType: IElementType): Boolean {
    return this?.node?.elementType === elementType
}

fun PsiElement.getNextNodeType(): IElementType? {
    return getNextNode()?.elementType
}

fun PsiElement.getNextNode(): ASTNode? {
    return node.treeNext
}

fun PsiElement.getNextNonEmptyNodeType(ignoreLineTerminator: Boolean): IElementType? {
    val next = getNextNonEmptyNode(ignoreLineTerminator)
    return next?.elementType
}

fun ASTNode.getNextNonEmptyNodeIgnoringComments(): ASTNode? {
    var node = this.getNextNonEmptyNode(true)
    while (node != null && (node.text.isBlank() || node.elementType in CaosScriptTokenSets.COMMENTS)) {
        node = node.getNextNonEmptyNode(true)
    }
    return node
}


fun PsiElement.getPreviousNonEmptySibling(ignoreLineTerminator: Boolean): PsiElement? {
    val node = getPreviousNonEmptyNode(ignoreLineTerminator)
    return node?.psi
}

fun ASTNode.getPreviousNonEmptyNodeIgnoringComments(): ASTNode? {
    var node = this.getPreviousNonEmptyNode(true)
    while (node != null && (node.text.trim().isEmpty() || node.elementType in CaosScriptTokenSets.COMMENTS)) {
        node = node.getPreviousNonEmptyNode(true)
    }
    return node
}

fun ASTNode?.getPreviousPossiblyEmptySibling(): ASTNode? {
    return this?.treePrev ?: getPrevInTreeParent(this)
}

fun ASTNode?.getPreviousNonEmptyNode(ignoreLineTerminator: Boolean): ASTNode? {
    var out: ASTNode = this?.previous ?: return null
    while (shouldSkipNode(out, ignoreLineTerminator)) {
        out = out.previous ?: return null
    }
    return out
}

val ASTNode.previous: ASTNode? get(){
    return this.treePrev ?: getPrevInTreeParent(this)
}

val ASTNode.next: ASTNode? get() {
    return this.treeNext ?: getNextInTreeParent(this)
}


val PsiElement.previous: PsiElement? get(){
    return this.node.treePrev?.psi ?: getPrevInTreeParent(this.node)?.psi
}

val PsiElement.next: PsiElement? get() {
    return this.node.treeNext?.psi ?: getNextInTreeParent(this.node)?.psi
}

private fun getPrevInTreeParent(out:ASTNode?): ASTNode? {
    var temp:ASTNode? = out?.treeParent ?: return null
    while (temp != null && temp.treePrev == null && temp.treeParent != null) {
        temp = temp.treeParent
    }
    return temp?.treePrev
}



fun ASTNode?.getNextPossiblyEmptySibling(): ASTNode? {
    return this?.treeNext ?: getNextInTreeParent(this)
}

fun ASTNode.getNextNonEmptySiblingIgnoringComments(): ASTNode? {
    var node = this.getNextNonEmptyNode(true)
    while (node != null && (node.text.trim().isEmpty() || node.elementType in CaosScriptTokenSets.COMMENTS)) {
        node = node.getNextNonEmptyNode(true)
    }
    return node
}

fun ASTNode?.getNextNonEmptyNode(ignoreLineTerminator: Boolean): ASTNode? {
    var out: ASTNode? = this?.next ?: return null
    while (out != null && shouldSkipNode(out, ignoreLineTerminator)) {
        out = out.next ?: return null
    }
    return out
}

private fun getNextInTreeParent(out:ASTNode?): ASTNode? {
    var temp:ASTNode? = out?.treeParent ?: return null
    while (temp != null && temp.treeNext == null && temp.treeParent != null) {
        temp = temp.treeParent
    }
    return temp?.treeNext
}

fun PsiElement.getNextNonEmptySibling(ignoreLineTerminator: Boolean): PsiElement? {
    val node = getNextNonEmptyNode(ignoreLineTerminator)
    return node?.psi
}

fun PsiElement.getNextNonEmptySiblingIgnoringComments(): PsiElement? {
    val node = node.getNextNonEmptyNodeIgnoringComments()
    return node?.psi
}

fun PsiElement?.getPreviousNonEmptyNode(ignoreLineTerminator: Boolean): ASTNode? {
    return this?.node?.getPreviousNonEmptyNode(ignoreLineTerminator)
}

fun PsiElement?.getNextNonEmptyNode(ignoreLineTerminator: Boolean): ASTNode? {
    var out: ASTNode = this?.node?.next ?: return null
    while (shouldSkipNode(out, ignoreLineTerminator)) {
        out = out.next ?: return null
    }
    return out
}

fun PsiElement.distanceFromStartOfLine() : Int? {
    val document = this.document?: return null
    return distanceFromStartOfLine(document)
}

fun PsiElement.distanceFromStartOfLine(editor:Editor) : Int {
    return this.distanceFromStartOfLine(editor.document)
}

fun PsiElement.distanceFromStartOfLine(document:Document) : Int {
    val elementStartOffset = this.textRange.startOffset
    val elementLineNumber = document.getLineNumber(elementStartOffset)
    val elementLineStartOffset = document.getLineStartOffset(elementLineNumber)
    //val elementLineStartOffset = StringUtil.lastIndexOf(document.text, '\n', 0, elementStartOffset)
    return elementStartOffset - elementLineStartOffset
}

val PsiElement.lineNumber:Int? get() {
    val elementStartOffset = this.textRange.startOffset
    return document?.getLineNumber(elementStartOffset)
}

private val isWhitespaceAtAll:List<IElementType> = listOf(
        TokenType.WHITE_SPACE,
        CaosScriptTypes.CaosScript_SPACE_LIKE,
        CaosScriptTypes.CaosScript_SPACE_LIKE_OR_NEWLINE,
        CaosScriptTypes.CaosScript_SPACE_,
        CaosScriptTypes.CaosScript_SPACE,
        CaosScriptTypes.CaosScript_NEWLINE,
        CaosScriptTypes.CaosScript_NEW_LINE,
        CaosScriptTypes.CaosScript_NEW_LINE_LIKE
)

private val newLineElementType:List<IElementType> = listOf(
        CaosScriptTypes.CaosScript_NEWLINE,
        CaosScriptTypes.CaosScript_NEW_LINE
)

fun ASTNode.isDirectlyPrecededByNewline(): Boolean {
    var node: ASTNode? = previous
    while (node != null) {
        val elementType = node.elementType
        if (elementType !in isWhitespaceAtAll)
            return false
        if (elementType in newLineElementType) {
            return true
        } else if (elementType == CaosScriptTypes.CaosScript_SPACE_LIKE_OR_NEWLINE) {
            if (node.text.contains("\n"))
                return true
        }
        node = node.previous
    }
    return false
}

fun ASTNode.getPrevSiblingOnTheSameLineSkipCommentsAndWhitespace(): ASTNode? {
    var node: ASTNode? = this.treePrev ?: getPrevInTreeParent(this)
    while (node != null) {
        return if (node.elementType == TokenType.WHITE_SPACE || CaosScriptTokenSets.COMMENTS.contains(node.elementType)) {
            if (node.text.contains("\n")) {
                null
            } else {
                node = if (node.treePrev != null) node.treePrev else getPrevInTreeParent(node)
                continue
            }
        } else node
    }

    return null
}

fun <PsiT: PsiElement> PsiElement?.thisOrParentAs(psiClass:Class<PsiT>) : PsiT? {
    return if (psiClass.isInstance(this)) {
        psiClass.cast(this)
    } else {
        this?.getParentOfType(psiClass)
    }
}

fun <PsiT : PsiElement> PsiElement?.hasSharedContextOfTypeStrict(psiElement2: PsiElement?, sharedClass: Class<PsiT>): Boolean {
    return this?.getSharedContextOfType(psiElement2, sharedClass) != null
}

fun <PsiT : PsiElement> PsiElement?.getSharedContextOfType(psiElement2: PsiElement?, sharedClass: Class<PsiT>): PsiT? {
    if (this == null || psiElement2 == null) {
        return null
    }
    val sharedContext = PsiTreeUtil.findCommonContext(this, psiElement2) ?: return null
    return if (sharedClass.isInstance(sharedContext)) {
        sharedClass.cast(sharedContext)
    } else PsiTreeUtil.getParentOfType(sharedContext, sharedClass)
}

fun <PsiT : PsiElement> PsiElement?.siblingOfTypeOccursAtLeastOnceBefore(siblingElementClass: Class<PsiT>): Boolean {
    var psiElement: PsiElement? = this?.prevSibling ?: return false
    while (psiElement != null) {
        if (siblingElementClass.isInstance(psiElement)) {
            return true
        }
        psiElement = psiElement.prevSibling
    }
    return false
}

fun <StubT : StubElement<*>> filterStubChildren(parent: StubElement<PsiElement>?, stubClass: Class<StubT>): List<StubT> {
    return if (parent == null) {
        emptyList()
    } else filterStubChildren(parent.childrenStubs, stubClass)
}

fun <StubT : StubElement<*>> filterStubChildren(children: List<StubElement<*>>?, stubClass: Class<StubT>): List<StubT> {
    return children?.mapNotNull{ child ->
        if (stubClass.isInstance(child))
            stubClass.cast(child)
        else
            null
    }.orEmpty()

}


internal fun shouldSkipNode(out: ASTNode?, ignoreLineTerminator: Boolean): Boolean {
    if (out == null) {
        return false
    }
    return if (ignoreLineTerminator && out.elementType === CaosScriptTypes.CaosScript_NEWLINE)
        true
    else if (out.elementType == TokenType.WHITE_SPACE)
        true
    else {
        out.text.let {
            if (!ignoreLineTerminator && it.contains("\n"))
                false
            else
                it.isBlank()
        }
    }
}


fun <PsiT:PsiElement> List<PsiFile>.collectElementsOfType(type:Class<PsiT>) : List<PsiT> {
    return flatMap { file ->
        PsiTreeUtil.collectElementsOfType(file, type)
    }
}