package com.gloud.document.util;

import com.aspose.words.FontSettings;
import com.aspose.slides.FontsLoader;

import java.io.File;

public class FontUtil {

    public static String detectSystemFontPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            String userFontPath = System.getProperty("user.home") + "/Library/Fonts/NanumGothic-Regular.ttf";
            if (new File(userFontPath).exists()) {
                return userFontPath;
            }
            String assetFontPath = "/System/Library/AssetsV2/com_apple_MobileAsset_Font7/bad9b4bf17cf1669dde54184ba4431c22dcad27b.asset/AssetData/NanumGothic.ttc";
            if (new File(assetFontPath).exists()) {
                return assetFontPath;
            }
            throw new RuntimeException("Nanum Gothic 폰트를 macOS에서 찾을 수 없습니다.");
        } else if (os.contains("linux")) {
            String linuxFontPath = "/usr/share/fonts/truetype/nanum/NanumGothic.ttf";
            if (new File(linuxFontPath).exists()) {
                return linuxFontPath;
            }
            throw new RuntimeException("Linux에서 Nanum Gothic 폰트를 찾을 수 없습니다.");
        } else {
            throw new UnsupportedOperationException("Unsupported OS for font detection: " + os);
        }
    }

    public static FontSettings createAsposeWordFontSettings() {
        FontSettings fontSettings = new FontSettings();
        String fontPath = detectSystemFontPath();
        fontSettings.setFontsFolder(new File(fontPath).getParent(), true);
        return fontSettings;
    }

    public static void loadAsposeSlidesFonts() {
        String fontPath = detectSystemFontPath();
        FontsLoader.loadExternalFonts(new String[] { new File(fontPath).getParent() });
    }
}