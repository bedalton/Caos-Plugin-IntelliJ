package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFileType
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.lang.CobFileType
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.blk.BlkFileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprFileType
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

@Suppress("DEPRECATION")
class CaosScriptFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(
            fileTypeConsumer: FileTypeConsumer) {
        fileTypeConsumer.consume(CaosScriptFileType.INSTANCE, CaosScriptFileType.DEFAULT_EXTENSION)
        fileTypeConsumer.consume(CaosDefFileType, CaosDefFileType.DEFAULT_EXTENSION)
        fileTypeConsumer.consume(SprFileType, SprFileType.DEFAULT_EXTENSION)
        fileTypeConsumer.consume(C16FileType, C16FileType.DEFAULT_EXTENSION)
        fileTypeConsumer.consume(BlkFileType, BlkFileType.DEFAULT_EXTENSION)
        fileTypeConsumer.consume(BlkFileType, "back")
        fileTypeConsumer.consume(S16FileType, S16FileType.DEFAULT_EXTENSION)
        fileTypeConsumer.consume(CobFileType, CobFileType.DEFAULT_EXTENSION)
        fileTypeConsumer.consume(AttFileType, AttFileType.DEFAULT_EXTENSION)
//        fileTypeConsumer.consume(PrayFileType, PrayFileType.DEFAULT_EXTENSION)
        //fileTypeConsumer.consume(SfcFileType, SfcFileType.DEFAULT_EXTENSION)
    }
}