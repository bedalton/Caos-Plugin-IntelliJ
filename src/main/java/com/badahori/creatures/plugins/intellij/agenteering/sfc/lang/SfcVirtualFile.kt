package com.badahori.creatures.plugins.intellij.agenteering.sfc.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariant
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcFile
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.SfcReader
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.externalSystem.service.execution.NotSupportedException
import com.intellij.openapi.util.ModificationTracker

/**
 * A virtual file class to handle SFC specific virtual files
 */
class SfcVirtualFile : CaosVirtualFile, ModificationTracker, HasVariant {
    constructor(name:String, content:ByteArray) : super(name, content)
    constructor(name:String, content:String) : super(name, content)

    init {
        assert (extension?.equalsIgnoreCase("sfc").orFalse()) { "SfcVirtualFile is meant to only contain SFC file data" }
    }

    val dataHolder by lazy {
        SfcReader.readFile(this, true, true)
    }

    val sfcData:SfcFile? = dataHolder.data

    val sfcError:String? = dataHolder.error

    override var variant: CaosVariant?
        get() = sfcData?.variant
        set(value) { throw NotSupportedException("You cannot set Creatures variant on SFC virtual files.") }
}