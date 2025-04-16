package com.gloud.document.util;

import com.gloud.document.enums.ContentType;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwplib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.*;

public class TextExtractionUtil {

    public static String extractText(ContentType contentType, InputStream is) throws Exception {
        String raw = switch (contentType) {
            case TXT, MD, CSV -> extractPlainText(is);
            case PDF -> extractFromPdf(is);
            case DOC -> extractFromDoc(is);
            case DOCX -> extractFromDocx(is);
            case PPT -> extractFromPpt(is);
            case PPTX -> extractFromPptx(is);
            case HWP -> extractFromHwp(is);
            case HWPX -> extractFromHwpx(is);
            default -> "(텍스트 미리보기를 지원하지 않는 파일 형식입니다)";
        };
        return normalizeText(raw);
    }

    private static String extractPlainText(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    private static String extractFromPdf(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String extractFromDoc(InputStream is) throws IOException {
        HWPFDocument doc = new HWPFDocument(is);
        return doc.getDocumentText();
    }

    private static String extractFromDocx(InputStream is) throws IOException {
        XWPFDocument docx = new XWPFDocument(is);
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph para : docx.getParagraphs()) {
            sb.append(para.getText()).append("\n");
        }
        return sb.toString();
    }

    private static String extractFromPpt(InputStream is) throws IOException {
        HSLFSlideShow ppt = new HSLFSlideShow(is);
        StringBuilder sb = new StringBuilder();
        for (HSLFSlide slide : ppt.getSlides()) {
            slide.getShapes().forEach(shape -> {
                if (shape instanceof HSLFTextShape textShape) {
                    sb.append(textShape.getText()).append("\n");
                }
            });
        }
        return sb.toString();
    }

    private static String extractFromPptx(InputStream is) throws IOException {
        XMLSlideShow pptx = new XMLSlideShow(is);
        StringBuilder sb = new StringBuilder();
        for (XSLFSlide slide : pptx.getSlides()) {
            slide.getShapes().forEach(shape -> {
                if (shape instanceof XSLFTextShape textShape) {
                    sb.append(textShape.getText()).append("\n");
                }
            });
        }
        return sb.toString();
    }

    private static String extractFromHwp(InputStream is) throws Exception {
        HWPFile hwp = HWPReader.fromInputStream(is);
        return TextExtractor.extract(hwp, TextExtractMethod.InsertControlTextBetweenParagraphText);
    }

    private static String extractFromHwpx(InputStream is) throws Exception {
        File hwpxFile = File.createTempFile("hwpx-preview", ".hwpx");
        try (FileOutputStream fos = new FileOutputStream(hwpxFile)) {
            is.transferTo(fos);
        }
        HWPXFile hwpx = HWPXReader.fromFile(hwpxFile);
        TextMarks textMarks = new TextMarks()
                .paraSeparatorAnd("\n\n")
                .lineBreakAnd("\n")
                .tabAnd("\t")
                .tableStartAnd("[TABLE_START]")
                .tableEndAnd("[TABLE_END]")
                .tableRowSeparatorAnd("\n")
                .tableCellSeparatorAnd("\t")
                .fieldStartAnd("{FIELD:")
                .fieldEndAnd("}")
                .textArtStartAnd("[TEXT_ART]")
                .textArtEndAnd("[/TEXT_ART]");

        String result = kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor.extract(
                hwpx,
                kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod.InsertControlTextBetweenParagraphText,
                true,
                textMarks
        );

        if (hwpxFile.exists()) {
            hwpxFile.delete();
        }

        return result;
    }

    public static String normalizeText(String raw) {
        return raw
                .replaceAll("\\r\\n", "\n")                 // CRLF → LF
                .replaceAll("\\t+", "    ")                 // 탭 → 4공백 (AI가 인식 잘함)
                .replaceAll(" {2,}", " ")                   // 다중 공백 제거
                .replaceAll("\\n{3,}", "\n\n")              // 3줄 이상 → 문단 구분 유지
                .replaceAll("[<>\\[\\]{}]", "")             // 구조에 혼란 주는 특수문자 제거
                .replaceAll("(?m)^\\s*-\\s*", "• ")          // 리스트 기호 통일
                .trim();
    }
}
