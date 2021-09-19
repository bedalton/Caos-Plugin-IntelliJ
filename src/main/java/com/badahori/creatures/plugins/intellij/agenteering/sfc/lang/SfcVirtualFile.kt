package com.badahori.creatures.plugins.intellij.agenteering.sfc.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariant
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcFile
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.SfcReader
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile

/**
 * A virtual file class to handle SFC specific virtual files
 */
@Suppress("unused")
class SfcVirtualFile : CaosVirtualFile, ModificationTracker, HasVariant {
    constructor(name:String, content:ByteArray) : super(name, content)
    constructor(name:String, content:String) : super(name, content)

    init {
        assert (extension?.equalsIgnoreCase("sfc").orFalse()) { "SfcVirtualFile is meant to only contain SFC file data" }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    val dataHolder by lazy {
        SfcReader.readFile(this, cache = true, safe = true)
    }

    val sfcData:SfcFile? = dataHolder.data

    val sfcError:String? = dataHolder.error

    override val variant: CaosVariant?
        get() = sfcData?.variant

    override fun setVariant(variant: CaosVariant?, explicit: Boolean) {
        throw UnsupportedOperationException("Setting a variant is not possible on an SFC file")
    }
}