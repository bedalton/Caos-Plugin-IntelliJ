package com.badahori.creatures.plugins.intellij.agenteering.sfc

import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.SfcReader
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.vfs.VirtualFile

class SfcBinaryDecompiler : BinaryFileDecompiler {
    override fun decompile(virtual: VirtualFile): CharSequence {
        return SfcReader.readFile(virtual.contentsToByteArray()).toString()
    }

}