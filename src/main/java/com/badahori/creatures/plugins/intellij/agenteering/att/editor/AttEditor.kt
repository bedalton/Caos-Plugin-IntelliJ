package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.att.AttJoint
import com.badahori.creatures.plugins.intellij.agenteering.att.AttPart
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.*

private val CaosVariant.attTreeData:Map<Char, AttPart> get() = mapOf(
    'a' to aAtt,
    'b' to bAtt,
    'c' to cAtt,
    'd' to dAtt,
    'e' to eAtt,
    'f' to fAtt,
    'g' to gAtt,
    'h' to hAtt,
    'i' to iAtt,
    'j' to jAtt,
    'k' to kAtt,
    'l' to lAtt
) + (if (isNotOld) {
    mapOf(
        'm' to mAtt,
        'n' to nAtt,
    ) + (if (this == CV) mapOf(
        'o' to oAtt,
        'p' to pAtt,
        'q' to qAtt
    ) else emptyMap())
} else emptyMap())

private val CaosVariant.aAtt: AttPart
    get() {
        return when (this) {
            C1, C2 -> AttPart(
                part = 'a',
                parent = AttJoint('b', 0)
            )

            CV -> AttPart(
                part = 'a',
                parent = AttJoint('b', 0),
                children = listOf(
                    AttJoint('o', 2),
                    AttJoint('p', 3),
                    AttJoint('q', 4)
                )
            )
            else -> AttPart(
                part = 'a',
                parent = AttJoint('b', 0),
                children = listOf(
                    AttJoint('o', 2, unused = true),
                    AttJoint('p', 3, unused = true),
                    AttJoint('q', 4, unused = true)
                )
            )
        }
    }
private val CaosVariant.bAtt: AttPart
    get() {
        return when (this) {
            C1, C2 -> AttPart(
                part = 'b',
                parent = null,
                children = listOf(
                    AttJoint('a', 0),
                    AttJoint('c', 1),
                    AttJoint('f', 2),
                    AttJoint('i', 3),
                    AttJoint('k', 4),
                )
            )
            else -> AttPart(
                part = 'b',
                parent = null,
                children = listOf(
                    AttJoint('a', 0),
                    AttJoint('c', 1),
                    AttJoint('f', 2),
                    AttJoint('i', 3),
                    AttJoint('k', 4),
                    AttJoint('m', 5),
                )
            )
        }
    }

private val cAtt:AttPart = AttPart(
    part = 'c',
    parent = AttJoint('b', 1),
    children = listOf(
        AttJoint('d', 1)
    )
)

private val dAtt:AttPart = AttPart(
    part = 'd',
    parent = AttJoint('c', 1),
    children = listOf(
        AttJoint('e', 1)
    )
)

private val eAtt:AttPart = AttPart(
    part = 'e',
    parent = AttJoint('d', 1)
)

private val fAtt:AttPart = AttPart(
    part = 'f',
    parent = AttJoint('b', 2),
    children = listOf(
        AttJoint('g', 1)
    )
)

private val gAtt:AttPart = AttPart(
    part = 'g',
    parent = AttJoint('f', 1),
    children = listOf(
        AttJoint('h', 1)
    )
)

private val hAtt:AttPart = AttPart(
    part = 'h',
    parent = AttJoint('g', 1)
)

private val iAtt:AttPart = AttPart(
    part = 'i',
    parent = AttJoint('b', 3),
    children = listOf(
        AttJoint('h', 1)
    )
)

private val jAtt:AttPart = AttPart(
    part = 'j',
    parent = AttJoint('i', 1)
)

private val kAtt:AttPart = AttPart(
    part = 'k',
    parent = AttJoint('b', 4),
    children = listOf(
        AttJoint('l', 1)
    )
)

private val lAtt:AttPart = AttPart(
    part = 'l',
    parent = AttJoint('k', 1)
)

private val mAtt:AttPart = AttPart(
    part = 'm',
    parent = AttJoint('b', 5),
    children = listOf(
        AttJoint('n', 1)
    )
)

private val nAtt:AttPart = AttPart(
    part = 'n',
    parent = AttJoint('m', 1)
)

private val oAtt:AttPart = AttPart(
    part = 'o',
    parent = AttJoint('a', 2)
)

private val pAtt:AttPart = AttPart(
    part = 'p',
    parent = AttJoint('a', 3)
)

private val qAtt:AttPart = AttPart(
    part = 'q',
    parent = AttJoint('q', 3)
)