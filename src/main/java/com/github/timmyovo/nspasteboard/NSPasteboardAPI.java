package com.github.timmyovo.nspasteboard;

import com.badahori.creatures.plugins.intellij.agenteering.utils.MacOsCopyKt;
import com.intellij.openapi.progress.ProcessCanceledException;

import java.util.concurrent.CancellationException;

public class NSPasteboardAPI {

    static {
        try {
            MacOsCopyKt.ensureMacOsCopyLib();
        } catch (Exception e) {
            if (e instanceof CancellationException) {
                throw (CancellationException) e;
            }
            if (e instanceof ProcessCanceledException) {
                throw (ProcessCanceledException) e;
            }
        }
    }

    public static String getClipboardContent(NSPasteboardType nsPasteboardType) {
        return getClipboardContent(nsPasteboardType.getType());
    }

    public static native void clearContent();

    public static native void writeClipboardFileURL(String file);

    public static native String getClipboardContent(int type);

    public static native void writeClipboardString(String string);

    public static native void writeClipboardFilesURL(String[] files);

    public enum NSPasteboardType {
        FILE_URL(3),
        STRING(8);

        private final int type;

        NSPasteboardType(int i) {
            this.type = i;
        }

        public int getType() {
            return this.type;
        }
    }
}