package com.gloud.document.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ContentType {
    PDF("application/pdf"),
    TXT("text/plain"),
    MD("text/markdown"),
    DOC("application/msword"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    HWP("application/vnd.hancom.hwp"),
    HWPX("application/vnd.hancom.hwpx"),
    CSV("text/csv"),
    PPT("application/vnd.ms-powerpoint"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation");


    private final String mimeType;

    ContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public static Optional<ContentType> fromExtension(String ext) {
        try {
            return Optional.of(ContentType.valueOf(ext.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<ContentType> fromMimeType(String mimeType) {
        return Arrays.stream(values())
                .filter(ct -> ct.mimeType.equalsIgnoreCase(mimeType))
                .findFirst();
    }
}