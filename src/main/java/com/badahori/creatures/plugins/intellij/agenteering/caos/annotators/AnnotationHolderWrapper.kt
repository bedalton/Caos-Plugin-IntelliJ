@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrBlank
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.ASTNode
import com.intellij.lang.annotation.AnnotationBuilder.FixBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.Contract

// Alias to allow swapping out builder implementation
internal typealias MyAnnotationBuilder = com.intellij.lang.annotation.AnnotationBuilder

///**
// * Annotation wrapper to try to unify calls in Intellij 191 and 201, where some annotation commands were deprecated
// */
//class AnnotationHolder(private val annotationHolder: AnnotationHolder) {
//
//    @Contract(pure = true)
//    fun newAnnotation(severity: HighlightSeverity, message: String): AnnotationBuilder {
//        return AnnotationBuilder(annotationHolder, severity, message)
//    }
//
//    @Contract(pure = true)
//    fun newErrorAnnotation(message: String): AnnotationBuilder {
//        return AnnotationBuilder(annotationHolder, HighlightSeverity.ERROR, message)
//    }
//
//    @Contract(pure = true)
//    fun newWarningAnnotation(message: String): AnnotationBuilder {
//        return AnnotationBuilder(annotationHolder, HighlightSeverity.WARNING, message)
//    }
//
//    @Contract(pure = true)
//    fun newWeakWarningAnnotation(message: String): AnnotationBuilder {
//        return AnnotationBuilder(annotationHolder, HighlightSeverity.WEAK_WARNING, message)
//    }
//
//    @Contract(pure = true)
//    fun newInfoAnnotation(message: String?): AnnotationBuilder {
//        return AnnotationBuilder(annotationHolder, HighlightSeverity.INFORMATION, message)
//    }
//
//    fun colorize(range: PsiElement, textAttributes: TextAttributesKey) {
////        annotationHolder.createAnnotation(HighlightSeverity.INFORMATION, range.textRange, null).apply {
////            enforcedTextAttributes = TextAttributes.ERASE_MARKER
////        }
//        val annotation = annotationHolder.createAnnotation(HighlightSeverity.INFORMATION, range.textRange, null)
//        annotation.textAttributes = textAttributes
//    }
//
//    fun colorize(range: TextRange, textAttributes: TextAttributesKey) {
//        annotationHolder.createAnnotation(HighlightSeverity.INFORMATION, range, null)
//            .enforcedTextAttributes = TextAttributes.ERASE_MARKER
//        val annotation = annotationHolder.createAnnotation(HighlightSeverity.INFORMATION, range, null)
//        annotation.textAttributes = textAttributes
//        //annotation.enforcedTextAttributes = textAttributes.defaultAttributes
//    }
//
//    fun colorize(range: ASTNode, textAttributes: TextAttributesKey) {
//        annotationHolder.createAnnotation(HighlightSeverity.INFORMATION, range.textRange, null)
//            .enforcedTextAttributes = TextAttributes.ERASE_MARKER
//        val annotation = annotationHolder.createAnnotation(HighlightSeverity.INFORMATION, range.textRange, null)
//        annotation.textAttributes = textAttributes
//        //annotation.enforcedTextAttributes = textAttributes.defaultAttributes
//    }
//}

/**
 * Annotation wrapper to try to unify calls in Intellij 191 and 201, where some annotation commands were deprecated
 */
internal data class AnnotationBuilderData(
    internal val message: String,
    internal val severity: HighlightSeverity,
    internal val range: TextRange? = null,
    internal val fixBuilderData: List<FixBuilderData> = listOf(),
    internal val fixes: List<IntentionAction> = listOf(),
    internal val enforcedTextAttributes: TextAttributes? = null,
    internal val textAttributes: TextAttributesKey? = null,
    internal val needsUpdateOnTyping: Boolean? = null,
    internal val highlightType: ProblemHighlightType? = null,
    internal val tooltip: String? = null,
    internal val afterEndOfLine: Boolean = false,
)

