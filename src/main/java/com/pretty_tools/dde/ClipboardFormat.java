/*
 * Copyright 2013 www.pretty-tools.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pretty_tools.dde;

import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosLibraryLoader;

/**
 * Standard clipboard formats.
 *
 * @author Alexander Kozlov (alex@pretty-tools.com)
 */
public enum ClipboardFormat
{
    CF_TEXT(1),
    CF_BITMAP(2),
    CF_METAFILEPICT(3),
    CF_SYLK(4),
    CF_DIF(5),
    CF_TIFF(6),
    CF_OEMTEXT(7),
    CF_DIB(8),
    CF_PALETTE(9),
    CF_PENDATA(10),
    CF_RIFF(11),
    CF_WAVE(12),
    CF_UNICODETEXT(13),
    CF_ENHMETAFILE(14);

    final int fmt;

    ClipboardFormat(int fmt)
    {
        this.fmt = fmt;
    }

    public int getNativeCode()
    {
        return fmt;
    }

    public static ClipboardFormat valueOf(int fmt)
    {
        for (ClipboardFormat clipboardFormat : ClipboardFormat.values())
        {
            if (fmt == clipboardFormat.getNativeCode())
                return clipboardFormat;
        }
        return null;
    }

    /**
     * Registers a new clipboard format.<br/>
     * If a registered format with the specified name already exists, a new format is not registered and the return value identifies the existing format. Note that the format name comparison is case-insensitive.
     *
     * @param format format to register
     * @return identifier of the registered clipboard format.
     * @throws UnsatisfiedLinkError on error
     * @throws DDEException on error
     */
    public static int registerClipboardFormat(String format) throws UnsatisfiedLinkError, DDEException
    {
        final int newFormat = nativeRegisterClipboardFormat(format);

        if (newFormat == 0)
            throw new DDEException("Format was not registered");

        return newFormat;
    }

    private synchronized native static int nativeRegisterClipboardFormat(String format) throws UnsatisfiedLinkError, DDEException;

    static// Loads the library, if available.
    {
        if ("64".equals(System.getProperty("sun.arch.data.model"))) {
            CaosLibraryLoader.loadLib("dde/JavaDDEx64");
        } else {
            CaosLibraryLoader.loadLib("dde/JavaDDE");
        }
    }
}
