package com.openc2e.plugins.intellij.caos.lang;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFileType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class CaosScriptFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(
            @NotNull
                    FileTypeConsumer fileTypeConsumer) {
        fileTypeConsumer.consume(CaosScriptFileType.INSTANCE, CaosScriptFileType.DEFAULT_EXTENSION);
        fileTypeConsumer.consume(CaosDefFileType.INSTANCE, CaosDefFileType.DEFAULT_EXTENSION);
    }
}