class AnnotationBuilder private constructor(
    private val annotationHolder: AnnotationHolder,
    internal val data: AnnotationBuilderData,
) {

    constructor(annotationHolder: AnnotationHolder, severity: HighlightSeverity, message: String?)
            : this(annotationHolder, AnnotationBuilderData(severity = severity, message = message ?: ""))

    @Contract(pure = true)
    fun range(range: TextRange): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(range = range))
    }

    @Contract(pure = true)
    fun range(element: PsiElement): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(range = element.textRange))
    }

    @Contract(pure = true)
    fun range(node: ASTNode): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(range = node.textRange))
    }

    @Contract(pure = true)
    fun afterEndOfLine(): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(afterEndOfLine = true))
    }

    @Contract(pure = true)
    fun withFix(fix: IntentionAction): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(fixes = data.fixes + fix))
    }

    @Contract(pure = true)
    private fun withFix(fix: FixBuilderData): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(fixBuilderData = data.fixBuilderData + fix))
    }

    @Contract(pure = true)
    fun withFixes(vararg fixes: IntentionAction): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(fixes = data.fixes + fixes))
    }

    @Contract(pure = true)
    fun needsUpdateOnTyping(needsUpdateOnTyping: Boolean): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(needsUpdateOnTyping = needsUpdateOnTyping))
    }

    @Contract(pure = true)
    fun needsUpdateOnTyping(): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(needsUpdateOnTyping = true))
    }

    @Contract(pure = true)
    fun highlightType(highlightType: ProblemHighlightType): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(highlightType = highlightType))
    }

    @Contract(pure = true)
    fun tooltip(tooltip: String): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(tooltip = tooltip))
    }

    @Contract(pure = true)
    fun textAttributes(textAttributes: TextAttributesKey): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(textAttributes = textAttributes))
    }

    @Contract(pure = true)
    fun enforcedTextAttributes(textAttributes: TextAttributes): AnnotationBuilder {
        return AnnotationBuilder(annotationHolder, data.copy(enforcedTextAttributes = textAttributes))
    }

    @Contract(pure = true)
    fun newFix(intentionAction: IntentionAction): FixBuilder {
        return FixBuilder._createFixBuilder(
            this,
            FixBuilderData(intentionAction = intentionAction, universal = intentionAction is LocalQuickFix)
        )
    }

    @Contract(pure = true)
    fun newLocalQuickFix(quickFix: LocalQuickFix, problemDescriptor: ProblemDescriptor): FixBuilder {
        return FixBuilder._createFixBuilder(
            this,
            FixBuilderData(quickFix = quickFix, problemDescriptor = problemDescriptor)
        )
    }

    @Contract(pure = true)
    fun create() {
        if (data.range == null)
            throw Exception("Cannot create annotation without range")
        var annotation = (data.message.nullIfEmpty()?.let {
            annotationHolder
                .newAnnotation(data.severity, it)
        } ?: annotationHolder.newSilentAnnotation(data.severity))
            .range(data.range)
        data.fixBuilderData.forEach {
            val intentionAction = it.intentionAction ?: it.quickFix as? IntentionAction
            val quickFix = it.quickFix ?: it.intentionAction as? LocalQuickFix
            val union: FixUnion? = if (quickFix != null && intentionAction != null)
                FixUnion(intentionAction = intentionAction, quickFix = quickFix)
            else
                null
            if (it.batch.orFalse()) {
                if (union != null) {
                    annotation = annotation
                        .newFix(union)
                        .withData(it)
                        .registerFix()
                } else {
                    LOGGER.severe("Batch fix set for ${data.message}, but fix is not batch compatible")
                }
            }
            if (union != null) {
                annotation.withFix(union)
            }
            if (union == null && it.batch.orFalse()) {
                annotation = (if (intentionAction != null) {
                    annotation
                        .newFix(intentionAction)
                        .withData(it)
                } else if (quickFix != null && it.problemDescriptor != null) {
                    annotation
                        .newLocalQuickFix(quickFix, it.problemDescriptor)
                        .withData(it)
                } else {
                    throw Exception("Cannot create fix without any fixes")
                }).registerFix()
            }
        }
        data.fixes.forEach {
            annotation = annotation.withFix(it)
        }

        data.tooltip?.let {
            annotation = annotation.tooltip(it)
        }
        data.enforcedTextAttributes?.let {
            annotation = annotation.enforcedTextAttributes(it)
        }
        data.textAttributes?.let {
            annotation = annotation.textAttributes(it)
        }
        data.needsUpdateOnTyping?.let {
            annotation = annotation.needsUpdateOnTyping(it)
        }
        data.highlightType?.let {
            annotation = annotation.highlightType(it)
        }
        if (data.afterEndOfLine) {
            annotation = annotation.afterEndOfLine()
        }
        annotation.create()
    }


    class FixBuilder private constructor(
        private val annotationBuilder: AnnotationBuilder,
        private val fixBuilderData: FixBuilderData,
    ) {

        @Contract(pure = true)
        fun range(range: TextRange): FixBuilder {
            return FixBuilder(annotationBuilder, fixBuilderData.copy(range = range))
        }

        @Contract(pure = true)
        fun key(key: HighlightDisplayKey): FixBuilder {
            return FixBuilder(annotationBuilder, fixBuilderData.copy(key = key))
        }

        @Contract(pure = true)
        fun batch(): FixBuilder {
            return FixBuilder(annotationBuilder, fixBuilderData.copy(batch = true))
        }

        @Contract(pure = true)
        fun registerFix(): AnnotationBuilder {
            return annotationBuilder.withFix(fixBuilderData)
        }

        companion object {
            @Suppress("FunctionName")
            internal fun _createFixBuilder(
                annotationBuilder: AnnotationBuilder,
                fixBuilderData: FixBuilderData,
            ): FixBuilder {
                return FixBuilder(annotationBuilder, fixBuilderData)
            }
        }
    }
}

