package com.badahori.creatures.plugins.intellij.agenteering.caos.lang;

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.lang.CobFileType;
import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16FileType;
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16FileType;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFileType;
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprFileType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class CaosScriptFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(
            @NotNull
                    FileTypeConsumer fileTypeConsumer) {
        fileTypeConsumer.consume(CaosScriptFileType.INSTANCE, CaosScriptFileType.DEFAULT_EXTENSION);
        fileTypeConsumer.consume(CaosDefFileType.INSTANCE, CaosDefFileType.DEFAULT_EXTENSION);
        fileTypeConsumer.consume(SprFileType.getINSTANCE(), SprFileType.getDEFAULT_EXTENSION());
        fileTypeConsumer.consume(C16FileType.getINSTANCE(), C16FileType.getDEFAULT_EXTENSION());
        fileTypeConsumer.consume(S16FileType.getINSTANCE(), S16FileType.getDEFAULT_EXTENSION());
        fileTypeConsumer.consume(CobFileType.getINSTANCE(), CobFileType.getDEFAULT_EXTENSION());
    }
}
