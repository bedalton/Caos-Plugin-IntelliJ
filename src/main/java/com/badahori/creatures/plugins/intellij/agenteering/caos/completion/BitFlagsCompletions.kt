package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesList
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.utils.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBScrollPane
import javax.swing.*

/**
 * Creates an action to allow selection of bitflags by checkboxes
 */
internal class GenerateBitFlagInsertHandler(
    private val valuesListName: String,
    private val valuesListValues: List<CaosValuesListValue>,
    private val prependSpace: Boolean,
    private val currentValue: Int = 0
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        val position = context.editor.caretModel.currentCaret.offset
        invokeLater {
            BitFlagsForm(
                position,
                0,
                context.editor,
                valuesListName,
                valuesListValues.sortedBy { it.value.toIntSafe() ?: 1000 },
                currentValue,
                prependSpace
            ).showAndGet()
        }
    }
}

/**
 * Intention action to insert/change bit-flag integer
 */
class GenerateBitFlagIntegerIntentionAction : PsiElementBaseIntentionAction(), LocalQuickFix {
    override fun startInWriteAction(): Boolean = false

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
            ?: return
        val editor = element.editor
            ?: return
        invoke(project, editor, element)
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return parse(element) != null
    }

    override fun getText(): String = CaosBundle.message("com.actions.generate-bit-flags-generic")

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {

        // Requires editor
        if (editor == null)
            return
        val data = parse(element)
            ?: return
        invoke(
            editor = editor,
            element = data.range,
            valuesListValues = data.values,
            valuesListName = data.listName,
            currentValue = data.currentValue ?: 0
        )
    }


    private fun invoke(
        editor: Editor,
        element: PsiElement,
        valuesListValues: List<CaosValuesListValue>,
        valuesListName: String,
        currentValue: Int
    ) {
        val position = element.startOffset
        val length = if (element.tokenType == WHITE_SPACE)
            0
        else
            element.textRange.let { it.endOffset - it.startOffset }
        BitFlagsForm(
            position,
            length,
            editor,
            valuesListName,
            valuesListValues,
            currentValue,
            element.previous?.tokenType != WHITE_SPACE
        ).showAndGet()
    }
}

/**
 * Creates a simple form of bitflag checkboxes
 */
private class BitFlagsForm(
    val position: Int,
    val consume: Int,
    private val editor: Editor,
    private val valuesListName: String,
    private val valuesListValues: List<CaosValuesListValue>,
    private val currentValue: Int = 0,
    private val prependSpace: Boolean
) : DialogWrapper(editor.project, true) {

    // Hold a list of checkboxes paired with their bit-flag values
    private lateinit var checkboxes: List<BitFlagSelection>

    init {
        title = "$valuesListName bit-flags builder"
        super.init()
    }

    override fun createCenterPanel(): JComponent {
        val out = JBScrollPane()
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("Select BitFlags for $valuesListName"))
        // Loops through all values to create their checkboxes
        checkboxes = valuesListValues.mapNotNull { key ->
            // Gets the value as an int, as an int value is required for bitflags
            key.value.toIntSafe()?.let {
                // Create the checkbox
                val checkBox = JCheckBox(key.value + " - " + key.name)
                if (currentValue and it > 0)
                    checkBox.isSelected = true
                panel.add(checkBox)
                // Create a selection object to hold both the checkbox
                // and the value list int value
                BitFlagSelection(it, checkBox)
            }
        }
        // Set the checkbox panel as the main view
        out.setViewportView(panel)
        return out
    }

    /**
     * Called on okay button clicked
     */
    override fun doOKAction() {
        var value = 0
        // Find all checked boxes, and 'OR' their values together
        // to get the full bitflag value
        // as in value = 2 | 4 | 16
        for (selection in checkboxes) {
            if (selection.checkbox.isSelected) {
                value = value or selection.key
            }
        }
        val prefix = if (prependSpace) {
            " "
        } else {
            ""
        }
        // If consume > 0, it means replace the range
        // Used if there is already a bitflag present
        if (consume > 0) {
            val range = TextRange(position, position + consume)
            EditorUtil.replaceText(editor, range, "$prefix$value", true)
        } else {
            EditorUtil.insertText(editor, "$prefix$value", position, true)
        }
        super.doOKAction()
    }
}

private data class BitFlagSelection(val key: Int, val checkbox: JCheckBox)

private data class FixData(
    val range: PsiElement,
    val valuesList: CaosValuesList,
    val currentValue: Int?,
    val prefixWithSpace: Boolean
) {

    val listName: String get() = valuesList.name
    val values: List<CaosValuesListValue> get() = valuesList.values

}

private fun parse(element: PsiElement): FixData? {
    val variant = element.variant.nullIfUnknown()
        ?: return null
    // Get assignment
    var assignment = element.getSelfOrParentOfType(CaosScriptCAssignment::class.java)
    val previousSibling = if (assignment == null || element.tokenType == WHITE_SPACE)
        element.getPreviousNonEmptySibling(false)
    else
        null
    if (assignment == null && previousSibling != null) {
        assignment = previousSibling as? CaosScriptCAssignment
            ?: PsiTreeUtil
                .collectElementsOfType(previousSibling, CaosScriptCAssignment::class.java)
                .firstOrNull()
    }
    val (range, valuesList) = if (assignment != null) {
        rangeFromAssignment(variant, element, assignment, previousSibling)
    } else {
        rangeFromCommand(variant, element, previousSibling)
    } ?: return null

    // Whether to prefix a space before adding bitflags value
    val prefixWithSpace = if (range.tokenType == WHITE_SPACE) {
        true
    } else {
        range !is CaosScriptRvalue
    }

    // Get current value to use in filling in the dialog
    val currentValue: Int? = (range as? CaosScriptRvalue)?.intValue

    return FixData(
        range,
        valuesList,
        currentValue ?: 0,
        prefixWithSpace
    )
}