internal data class FixBuilderData(
    internal val quickFix: LocalQuickFix? = null,
    internal val intentionAction: IntentionAction? = null,
    internal val range: TextRange? = null,
    internal val key: HighlightDisplayKey? = null,
    internal val universal: Boolean? = null,
    internal val batch: Boolean? = null,
    internal val problemDescriptor: ProblemDescriptor? = null,

    )

class FixUnion(val quickFix: LocalQuickFix, val intentionAction: IntentionAction) : IntentionAction by intentionAction,
    LocalQuickFix by quickFix {

    override fun getText(): String = intentionAction.text

    override fun getFamilyName(): String = quickFix.familyName

    override fun startInWriteAction(): Boolean = intentionAction.startInWriteAction() || quickFix.startInWriteAction()

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        intentionAction.isAvailable(project, editor, file)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) = quickFix.applyFix(project, descriptor)

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) =
        intentionAction.invoke(project, editor, file)


}

/**
 * Clears formatting in a given range
 */
fun AnnotationHolder.clearTextAttributes(range: TextRange) {
    newSilentAnnotation(HighlightInfoType.SYMBOL_TYPE_SEVERITY)
        .range(range)
        .enforcedTextAttributes(TextAttributes.ERASE_MARKER)
        .create()
}

/**
 * Colorizes/Styles a AST node
 */
fun AnnotationHolder.colorize(node: ASTNode, textAttributes: TextAttributesKey, recursive: Boolean = true) {
    if (recursive) {
        var child: ASTNode? = node.firstChildNode
        while (child != null) {
            colorize(child, textAttributes, true)
            child = child.treeNext
        }
    }

    // Actually colorize
    newSilentAnnotation(HighlightInfoType.SYMBOL_TYPE_SEVERITY)
        .range(node)
//        .enforcedTextAttributes(TextAttributes.ERASE_MARKER)
        .textAttributes(textAttributes)
        .create()
}

