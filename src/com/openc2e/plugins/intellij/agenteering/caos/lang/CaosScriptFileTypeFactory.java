package com.openc2e.plugins.intellij.agenteering.caos.lang;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.openc2e.plugins.intellij.agenteering.caos.def.lang.CaosDefFileType;
import com.openc2e.plugins.intellij.agenteering.sprites.spr.SprFileType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class CaosScriptFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(
            @NotNull
                    FileTypeConsumer fileTypeConsumer) {
        fileTypeConsumer.consume(CaosScriptFileType.INSTANCE, CaosScriptFileType.DEFAULT_EXTENSION);
        fileTypeConsumer.consume(CaosDefFileType.INSTANCE, CaosDefFileType.DEFAULT_EXTENSION);
        fileTypeConsumer.consume(SprFileType.getINSTANCE(), SprFileType.Companion.getDEFAULT_EXTENSION());
    }
}