private fun rangeFromAssignment(
    variant: CaosVariant,
    element: PsiElement,
    assignment: CaosScriptCAssignment,
    previousSibling: PsiElement?
): Pair<PsiElement, CaosValuesList>? {

    // Get the parent argument if any
    val parentArgument = element.getSelfOrParentOfType(CaosScriptArgument::class.java)
    if (parentArgument != null) {
        // If parent argument is RValue, then it is the one that should be replaced
        if (parentArgument is CaosScriptRvalueLike) {
            return getValuesList(variant, assignment)?.let { list -> Pair(parentArgument, list) }
        } else {
            // If this argument is
            val next = parentArgument.next
                ?: return null

            if (next.tokenType == WHITE_SPACE || next is CaosScriptRvalue) {
                return getValuesList(variant, assignment)?.let { list -> Pair(next, list) }

                // If this element is not whitespace or RValue
                // Then this element is not the one we are looking for
                // Return null
            } else if (element.tokenType != WHITE_SPACE) {
                return null
            }
        }
    }

    // Check whether this PSI element is a child of the CAOS assignment
    val isChild = PsiTreeUtil.isAncestor(assignment, element, false)

//    // If this element is not in assignment and is not a whitespace,
//    // then it has nothing to do with our command
//    if (!isChild && element.tokenType != WHITE_SPACE)
//        return null
//    if (element.tokenType == WHITE_SPACE) {
    if (previousSibling == null)
        return null

    // If this element is outside the assignment element, find previous assignment
    // This should be the argument
    val previous = if (!isChild) {
        assignment
            .lastChild
            ?.let {
                if (it.tokenType == WHITE_SPACE) {
                    it.getPreviousNonEmptySibling(false)
                } else {
                    it
                }
            }
            ?: return null
    } else // Previous sibling should be inside assignment and not be whitespace
        previousSibling

    // If previous is not an assignment, then it has to be the command
    // And the command cannot be followed by bitflags in assignment
    if (previous !is CaosScriptArgument) {
        return null
    }

    // IF previous is RValue, that is the one we need to replace
    if (previous is CaosScriptRvalue) {
        return getValuesList(variant, assignment)?.let { list -> Pair(previous, list) }
    }

    // If previous is LValue, then this we should return this whitespace element
    if (previous is CaosScriptLvalue) {
        return getValuesList(variant, assignment)?.let { list -> Pair(element, list) }
    }
//    }
    return null
}


/**
 * Gets the values list and element to replace from a command element parent
 */
private fun rangeFromCommand(
    variant: CaosVariant,
    element: PsiElement,
    previousSibling: PsiElement?
): Pair<PsiElement, CaosValuesList>? {
    // Argument will hold the item possibly to be replaced
    var argument: CaosScriptArgument?

    // Get command element from either previous sibling or element itself
    val commandElement: CaosScriptCommandElement = if (element.tokenType == WHITE_SPACE) {
        // Try and get previous as CAOS command argument
        if (previousSibling is CaosScriptArgument) {
            argument = previousSibling
            // Get argument parent as command element
            argument.parent as CaosScriptCommandElement
        } else {
            // Try to get previous as CAOS command
            val temp = previousSibling as? CaosScriptCommandElement
                ?: PsiTreeUtil.collectElementsOfType(previousSibling, CaosScriptCommandElement::class.java)
                    .firstOrNull()
                ?: return null
            // Get argument as last argument in chain, as this should be
            // followed by our element in a completely flat tree
            argument = temp.arguments.lastOrNull()
            temp
        }
    } else {
        // If element is not whitespace, then it should be an argument or a child of an argument
        element
            // Set command from argument
            .getSelfOrParentOfType(CaosScriptArgument::class.java).let { arg ->
                // If there is no argument
                if (arg != null) {
                    argument = arg
                    arg.parent as? CaosScriptCommandElement
                        ?: return null
                } else {
                    val tempCommand = element.getParentOfType(CaosScriptCommandElement::class.java)
                        ?: return null
                    // Get argument as last argument in chain, as this should be
                    // followed by our element in a completely flat tree
                    argument = tempCommand.arguments.lastOrNull()
                    tempCommand
                }
            }

    }

    // Get command information for parameter values list
    val command = commandElement.commandDefinition
        ?: return null
    // If arg is null, then cursor is right after command name so parameter is 0
    val parameterIndex = argument?.index ?: 0
    val parameter = command.parameters.getOrNull(parameterIndex)
        ?: return null
    val valuesList = parameter.valuesList[variant]
        ?: return null
    return Pair(argument ?: element, valuesList)
}

/**
 * Gets the values list for this assignment element
 */
private fun getValuesList(variant: CaosVariant, assignment: CaosScriptCAssignment): CaosValuesList? {
    if (!assignment.containingFile.isValid)
        return null
    if (DumbService.isDumb(assignment.project))
        return null
    val commandDefinition = assignment.lvalue?.commandDefinition
        ?: return null
    return commandDefinition.returnValuesList[variant]
}