/**
 * Colorizes/Styles a psi element
 */
fun AnnotationHolder.colorize(element: PsiElement, textAttributes: TextAttributesKey, recursive: Boolean = true) {
    if (recursive) {
        element.children.forEach { child ->
            colorize(child, textAttributes, true)
        }
    }
    clearTextAttributes(element.textRange)

    // Actually colorize
    newSilentAnnotation(HighlightInfoType.SYMBOL_TYPE_SEVERITY)
        .range(element)
//        .enforcedTextAttributes(TextAttributes.ERASE_MARKER)
        .textAttributes(textAttributes)
        .create()
}

/**
 * Colorizes/Styles a text range within a file
 */
fun AnnotationHolder.colorize(range: TextRange, textAttributes: TextAttributesKey) {
    // Remove prior formatting
    clearTextAttributes(range)

    // Actually colorize
    newSilentAnnotation(HighlightInfoType.SYMBOL_TYPE_SEVERITY)
        .range(range)
//        .enforcedTextAttributes(TextAttributes.ERASE_MARKER)
        .textAttributes(textAttributes)
        .create()
}

//
//@Contract(pure = true)
//fun AnnotationHolder.newErrorAnnotation(message: String): MyAnnotationBuilder {
//    return AnnotationBuilder(this, HighlightSeverity.ERROR, message)
//}
//
//@Contract(pure = true)
//fun AnnotationHolder.newWarningAnnotation(message: String): MyAnnotationBuilder {
//    return AnnotationBuilder(this, HighlightSeverity.WARNING, message)
//}
//
//@Contract(pure = true)
//fun AnnotationHolder.newWeakWarningAnnotation(message: String): MyAnnotationBuilder {
//    return AnnotationBuilder(this, HighlightSeverity.WEAK_WARNING, message)
//}
//
//@Contract(pure = true)
//fun AnnotationHolder.newInfoAnnotation(message: String?): MyAnnotationBuilder {
//    return AnnotationBuilder(this, HighlightSeverity.INFORMATION, message)
//}
//
//@Contract(pure = true)
//fun AnnotationHolder.newInfoAnnotation(): MyAnnotationBuilder {
//    return AnnotationBuilder(this, HighlightSeverity.INFORMATION, null)
//}

@Contract(pure = true)
fun AnnotationHolder.newErrorAnnotation(message: String): MyAnnotationBuilder {
    return newAnnotation(
        HighlightSeverity.ERROR,
        message
    )
}

@Contract(pure = true)
fun AnnotationHolder.newWarningAnnotation(message: String): MyAnnotationBuilder {
    return newAnnotation(
        HighlightSeverity.WARNING,
        message
    )
}

@Contract(pure = true)
fun AnnotationHolder.newWeakWarningAnnotation(message: String): MyAnnotationBuilder {
    return newAnnotation(
        HighlightSeverity.WARNING,
        message
    )
}

@Contract(pure = true)
fun AnnotationHolder.newInfoAnnotation(message: String?): MyAnnotationBuilder {
    return if (message.isNotNullOrBlank()) {
        newAnnotation(
            HighlightSeverity.INFORMATION,
            message
        )
    } else {
        newSilentAnnotation(
            HighlightSeverity.INFORMATION
        )
    }
}
private fun FixBuilder.withData(data: FixBuilderData): FixBuilder {
    return if (data.range != null) {
        if (data.key != null) {
            this.range(data.range)
                .key(data.key)
        } else {
            this.range(data.range)
        }
    } else if (data.key != null) {
        this.key(data.key)
    } else {
        this
    }
}

@Contract(pure = true)
internal fun com.intellij.lang.annotation.AnnotationBuilder.withFixes(vararg fixes: IntentionAction): MyAnnotationBuilder {
    var annotation = this
    for (fix in fixes) {
        annotation = annotation
            .newFix(fix)
            .registerFix()
    }
    return annotation
}