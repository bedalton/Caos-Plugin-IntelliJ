package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.impl.PrayAgentBlockImpl
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.impl.PrayPrayTagImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.writeList
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readList
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

interface PrayAgentBlockStub: StubElement<PrayAgentBlockImpl> {
    val blockTag: String?
    val blockName: String?
    val tags: List<PrayTagStruct<*>>
}

class PrayAgentBlockStubImpl(
    parent: StubElement<*>?,
    override val blockTag: String?,
    override val blockName: String?,
    override val tags: List<PrayTagStruct<*>>
): StubBase<PrayAgentBlockImpl>(parent, PrayAgentBlockStubType), PrayAgentBlockStub

interface PrayTagStub: StubElement<PrayPrayTagImpl> {
    val tagName: String
    val tagValueRaw: String
    val intValue: Int?
    val stringValue: String?
    val isString: Boolean
    val isInt: Boolean
}

class PrayTagStubImpl(
    parent: StubElement<*>?,
    override val tagName: String,
    override val tagValueRaw: String,
    override val intValue: Int?,
    override val stringValue: String?
): StubBase<PrayPrayTagImpl>(parent, PrayTagStubType), PrayTagStub {
    override val isString: Boolean get() = stringValue != null
    override val isInt: Boolean get() = intValue != null
}

sealed class PrayTagStruct<T>(
    open val tag: String,
    open val value: T,
    open val indexInFile: Int
) {
    data class StringTag(
        override val tag: String,
        override val value: String,
        override val indexInFile: Int
    ): PrayTagStruct<String>(
        tag, value, indexInFile
    )

    /**
     * A Pray string tag defined using an '@' separator
     * denoting an inlining of the files text
     */
    data class InliningTag(
        override val tag: String,
        override val value: String,
        override val indexInFile: Int
    ): PrayTagStruct<String>(
        tag, value, indexInFile
    )

    data class IntTag(
        override val tag: String,
        override val value: Int,
        override val indexInFile: Int
    ): PrayTagStruct<Int>(
        tag, value, indexInFile
    )

}

data class AgentBlockStruct(
    val blockTag: String?,
    val blockName: String?,
    val tags: List<PrayTagStruct<*>>
)

data class InlineFileStruct(
    val blockTag: String?,
    val blockName: String?,
    val inputFile: String?
)

internal fun StubOutputStream.writeAgentBlock(block: AgentBlockStruct) {
    writeName(block.blockTag)
    writeName(block.blockName)
    writeList(block.tags) {
        writeTag(it)
    }
}

internal fun StubInputStream.readAgentBlock(): AgentBlockStruct {
    val blockTag = readNameAsString()
    val blockName = readNameAsString()
    val tags = readList { readTag() }
    return AgentBlockStruct(
        blockTag = blockTag,
        blockName = blockName,
        tags = tags
    )
}

internal fun StubOutputStream.writeTag(tag: PrayTagStruct<*>) {
    val type = when (tag) {
        is PrayTagStruct.StringTag -> 0
        is PrayTagStruct.IntTag -> 1
        is PrayTagStruct.InliningTag -> 2
    }
    writeInt(type)
    writeName(tag.tag)
    val value = tag.value
    if (value is Int) {
        writeInt(value)
    } else {
        writeName(value.toString())
    }
    writeInt(tag.indexInFile)
}
internal fun StubInputStream.readTag(): PrayTagStruct<*> {
    val type = readInt()
    val tag = readNameAsString()!!
    return when (type) {
        0 -> PrayTagStruct.StringTag(
            tag = tag,
            value = readNameAsString()!!,
            indexInFile = readInt()
        )
        1 -> PrayTagStruct.IntTag(
            tag = tag,
            value = readInt(),
            indexInFile = readInt()
        )
        2 -> PrayTagStruct.InliningTag(
            tag = tag,
            value = readNameAsString()!!,
            indexInFile = readInt()
        )
        else -> throw Exception("Unexpected tag struct type: $type")
    }
}


internal fun StubOutputStream.writeInlineFile(block: InlineFileStruct) {
    writeName(block.blockTag)
    writeName(block.blockName)
    writeName(block.inputFile)
}

internal fun StubInputStream.readInlineFile(): InlineFileStruct {
    val blockTag = readNameAsString()!!
    val blockName = readNameAsString()
    val inputFile = readNameAsString()
    return InlineFileStruct(
        blockTag = blockTag,
        blockName = blockName,
        inputFile = inputFile
    )
}