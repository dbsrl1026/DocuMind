package com.gloud.document.hwpxlib;

import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class HwpxFileLoader {

    public static HWPXFile fromInputStream(InputStream is) throws Exception {
        File tempFile = File.createTempFile("upload-", ".hwpx");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            is.transferTo(fos);
        }
        return HWPXReader.fromFile(tempFile);
    }
}
