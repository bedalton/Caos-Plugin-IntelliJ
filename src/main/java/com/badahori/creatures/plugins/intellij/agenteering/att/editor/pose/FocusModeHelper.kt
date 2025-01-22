package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.FocusMode.*

object FocusModeHelper {

    /**
     * Sets the visibility mask given a focus part
     *
     * @param focusModeInt the index for the focus mode
     * @param part part to focus
     * @return visibility mask for the part and selected focus mode
     */
    @JvmStatic
    fun getVisibilityMask(focusModeInt:Int, part: Char): Map<Char, PoseRenderer.PartVisibility> {
        return getVisibilityMask(FocusMode.fromValue(focusModeInt), part)
    }

    /**
     * Sets the visibility mask given a focus part
     *
     * @param part part to focus
     * @return visibility mask for the part and selected focus mode
     */
    @JvmStatic
    fun getVisibilityMask(focusMode:FocusMode, part: Char): Map<Char, PoseRenderer.PartVisibility> {
        var parts: MutableMap<Char, PoseRenderer.PartVisibility> = mutableMapOf()
        var associatedPartVisibility: PoseRenderer.PartVisibility? = null
        when (focusMode) {
            EVERYTHING -> parts = PoseRenderer.PartVisibility.allVisible
            GHOST -> {
                parts = PoseRenderer.PartVisibility.allGhost
                associatedPartVisibility = PoseRenderer.PartVisibility.GHOST
            }
            GHOST_SOLO -> {
                parts = PoseRenderer.PartVisibility.allHidden
                associatedPartVisibility = PoseRenderer.PartVisibility.GHOST
                parts.put('b', PoseRenderer.PartVisibility.HIDDEN)
            }
            SOLO -> {
                parts = PoseRenderer.PartVisibility.allHidden
                associatedPartVisibility = PoseRenderer.PartVisibility.VISIBLE
            }
            SOLO_WITH_BODY -> {
                parts = PoseRenderer.PartVisibility.allHidden
                associatedPartVisibility = PoseRenderer.PartVisibility.VISIBLE
                parts.put('b', PoseRenderer.PartVisibility.VISIBLE)
            }
            SOLO_GHOST_BODY -> {
                parts = PoseRenderer.PartVisibility.allHidden
                associatedPartVisibility = PoseRenderer.PartVisibility.VISIBLE
                parts.put('b', PoseRenderer.PartVisibility.GHOST)
            }
        }
        if (associatedPartVisibility != null) {
            applyVisibility(parts, PoseCalculator.getAssociatedParts(part), associatedPartVisibility)
        }
        parts[part] = PoseRenderer.PartVisibility.VISIBLE
        return parts
    }

    /**
     * Applies the visibility to the body parts for use in the renderer
     *
     * @param parts      all parts in pose system
     * @param associated which parts to apply visibility to
     * @param visibility what visibility to apply
     */
    private fun applyVisibility(
        parts: MutableMap<Char, PoseRenderer.PartVisibility>,
        associated: List<Char>,
        visibility: PoseRenderer.PartVisibility
    ) {
        for (part in associated) {
            parts[part] = visibility
        }
    }

}

enum class FocusMode(val index:Int, val commonName:String) {
    EVERYTHING(0, "Everything"),
    GHOST(1, "Ghost"),
    GHOST_SOLO(2, "Ghost (Solo)"),
    SOLO(3, "Solo"),
    SOLO_WITH_BODY(4, "Solo (with Body)"),
    SOLO_GHOST_BODY(5, "Solo (Ghost Body)");


    companion object {
        @JvmStatic
        fun fromValue(value: Int): FocusMode {
            return when (value) {
                EVERYTHING.index -> EVERYTHING
                GHOST.index -> GHOST
                GHOST_SOLO.index -> GHOST_SOLO
                SOLO.index -> SOLO
                SOLO_WITH_BODY.index -> SOLO_WITH_BODY
                SOLO_GHOST_BODY.index -> SOLO_GHOST_BODY
                else -> EVERYTHING
            }
        }

        @JvmStatic
        fun getLocalizedOptions(): Array<String> {
            return arrayOf(
                "focus-mode.everything",
                "focus-mode.ghost",
                "focus-mode.ghost-solo",
                "focus-mode.solo",
                "focus-mode.solo.with-body",
                "focus-mode.solo.with-body.ghost",
            )
        }

        @JvmStatic
        fun toStringArray(): Array<String> {
            return values().map { it.commonName }.toTypedArray()
        }
    }
}