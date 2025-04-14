package com.gloud.document.util;


import com.aspose.words.FontSettings;
import com.gloud.document.enums.ContentType;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;

import java.io.*;
import java.nio.charset.StandardCharsets;

import com.gloud.document.hwpxlib.HwpxFileLoader;
import com.gloud.document.hwpxlib.HwpxPdfRenderer;

import kr.dogfoot.hwp2hwpx.Hwp2Hwpx;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwpxlib.object.HWPXFile;


public class DocumentToPdfConverter {

    public static File convertToPdf(ContentType contentType, InputStream inputStream, String originalFilename) throws Exception {
        return switch (contentType) {
            case TXT, MD, CSV -> convertPlainTextToPdf(inputStream);
            case DOC -> convertDocToPdf(inputStream);
            case DOCX -> convertDocxToPdf(inputStream);
            case PPT -> convertPptToPdf(inputStream);
            case PPTX -> convertPptxToPdf(inputStream);
            case HWP -> convertHwpToPdf(inputStream);
            case HWPX -> convertHwpxToPdf(inputStream);
            case PDF -> copyAsPdf(inputStream);
            default -> throw new UnsupportedOperationException("지원하지 않는 미리보기 변환 형식입니다: " + contentType);
        };
    }

    private static File convertPlainTextToPdf(InputStream is) throws Exception {
        File outputPdf = File.createTempFile("preview-", ".pdf");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             FileOutputStream fos = new FileOutputStream(outputPdf)) {

            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();
            String line;
            BaseFont baseFont = BaseFont.createFont(FontUtil.detectSystemFontPath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font font = new Font(baseFont, 12);
            while ((line = reader.readLine()) != null) {
                Paragraph paragraph = new Paragraph(line, font);
                document.add(paragraph);
            }
            document.close();
        }
        return outputPdf;
    }

    private static File convertDocToPdf(InputStream is) throws Exception {
        File outputPdf = File.createTempFile("preview-", ".pdf");
        FontSettings fontSettings = FontUtil.createAsposeWordFontSettings();
        com.aspose.words.Document doc = new com.aspose.words.Document(is);
        doc.setFontSettings(fontSettings);
        doc.save(outputPdf.getAbsolutePath(), com.aspose.words.SaveFormat.PDF);

        return outputPdf;
    }

    private static File convertDocxToPdf(InputStream is) throws Exception {
        File outputPdf = File.createTempFile("preview-", ".pdf");
        FontSettings fontSettings = FontUtil.createAsposeWordFontSettings();
        com.aspose.words.Document doc = new com.aspose.words.Document(is);
        doc.setFontSettings(fontSettings);
        doc.save(outputPdf.getAbsolutePath(), com.aspose.words.SaveFormat.PDF);

        return outputPdf;
    }

    private static File convertPptToPdf(InputStream is) throws Exception {
        File outputPdf = File.createTempFile("preview-", ".pdf");
        FontUtil.loadAsposeSlidesFonts();
        Presentation pres = new Presentation(is);
        pres.save(outputPdf.getAbsolutePath(), SaveFormat.Pdf);

        return outputPdf;
    }

    private static File convertPptxToPdf(InputStream is) throws Exception {
        File outputPdf = File.createTempFile("preview-", ".pdf");
        FontUtil.loadAsposeSlidesFonts();
        Presentation pres = new Presentation(is);
        pres.save(outputPdf.getAbsolutePath(), SaveFormat.Pdf);

        return outputPdf;
    }

    private static File convertHwpToPdf(InputStream is) throws Exception {
        HWPFile fromFile = HWPReader.fromInputStream(is);
        HWPXFile toFile = Hwp2Hwpx.toHWPX(fromFile);
        return HwpxPdfRenderer.renderToPdf(toFile);
    }

    private static File convertHwpxToPdf(InputStream is) throws Exception {
        HWPXFile hwpxFile = HwpxFileLoader.fromInputStream(is);
        return HwpxPdfRenderer.renderToPdf(hwpxFile);
    }

    private static File copyAsPdf(InputStream is) throws IOException {
        File outputPdf = File.createTempFile("preview-", ".pdf");
        try (OutputStream os = new FileOutputStream(outputPdf)) {
            is.transferTo(os);
        }
        return outputPdf;
    }
}
